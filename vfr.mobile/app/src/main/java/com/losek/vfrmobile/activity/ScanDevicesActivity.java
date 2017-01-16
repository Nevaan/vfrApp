package com.losek.vfrmobile.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.activity.adapter.DevicesListAdapter;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureBattery;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureMagnetometer;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.NodeScanActivity;

public class ScanDevicesActivity extends NodeScanActivity implements AbsListView.OnItemClickListener {

    private static final String LOG = "VfrScanDevActivity";
    Button startScan;

    private int timeoutMs = 10000;
    private ArrayAdapter<Node> devicesListAdapter;
    private String currentTag;
    private VfrApplication appVariables;

    private Manager.ManagerListener mUpdateDiscoverGui = new Manager.ManagerListener() {
        @Override
        public void onDiscoveryChange(Manager m, boolean enabled) {
            if (!enabled)
                ScanDevicesActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopNodeDiscovery();
                        startScan.setText(R.string.start_button);
                    }
                });
        }

        @Override
        public void onNodeDiscovered(Manager m, final Node node) {
            ScanDevicesActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    devicesListAdapter.add(node);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_devices);
        startScan = (Button) findViewById(R.id.lookForDevices);

        AbsListView devicesListView = (AbsListView) findViewById(R.id.weSuNodesList);
        devicesListAdapter = new DevicesListAdapter(this, R.layout.device_list_element);
        devicesListView.setAdapter(devicesListAdapter);
        devicesListView.setOnItemClickListener(this);
        mManager.resetDiscovery();
        devicesListAdapter.addAll(mManager.getNodes());
        appVariables = (VfrApplication) getApplication();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            currentTag = bundle.getString("tag");
        }

        mManager.addListener(mUpdateDiscoverGui);
    }

    public void discoverDevices(View view) {
        if (Manager.getSharedInstance().isDiscovering()) {
            super.stopNodeDiscovery();
            startScan.setText(R.string.start_button);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    devicesListAdapter.clear();
                    mManager.resetDiscovery();
                    startScan.setText(R.string.stop_scan);
                }
            });
            super.startNodeDiscovery(timeoutMs);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {


        final Node selectedNode = devicesListAdapter.getItem(position);

        if(VfrApplication.isNodePaired(selectedNode)) {
            return;
        }

        Thread getListItemThread = new Thread(new Runnable() {
            @Override
            public void run() {
                selectedNode.connect(getApplicationContext());
                selectedNode.addNodeStateListener(stateListener);

                switch (currentTag) {
                    case "helmetTag":
                        appVariables.setHelmetTag(selectedNode);
                        VfrApplication.setHelmetTagFriendlyName(selectedNode.getFriendlyName());
                        break;
                    case "cockpitTag":
                        appVariables.setCockpitTag(selectedNode);
                        VfrApplication.setCockpitTagFriendlyName(selectedNode.getFriendlyName());
                }
                mManager.stopDiscovery();
                ScanDevicesActivity.this.finish();
            }
        });
        getListItemThread.start();
    }


    private Node.NodeStateListener stateListener = new Node.NodeStateListener() {
        @Override
        public void onStateChange(final Node node, Node.State newState, Node.State prevState) {

            if (newState.equals(Node.State.Connected)) {
                switch (appVariables.getPairedAttributeName(node)) {
                    case "helmet":
                        Log.e(LOG,"setting helmetTag");
                        appVariables.setHelmetTag(node);
                        break;
                    case "cockpit":
                        Log.e(LOG,"settingCockpitTag");
                        appVariables.setCockpitTag(node);
                };
            }

            if(prevState.equals(Node.State.Connecting) && newState.equals(Node.State.Connected)) {
                node.enableNotification(node.getFeature(FeatureAcceleration.class));
                node.enableNotification(node.getFeature(FeatureGyroscope.class));
                node.enableNotification(node.getFeature(FeatureMagnetometer.class));
                node.enableNotification(node.getFeature(FeatureBattery.class));
            }

            if (newState.equals(Node.State.Dead)) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("isReconnecting",true);
                startActivity(intent);

                if (appVariables.isNodePaired(node)) {
                    if (node.equals(appVariables.getHelmetTag())) {
                        Log.d(LOG, "Unpaired helmet because of node lost!");
                        appVariables.setHelmetTag(null);
                    }
                    if (node.equals(appVariables.getCockpitTag())) {
                        Log.d(LOG, "Unpaired cockpit because of node lost!");
                        appVariables.setCockpitTag(null);
                    }

                }
            }

            if ((prevState.equals(Node.State.Connected) || prevState.equals(Node.State.Connecting)) && newState.equals(Node.State.Dead)) {
                node.connect(getApplicationContext());
            }


            if (newState.equals(Node.State.Unreachable)) {
                if (appVariables.isNodePaired(node)) {
                    if (node.equals(appVariables.getHelmetTag())) {
                        Log.d(LOG, "Unpaired helmet because of node unreachable!");
                        appVariables.setHelmetTag(null);
                        VfrApplication.setHelmetTagFriendlyName("");
                    }
                    if (node.equals(appVariables.getCockpitTag())) {
                        Log.d(LOG, "Unpaired cockpit because of node unreachable!");
                        appVariables.setCockpitTag(null);
                        VfrApplication.setCockpitTagFriendlyName("");
                    }

                }
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("unreachableNode",VfrApplication.getPairedAttributeName(node));
                startActivity(intent);
            }

            if(prevState.equals(Node.State.Connecting) && (!newState.equals(Node.State.Connected) && !newState.equals(Node.State.Connecting)&& !newState.equals(Node.State.Unreachable))) {
                node.disconnect();
            }

        }
    };

}
