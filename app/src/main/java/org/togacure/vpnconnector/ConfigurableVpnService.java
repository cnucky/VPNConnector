package org.togacure.vpnconnector;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.togacure.vpnconnector.db.DBHelper;
import org.togacure.vpnconnector.db.entity.VpnConfig;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.hexene.localvpn.ByteBufferPool;
import xyz.hexene.localvpn.Packet;
import xyz.hexene.localvpn.TCPInput;
import xyz.hexene.localvpn.TCPOutput;
import xyz.hexene.localvpn.UDPInput;
import xyz.hexene.localvpn.UDPOutput;

/**
 * Created by togacure on 11/13/2016.
 */

public class ConfigurableVpnService extends VpnService {

    private ParcelFileDescriptor vpnInterface = null;

    private PendingIntent pendingIntent;

    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;

    private Selector udpSelector;
    private Selector tcpSelector;

    @Override
    public void onCreate() {
        super.onCreate();
        setupVPN();
        try {
            udpSelector = Selector.open();
            tcpSelector = Selector.open();
            deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
            deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
            networkToDeviceQueue = new ConcurrentLinkedQueue<>();

            executorService = Executors.newFixedThreadPool(5);
            executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
            executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, udpSelector, this));
            executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector));
            executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));
            executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(),
                    deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));

            DBHelper.instance().stateRunning(true);
        } catch (IOException e) {
            cleanup();
        }
    }

    private void setupVPN() {
        final String session = DBHelper.instance().stateSessionName();
        if (session == null)
            return;
        final VpnConfig cfg = DBHelper.instance().getVpnConfig(session);
        if (vpnInterface != null || cfg == null)
            return;
        Log.i("Service.setupVPN", String.format("session: %s address: %s route: %s dns: %s domain: %s mtu: %s",
                session, cfg.getAddress(), cfg.getRoute(), cfg.getDns(), cfg.getSearchDomain(), cfg.getMtu()));
        if (cfg.getAddress() == null || cfg.getAddress().isEmpty() || cfg.getRoute() == null || cfg.getRoute().isEmpty())
            return;
        Builder builder = new Builder();
        builder.setSession(session);
        builder.addAddress(cfg.getAddress(), 32);
        builder.addRoute(cfg.getRoute(), 0);
        if (cfg.getDns() != null)
            builder.addDnsServer(cfg.getDns());
        if (cfg.getSearchDomain() != null)
            builder.addSearchDomain(cfg.getSearchDomain());
        if (cfg.getMtu() != null)
            builder.setMtu(cfg.getMtu());
        vpnInterface = builder.setSession(getString(R.string.app_name)).setConfigureIntent(pendingIntent).establish();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        cleanup();
    }

    private void cleanup() {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        ByteBufferPool.clear();
        closeResources(udpSelector, tcpSelector, vpnInterface);
        DBHelper.instance().stateRunning(false);
    }

    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (IOException e) {

            }
        }
    }

    private static class VPNRunnable implements Runnable {
        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
        private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
        private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

        public VPNRunnable(FileDescriptor vpnFileDescriptor,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                           ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        @Override
        public void run() {
            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();

            try {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived;
                while (!Thread.interrupted()) {
                    if (dataSent)
                        bufferToNetwork = ByteBufferPool.acquire();
                    else
                        bufferToNetwork.clear();

                    int readBytes = vpnInput.read(bufferToNetwork);
                    if (readBytes > 0) {
                        dataSent = true;
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);
                        if (packet.isUDP()) {
                            deviceToNetworkUDPQueue.offer(packet);
                        }
                        else if (packet.isTCP()) {
                            deviceToNetworkTCPQueue.offer(packet);
                        }
                        else {
                            Log.w("VPNRunnable.run", String.format("Unknown packet type: %s", packet.ip4Header.toString()));
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }

                    ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                    if (bufferFromNetwork != null) {
                        bufferFromNetwork.flip();
                        while (bufferFromNetwork.hasRemaining())
                            vpnOutput.write(bufferFromNetwork);
                        dataReceived = true;

                        ByteBufferPool.release(bufferFromNetwork);
                    } else {
                        dataReceived = false;
                    }

                    if (!dataSent && !dataReceived)
                        Thread.sleep(10);
                }
            }
            catch (InterruptedException e) {
                Log.i("VPNRunnable.run", "Stopping");
            }
            catch (IOException e) {
                Log.w("VPNRunnable.run", e.toString(), e);
            }
            finally {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }

}
