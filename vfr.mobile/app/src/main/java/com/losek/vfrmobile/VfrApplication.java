package com.losek.vfrmobile;

import android.app.Application;

import com.st.BlueSTSDK.Node;

/**
 * Created by pawel on 16.11.2016.
 */

public class VfrApplication extends Application {
    private Node cockpitTag;
    private Node helmetTag;

    public Node getCockpitTag() {
        return cockpitTag;
    }

    public void setCockpitTag(Node cockpitTag) {
        this.cockpitTag = cockpitTag;
    }

    public Node getHelmetTag() {
        return helmetTag;
    }

    public void setHelmetTag(Node helmetTag) {
        this.helmetTag = helmetTag;
    }
}
