package com.losek.vfrmobile;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PairDeviceActivity extends ListActivity {

    public static int REQUEST_BLUETOOTH = 1;
    private BluetoothAdapter mBluetoothAdapter;

    private List foundDevices = new ArrayList();
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
                            System.exit(0);
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

        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);

    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
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
                foundDevices.clear();
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

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    showToast("Unpaired");
                }

            }
        }
    };

    private void showToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();
    }


}
