package com.losek.vfrmobile;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by pawel on 13.11.2016.
 */

public class DevicesListAdapter extends ArrayAdapter<BluetoothDevice> {

    public DevicesListAdapter(Context ctx, int textViewResourceId) {
        super(ctx,textViewResourceId);
    }

    public DevicesListAdapter(Context ctx, int resource, List<BluetoothDevice> items) {
        super(ctx,resource,items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.bt_devices_list, null);
        }

        BluetoothDevice p = getItem(position);

        if (p != null) {

            TextView tt1 = (TextView) v.findViewById(R.id.btDevicesRow);

            if (tt1 != null) {
                tt1.setText(p.getAddress());
            }
        }

        return v;
    }
}
