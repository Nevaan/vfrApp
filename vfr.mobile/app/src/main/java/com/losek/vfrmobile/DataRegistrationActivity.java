package com.losek.vfrmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Node;

import java.util.ArrayList;
import java.util.List;

public class DataRegistrationActivity extends AppCompatActivity {

    private VfrApplication vfrApp;
    private Feature gyroscopeFeature;
    private Feature accelerometerFeature;

    private Feature.FeatureListener gyroListener;
    private Feature.FeatureListener accListener;


    private List<String> devicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_registration);
        vfrApp = (VfrApplication) getApplication();

        devicesList.add("WeSU 1");
        devicesList.add("WeSu 2");
        Spinner s = (Spinner) findViewById(R.id.live_data_select_device);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, devicesList);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        s.setAdapter(adapter);


        initializeNames();

//        gyroscopeFeature = vfrApp.getCockpitTag().getFeature(FeatureGyroscope.class);
//        accelerometerFeature = vfrApp.getCockpitTag().getFeature(FeatureAcceleration.class);

        //gyroListener = new GenericFragmentUpdate((TextView) findViewById(R.id.gyroscope_value));
        //accListener = new GenericFragmentUpdate((TextView) findViewById(R.id.acc_value));

//        gyroscopeFeature.addFeatureListener(gyroListener);
  //      accelerometerFeature.addFeatureListener(accListener);

    //    vfrApp.getCockpitTag().enableNotification(gyroscopeFeature);
      //  vfrApp.getCockpitTag().enableNotification(accelerometerFeature);

    }


    private void initializeNames(){
        //TextView cockpitTagTextView = (TextView) findViewById(R.id.cockpitTagNameLabel);

        Node cockpitNode = vfrApp.getCockpitTag();

//        if(cockpitNode != null) {
  //          cockpitTagTextView.setText(cockpitNode.getFriendlyName());
    //    } else {
      //      cockpitTagTextView.setText("Cockpit device is not chosen!");
        //}
    }

    private class GenericFragmentUpdate implements Feature.FeatureListener {

        final private TextView mTextView;

        public GenericFragmentUpdate(TextView text) {
            mTextView = text;
        }

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {
           /* String x = f.getSample().data[0].toString();
            String y = f.getSample().data[1].toString();
            String z = f.getSample().data[2].toString();
            final String featureDump = "X: " + x + " Y: " + y + " Z: " + z;*/
            final String featureDump = f.getSample().toString();
            DataRegistrationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(featureDump);
                }
            });
        }
    }

}
