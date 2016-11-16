package com.losek.vfrmobile;

import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.NodeScanActivity;

public class ScanDevices extends NodeScanActivity {

    private int timeoutMs = 10000;
    private ArrayAdapter devicesListAdapter;

    private Manager.ManagerListener mUpdateDiscoverGui = new Manager.ManagerListener() {

        /**
         * call the stopNodeDiscovery for update the gui state
         * @param m manager that start/stop the process
         * @param enabled true if a new discovery start, false otherwise
         */
        @Override
        public void onDiscoveryChange(Manager m, boolean enabled) {
            if (!enabled)
                ScanDevices.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopNodeDiscovery();
                    }//run
                });
        }//onDiscoveryChange

        @Override
        public void onNodeDiscovered(Manager m, Node node) {
            devicesListAdapter.add(node);
        }//onNodeDiscovered
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_devices);
        AbsListView devicesListView = (AbsListView) findViewById(R.id.weSuNodesList);
        devicesListAdapter = new DevicesListAdapter(this, R.layout.bt_devices_list);
        devicesListView.setAdapter(devicesListAdapter);
        devicesListAdapter.addAll(mManager.getNodes());
        mManager.addListener(mUpdateDiscoverGui);
    }

    @Override
    protected void onStart() {
        super.onStart();
        discoverDevices();
    }

    private void discoverDevices() {
        super.startNodeDiscovery(timeoutMs);
        invalidateOptionsMenu();
    }

}
