package com.losek.vfrmobile.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.util.VfrApplication;
import com.losek.vfrmobile.service.DataRegistrationService;
import com.st.BlueSTSDK.Node;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer{

    private VfrApplication vfrApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vfrApp = (VfrApplication) getApplication();


        // BLE not supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && !(Build.MODEL.equals("Android SDK built for x86"))) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        initializeTagNames();
        vfrApp.addObserver(this);

    }

    public void pairCockpitTag(View view) {
        Intent intent = new Intent(this, ScanDevicesActivity.class);
        intent.putExtra("tag","cockpitTag");
        startActivity(intent);
    }

    public void pairHelmetTag(View view) {
        Intent intent = new Intent(this, ScanDevicesActivity.class);
        intent.putExtra("tag","helmetTag");
        startActivity(intent);
    }

    public void startRegistering(View view) {
        Intent serviceIntent = new Intent(this, DataRegistrationService.class);
        startService(serviceIntent);
    }

    public void stopRegistering(View view) {
        Intent serviceIntent = new Intent(this, DataRegistrationService.class);
        stopService(serviceIntent);
    }

    public void showLiveData(View view) {
        if(vfrApp.getCockpitTag() == null) {
            return;
        }
        Intent intent = new Intent(this, LiveDataActivity.class);
        startActivity(intent);
    }

    public void exportData(View view) {
        Intent intent = new Intent(this, RecordingsHistoryActivity.class);
        startActivity(intent);
    }

    public void initializeTagNames() {
        final TextView cockpitTagTextView = (TextView) findViewById(R.id.activity_main_cockpit_tag_value);
        final TextView helmetTagTextView = (TextView) findViewById(R.id.activity_main_helmet_tag_value);

        final Node cockpitNode = vfrApp.getCockpitTag();
        final Node helmetNode = vfrApp.getHelmetTag();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(cockpitNode != null) {
                    cockpitTagTextView.setText(cockpitNode.getFriendlyName());
                } else {
                    cockpitTagTextView.setText("Not paired yet!");
                }

                if(helmetNode!= null) {
                    helmetTagTextView.setText(helmetNode.getFriendlyName());
                } else {
                    helmetTagTextView.setText("Not paired yet!");
                }
            }
        });

    }


    @Override
    public void update(Observable observable, Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initializeTagNames();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e("vfr","MainActivity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
