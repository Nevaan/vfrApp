package com.losek.vfrmobile;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.st.BlueSTSDK.Node;

import org.w3c.dom.Text;

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
        Intent intent = new Intent(this, ScanDevices.class);
        intent.putExtra("tag","cockpitTag");
        startActivity(intent);
    }

    public void pairHelmetTag(View view) {
        Intent intent = new Intent(this, ScanDevices.class);
        intent.putExtra("tag","helmetTag");
        startActivity(intent);
    }

    public void startRegistering(View view) {

    }

    public void stopRegistering(View view) {

    }

    public void showLiveData(View view) {
        Intent intent = new Intent(this, DataRegistrationActivity.class);
        startActivity(intent);
    }

    public void exportData(View view) {

    }

    public void initializeTagNames() {
        TextView cockpitTagTextView = (TextView) findViewById(R.id.activity_main_cockpit_tag_value);
        TextView helmetTagTextView = (TextView) findViewById(R.id.activity_main_helmet_tag_value);

        Node cockpitNode = vfrApp.getCockpitTag();
        Node helmetNode = vfrApp.getHelmetTag();

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
        super.onDestroy();
        Log.e("vfr", "destroyed");
        vfrApp.deleteObserver(this);
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.e("vfr", "stopped");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.e("vfr", "paused");
    }

}
