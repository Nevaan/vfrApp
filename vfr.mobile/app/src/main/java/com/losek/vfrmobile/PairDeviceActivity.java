package com.losek.vfrmobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class PairDeviceActivity extends AppCompatActivity /*implements DeviceListFragment.OnFragmentInteractionListener*/{

    public static int REQUEST_BLUETOOTH = 1;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

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
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }

    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here
            System.out.println("--== DEVICE FOUND ==--");
            System.out.println(device.getName());
            System.out.println(device.getType());
        }
    };

    public void listDevices(View view) {
        System.out.println("--== Started scanning for BTLE devices");
        mBluetoothAdapter.startLeScan(leScanCallback);
        try {
            Thread.sleep(10000);
        } catch (Exception e) {

        }
        
        System.out.println("--== Stopped scanning for BTLE devices");
        mBluetoothAdapter.stopLeScan(leScanCallback);
    }


}
