package com.losek.vfrmobile.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.service.DataRegistrationService;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Node;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {

    private static final String LOG = "VfrMainActivity";
    private VfrApplication vfrApp;
    private static final int REQUEST_PERMISSIONS = 20;

    Button startRegistering;
    Button stopRegistering;
    Button liveDataButton;
    Button pairCockpitTag;
    Button pairHelmetTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vfrApp = (VfrApplication) getApplication();

        startRegistering = (Button) findViewById(R.id.start_button);
        stopRegistering = (Button) findViewById(R.id.stop_button);
        liveDataButton = (Button) findViewById(R.id.current_reads_button);
        pairCockpitTag = (Button) findViewById(R.id.activity_main_cockpit_pair_button);
        pairHelmetTag = (Button) findViewById(R.id.activity_main_helmet_pair_button);


        startRegistering.setEnabled(false);
        stopRegistering.setEnabled(false);
        liveDataButton.setEnabled(false);

        // BLE not supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && !(Build.MODEL.equals("Android SDK built for x86"))) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        initializeTagNames();
        vfrApp.addObserver(this);

    }

    public void pairCockpitTag(View view) {
        if (pairCockpitTag.getText().equals(getResources().getString(R.string.pair_button))) {
            Intent intent = new Intent(this, ScanDevicesActivity.class);
            intent.putExtra("tag", "cockpitTag");
            startActivity(intent);
        } else {
            VfrApplication.getCockpitTag().disconnect();
            vfrApp.setCockpitTag(null);
        }
    }

    public void pairHelmetTag(View view) {
        if (pairHelmetTag.getText().equals(getResources().getString(R.string.pair_button))) {
            Intent intent = new Intent(this, ScanDevicesActivity.class);
            intent.putExtra("tag", "helmetTag");
            startActivity(intent);
        } else {
            VfrApplication.getHelmetTag().disconnect();
            vfrApp.setHelmetTag(null);
        }
    }

    public void startRegistering(View view) {

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Please Grant Permissions",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSIONS);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            }
        } else {
            //Call whatever you want
        }

        Intent serviceIntent = new Intent(this, DataRegistrationService.class);
        startService(serviceIntent);

        startRegistering.setEnabled(false);
        stopRegistering.setEnabled(true);
    }

    public void stopRegistering(View view) {
        Intent serviceIntent = new Intent(this, DataRegistrationService.class);
        stopService(serviceIntent);

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        File[] files = f.listFiles();
        for (File filen : files) {
            Uri contentUri = Uri.fromFile(filen);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }

        startRegistering.setEnabled(true);
        stopRegistering.setEnabled(false);
    }

    public void showLiveData(View view) {
        if (vfrApp.getCockpitTag() == null && vfrApp.getHelmetTag() == null) {
            return;
        }
        Intent intent = new Intent(this, LiveDataActivity.class);
        startActivity(intent);
    }

    public void initializeTagNames() {
        final TextView cockpitTagTextView = (TextView) findViewById(R.id.activity_main_cockpit_tag_value);
        final TextView helmetTagTextView = (TextView) findViewById(R.id.activity_main_helmet_tag_value);
        final Button cockpitPairButton = (Button) findViewById(R.id.activity_main_cockpit_pair_button);
        final Button helmetPairButton = (Button) findViewById(R.id.activity_main_helmet_pair_button);
        final Button currentReads = (Button) findViewById(R.id.current_reads_button);

        final Node cockpitNode = vfrApp.getCockpitTag();
        final Node helmetNode = vfrApp.getHelmetTag();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cockpitNode != null) {
                    cockpitTagTextView.setText(cockpitNode.getFriendlyName());
                    cockpitPairButton.setText(R.string.unpair_button);
                    startRegistering.setEnabled(true);
                    currentReads.setEnabled(true);
                } else {
                    cockpitTagTextView.setText("Not paired yet!");
                    cockpitPairButton.setText(R.string.pair_button);
                }

                if (helmetNode != null) {
                    helmetTagTextView.setText(helmetNode.getFriendlyName());
                    helmetPairButton.setText(R.string.unpair_button);
                    startRegistering.setEnabled(true);
                    currentReads.setEnabled(true);
                } else {
                    helmetTagTextView.setText("Not paired yet!");
                    helmetPairButton.setText(R.string.pair_button);
                }

                if(cockpitNode == null && helmetNode == null) {
                    currentReads.setEnabled(false);
                    startRegistering.setEnabled(false);
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
        super.onDestroy();

    }
}
