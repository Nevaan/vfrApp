package com.losek.vfrmobile.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.losek.vfrmobile.activity.adapter.DevicesListAdapter;
import com.losek.vfrmobile.R;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.NodeScanActivity;

public class ScanDevicesActivity extends NodeScanActivity implements AbsListView.OnItemClickListener {

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
        devicesListAdapter = new DevicesListAdapter(this, R.layout.bt_devices_list);
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
            super.startNodeDiscovery(timeoutMs);
            startScan.setText(R.string.stop_scan);
            invalidateOptionsMenu();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {

        final Node selectedNode = devicesListAdapter.getItem(position);

        if (selectedNode.isConnected()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ScanDevicesActivity.this);
            dialogBuilder.setMessage(R.string.paired_device_dialog_prompt).setCancelable(false)
                    .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            selectedNode.disconnect();
                            appVariables.unpairNode(selectedNode);
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
        }
        Thread getListItemThread = new Thread(new Runnable() {
            @Override
            public void run() {
                switch (currentTag) {
                    case "helmetTag":
                        appVariables.setHelmetTag(selectedNode);
                        break;
                    case "cockpitTag":
                        appVariables.setCockpitTag(selectedNode);
                        break;
                    default:
                        System.out.println("Error! I dont know what tag to pair");
                }
                selectedNode.connect(getApplicationContext());
                selectedNode.addNodeStateListener(stateListener);
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
                    Log.d("VfrApp", "DEAD -> CONNECTING: attempt to connect after dying!");
                }

                if (newState.equals(Node.State.Connected)) {
                    Log.e("VfrApp", prevState.toString() + " -> CONNECTED : connected after dying!");
                    appVariables.setCockpitTag(node);
                }

                if(prevState.equals(Node.State.Connecting) && newState.equals(Node.State.Connected)) {
                    Log.e("VfrApp", prevState.toString() + " -> CONNECTED : nice one!");
                }

                if (newState.equals(Node.State.Dead)) {
                    Log.e("VfrApp", prevState.toString() + " -> DEAD : attempt to disconnect");
                    //node.disconnect();
                    if (appVariables.isNodePaired(node)) {
                        if (node.equals(appVariables.getHelmetTag())) {
                            Log.d("VfrApp", "Unpaired helmet because of node lost!");
                            appVariables.setHelmetTag(null);
                        }
                        if (node.equals(appVariables.getCockpitTag())) {
                            Log.d("VfrApp", "Unpaired cockpit because of node lost!");
                            appVariables.setCockpitTag(null);
                        }

                    }
                    Log.e("VfrApp", "Node lost completely");
                }

            if ((prevState.equals(Node.State.Connected) || prevState.equals(Node.State.Connecting)) && newState.equals(Node.State.Dead)) {
                Log.e("VfrApp", prevState.toString() + " -> DEAD: Node Lost!!!");
                node.connect(getApplicationContext());
                if (!node.isConnected()) {

                } else {
                    Log.e("VfrApp", "Node connected again");
                }
            }
            if (newState.equals(Node.State.Disconnecting)) {
                Log.e("VfrApp", prevState.toString() + " -> DISCONNECTING : attempt to disconnect");
            }
            if (newState.equals(Node.State.Idle)) {
                Log.e("VfrApp", prevState.toString() + " -> IDLE : attempt to disconnect");
            }
            if (newState.equals(Node.State.Lost)) {
                Log.e("VfrApp", prevState.toString() + " -> LOST : attempt to disconnect");
            }
            if (newState.equals(Node.State.Unreachable)) {
                Log.e("VfrApp", prevState.toString() + " -> UNREACHABLE : attempt to disconnect");
            }

/*
            if ((newState == Node.State.Unreachable ||
                    newState == Node.State.Dead ||
                    newState == Node.State.Lost)) {
                switch (newState) {
                    case Dead:
                        Log.d("VfrApp", "Node state dead, attempting to reconnect (" + node.getFriendlyName() + ")");
                        node.connect(getApplicationContext());
                        break;
                    case Unreachable:
                        Log.d("VfrApp", "Node state unreachable (" + node.getFriendlyName() + ")");
                        break;
                    case Lost:
                        Log.d("VfrApp", "Node state lost (" + node.getFriendlyName() + ")");
                }

                Log.d("VfrApp","IsConnected? " + node.isConnected());

            }

            if(newState == Node.State.Connecting) {

            }*/

/*            if(newState != Node.State.Connected && newState != Node.State.Connecting) {
                if (node.isConnected()) {
                    Log.d("VfrApp", "Connected successfully!");
                } else {
                    Log.e("VfrApp", "Node Lost!!!");
                    if (appVariables.isNodePaired(node)) {
                        if (node.equals(appVariables.getHelmetTag())) {
                            Log.d("VfrApp", "Unpaired helmet because of node lost!");
                            appVariables.setHelmetTag(null);
                        }
                        if (node.equals(appVariables.getCockpitTag())) {
                            Log.d("VfrApp", "Unpaired cockpit because of node lost!");
                            appVariables.setCockpitTag(null);
                        }
                    }
                }
            }*/
        }
    };

}
