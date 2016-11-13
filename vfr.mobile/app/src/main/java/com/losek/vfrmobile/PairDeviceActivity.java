package com.losek.vfrmobile;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PairDeviceActivity extends ListActivity {

    public static int REQUEST_BLUETOOTH = 1;
    private BluetoothAdapter mBluetoothAdapter;

    private List foundDevices = new ArrayList();
    private Set foundDevicesSet = new HashSet();
    ArrayAdapter<BluetoothDevice> btDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pair_device);
        ListView listView = getListView();
        btDevicesAdapter= new DevicesListAdapter(this, R.layout.bt_devices_list, foundDevices);

        listView.setAdapter(btDevicesAdapter);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Phone does not support Bluetooth so let the user know and exit.
        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(PairDeviceActivity.this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_BLUETOOTH);
            }
        }

    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here
            System.out.println("--== DEVICE FOUND ==--");


            if(!foundDevices.contains(device)) {
                runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        foundDevices.add(device);
                        btDevicesAdapter.notifyDataSetChanged();
                    }
                });
            }

        }
    };

    public void listDevices(View view) {
        System.out.println("--== Started scanning for BTLE devices");


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Button searchDevicesButton = (Button) findViewById(R.id.search_devices_button);
                mBluetoothAdapter.startLeScan(leScanCallback);
                try {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            searchDevicesButton.setEnabled(false);
                        }
                    });
                    Thread.sleep(10000);
                } catch (Exception e) {

                }
                runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        searchDevicesButton.setEnabled(true);
                    }
                });
                System.out.println("--== Stopped scanning for BTLE devices");
                mBluetoothAdapter.stopLeScan(leScanCallback);
            }
        });
        thread.start();


    }

}
