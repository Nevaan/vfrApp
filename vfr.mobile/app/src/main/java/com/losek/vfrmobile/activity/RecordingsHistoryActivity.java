package com.losek.vfrmobile.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.losek.vfrmobile.R;

public class RecordingsHistoryActivity extends AppCompatActivity {

    private static final String LOG = "VfrRecordingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings_history);
    }
}
