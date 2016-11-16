package com.losek.vfrmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.NodeScanActivity;

public class ScanDevices extends NodeScanActivity implements AbsListView.OnItemClickListener{

    private int timeoutMs = 10000;
    private ArrayAdapter<Node> devicesListAdapter;
    private String currentTag;

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
        public void onNodeDiscovered(Manager m, final Node node) {
            ScanDevices.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    devicesListAdapter.add(node);
                }
            });
        }//onNodeDiscovered
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_devices);

        AbsListView devicesListView = (AbsListView) findViewById(R.id.weSuNodesList);
        devicesListAdapter = new DevicesListAdapter(this, R.layout.bt_devices_list);
        devicesListView.setAdapter(devicesListAdapter);
        devicesListView.setOnItemClickListener(this);
        devicesListAdapter.addAll(mManager.getNodes());

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            currentTag = bundle.getString("tag");
        }

        mManager.addListener(mUpdateDiscoverGui);
    }

/*    @Override
    protected void onStart() {
        super.onStart();
    }*/

    public void discoverDevices(View view) {
        super.startNodeDiscovery(timeoutMs);
        invalidateOptionsMenu();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
        Thread getListItemThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Node selectedNode = devicesListAdapter.getItem(position);
                System.out.println("Clicked Item: " + selectedNode.getTag());
                Intent goBack = new Intent(ScanDevices.this, MainActivity.class);
                VfrApplication appVariables = (VfrApplication) getApplication();
                switch(currentTag) {
                    case "helmetTag":
                        appVariables.setHelmetTag(selectedNode);
                        break;
                    case "cockpitTag":
                        appVariables.setCockpitTag(selectedNode);
                        break;
                    default:
                        System.out.println("Error! I dont know what tag to pair");
                }
                startActivity(goBack);
            }
        });
        getListItemThread.start();


    }
}
