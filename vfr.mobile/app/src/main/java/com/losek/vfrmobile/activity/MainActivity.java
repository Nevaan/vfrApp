package com.losek.vfrmobile.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    protected void onNewIntent(Intent intent) {
        if (intent != null)
            setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle bundle = getIntent().getExtras();

        if(bundle != null) {
            String unreachableNodeName = bundle.getString("unreachableNode");

            if (unreachableNodeName != null) {
                Log.e(LOG, "Stopped registering because of node unreachable");
                Intent serviceIntent = new Intent(getApplicationContext(), DataRegistrationService.class);
                stopService(serviceIntent);
                cleanupAfterRegister();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setMessage(R.string.node_unreachable_dialog).setCancelable(false)
                        .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                return;
                            }
                        });

                AlertDialog alert = dialogBuilder.create();
                alert.setTitle(getString(R.string.node_unreachable_title));
                alert.show();
            }

            Boolean nodeReconnect = bundle.getBoolean("isReconnecting");

            if(nodeReconnect) {
                Log.d(LOG, "Trying to reconnect node");
                Toast.makeText(this, R.string.reconnect_attempt, Toast.LENGTH_LONG).show();
            }
            getIntent().removeExtra("isReconnecting");
            getIntent().removeExtra("unreachableNode");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkIfRegisterServiceIsRunning()){
            Intent serviceIntent = new Intent(this, DataRegistrationService.class);
            stopService(serviceIntent);
        }

    }

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
        updateTagNames();
        vfrApp.addObserver(this);

    }

    public void pairTagButtonHandler(View view) {
        String tag = "";

        switch(view.getId()) {
            case R.id.activity_main_cockpit_pair_button:
                tag = "cockpitTag";
                break;
            case R.id.activity_main_helmet_pair_button:
                tag = "helmetTag";
        }

        if (((Button) view).getText().equals(getResources().getString(R.string.pair_button))) {
            Intent intent = new Intent(this, ScanDevicesActivity.class);
            intent.putExtra("tag", tag);
            startActivity(intent);
        } else {
            if(tag.equals("cockpitTag")) {
                VfrApplication.getCockpitTag().disconnect();
                vfrApp.setCockpitTag(null);
                VfrApplication.setCockpitTagFriendlyName("");
            }
            if(tag.equals("helmetTag")) {
                VfrApplication.getHelmetTag().disconnect();
                vfrApp.setHelmetTag(null);
                VfrApplication.setHelmetTagFriendlyName("");
            }
        }
    }

    public void startRegistering(View view) {

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Application do not have permission to save recordings, please grant to use this functionality.",
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
            Intent serviceIntent = new Intent(this, DataRegistrationService.class);
            startService(serviceIntent);
            getCallingActivity();
            startRegistering.setEnabled(false);
            stopRegistering.setEnabled(true);
            pairCockpitTag.setEnabled(false);
            pairHelmetTag.setEnabled(false);
        }

    }

    public void stopRegistering(View view) {
        Intent serviceIntent = new Intent(this, DataRegistrationService.class);
        stopService(serviceIntent);
        cleanupAfterRegister();
    }

    private void cleanupAfterRegister() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File documentsDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        File[] documentDirectoryContent = documentsDirectory.listFiles();
        for (File file : documentDirectoryContent) {
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }

        startRegistering.setEnabled(true);
        stopRegistering.setEnabled(false);
        pairCockpitTag.setEnabled(true);
        pairHelmetTag.setEnabled(true);
    }

    public void showLiveData(View view) {
        if (vfrApp.getCockpitTag() == null && vfrApp.getHelmetTag() == null) {
            return;
        }
        Intent intent = new Intent(this, LiveDataActivity.class);
        startActivity(intent);
    }

    public void updateTagNames() {
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
                    if (!checkIfRegisterServiceIsRunning()) startRegistering.setEnabled(true);
                    currentReads.setEnabled(true);
                } else {
                    cockpitTagTextView.setText("Not paired yet!");
                    cockpitPairButton.setText(R.string.pair_button);
                }

                if (helmetNode != null) {
                    helmetTagTextView.setText(helmetNode.getFriendlyName());
                    helmetPairButton.setText(R.string.unpair_button);
                    if(!checkIfRegisterServiceIsRunning()) startRegistering.setEnabled(true);
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

    private boolean checkIfRegisterServiceIsRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DataRegistrationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    @Override
    public void update(Observable observable, Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTagNames();
            }
        });
    }

}
