package com.losek.vfrmobile.util;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;

import com.st.BlueSTSDK.Node;

import java.util.HashSet;
import java.util.Observer;
import java.util.Set;

/**
 * Created by pawel on 16.11.2016.
 */

public class VfrApplication extends Application {

    private static final String LOG = "VfrApplication";

    private static Set<Observer> observers = new HashSet<>();
    private static Node cockpitTag;
    private static Node helmetTag;

    public static Node getCockpitTag() {
        return cockpitTag;
    }

    public void setCockpitTag(Node cockpitTag) {
        synchronized (this) {
            Log.d(LOG, "Setting cockpit tag to " + cockpitTag);
            this.cockpitTag = cockpitTag;
            updateObservers();
        }
    }

    public static Node getHelmetTag() {
        return helmetTag;
    }

    public void setHelmetTag(Node helmetTag) {
        synchronized (this) {
            this.helmetTag = helmetTag;
            updateObservers();
        }
    }

    public boolean isNodePaired(Node node) {
        return (node.equals(cockpitTag) || node.equals(helmetTag)) ? true : false;
    }

    private void updateObservers() {
        for (Observer o : observers) {
            Log.d(LOG,"Updating observers - one of values changed");
            o.update(null,null);
        }
    }

    public void addObserver(Observer o) {
        observers.add(o);
        Log.d(LOG,"Adding new observer for vfr application class, count: " + observers.size());
    }

    public void deleteObserver(Observer o) {
        observers.remove(o);
        Log.d(LOG,"Removing observer of vfr application class, count: " + observers.size());
    }

    @NonNull
    public static String getPairedAttributeName(Node node) {
        if(node.equals(helmetTag)){
            return "helmet";
        }
        if(node.equals(cockpitTag)) {
            return "cockpit";
        }
        return "";
    }

    public void unsetTag(Node node) {
        if(isNodePaired(node)) {
            if (node.equals(helmetTag)) {
                Log.d(LOG, "Unpaired helmet because of node lost!");
                setHelmetTag(null);
            }
            if (node.equals(cockpitTag)) {
                Log.d(LOG, "Unpaired cockpit because of node lost!");
                setCockpitTag(null);
            }
        }
    }


}
