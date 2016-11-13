package org.togacure.vpnconnector;

import android.content.Intent;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.togacure.vpnconnector.db.DBHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by togacure on 11/12/2016.
 */

public class VPNConnectorActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private ProgressBar progress;
    private AtomicReference<String> selectedName = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DBHelper.init(getApplicationContext());

        setContentView(R.layout.activity_vpnconnector);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);

        final ListView configList = (ListView)findViewById(R.id.vpnConfigsList);
        configList.setAdapter(adapter);
        configList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        configList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedName.set(adapter.getItem(position));
            }
        });

        progress = (ProgressBar)findViewById(R.id.progressBar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.flt_btn_plus);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VPNConnectorActivity.this, InputConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_vpnconnector, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewConfigs();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id != R.id.action_vpn_start) {
            return super.onOptionsItemSelected(item);
        }

        final String cfgName = selectedName.get();

        if (cfgName == null || cfgName.isEmpty()) {
            Toast.makeText(getApplicationContext(), "please select config", Toast.LENGTH_SHORT).show();
            return true;
        }

        DBHelper.instance().stateSessionName(cfgName);

        if (DBHelper.instance().stateRunning()) {
            Toast.makeText(getApplicationContext(), "always running", Toast.LENGTH_SHORT).show();
            return true;
        }

        Intent intent = VpnService.prepare(this);
        if (intent != null)
            startActivityForResult(intent, 1);
        else
            onActivityResult(1, RESULT_OK, null);

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DBHelper.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1 || resultCode != RESULT_OK) {
            Toast.makeText(getApplicationContext(), "start vpn denied", Toast.LENGTH_SHORT).show();
            return;
        }
        startService(new Intent(this, ConfigurableVpnService.class));
    }

    private void viewConfigs() {
        new LoadConfigsTask().execute();
    }

    private class LoadConfigsTask extends AsyncTask<Object, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<String> doInBackground(Object... params) {
            return DBHelper.instance().getAllVpnConfigNames();
        }

        @Override
        protected void onPostExecute(final List<String> configs) {
            adapter.clear();
            adapter.addAll(configs);
            adapter.notifyDataSetChanged();
            progress.setVisibility(View.GONE);
        }
    }
}
