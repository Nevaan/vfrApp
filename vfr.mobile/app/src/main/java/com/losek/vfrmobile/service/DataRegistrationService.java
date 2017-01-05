package com.losek.vfrmobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.losek.vfrmobile.util.VfrApplication;
import com.losek.vfrmobile.database.DatabaseRepository;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureMagnetometer;

/**
 * Created by pawel on 05.01.2017.
 */

public class DataRegistrationService extends Service {

    private DatabaseRepository dbRepository;

    private Feature cockpitGyroscopeFeature;
    private Feature cockpitAccelerometerFeature;
    private Feature cockpitMagnetometerFeature;

    private Feature.FeatureListener gyroXListener;
    private Feature.FeatureListener accXListener;
    private Feature.FeatureListener magXListener;

    private Feature.FeatureListener gyroYListener;
    private Feature.FeatureListener accYListener;
    private Feature.FeatureListener magYListener;

    private Feature.FeatureListener gyroZListener;
    private Feature.FeatureListener accZListener;
    private Feature.FeatureListener magZListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("DataRegistrationService", "Service created");
        dbRepository = new DatabaseRepository(getApplicationContext());

        cockpitGyroscopeFeature = VfrApplication.getCockpitTag().getFeature(FeatureGyroscope.class);
        cockpitAccelerometerFeature = VfrApplication.getCockpitTag().getFeature(FeatureAcceleration.class);
        cockpitMagnetometerFeature = VfrApplication.getCockpitTag().getFeature(FeatureMagnetometer.class);
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        Log.d("DataRegistrationService", "Command started");

        gyroXListener = new VariableUpdateListener();
        accXListener = new VariableUpdateListener();
        magXListener = new VariableUpdateListener();


        gyroYListener = new VariableUpdateListener();
        accYListener = new VariableUpdateListener();
        magYListener = new VariableUpdateListener();

        gyroZListener = new VariableUpdateListener();
        accZListener = new VariableUpdateListener();
        magZListener = new VariableUpdateListener();

        cockpitGyroscopeFeature.addFeatureListener(gyroXListener);
        cockpitGyroscopeFeature.addFeatureListener(gyroYListener);
        cockpitGyroscopeFeature.addFeatureListener(gyroZListener);

        cockpitAccelerometerFeature.addFeatureListener(accXListener);
        cockpitAccelerometerFeature.addFeatureListener(accYListener);
        cockpitAccelerometerFeature.addFeatureListener(accZListener);

        cockpitMagnetometerFeature.addFeatureListener(magXListener);
        cockpitMagnetometerFeature.addFeatureListener(magYListener);
        cockpitMagnetometerFeature.addFeatureListener(magZListener);

        VfrApplication.getCockpitTag().enableNotification(cockpitMagnetometerFeature);
        VfrApplication.getCockpitTag().enableNotification(cockpitAccelerometerFeature);
        VfrApplication.getCockpitTag().enableNotification(cockpitGyroscopeFeature);

        dbRepository.createRecording();

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d("DataRegistrationService", "Service destroyed");
        cockpitGyroscopeFeature.removeFeatureListener(gyroXListener);
        cockpitGyroscopeFeature.removeFeatureListener(gyroYListener);
        cockpitGyroscopeFeature.removeFeatureListener(gyroZListener);

        cockpitAccelerometerFeature.removeFeatureListener(accXListener);
        cockpitAccelerometerFeature.removeFeatureListener(accYListener);
        cockpitAccelerometerFeature.removeFeatureListener(accZListener);

        cockpitMagnetometerFeature.removeFeatureListener(magXListener);
        cockpitMagnetometerFeature.removeFeatureListener(magYListener);
        cockpitMagnetometerFeature.removeFeatureListener(magZListener);

        dbRepository.getRecordingList();
        dbRepository.closeDbConnection();
    }

    private class VariableUpdateListener implements Feature.FeatureListener {
        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {
           // Log.d("RegisterService",sample.toString());
        }
    }

}
