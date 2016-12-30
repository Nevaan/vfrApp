package com.losek.vfrmobile;

import android.app.Application;
import android.util.Log;

import com.st.BlueSTSDK.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by pawel on 16.11.2016.
 */

public class VfrApplication extends Application {

    private static Set<Observer> observers = new HashSet<>();
    private static Node cockpitTag;
    private static Node helmetTag;

    public Node getCockpitTag() {
        return cockpitTag;
    }

    public void setCockpitTag(Node cockpitTag) {
        this.cockpitTag = cockpitTag;
        updateObservers();
    }

    public Node getHelmetTag() {
        return helmetTag;
    }

    public void setHelmetTag(Node helmetTag) {
        this.helmetTag = helmetTag;
        updateObservers();
    }

    public boolean isNodePaired(Node node) {
        return (node.equals(cockpitTag) || node.equals(helmetTag)) ? true : false;
    }

    private void updateObservers() {
        for (Observer o : observers) {
            Log.d("VfrApp","Updating observers - one of values changed");
            o.update(null,null);
        }
    }

    public void addObserver(Observer o) {
        observers.add(o);
        Log.d("VfrApp","Adding new observer for vfr application class, count: " + observers.size());
    }

    public void deleteObserver(Observer o) {
        observers.remove(o);
        Log.d("VfrApp","Removing observer of vfr application class, count: " + observers.size());
    }

}
