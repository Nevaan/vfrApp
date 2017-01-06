package com.losek.vfrmobile.activity.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.database.model.Recording;

/**
 * Created by pawel on 06.01.2017.
 */

public class RecordingListAdapter extends ArrayAdapter<Recording> {

    public static final String LOG = "VfrRecordListAdapter";

    public RecordingListAdapter(Context ctx, int textViewResourceId) {
        super(ctx, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View resultView = convertView;

        if (resultView == null) {
            LayoutInflater layoutInflater;
            layoutInflater = LayoutInflater.from(getContext());
            resultView = layoutInflater.inflate(R.layout.recording_history_list_element, null);
        }

        Recording recording = getItem(position);

        if (recording != null) {

            TextView recordingElementTextView = (TextView) resultView.findViewById(R.id.recording_history_element_text);

            if (recordingElementTextView != null) {
                recordingElementTextView.setText(recording.getStartTime());
            }

        }

        resultView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(LOG,"cliecked as hell");
                return true;
            }
        });

        return resultView;
    }

}
