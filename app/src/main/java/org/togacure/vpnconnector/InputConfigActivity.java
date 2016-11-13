package org.togacure.vpnconnector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.togacure.vpnconnector.db.DBHelper;
import org.togacure.vpnconnector.db.entity.VpnConfig;

/**
 * Created by togacure on 11/13/2016.
 */

public class InputConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_config);

        final Button ok = (Button)findViewById(R.id.buttonSave);
        final Button cancel = (Button)findViewById(R.id.cancelButton);

        InputFilter ipFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    final String destTxt = dest.toString();
                    if (destTxt.matches("^(?!0)(?!.*\\.$)((1?\\d?\\d|25[0-5]|2[0-4]\\d)(\\.|$)){4}$"))
                        return destTxt;
                }
                return null;
            }
        };

        InputFilter domainFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    final String destTxt = dest.toString();
                    if (destTxt.
                            matches("^(([a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]|[a-zA-Z0-9])\\.)*[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}$"))
                        return destTxt;
                }
                return null;
            }
        };

        InputFilter portFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    final String destTxt = dest.toString();
                    if (destTxt.matches("^\\d+$") && destTxt.length() < 5 && Integer.parseInt(destTxt) < 0xffff)
                        return destTxt;
                }
                return null;
            }
        };

        InputFilter[] ipFilters = new InputFilter[1];
        ipFilters[0] = ipFilter;

        InputFilter[] domainFilters = new InputFilter[1];
        domainFilters[0] = domainFilter;

        InputFilter[] portFilters = new InputFilter[1];
        portFilters[0] = portFilter;

        ((EditText)findViewById(R.id.inputIp)).setFilters(ipFilters);
        ((EditText)findViewById(R.id.inputRouteIp)).setFilters(ipFilters);
        ((EditText)findViewById(R.id.inputDns)).setFilters(ipFilters);

        ((EditText)findViewById(R.id.inputPort)).setFilters(portFilters);
        ((EditText)findViewById(R.id.inputRoutePort)).setFilters(portFilters);
        ((EditText)findViewById(R.id.inputMtu)).setFilters(portFilters);

        ((EditText)findViewById(R.id.inputSearchDomain)).setFilters(domainFilters);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final VpnConfig cfg = new VpnConfig();

                final String name = ((EditText)findViewById(R.id.inputName)).getText().toString();

                Log.d("InputActivity.onClick", String.format("name: %s", name));

                if (name == null || name.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "name is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                cfg.setName(name);

                final String address = ((EditText)findViewById(R.id.inputIp)).getText().toString();

                Log.d("InputActivity.onClick", String.format("address: %s", address));

                if (address == null || address.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "IP address is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                String port = ((EditText)findViewById(R.id.inputPort)).getText().toString();

                Log.d("InputActivity.onClick", String.format("port: %s", port));

                cfg.setAddress(address + (port != null && !port.isEmpty() ? ":" + port : ""));

                final String route = ((EditText)findViewById(R.id.inputRouteIp)).getText().toString();

                Log.d("InputActivity.onClick", String.format("route: %s", route));

                if (route == null || route.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "route IP address is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                port = ((EditText)findViewById(R.id.inputPort)).getText().toString();

                Log.d("InputActivity.onClick", String.format("port: %s", port));

                cfg.setRoute(route + (port != null && !port.isEmpty() ? ":" + port : ""));

                final String dns = ((EditText)findViewById(R.id.inputDns)).getText().toString();

                Log.d("InputActivity.onClick", String.format("dns: %s", dns));

                if (dns!= null && !dns.isEmpty()) {
                    cfg.setDns(dns);
                }

                final String searchDomain = ((EditText)findViewById(R.id.inputSearchDomain)).getText().toString();

                Log.d("InputActivity.onClick", String.format("searchDomain: %s", searchDomain));

                if (searchDomain!= null && !searchDomain.isEmpty()) {
                    cfg.setSearchDomain(searchDomain);
                }

                final String mtu = ((EditText)findViewById(R.id.inputMtu)).getText().toString();

                Log.d("InputActivity.onClick", String.format("mtu: %s", mtu));

                if (mtu!= null && !mtu.isEmpty()) {
                    cfg.setMtu(Integer.parseInt(mtu));
                }

                DBHelper.instance().saveOrUpdate(cfg);

                finish();
            }
        });
    }
}
