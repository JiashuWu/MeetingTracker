package com.app.eresearch.meetingtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static String databaseName = "MeetingTracker.db";
    public static int databaseVersion = 1;

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory cursorFactory, int version) {
        super(context, name, cursorFactory, version);
    }

    private static final String CREATE_USER_TABLE = "CREATE TABLE User ("
            + "username TEXT PRIMARY KEY, "
            + "password TEXT NOT NULL, "
            + "phone INTEGER )";

    private static final String CREATE_MEETING_TABLE = "CREATE TABLE Meeting ("
            + "meetingId INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "meetingCreatorLogin TEXT NOT NULL REFERENCES User(username), "
            + "meetingTopic TEXT NOT NULL, "
            + "meetingNotes TEXT, "
            + "meetingDateTime TEXT NOT NULL, "
            + "attendeeEmail TEXT NOT NULL, "
            + "meetingDiarization TEXT )";

    @Override
    public void onCreate (SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_USER_TABLE);
        sqLiteDatabase.execSQL(CREATE_MEETING_TABLE);
    }

    @Override
    public void onUpgrade (SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS User");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Meeting");
        onCreate(sqLiteDatabase);
    }

}
