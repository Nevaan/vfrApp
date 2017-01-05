package com.losek.vfrmobile.database.model;

/**
 * Created by pawel on 05.01.2017.
 */

public class Recording {

    private int id;
    private String startTime;

    public Recording() {
    }

    public Recording(int id, String startTime) {
        this.id = id;
        this.startTime = startTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
