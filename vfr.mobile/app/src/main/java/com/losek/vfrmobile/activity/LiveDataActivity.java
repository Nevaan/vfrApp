package com.losek.vfrmobile.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureBattery;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureMagnetometer;

import java.util.ArrayList;
import java.util.List;

public class LiveDataActivity extends AppCompatActivity{

    public enum Characteristic {
        X, Y, Z, BATTERY
    }

    private VfrApplication vfrApp;
    private Feature cockpitGyroscopeFeature;
    private Feature cockpitAccelerometerFeature;
    private Feature cockpitMagnetometerFeature;
    private Feature cockpitBatteryFeature;

    private Feature.FeatureListener cockpitGyroXListener;
    private Feature.FeatureListener cockpitAccXListener;
    private Feature.FeatureListener cockpitMagXListener;

    private Feature.FeatureListener cockpitGyroYListener;
    private Feature.FeatureListener cockpitAccYListener;
    private Feature.FeatureListener cockpitMagYListener;

    private Feature.FeatureListener cockpitGyroZListener;
    private Feature.FeatureListener cockpitAccZListener;
    private Feature.FeatureListener cockpitMagZListener;

    private Feature.FeatureListener cockpitBatteryListener;

    private Feature helmetGyroscopeFeature;
    private Feature helmetAccelerometerFeature;
    private Feature helmetMagnetometerFeature;
    private Feature helmetBatteryFeature;

    private Feature.FeatureListener helmetGyroXListener;
    private Feature.FeatureListener helmetAccXListener;
    private Feature.FeatureListener helmetMagXListener;

    private Feature.FeatureListener helmetGyroYListener;
    private Feature.FeatureListener helmetAccYListener;
    private Feature.FeatureListener helmetMagYListener;

    private Feature.FeatureListener helmetGyroZListener;
    private Feature.FeatureListener helmetAccZListener;
    private Feature.FeatureListener helmetMagZListener;

    private Feature.FeatureListener helmetBatteryListener;

