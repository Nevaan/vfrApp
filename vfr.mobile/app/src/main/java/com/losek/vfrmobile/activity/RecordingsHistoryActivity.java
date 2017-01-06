package com.losek.vfrmobile.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.activity.adapter.RecordingListAdapter;
import com.losek.vfrmobile.database.DatabaseRepository;
import com.losek.vfrmobile.database.model.Recording;

import java.util.List;

public class RecordingsHistoryActivity extends AppCompatActivity {

    private static final String LOG = "VfrRecordingsActivity";

    private DatabaseRepository dbRepository;

    private ListView recordingListView;
    private ArrayAdapter<Recording> recordingArrayAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings_history);

        dbRepository = new DatabaseRepository(this);

        recordingListView = (ListView) findViewById(R.id.recording_list);
        recordingListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        List<Recording> recordings = dbRepository.getRecordingList();
        recordingArrayAdapter = new RecordingListAdapter(this, R.layout.recording_history_list_element);
        recordingArrayAdapter.addAll(recordings);

        recordingListView.setAdapter(recordingArrayAdapter);

    }
}
