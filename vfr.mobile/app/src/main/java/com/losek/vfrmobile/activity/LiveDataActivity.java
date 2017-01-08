package com.losek.vfrmobile.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureBattery;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureMagnetometer;

import java.util.ArrayList;
import java.util.List;

public class LiveDataActivity extends AppCompatActivity {

    public enum Characteristic {
        X, Y, Z, BATTERY
    }

    private VfrApplication vfrApp;
    private Feature gyroscopeFeature;
    private Feature accelerometerFeature;
    private Feature magnetometerFeature;
    private Feature batteryFeature;

    private Feature.FeatureListener gyroXListener;
    private Feature.FeatureListener accXListener;
    private Feature.FeatureListener magXListener;

    private Feature.FeatureListener gyroYListener;
    private Feature.FeatureListener accYListener;
    private Feature.FeatureListener magYListener;

    private Feature.FeatureListener gyroZListener;
    private Feature.FeatureListener accZListener;
    private Feature.FeatureListener magZListener;

    private Feature.FeatureListener batteryListener;

    private List<String> devicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_data);
        vfrApp = (VfrApplication) getApplication();

        devicesList.add("WeSU 1");
        devicesList.add("WeSu 2");
        Spinner s = (Spinner) findViewById(R.id.live_data_select_device);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, devicesList);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        if(vfrApp.getCockpitTag() != null) {
            gyroscopeFeature = vfrApp.getCockpitTag().getFeature(FeatureGyroscope.class);
            accelerometerFeature = vfrApp.getCockpitTag().getFeature(FeatureAcceleration.class);
            magnetometerFeature = vfrApp.getCockpitTag().getFeature(FeatureMagnetometer.class);
            batteryFeature = vfrApp.getCockpitTag().getFeature(FeatureBattery.class);

            gyroXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_gyro_x_val));
            accXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_acc_x_val));
            magXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_magneto_x_val));


            gyroYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_gyro_y_val));
            accYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_acc_y_val));
            magYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_magneto_y_val));

            gyroZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_gyro_z_val));
            accZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_acc_z_val));
            magZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_magneto_z_val));

            batteryListener = new GenericFeatureListener(Characteristic.BATTERY, (TextView) findViewById(R.id.live_data_battery_val));

            gyroscopeFeature.addFeatureListener(gyroXListener);
            gyroscopeFeature.addFeatureListener(gyroYListener);
            gyroscopeFeature.addFeatureListener(gyroZListener);

            accelerometerFeature.addFeatureListener(accXListener);
            accelerometerFeature.addFeatureListener(accYListener);
            accelerometerFeature.addFeatureListener(accZListener);

            magnetometerFeature.addFeatureListener(magXListener);
            magnetometerFeature.addFeatureListener(magYListener);
            magnetometerFeature.addFeatureListener(magZListener);

            batteryFeature.addFeatureListener(batteryListener);
        }

        if(vfrApp.getHelmetTag() != null) {
//TODO: Helmet tag
        }
/*        vfrApp.getCockpitTag().enableNotification(gyroscopeFeature);
        vfrApp.getCockpitTag().enableNotification(accelerometerFeature);
        vfrApp.getCockpitTag().enableNotification(batteryFeature);*/
    }

    private class GenericFeatureListener implements Feature.FeatureListener {

        final private TextView mTextView;
        final private Characteristic characteristic;

        public GenericFeatureListener(Characteristic c, TextView text) {
            characteristic = c;
            mTextView = text;
        }

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {
            final String value;
            switch (characteristic) {
                case X:
                    value = sample.data[0].toString();
                    break;
                case Y:
                    value = sample.data[1].toString();
                    break;
                case Z:
                    value = sample.data[2].toString();
                    break;
                case BATTERY:
                    value = sample.data[0].toString() + "%";
                    break;
                default:
                    value="";
            }
            LiveDataActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(value);
                }
            });
        }
    }

}
