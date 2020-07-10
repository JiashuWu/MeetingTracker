package com.app.eresearch.meetingtracker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class HomeActivity extends AppCompatActivity {

    private TextView greetingTextView;
    private MaterialCardView upcomingCardView;
    private MaterialCardView createCardView;
    private MaterialCardView scanCardView;
    private MaterialCardView meetingCardView;

    private TextView upcomingTimeTextView;
    private TextView upcomingTopicTextView;

    private String currentDateTime = "";
    private String currentUser = "";
    private SharedPreferences sharedPreferences = null;

    private String upcomingMeetingTopic = "";
    private String upcomingMeetingNotes = "";
    private String upcomingMeetingDateTime = "";
    private String upcomingMeetingAttendeeString = "";
    private String upcomingMeetingId = "";
    private String upcomingMeetingDiarization = "";

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
        currentUser = sharedPreferences.getString("LOGIN", "");

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        upcomingCardView = findViewById(R.id.activity_home_upcoming_card);
        createCardView = findViewById(R.id.activity_home_create_card);
        scanCardView = findViewById(R.id.activity_home_scan_card);
        meetingCardView = findViewById(R.id.activity_home_meeting_card);

        greetingTextView = findViewById(R.id.activity_home_greeting_textview);
        upcomingTimeTextView = findViewById(R.id.activity_home_upcomingtime_textview);
        upcomingTopicTextView = findViewById(R.id.activity_home_upcomingtopic_textview);

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour >= 0 && currentHour <= 12) {
            greetingTextView.setText("Good Morning");
        }
        else if (currentHour > 12 && currentHour <= 17) {
            greetingTextView.setText("Good Afternoon");
        }
        else if (currentHour > 17 && currentHour <= 18) {
            greetingTextView.setText("Good Evening");
        }
        else if (currentHour > 18) {
            greetingTextView.setText("Good Night");
        }

        createCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ScheduleMeetingActivity.class);
                HomeActivity.this.startActivity(intent);
                HomeActivity.this.finish();
            }
        });

        scanCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
                HomeActivity.this.startActivity(intent);
                HomeActivity.this.finish();
            }
        });

        meetingCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, MeetingListActivity.class);
                HomeActivity.this.startActivity(intent);
                HomeActivity.this.finish();
            }
        });

        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
        currentDateTime = new SimpleDateFormat("yyyy MM dd@HH:mm").format(Calendar.getInstance().getTime()) + "@00:00";
        String query = "SELECT COUNT(*) FROM Meeting WHERE meetingCreatorLogin = ? AND meetingDateTime >= ?";
        Cursor cursor = sqLiteDatabase.rawQuery(query, new String [] {currentUser, currentDateTime});
        int meetingCount = 0;
        while (cursor.moveToNext()) {
            meetingCount = cursor.getInt(0);
        }
        if (cursor != null) { cursor.close(); }
        if (meetingCount == 0) {
            upcomingCardView.setVisibility(View.GONE);
        }
        else {
            query = "SELECT * FROM Meeting WHERE meetingCreatorLogin = ? AND meetingDateTime >= ? ORDER BY meetingDateTime ASC LIMIT 1";
            cursor = sqLiteDatabase.rawQuery(query, new String [] {currentUser, currentDateTime});
            while (cursor.moveToNext()) {
                HashMap <String, String> result = Utils.convertDatabaseDateTimeToMeetingDateTime(cursor.getString(4));
                upcomingTimeTextView.setText(result.get("meetingDate") + " " + result.get("meetingStartingTime"));
                upcomingTopicTextView.setText("Topic: " + cursor.getString(2));
                upcomingMeetingId = cursor.getString(0);
                upcomingMeetingTopic = cursor.getString(2);
                upcomingMeetingNotes = cursor.getString(3);
                upcomingMeetingDateTime = cursor.getString(4);
                upcomingMeetingAttendeeString = cursor.getString(5);
                upcomingMeetingDiarization = cursor.getString(6);
            }
            upcomingTopicTextView.setText("Topic: " + upcomingMeetingTopic);
            upcomingTimeTextView.setText(Utils.convertDatabaseDateToMeetingDate(upcomingMeetingDateTime.split("@")[0]) + " at " + upcomingMeetingDateTime.split("@")[1]);

            upcomingCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, DetailActivity.class);
                    intent.putExtra("callingOrigin", "HomeActivity");
                    intent.putExtra("meetingTopic", upcomingMeetingTopic);
                    intent.putExtra("meetingNotes", upcomingMeetingNotes);
                    intent.putExtra("meetingDateTime", upcomingMeetingDateTime);
                    intent.putExtra("meetingAttendeeString", String.join(", ", upcomingMeetingAttendeeString.split(",")));
                    intent.putExtra("meetingId", upcomingMeetingId);
                    intent.putExtra("meetingDiarization", upcomingMeetingDiarization);
                    HomeActivity.this.startActivity(intent);
                    HomeActivity.this.finish();
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setTitle("Log out");
            builder.setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
                    sharedPreferences.edit().putString("LOGIN", "").commit();
                    Intent intent = new Intent (HomeActivity.this, LoginActivity.class);
                    HomeActivity.this.startActivity(intent);
                    HomeActivity.this.finish();
                }
            });
            builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Should do nothing here.
                }
            });
            builder.setMessage("Log out the current account? ");
            builder.create();
            builder.show();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}
