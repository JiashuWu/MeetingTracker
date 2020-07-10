package com.app.eresearch.meetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class DetailActivity extends AppCompatActivity {

    private TextView meetingTopicTextView;
    private TextView meetingNotesTextView;
    private TextView meetingDateTimeTextView;
    private TextView meetingAttendeeTextView;
    private Button startButton;
    private Button deleteButton;
    private Button visualizationWaitButton;
    private Button visualizationReadyButton;

    private String timeZoneID = TimeZone.getDefault().getID();
    private String timeZoneDisplayName = TimeZone.getDefault().getDisplayName();

    private Calendar meetingStartingCalendarTime = Calendar.getInstance();
    private Calendar meetingEndingCalendarTime = Calendar.getInstance();

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private Bitmap meetingQRCodeBitmap;

    private DatabaseHelper databaseHelper;

    private String timeRemains = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if (getActionBar() != null) { getActionBar().setTitle("Your Meeting"); }
        if (getSupportActionBar() != null) { getSupportActionBar().setTitle("Your Meeting"); }

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        meetingTopicTextView = findViewById(R.id.activity_detail_meetingtopic_textview);
        meetingNotesTextView = findViewById(R.id.activity_detail_meetingnotes_textview);
        meetingDateTimeTextView = findViewById(R.id.activity_detail_meetingdatetime_textview);
        meetingAttendeeTextView = findViewById(R.id.activity_detail_meetingattendee_textview);

        startButton = findViewById(R.id.activity_detail_start_button);
        deleteButton = findViewById(R.id.activity_detail_delete_button);
        visualizationWaitButton = findViewById(R.id.activity_detail_visualizationwait_button);
        visualizationReadyButton = findViewById(R.id.activity_detail_visualizationready_button);

        if (getIntent().getStringExtra("meetingDiarization").equalsIgnoreCase("")) {
            visualizationWaitButton.setVisibility(View.VISIBLE);
            visualizationReadyButton.setVisibility(View.GONE);
        }
        else {
            visualizationWaitButton.setVisibility(View.GONE);
            visualizationReadyButton.setVisibility(View.VISIBLE);
        }

        meetingTopicTextView.setText(getIntent().getStringExtra("meetingTopic"));
        if (getIntent().getStringExtra("meetingNotes") == null || getIntent().getStringExtra("meetingNotes").equalsIgnoreCase("null") || getIntent().getStringExtra("meetingNotes").equalsIgnoreCase("")) {
            meetingNotesTextView.setText("No notes. ");
        }
        else {
            meetingNotesTextView.setText(getIntent().getStringExtra("meetingNotes"));
        }
        meetingDateTimeTextView.setText(Utils.reformatDateTimeString(getIntent().getStringExtra("meetingDateTime")));
        meetingAttendeeTextView.setText(String.join("\n", getIntent().getStringExtra("meetingAttendeeString").replace(",  ", ", ").split(", ")));

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle("Delete Meeting");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                        sqLiteDatabase.delete("Meeting", "meetingId = ?", new String [] {getIntent().getStringExtra("meetingId")});
                        if (getIntent().getStringExtra("callingOrigin").equals("MeetingAdapter")) {
                            Intent intent = new Intent(DetailActivity.this, MeetingListActivity.class);
                            DetailActivity.this.startActivity(intent);
                            DetailActivity.this.finish();
                        }
                        else {
                            Intent intent = new Intent(DetailActivity.this, HomeActivity.class);
                            DetailActivity.this.startActivity(intent);
                            DetailActivity.this.finish();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here
                    }
                });
                builder.setMessage("Discard draft meeting schedule? ");
                builder.create();
                builder.show();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> result = Utils.convertDatabaseDateTimeToMeetingDateTime(getIntent().getStringExtra("meetingDateTime"));
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd HH:mm");
                try {
                    Date meetingStarting = simpleDateFormat.parse(Utils.convertMeetingDateToDatabaseDate(result.get("meetingDate")) + " " + result.get("meetingStartingTime"));
                    Calendar calendar = Calendar.getInstance();
                    Date currentTime = calendar.getTime();
                    if (meetingStarting.getTime() >= currentTime.getTime()) {
                        long seconds = (meetingStarting.getTime() - currentTime.getTime()) / 1000;
                        long minutes = seconds / 60;
                        long hours = minutes / 60;
                        long days = hours / 24;
                        if (days != 0 || hours != 0) {
                            timeRemains = days + " days and " + hours % 24 + " hours remain till the beginning of this meeting. Start the meeting now? ";
                        }
                        else {
                            timeRemains = minutes + " minutes remain till the beginning of this meeting. Start the meeting now? ";
                        }
                    }
                    else {
                        long seconds = (currentTime.getTime() - meetingStarting.getTime()) / 1000;
                        long minutes = seconds / 60;
                        long hours = minutes / 60;
                        long days = hours / 24;
                        if (days != 0 || hours != 0) {
                            timeRemains = days + " days and " + hours % 24 + " hours passed from the beginning of this meeting. Start the meeting now? ";
                        }
                        else {
                            timeRemains = minutes + " minutes passed from the beginning of this meeting. Start the meeting now? ";
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle("Start Meeting");
                builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(DetailActivity.this, RecordingActivity.class);
                        intent.putExtra("meetingId", getIntent().getStringExtra("meetingId"));
                        intent.putExtra("meetingTopic", getIntent().getStringExtra("meetingTopic"));
                        DetailActivity.this.startActivity(intent);
                        DetailActivity.this.finish();
                    }
                });
                builder.setNegativeButton("Go Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here
                    }
                });
                builder.setMessage(timeRemains);
                builder.create();
                builder.show();
            }
        });

        visualizationWaitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle("Meeting haven't started");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here.
                    }
                });
                builder.setMessage("Meeting haven't started yet. No meeting participation analysis available to view. ");
                builder.create();
                builder.show();
            }
        });

        visualizationReadyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                String query = "SELECT meetingDiarization FROM Meeting WHERE meetingId = ?";
                Cursor cursor = sqLiteDatabase.rawQuery(query, new String [] {getIntent().getStringExtra("meetingId")});
                String meetingDiarization = "";
                while (cursor.moveToNext()) {
                    meetingDiarization = cursor.getString(0);
                }
                if (meetingDiarization.equalsIgnoreCase("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                    builder.setTitle("Meeting haven't started");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Should do nothing here.
                        }
                    });
                    builder.setMessage("Meeting haven't started yet. No meeting participation analysis available to view. ");
                    builder.create();
                    builder.show();
                }
                else {
                    Intent intent = new Intent(DetailActivity.this, VisualizationActivity.class);
                    intent.putExtra("meetingDiarizationString", meetingDiarization);
                    DetailActivity.this.startActivity(intent);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String[] PERMISSIONS = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"};
        int permission = ContextCompat.checkSelfPermission(DetailActivity.this,
                "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DetailActivity.this, PERMISSIONS, 1);
        }
        else {
            // Handle item selection
            HashMap<String, Object> eventHashMap = new HashMap<>();
            eventHashMap.put("attendeeEmail", Utils.convertAttendeeStringToArrayList(String.join(",", getIntent().getStringExtra("meetingAttendeeString").split(", "))));
            eventHashMap.put("topic", getIntent().getStringExtra("meetingTopic"));
            eventHashMap.put("notes", getIntent().getStringExtra("meetingNotes"));
            HashMap<String, String> result = Utils.convertDatabaseDateTimeToMeetingDateTime(getIntent().getStringExtra("meetingDateTime"));
            eventHashMap.put("date", result.get("meetingDate"));
            HashMap<String, Integer> dateHashMap = Utils.convertMeetingDate(result.get("meetingDate"));
            HashMap<String, Integer> startingTimeHashMap = Utils.convertMeetingTime(result.get("meetingStartingTime"));
            HashMap<String, Integer> endingTimeHashMap = Utils.convertMeetingTime(result.get("meetingEndingTime"));
            meetingStartingCalendarTime.set(dateHashMap.get("year"), dateHashMap.get("month") - 1, dateHashMap.get("day"), startingTimeHashMap.get("hour"), startingTimeHashMap.get("minute"));
            meetingEndingCalendarTime.set(dateHashMap.get("year"), dateHashMap.get("month") - 1, dateHashMap.get("day"), endingTimeHashMap.get("hour"), endingTimeHashMap.get("minute"));
            eventHashMap.put("startingTime", meetingStartingCalendarTime);
            eventHashMap.put("endingTime", meetingEndingCalendarTime);
            eventHashMap.put("timezoneId", timeZoneID);
            eventHashMap.put("meetingDiarizationString", getIntent().getStringExtra("meetingDiarization"));

            String encodedMeetingString = Utils.encodingMeetingString(eventHashMap);
            try {
                String type = "QR Code";
                meetingQRCodeBitmap = Utils.CreateImage(encodedMeetingString, type);
            } catch (WriterException we) {
                we.printStackTrace();
            }

            Intent intentEmail = new Intent(Intent.ACTION_SEND);
            intentEmail.setType("message/frc822");
            ArrayList<String> attendeeEmailArrayList = Utils.convertAttendeeStringToArrayList(String.join(",", getIntent().getStringExtra("meetingAttendeeString").split(", ")));
            intentEmail.putExtra(Intent.EXTRA_EMAIL, attendeeEmailArrayList.toArray(new String[attendeeEmailArrayList.size()]));
            intentEmail.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra("meetingTopic"));
            intentEmail.putExtra(Intent.EXTRA_TEXT, Utils.emailComposer("", getIntent().getStringExtra("meetingTopic"), getIntent().getStringExtra("meetingNotes"), result.get("meetingDate"), result.get("meetingStartingTime"), result.get("meetingEndingTime"), timeZoneDisplayName, true));

            String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), meetingQRCodeBitmap, "Meeting", null);
            Uri bitmapUri = Uri.parse(bitmapPath);
            intentEmail.putExtra(Intent.EXTRA_STREAM, bitmapUri);

            switch (item.getItemId()) {
                case R.id.action_calendar:
                    executor = ContextCompat.getMainExecutor(DetailActivity.this);
                    biometricPrompt = new BiometricPrompt(DetailActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                            Intent intentCalendar = Utils.addEventToSystemCalendar(eventHashMap);
                            DetailActivity.this.startActivityForResult(intentCalendar, 1000);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                    promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Biometric login")
                            .setSubtitle("Log in using your biometric credential")
                            .setDeviceCredentialAllowed(true)
                            .build();
                    biometricPrompt.authenticate(promptInfo);
                    return true;
                case R.id.action_email:
                    executor = ContextCompat.getMainExecutor(DetailActivity.this);
                    biometricPrompt = new BiometricPrompt(DetailActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                            DetailActivity.this.startActivityForResult(Intent.createChooser(intentEmail, "Select Email Client"), 2000);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                    promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Biometric login")
                            .setSubtitle("Log in using your biometric credential")
                            .setDeviceCredentialAllowed(true)
                            .build();
                    biometricPrompt.authenticate(promptInfo);
                    return true;
                case R.id.action_qrcode:
                    AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                    LayoutInflater layoutInflater = (DetailActivity.this).getLayoutInflater();
                    builder.setTitle("Meeting QR Code");
                    builder.setCancelable(false);
                    builder.setIcon(R.mipmap.ic_appicon_round);
                    View view = layoutInflater.inflate(R.layout.qrcode_layout, null);
                    ImageView qrcodeImageView = view.findViewById(R.id.qrcode_imageview);
                    qrcodeImageView.setImageBitmap(meetingQRCodeBitmap);
                    builder.setView(view)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Should do nothing here.
                                }
                            });
                    builder.create();
                    builder.show();

                    return true;
                default:
                    if (getIntent().getStringExtra("callingOrigin").equals("MeetingAdapter")) {
                        Intent intent = new Intent(DetailActivity.this, MeetingListActivity.class);
                        DetailActivity.this.startActivity(intent);
                        DetailActivity.this.finish();
                    }
                    else {
                        Intent intent = new Intent(DetailActivity.this, HomeActivity.class);
                        DetailActivity.this.startActivity(intent);
                        DetailActivity.this.finish();
                    }
                    return true;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            if (getIntent().getStringExtra("callingOrigin").equals("MeetingAdapter")) {
                Intent intent = new Intent(DetailActivity.this, MeetingListActivity.class);
                DetailActivity.this.startActivity(intent);
                DetailActivity.this.finish();
            }
            else {
                Intent intent = new Intent(DetailActivity.this, HomeActivity.class);
                DetailActivity.this.startActivity(intent);
                DetailActivity.this.finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}