    private List<String> devicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_data);
        vfrApp = (VfrApplication) getApplication();

        devicesList.add("Cockpit tag");
        devicesList.add("Helmet tag");

        Spinner s = (Spinner) findViewById(R.id.live_data_select_device);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, devicesList);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        if(vfrApp.getCockpitTag() != null) {
            cockpitGyroscopeFeature = vfrApp.getCockpitTag().getFeature(FeatureGyroscope.class);
            cockpitAccelerometerFeature = vfrApp.getCockpitTag().getFeature(FeatureAcceleration.class);
            cockpitMagnetometerFeature = vfrApp.getCockpitTag().getFeature(FeatureMagnetometer.class);
            cockpitBatteryFeature = vfrApp.getCockpitTag().getFeature(FeatureBattery.class);

            cockpitGyroXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_gyro_x_val));
            cockpitAccXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_acc_x_val));
            cockpitMagXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_magneto_x_val));


            cockpitGyroYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_gyro_y_val));
            cockpitAccYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_acc_y_val));
            cockpitMagYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_magneto_y_val));

            cockpitGyroZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_gyro_z_val));
            cockpitAccZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_acc_z_val));
            cockpitMagZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_magneto_z_val));

            cockpitBatteryListener = new GenericFeatureListener(Characteristic.BATTERY, (TextView) findViewById(R.id.live_data_battery_val));
        }

        if(vfrApp.getHelmetTag() != null) {
            helmetGyroscopeFeature = vfrApp.getHelmetTag().getFeature(FeatureGyroscope.class);
            helmetAccelerometerFeature = vfrApp.getHelmetTag().getFeature(FeatureAcceleration.class);
            helmetMagnetometerFeature = vfrApp.getHelmetTag().getFeature(FeatureMagnetometer.class);
            helmetBatteryFeature = vfrApp.getHelmetTag().getFeature(FeatureBattery.class);

            helmetGyroXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_gyro_x_val));
            helmetAccXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_acc_x_val));
            helmetMagXListener = new GenericFeatureListener(Characteristic.X, (TextView) findViewById(R.id.live_data_magneto_x_val));


            helmetGyroYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_gyro_y_val));
            helmetAccYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_acc_y_val));
            helmetMagYListener = new GenericFeatureListener(Characteristic.Y, (TextView) findViewById(R.id.live_data_magneto_y_val));

            helmetGyroZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_gyro_z_val));
            helmetAccZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_acc_z_val));
            helmetMagZListener = new GenericFeatureListener(Characteristic.Z, (TextView) findViewById(R.id.live_data_magneto_z_val));

            helmetBatteryListener = new GenericFeatureListener(Characteristic.BATTERY, (TextView) findViewById(R.id.live_data_battery_val));
        }

        final Spinner spinner = (Spinner) findViewById(R.id.live_data_select_device);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 && vfrApp.getCockpitTag() != null) {

                    if(helmetGyroXListener != null) helmetGyroscopeFeature.removeFeatureListener(helmetGyroXListener);
                    if(helmetGyroYListener != null)helmetGyroscopeFeature.removeFeatureListener(helmetGyroYListener);
                    if(helmetGyroZListener != null)helmetGyroscopeFeature.removeFeatureListener(helmetGyroZListener);

                    if(helmetAccXListener != null) helmetAccelerometerFeature.removeFeatureListener(helmetAccXListener);
                    if(helmetAccYListener != null)helmetAccelerometerFeature.removeFeatureListener(helmetAccYListener);
                    if(helmetAccZListener != null)helmetAccelerometerFeature.removeFeatureListener(helmetAccZListener);

                    if(helmetMagXListener!=null) helmetMagnetometerFeature.removeFeatureListener(helmetMagXListener);
                    if(helmetMagYListener!=null)helmetMagnetometerFeature.removeFeatureListener(helmetMagYListener);
                    if(helmetMagZListener!=null)helmetMagnetometerFeature.removeFeatureListener(helmetMagZListener);

                    if(helmetBatteryListener != null) helmetBatteryFeature.removeFeatureListener(helmetBatteryListener);

                    cockpitGyroscopeFeature.addFeatureListener(cockpitGyroXListener);
                    cockpitGyroscopeFeature.addFeatureListener(cockpitGyroYListener);
                    cockpitGyroscopeFeature.addFeatureListener(cockpitGyroZListener);

                    cockpitAccelerometerFeature.addFeatureListener(cockpitAccXListener);
                    cockpitAccelerometerFeature.addFeatureListener(cockpitAccYListener);
                    cockpitAccelerometerFeature.addFeatureListener(cockpitAccZListener);

                    cockpitMagnetometerFeature.addFeatureListener(cockpitMagXListener);
                    cockpitMagnetometerFeature.addFeatureListener(cockpitMagYListener);
                    cockpitMagnetometerFeature.addFeatureListener(cockpitMagZListener);

                    cockpitBatteryFeature.addFeatureListener(cockpitBatteryListener);
                }

                if(position == 0 && vfrApp.getCockpitTag() == null) {
                    spinner.setSelection(1);
                    Toast.makeText(getApplicationContext(), "Cockpit tag is not paired",
                            Toast.LENGTH_LONG).show();
                }

                if(position == 1 && vfrApp.getHelmetTag() != null) {
//TODO: Helmet tag
                    if(cockpitGyroXListener != null) cockpitGyroscopeFeature.removeFeatureListener(cockpitGyroXListener);
                    if(cockpitGyroYListener != null)cockpitGyroscopeFeature.removeFeatureListener(cockpitGyroYListener);
                    if(cockpitGyroZListener != null)cockpitGyroscopeFeature.removeFeatureListener(cockpitGyroZListener);

                    if(cockpitAccXListener!=null) cockpitAccelerometerFeature.removeFeatureListener(cockpitAccXListener);
                    if(cockpitAccYListener!=null)cockpitAccelerometerFeature.removeFeatureListener(cockpitAccYListener);
                    if(cockpitAccZListener!=null)cockpitAccelerometerFeature.removeFeatureListener(cockpitAccZListener);

                    if(cockpitMagXListener!=null)cockpitMagnetometerFeature.removeFeatureListener(cockpitMagXListener);
                    if(cockpitMagYListener!=null)cockpitMagnetometerFeature.removeFeatureListener(cockpitMagYListener);
                    if(cockpitMagZListener!=null)cockpitMagnetometerFeature.removeFeatureListener(cockpitMagZListener);

                    if(cockpitBatteryListener!=null)cockpitBatteryFeature.removeFeatureListener(cockpitBatteryListener);

                    helmetGyroscopeFeature.addFeatureListener(helmetGyroXListener);
                    helmetGyroscopeFeature.addFeatureListener(helmetGyroYListener);
                    helmetGyroscopeFeature.addFeatureListener(helmetGyroZListener);

                    helmetAccelerometerFeature.addFeatureListener(helmetAccXListener);
                    helmetAccelerometerFeature.addFeatureListener(helmetAccYListener);
                    helmetAccelerometerFeature.addFeatureListener(helmetAccZListener);

                    helmetMagnetometerFeature.addFeatureListener(helmetMagXListener);
                    helmetMagnetometerFeature.addFeatureListener(helmetMagYListener);
                    helmetMagnetometerFeature.addFeatureListener(helmetMagZListener);

                    helmetBatteryFeature.addFeatureListener(helmetBatteryListener);
                }

                if(position == 1 && vfrApp.getHelmetTag() == null) {
                    spinner.setSelection(0);
                    Toast.makeText(getApplicationContext(), "Helmet tag is not paired",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
