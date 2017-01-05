package com.losek.vfrmobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.losek.vfrmobile.database.model.Recording;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by pawel on 05.01.2017.
 */

public class DatabaseRepository extends SQLiteOpenHelper {

    private static final String LOG = "VfrDbRepo";

    private static final int DB_VERSION = 2;

    private static final String DB_NAME = "vfrAppDatabase";

    private static final String RECORDINGS_TABLE_NAME = "recordings";
    private static final String ACCELEROMETER_TABLE_NAME = "accelerometerValues";
    private static final String GYROSCOPE_TABLE_NAME = "gyroscopeValues";
    private static final String MAGNETOMETER_TABLE_NAME = "magnetometerValues";

    // recording columns
    private static final String RECORDING_ID = "id";
    private static final String RECORDING_TIMESTAMP = "startTime";

    // table create statements
    private static final String CREATE_TABLE_RECORDINGS = "CREATE TABLE " + RECORDINGS_TABLE_NAME + " (" + RECORDING_ID + " INTEGER PRIMARY KEY, " + RECORDING_TIMESTAMP + " DATETIME)";

    public DatabaseRepository(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RECORDINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECORDINGS_TABLE_NAME);

        onCreate(db);
    }

    public long createRecording() {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(RECORDING_TIMESTAMP, getDateTime());

        long newRecordingId = db.insert(RECORDINGS_TABLE_NAME, null, values);

        return newRecordingId;
    }

    public List<Recording> getRecordingList() {
        List<Recording> recordingList = new ArrayList<>();
        String selectRecordingsQuery = "SELECT * FROM " + RECORDINGS_TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectRecordingsQuery, null);

        if(cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(RECORDING_ID));
                String startDate = cursor.getString(cursor.getColumnIndex(RECORDING_TIMESTAMP));
                Recording recording = new Recording(id, startDate);

                recordingList.add(recording);
            } while (cursor.moveToNext());
        }

        Log.d(LOG, "Acquired " + recordingList.size() + " recordings.");
        return recordingList;
    }

    public void deleteRecording(long recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECORDINGS_TABLE_NAME, RECORDING_ID + " = ?", new String[] {String.valueOf(recordingId)});

    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void closeDbConnection() {
        SQLiteDatabase db = this.getReadableDatabase();
        if(db!=null & db.isOpen()){
            db.close();
        }
    }

}
