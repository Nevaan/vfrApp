package com.losek.vfrmobile;

import android.os.Bundle;
import com.st.BlueSTSDK.Utils.NodeScanActivity;

public class ScanDevices extends NodeScanActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_devices);
    }
}
