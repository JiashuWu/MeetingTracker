package com.app.eresearch.meetingtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;

public class MeetingListActivity extends AppCompatActivity {

    private RecyclerView grandRecyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private RelativeLayout addMeetingRelativeLayout;
    private Button addMeetingButton;

    private EditText searchEditText;
    private Chip todoChip;
    private Chip doneChip;

    private String currentDateTime = "";
    private boolean displayTodo = true;
    private boolean displayDone = true;
    private String searchTerm = "";

    private ArrayList<Meeting> meetingArrayList;

    private DatabaseHelper databaseHelper;

    private String currentUser = "";
    private SharedPreferences sharedPreferences = null;

    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_list);
        context = getBaseContext();

        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
        currentUser = sharedPreferences.getString("LOGIN", "");

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        searchEditText = findViewById(R.id.activity_meetinglist_search_edittext);
        todoChip = findViewById(R.id.activity_meetinglist_todo_chip);
        doneChip = findViewById(R.id.activity_meetinglist_done_chip);

        addMeetingRelativeLayout = findViewById(R.id.activity_meetinglist_addmeeting_relativelayout);
        addMeetingButton = findViewById(R.id.activity_meetinglist_addmeeting_button);

        grandRecyclerView = findViewById(R.id.activity_meetinglist_grand_recyclerview);

        retrieveMeetingArrayList();

        addMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MeetingListActivity.this, ScheduleMeetingActivity.class);
                MeetingListActivity.this.startActivity(intent);
                MeetingListActivity.this.finish();
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                searchTerm = searchEditText.getText().toString();
                displayTodo = todoChip.isChecked();
                displayDone = doneChip.isChecked();
                currentDateTime = new SimpleDateFormat("yyyy MM dd@HH:mm").format(Calendar.getInstance().getTime()) + "@00:00";
                retrieveMeetingArrayList();
            }
        });
        
        todoChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked || displayDone) {
                    displayTodo = isChecked;
                    searchTerm = searchEditText.getText().toString();
                    currentDateTime = new SimpleDateFormat("yyyy MM dd@HH:mm").format(Calendar.getInstance().getTime()) + "@00:00";
                    retrieveMeetingArrayList();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Select at least one option. ", Toast.LENGTH_LONG).show();
                    displayTodo = true;
                    todoChip.setChecked(true);
                }
            }
        });
        
        doneChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (displayTodo || isChecked) {
                    displayDone = isChecked;
                    searchTerm = searchEditText.getText().toString();
                    currentDateTime = new SimpleDateFormat("yyyy MM dd@HH:mm").format(Calendar.getInstance().getTime()) + "@00:00";
                    retrieveMeetingArrayList();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Select at least one option. ", Toast.LENGTH_LONG).show();
                    displayDone = true;
                    doneChip.setChecked(true);
                }
            }
        });
    }

    public void retrieveMeetingArrayList() {
        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
        meetingArrayList = new ArrayList<>();
        String query = "";
        Cursor cursor = null;
        if (searchTerm.isEmpty()) {
            if (displayDone && displayTodo) {
                query = "SELECT * FROM Meeting WHERE meetingCreatorLogin = ?";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentUser});
            }
            else if (displayDone) {
                query = "SELECT * FROM Meeting WHERE meetingDateTime < ? AND meetingCreatorLogin = ?";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentDateTime, currentUser});
            }
            else if (displayTodo) {
                query = "SELECT * FROM Meeting WHERE meetingDateTime >= ? AND meetingCreatorLogin = ?";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentDateTime, currentUser});
            }
        }
        else {
            if (displayDone && displayTodo) {
                query = "SELECT * FROM Meeting WHERE meetingCreatorLogin = ? AND (meetingTopic LIKE '%" + searchTerm + "%' OR meetingNotes LIKE '%" + searchTerm + "%')";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentUser});
            }
            else if (displayDone) {
                query = "SELECT * FROM Meeting WHERE meetingDateTime < ? AND meetingCreatorLogin = ? AND (meetingTopic LIKE '%" + searchTerm + "%' OR meetingNotes LIKE '%" + searchTerm + "%')";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentDateTime, currentUser});
            }
            else if (displayTodo) {
                query = "SELECT * FROM Meeting WHERE meetingDateTime >= ? AND meetingCreatorLogin = ? AND (meetingTopic LIKE '%" + searchTerm + "%' OR meetingNotes LIKE '%" + searchTerm + "%')";
                cursor = sqLiteDatabase.rawQuery(query, new String [] {currentDateTime, currentUser});
            }
        }

        if (cursor != null) {
            int meetingCount = 0;
            while (cursor.moveToNext()) {
                meetingCount += 1;
                HashMap<String, Object> meetingHashMap = new HashMap<>();
                meetingHashMap.put("meetingId", cursor.getString(0));
                meetingHashMap.put("meetingCreatorLogin", cursor.getString(1));
                meetingHashMap.put("meetingTopic", cursor.getString(2));
                meetingHashMap.put("meetingNotes", cursor.getString(3));
                meetingHashMap.put("meetingDateTime", cursor.getString(4));
                meetingHashMap.put("attendeeEmail", cursor.getString(5));
                meetingHashMap.put("meetingDiarization", cursor.getString(6));
                meetingArrayList.add(new Meeting(meetingHashMap));
            }
            grandRecyclerView.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            adapter = new MeetingAdapter(meetingArrayList);
            grandRecyclerView.setLayoutManager(layoutManager);
            grandRecyclerView.setAdapter(adapter);
            if (meetingCount == 0) {
                addMeetingRelativeLayout.setVisibility(View.VISIBLE);
            }
            else {
                addMeetingRelativeLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_meetinglist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_meetinglist_add: intent = new Intent(MeetingListActivity.this, ScheduleMeetingActivity.class); break;
            case R.id.action_meetinglist_qrcode: intent = new Intent(MeetingListActivity.this, ScannerActivity.class); break;
        }
        MeetingListActivity.this.startActivity(intent);
        MeetingListActivity.this.finish();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            Intent intent = new Intent(MeetingListActivity.this, HomeActivity.class);
            MeetingListActivity.this.startActivity(intent);
            MeetingListActivity.this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

}
