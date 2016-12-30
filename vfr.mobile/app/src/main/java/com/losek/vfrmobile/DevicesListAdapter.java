package com.losek.vfrmobile;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

/**
 * Created by pawel on 13.11.2016.
 */

public class DevicesListAdapter extends ArrayAdapter<Node> implements Manager.ManagerListener {

    private Activity mActivity;

    public DevicesListAdapter(Context ctx, int textViewResourceId) {
        super(ctx, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.bt_devices_list, null);
        }

        Node p = getItem(position);

        if (p != null) {

            TextView tt1 = (TextView) v.findViewById(R.id.deviceName);
            TextView tt2 = (TextView) v.findViewById(R.id.deviceAddress);

            if (tt1 != null) {
                tt1.setText(p.getName());
            }

            if (tt2 != null) {
                String name = p.getFriendlyName();
                if(p.isConnected()){
                    name = name.concat(" (paired)");
                }
                tt2.setText(name);
            }
        }

        return v;
    }

    @Override
    public void onDiscoveryChange(Manager m, boolean enabled) {

    }

    @Override
    public void onNodeDiscovered(Manager m, final Node node) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VfrApplication app = (VfrApplication) mActivity.getApplication();
                if (app.isNodePaired(node)) {
                    add(node);
                }
            }//run
        });
    }


}
