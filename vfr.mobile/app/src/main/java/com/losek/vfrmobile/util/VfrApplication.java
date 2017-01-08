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
    private static String cockpitTagFriendlyName;
    private static String helmetTagFriendlyName;
    private static Node cockpitTag;
    private static Node helmetTag;


    public static Node getCockpitTag() {
        return cockpitTag;
    }

    public void setCockpitTag(Node cockpitTag) {
            Log.d(LOG, "Setting cockpit tag to " + cockpitTag);
            this.cockpitTag = cockpitTag;
            updateObservers();

    }

    public static Node getHelmetTag() {
        return helmetTag;
    }

    public void setHelmetTag(Node helmetTag) {
            this.helmetTag = helmetTag;
            updateObservers();

    }

    public static void setCockpitTagFriendlyName(String cockpitTagFriendlyName) {
        VfrApplication.cockpitTagFriendlyName = cockpitTagFriendlyName;
    }

    public static void setHelmetTagFriendlyName(String helmetTagFriendlyName) {
        VfrApplication.helmetTagFriendlyName = helmetTagFriendlyName;
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
        if(node.getFriendlyName().equals(helmetTagFriendlyName)){
            return "helmet";
        }
        if(node.getFriendlyName().equals(cockpitTagFriendlyName)) {
            return "cockpit";
        }
        return "ERROR";
    }


}
