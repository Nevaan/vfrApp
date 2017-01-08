package com.losek.vfrmobile.activity;

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


    //TODO: co jak node zniknie?
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


/*
        if (selectedNode.isConnected()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ScanDevicesActivity.this);
            dialogBuilder.setMessage(R.string.paired_device_dialog_prompt).setCancelable(false)
                    .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            selectedNode.disconnect();
                            //appVariables.unpairNode(selectedNode);
                            return;
                        }
                    })
                    .setNegativeButton(R.string.no_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

            AlertDialog alert = dialogBuilder.create();
            alert.setTitle(getString(R.string.unpair_device_dialog_title));
            alert.show();

            return;
        }*/
        final Node selectedNode = devicesListAdapter.getItem(position);
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


                if (prevState.equals(Node.State.Dead) && newState.equals(Node.State.Connecting)) {
                    Log.d(LOG, "DEAD -> CONNECTING: attempt to connect after dying!");
                }

                if (newState.equals(Node.State.Connected)) {
                    Log.e(LOG, prevState.toString() + " -> CONNECTED : connected after dying!");
                    switch (appVariables.getPairedAttributeName(node)) {
                        case "helmet":
                            Log.e(LOG,"setting helmetTag");
                            appVariables.setHelmetTag(node);
                            break;
                        case "cockpit":
                            Log.e(LOG,"settingCockpitTag");
                            appVariables.setCockpitTag(node);
                            break;
                        default:
                            Log.e(LOG,"o o! tu jest nasz problem");
                    };
                }

                if(prevState.equals(Node.State.Connecting) && newState.equals(Node.State.Connected)) {
                    Log.e(LOG, "Notify enable before again connect: " + node.isEnableNotification(node.getFeature(FeatureAcceleration.class)));

                    node.enableNotification(node.getFeature(FeatureAcceleration.class));
                    node.enableNotification(node.getFeature(FeatureGyroscope.class));
                    node.enableNotification(node.getFeature(FeatureMagnetometer.class));
                    node.enableNotification(node.getFeature(FeatureBattery.class));

                    Log.e(LOG, prevState.toString() + " -> CONNECTED : nice one!");
                }

                if (newState.equals(Node.State.Dead)) {
                    Log.e(LOG, prevState.toString() + " -> DEAD : attempt to disconnect");
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
                    Log.e(LOG, "Node lost completely");
                }

            if ((prevState.equals(Node.State.Connected) || prevState.equals(Node.State.Connecting)) && newState.equals(Node.State.Dead)) {
                Log.e(LOG, prevState.toString() + " -> DEAD: Node Lost!!!, calling .connect");
                node.connect(getApplicationContext());
                if (!node.isConnected()) {

                } else {
                    Log.e(LOG, "Node connected again");
                }
            }

            /*
            if (newState.equals(Node.State.Idle)) {
                Log.e(LOG, prevState.toString() + " -> IDLE : attempt to disconnect");
            }
            if (newState.equals(Node.State.Lost)) {
                Log.e(LOG, prevState.toString() + " -> LOST : attempt to disconnect");
            }
            if (newState.equals(Node.State.Unreachable)) {
                Log.e(LOG, prevState.toString() + " -> UNREACHABLE : attempt to disconnect");
            }
            if (newState.equals(Node.State.Init)) {
                Log.e(LOG, prevState.toString() + " -> Init : attempt to disconnect");
            }*/
        }
    };

}
