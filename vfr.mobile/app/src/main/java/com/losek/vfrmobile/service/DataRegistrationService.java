package com.losek.vfrmobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureMagnetometer;
import com.st.BlueSTSDK.Log.FeatureLogCSVFile;
import com.st.BlueSTSDK.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pawel on 05.01.2017.
 */

public class DataRegistrationService extends Service {

    private final static String LOG = "VfrDataRegisterService";

    private static FeatureLogCSVFile csvLogger;

    private Feature cockpitGyroscopeFeature;
    private Feature cockpitAccelerometerFeature;
    private Feature cockpitMagnetometerFeature;

    private Feature helmetGyroscopeFeature;
    private Feature helmetAccelerometerFeature;
    private Feature helmetMagnetometerFeature;

    private Node cockpitTag;
    private Node helmetTag;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(LOG, "Service created");

        String documentsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath();

        cockpitTag = VfrApplication.getCockpitTag();
        helmetTag = VfrApplication.getHelmetTag();

        List<Node> nodes = new ArrayList<>();
        if(cockpitTag != null) {
            nodes.add(cockpitTag);
        }

        if(helmetTag != null) {
            nodes.add(helmetTag);
        }

        csvLogger = new FeatureLogCSVFile(documentsDirectoryPath, nodes);
        csvLogger.addNodeMapping(cockpitTag.getFriendlyName(), "cockpit");
        csvLogger.addNodeMapping(helmetTag.getFriendlyName(), "helmet");

        if(cockpitTag!=null) {
            cockpitGyroscopeFeature = cockpitTag.getFeature(FeatureGyroscope.class);
            cockpitAccelerometerFeature = cockpitTag.getFeature(FeatureAcceleration.class);
            cockpitMagnetometerFeature = cockpitTag.getFeature(FeatureMagnetometer.class);
        }

        if(helmetTag != null) {
            helmetGyroscopeFeature = helmetTag.getFeature(FeatureGyroscope.class);
            helmetAccelerometerFeature = helmetTag.getFeature(FeatureAcceleration.class);
            helmetMagnetometerFeature = helmetTag.getFeature(FeatureMagnetometer.class);
        }
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        Log.d(LOG, "Service started");

        if(cockpitTag != null) {
            cockpitAccelerometerFeature.addFeatureLoggerListener(csvLogger);
            cockpitGyroscopeFeature.addFeatureLoggerListener(csvLogger);
            cockpitMagnetometerFeature.addFeatureLoggerListener(csvLogger);
            Log.d(LOG, "Added listener for cockpit tag features");
        }

        if(helmetTag != null) {
            helmetGyroscopeFeature.addFeatureLoggerListener(csvLogger);
            helmetAccelerometerFeature.addFeatureLoggerListener(csvLogger);
            helmetMagnetometerFeature.addFeatureLoggerListener(csvLogger);
            Log.d(LOG, "Added listener for helmet tag features");

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){

        if(cockpitTag != null) {
            cockpitAccelerometerFeature.removeFeatureLoggerListener(csvLogger);
            cockpitGyroscopeFeature.removeFeatureLoggerListener(csvLogger);
            cockpitMagnetometerFeature.removeFeatureLoggerListener(csvLogger);

            Log.d(LOG, "Removed listener for cockpit tag features");

        }

        if(helmetTag != null) {
            helmetGyroscopeFeature.removeFeatureLoggerListener(csvLogger);
            helmetAccelerometerFeature.removeFeatureLoggerListener(csvLogger);
            helmetMagnetometerFeature.removeFeatureLoggerListener(csvLogger);
            Log.d(LOG, "Removed listener for helmet tag features");

        }

        Log.d(LOG, "Service destroyed");

    }
}
