package com.losek.vfrmobile.activity.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.losek.vfrmobile.R;
import com.losek.vfrmobile.util.VfrApplication;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

/**
 * Created by pawel on 13.11.2016.
 */

public class DevicesListAdapter extends ArrayAdapter<Node> implements Manager.ManagerListener {
    private static final String LOG = "VfrDeviceListAdapter";

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
            v = vi.inflate(R.layout.device_list_element, null);
        }

        Node p = getItem(position);

        if (p != null) {

            TextView tt1 = (TextView) v.findViewById(R.id.deviceName);
            TextView tt2 = (TextView) v.findViewById(R.id.deviceAddress);

            if (tt1 != null) {
                String name = p.getFriendlyName();
                if(VfrApplication.isNodePaired(p)){
                    name = name.concat(" (paired as "+ VfrApplication.getPairedAttributeName(p) + ")");
                    v.setEnabled(false);
                }
                tt1.setText(name);
            }

            if (tt2 != null) {
                tt2.setText(p.getTag());
            }
        }

        return v;
    }

    @Override
    public void onDiscoveryChange(Manager m, boolean enabled) {
        Log.e(LOG,"Discovery change!");
    }

    @Override
    public void onNodeDiscovered(Manager m, final Node node) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                add(node);
            }//run
        });
    }


}
