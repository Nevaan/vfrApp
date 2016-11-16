package com.losek.vfrmobile;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

            Button pair = (Button) v.findViewById(R.id.pairDeviceButton);
            pair.setOnClickListener(new RowClickEventListener(position));
        }

        BluetoothDevice p = getItem(position);

        if (p != null) {

            TextView tt1 = (TextView) v.findViewById(R.id.btDevicesRow);
            TextView tt2 = (TextView) v.findViewById(R.id.btStatus);

            tt1.setText(p.getAddress());
            if(p.getName() != null) {
                tt2.setText(p.getName());
            } else {
                tt2.setText("noName");
            }
        }

        return v;
    }

    class RowClickEventListener implements View.OnClickListener {
        int position;
        BluetoothDevice currentDevice;
        public RowClickEventListener(int pos) {
            this.position = pos;
            currentDevice = getItem(pos);
        }

        public void onClick(View v){
            try {
                //Method method = currentDevice.getClass().getMethod("createBond", (Class[]) null);
                //method.invoke(currentDevice, (Object[]) null);
                MainActivity.cockpitTag = currentDevice;
               // currentDevice.connectGatt(getContext(),true,)
            } catch (Exception e) {
                e.printStackTrace();
            }

            int a = 4;
        }

    }
}
