package com.app.eresearch.meetingtracker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.WriterException;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.security.AccessController.getContext;

public class ScheduleMeetingActivity extends AppCompatActivity {

    private long backKeyPressedTime = 0;

    private TextInputLayout emailTextInputLayout, topicTextInputLayout, notesTextInputLayout, qrcodeTextInputLayout;
    private EditText emailEditText, topicEditText, notesEditText;
    private ImageView addPersonImageView;
    private Button meetingDateButton, meetingTimeButton, doneButton;
    private ChipGroup attendeeChipGroup, actionChipGroup, emailChipGroup;
    private TextView attendeeTextView, emailTextView, timeRemainTextView, qrcodeTextView;
    private Chip calendarChip, emailChip, gmailChip, outlookChip, chromeChip, browserChip;
    private AutoCompleteTextView qrcodeDropDownMenu;

    private ArrayList<String> attendeeEmailArrayList;
    private String meetingTopic = "";
    private String meetingNotes = "";
    private String meetingDate = "";
    private int meetingYear = 0;
    private int meetingMonth = 0;
    private int meetingDay = 0;

    private String meetingStartingTime = "";
    private int meetingStartingHour = 0;
    private int meetingStartingMinute = 0;

    private String meetingEndingTime = "";
    private int meetingEndingHour = 0;
    private int meetingEndingMinute = 0;

    private Calendar meetingStartingCalendarTime = Calendar.getInstance();
    private Calendar meetingEndingCalendarTime = Calendar.getInstance();

    private boolean addToCalendar = true;
    private boolean sendViaEmail = true;
    private String emailMedium = "";

    private static final int REQUEST_PERMISSION_READ_CALENDAR = 0;
    private static final int REQUEST_PERMISSION_WRITE_CALENDAR = 1;

    private String timeZoneID = TimeZone.getDefault().getID();
    private String timeZoneDisplayName = TimeZone.getDefault().getDisplayName();

    private boolean isGmailInstalled = false;
    private boolean isOutlookInstalled = false;
    private boolean isChromeInstalled = false;
    private boolean shouldUseDefaultBrowser = false;

    private boolean useQRCode = true;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private boolean isBiometricAvailable = false;
    private boolean isBiometricEnrolled = false;

    private Bitmap meetingQRCodeBitmap;

    private String currentUser = "";
    private SharedPreferences sharedPreferences = null;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting);

        // set calendar locale
        String localeEn = "en_US";
        Locale locale = new Locale(localeEn);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());

        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
        currentUser = sharedPreferences.getString("LOGIN", "");

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        isGmailInstalled = Utils.isAppInstalled(getApplicationContext(), "gmail");
        isOutlookInstalled = Utils.isAppInstalled(getApplicationContext(), "outlook");
        isChromeInstalled = Utils.isAppInstalled(getApplicationContext(), "chrome");
        shouldUseDefaultBrowser = (!isGmailInstalled) && (!isOutlookInstalled) && (!isChromeInstalled);

        emailTextInputLayout = findViewById(R.id.activity_schedule_email_textinputlayout);
        emailEditText = findViewById(R.id.activity_schedule_email_edittext);

        topicTextInputLayout = findViewById(R.id.activity_schedule_topic_textinputlayout);
        topicEditText = findViewById(R.id.activity_schedule_topic_edittext);

        notesTextInputLayout = findViewById(R.id.activity_schedule_notes_textinputlayout);
        notesEditText = findViewById(R.id.activity_schedule_notes_edittext);

        qrcodeTextInputLayout = findViewById(R.id.activity_schedule_qrcode_textinputlayout);

        meetingDateButton = findViewById(R.id.activity_schedule_date_button);
        meetingTimeButton = findViewById(R.id.activity_schedule_time_button);
        doneButton = findViewById(R.id.activity_schedule_done_button);

        addPersonImageView = findViewById(R.id.activity_schedule_addperson_imageview);

        attendeeChipGroup = findViewById(R.id.activity_schedule_attendee_chipgroup);
        actionChipGroup = findViewById(R.id.activity_schedule_actions_chipgroup);
        emailChipGroup = findViewById(R.id.activity_schedule_email_chipgroup);

        attendeeTextView = findViewById(R.id.activity_schedule_attendee_textview);
        emailTextView = findViewById(R.id.activity_schedule_email_textview);
        timeRemainTextView = findViewById(R.id.activity_schedule_timeremain_textview);
        qrcodeTextView = findViewById(R.id.activity_schedule_qrcode_textview);

        qrcodeDropDownMenu = findViewById(R.id.activity_schedule_qrcode_dropdownmenu);

        timeRemainTextView.setVisibility(View.GONE);

        calendarChip = findViewById(R.id.activity_schedule_calendar_chip);
        emailChip = findViewById(R.id.activity_schedule_email_chip);
        gmailChip = findViewById(R.id.activity_schedule_gmail_chip);
        outlookChip = findViewById(R.id.activity_schedule_outlook_chip);
        chromeChip = findViewById(R.id.activity_schedule_chrome_chip);
        browserChip = findViewById(R.id.activity_schedule_browser_chip);
        if (isGmailInstalled) {
            emailMedium = "gmail";
            gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.gmailSelected)));
            outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
        }
        else if (isOutlookInstalled) {
            emailMedium = "outlook";
            gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.outlookSelected)));
            chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
        }
        else if (isChromeInstalled) {
            emailMedium = "chrome";
            gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.chromeSelected)));
            browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
        }

        attendeeEmailArrayList = new ArrayList<>();

        if (shouldUseDefaultBrowser) {
            gmailChip.setVisibility(View.GONE);
            outlookChip.setVisibility(View.GONE);
            chromeChip.setVisibility(View.GONE);
            gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.browserSelected)));
            emailMedium = "browser";
        }
        else if (!isGmailInstalled) {
            gmailChip.setVisibility(View.GONE);
            browserChip.setVisibility(View.GONE);
        }
        else if (!isOutlookInstalled) {
            outlookChip.setVisibility(View.GONE);
            browserChip.setVisibility(View.GONE);
        }
        else if (!isChromeInstalled) {
            chromeChip.setVisibility(View.GONE);
            browserChip.setVisibility(View.GONE);
        }

        // emailEditText: Set helper texts for email edit text if it's invalid
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Don't need to do anything here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Don't need to do anything here
            }

            @Override
            public void afterTextChanged(Editable s) {
                String emailEntered = emailEditText.getText().toString();
                if (emailEntered == null || (emailEntered != null && emailEntered.length() == 0)) {
                    emailTextInputLayout.setHelperText("Please fill in your email address");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                }
                else if (!Utils.checkEmail(emailEntered)) {
                    emailTextInputLayout.setHelperText("Email address invalid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }
                else {
                    emailTextInputLayout.setHelperText("Email address valid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.pass)));
                }
            }
        });

        addPersonImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String attendeeEmailEntered = emailEditText.getText().toString();
                if (attendeeEmailEntered == null || (attendeeEmailEntered != null && attendeeEmailEntered.length() == 0)) {
                    emailTextInputLayout.setHelperText("Email address invalid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }
                else if (!Utils.checkEmail(attendeeEmailEntered)) {
                    emailTextInputLayout.setHelperText("Email address invalid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }
                else {
                    if (attendeeEmailArrayList.contains(attendeeEmailEntered)) {
                        emailTextInputLayout.setHelperText("Email address already added");
                        emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.warning)));
                    }
                    else {
                        Chip chip = new Chip(ScheduleMeetingActivity.this);
                        chip.setText(attendeeEmailEntered);
                        chip.setCloseIconVisible(true);
                        chip.setCheckable(false);
                        chip.setClickable(false);
                        chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                        chip.setChipStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                        chip.setChipStrokeWidth(getResources().getDimension(R.dimen.chip_stroke_width));
                        chip.setChipIcon(getResources().getDrawable(Utils.retrieveColourfulInitialIcon(attendeeEmailEntered.charAt(0))));
                        chip.setOnCloseIconClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                attendeeChipGroup.removeView(chip);
                                attendeeEmailArrayList.remove(chip.getText());

                                if (attendeeChipGroup.getChildCount() == 0) {
                                    attendeeChipGroup.setVisibility(View.GONE);
                                }

                                if (attendeeEmailArrayList.size() == 1) {
                                    attendeeTextView.setText("Attendee (" + String.valueOf(attendeeEmailArrayList.size()) + " person)");
                                }
                                else if (attendeeEmailArrayList.size() == 0) {
                                    attendeeTextView.setText("Attendee");
                                }
                                else {
                                    attendeeTextView.setText("Attendee (" + String.valueOf(attendeeEmailArrayList.size()) + " persons) ");
                                }
                            }
                        });
                        attendeeChipGroup.addView(chip);
                        attendeeChipGroup.setVisibility(View.VISIBLE);

                        attendeeEmailArrayList.add(attendeeEmailEntered);

                        if (attendeeEmailArrayList.size() == 1) {
                            attendeeTextView.setText("Attendee (" + String.valueOf(attendeeEmailArrayList.size()) + " person)");
                        }
                        else if (attendeeEmailArrayList.size() == 0) {
                            attendeeTextView.setText("Attendee");
                        }
                        else {
                            attendeeTextView.setText("Attendee (" + String.valueOf(attendeeEmailArrayList.size()) + " persons) ");
                        }

                        emailEditText.setText("");
                        emailTextInputLayout.setHelperText("Please fill in your email address");
                        emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                    }
                }
            }
        });

        topicEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Don't need to do anything here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Don't need to do anything here
            }

            @Override
            public void afterTextChanged(Editable s) {
                String topicEntered = topicEditText.getText().toString();
                if (topicEntered == null || (topicEntered != null && topicEntered.length() == 0)) {
                    topicTextInputLayout.setHelperText("Please fill in your meeting topic");
                    topicTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                }
                else {
                    topicTextInputLayout.setHelperText("Please fill in your meeting topic");
                    topicTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.pass)));
                }
            }
        });

        meetingDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                builder.setTitleText("Choose Meeting Date");
                MaterialDatePicker<Long> picker = builder.build();
                picker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                    @Override
                    public void onPositiveButtonClick(Long selection) {
                        meetingDate = Utils.generateDate(String.valueOf(picker.getHeaderText()));
                        HashMap<String, Integer> dateHashMap = Utils.convertMeetingDate(meetingDate);
                        meetingYear = dateHashMap.get("year");
                        meetingMonth = dateHashMap.get("month");
                        meetingDay = dateHashMap.get("day");
                        Calendar currentDate = Calendar.getInstance();
                        currentDate.set(currentDate.HOUR, 0);
                        currentDate.set(currentDate.HOUR_OF_DAY, 0);
                        currentDate.set(currentDate.MINUTE, 0);
                        currentDate.set(currentDate.SECOND, 0);
                        currentDate.set(currentDate.MILLISECOND, 0);
                        if (picker.getSelection() >= currentDate.getTimeInMillis()) {
                            meetingDateButton.setText(meetingDate);
                            meetingDateButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                            if (!meetingStartingTime.equalsIgnoreCase("") && !meetingEndingTime.equalsIgnoreCase("")) {
                                meetingStartingCalendarTime.set(meetingYear, meetingMonth - 1, meetingDay, meetingStartingHour, meetingStartingMinute);
                                meetingEndingCalendarTime.set(meetingYear, meetingMonth - 1, meetingDay, meetingEndingHour, meetingEndingMinute);
                                int minutesRemains = (int) TimeUnit.MILLISECONDS.toMinutes(meetingStartingCalendarTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
                                if (minutesRemains / 1440 != 0) {
                                    timeRemainTextView.setText("Meeting starts in " + String.valueOf(minutesRemains / 1440) + " days and " + String.valueOf(minutesRemains % 1440 / 60) + " hours. ");
                                }
                                else {
                                    timeRemainTextView.setText("Meeting starts in " + String.valueOf(minutesRemains / 60) + " hours and " + String.valueOf(minutesRemains % 60) + " minutes. ");
                                }
                                timeRemainTextView.setVisibility(View.VISIBLE);
                            }
                        }
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleMeetingActivity.this);
                            builder.setTitle("Invalid Date Selection");
                            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    meetingDateButton.setText("Meeting Date");
                                }
                            });
                            builder.setMessage("Meeting start date should be today or after today. ");
                            builder.create();
                            builder.show();
                        }
                    }
                });
                picker.show(getSupportFragmentManager(), picker.toString());
            }
        });

        meetingTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!meetingDate.equals("")) {
                    Calendar calendar = Calendar.getInstance();
                    TimePickerDialog timePickerDialogStartingTime = new TimePickerDialog(ScheduleMeetingActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            meetingStartingTime = Utils.generateTime(hourOfDay, minute);
                            meetingStartingHour = hourOfDay;
                            meetingStartingMinute = minute;
                            // After choosing the meeting starting time, let the user to choose the meeting ending time
                            Calendar calendar = Calendar.getInstance();
                            TimePickerDialog timePickerDialogEndingTime = new TimePickerDialog(ScheduleMeetingActivity.this, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    meetingEndingTime = Utils.generateTime(hourOfDay, minute);
                                    if (meetingEndingTime.compareToIgnoreCase(meetingStartingTime) >= 0) {
                                        meetingTimeButton.setText(meetingStartingTime + " - " + meetingEndingTime);
                                        meetingEndingHour = hourOfDay;
                                        meetingEndingMinute = minute;
                                        meetingStartingCalendarTime.set(meetingYear, meetingMonth - 1, meetingDay, meetingStartingHour, meetingStartingMinute);
                                        meetingEndingCalendarTime.set(meetingYear, meetingMonth - 1, meetingDay, meetingEndingHour, meetingEndingMinute);
                                        int minutesRemains = (int) TimeUnit.MILLISECONDS.toMinutes(meetingStartingCalendarTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
                                        if (minutesRemains / 1440 != 0) {
                                            timeRemainTextView.setText("Meeting starts in " + String.valueOf(minutesRemains / 1440) + " days and " + String.valueOf(minutesRemains % 1440 / 60) + " hours. ");
                                        }
                                        else {
                                            timeRemainTextView.setText("Meeting starts in " + String.valueOf(minutesRemains / 60) + " hours and " + String.valueOf(minutesRemains % 60) + " minutes. ");
                                        }
                                        timeRemainTextView.setVisibility(View.VISIBLE);
                                        meetingTimeButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleMeetingActivity.this);
                                        builder.setTitle("Invalid Time Selection");
                                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        });
                                        builder.setMessage("Meeting end time should be after the meeting start time. ");
                                        builder.create();
                                        builder.show();
                                    }
                                }
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(ScheduleMeetingActivity.this));
                            timePickerDialogEndingTime.setTitle("Choose Meeting Ending Time");
                            timePickerDialogEndingTime.create();
                            timePickerDialogEndingTime.show();
                        }
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(ScheduleMeetingActivity.this));
                    timePickerDialogStartingTime.setTitle("Choose Meeting Starting Time");
                    timePickerDialogStartingTime.create();
                    timePickerDialogStartingTime.show();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleMeetingActivity.this);
                    builder.setTitle("Meeting Date Invalid");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.setMessage("Please select meeting date first. ");
                    builder.create();
                    builder.show();
                }
            }
        });

        calendarChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                addToCalendar = isChecked;
            }
        });

        qrcodeDropDownMenu.setText("Yes, Generate & Send QR Code");
        String[] qrcodeOptions = new String[] {"Yes, Generate & Send QR Code", "No, Don't generate QR Code"};
        ArrayAdapter adapter = new ArrayAdapter<>(ScheduleMeetingActivity.this, R.layout.dropdown_menu_popup_item, qrcodeOptions);
        qrcodeDropDownMenu.setAdapter(adapter);
        qrcodeDropDownMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: useQRCode = true; break;
                    case 1: useQRCode = false; break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        emailChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendViaEmail = isChecked;
                if (!isChecked) {
                    emailTextView.setVisibility(View.GONE);
                    emailChipGroup.setVisibility(View.GONE);
                    qrcodeTextView.setVisibility(View.GONE);
                    qrcodeTextInputLayout.setVisibility(View.GONE);
                    qrcodeDropDownMenu.setVisibility(View.GONE);
                }
                else {
                    emailTextView.setVisibility(View.VISIBLE);
                    emailChipGroup.setVisibility(View.VISIBLE);
                    qrcodeTextView.setVisibility(View.VISIBLE);
                    qrcodeTextInputLayout.setVisibility(View.VISIBLE);
                    qrcodeDropDownMenu.setVisibility(View.VISIBLE);
                }
            }
        });

        gmailChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailMedium = "gmail";
                gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.gmailSelected)));
                outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            }
        });

        outlookChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailMedium = "outlook";
                gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.outlookSelected)));
                chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            }
        });

        chromeChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailMedium = "chrome";
                gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.chromeSelected)));
                browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            }
        });

        browserChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailMedium = "browser";
                gmailChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                outlookChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                chromeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
                browserChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.browserSelected)));
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                meetingTopic = topicEditText.getText().toString();
                meetingNotes = notesEditText.getText().toString();

                boolean allowProceed = false;

                String attendeeEmailEntered = emailEditText.getText().toString();
                if (attendeeEmailEntered.length() != 0) {
                    if (!Utils.checkEmail(attendeeEmailEntered)) {
                        emailTextInputLayout.setHelperText("Email address invalid");
                        emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    }
                    else {
                        if (attendeeEmailArrayList.contains(attendeeEmailEntered)) {
                            emailTextInputLayout.setHelperText("Email address already added");
                            emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.warning)));
                        }
                        else {
                            attendeeEmailArrayList.add(attendeeEmailEntered);
                            allowProceed = true;
                        }
                    }
                }
                else {
                    allowProceed = attendeeEmailArrayList.size() != 0;
                    if (attendeeEmailArrayList.size() == 0) {
                        emailTextInputLayout.setHelperText("Add at least one attendee");
                        emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    }
                }

                allowProceed = allowProceed && (!meetingTopic.isEmpty());
                if (meetingTopic.isEmpty()) {
                    topicTextInputLayout.setHelperText("Meeting topic cannot be empty");
                    topicTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }

                allowProceed = allowProceed && (!meetingDate.isEmpty());
                if (meetingDate.isEmpty()) {
                    meetingDateButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }

                allowProceed = allowProceed && (!meetingStartingTime.isEmpty());
                if (meetingStartingTime.isEmpty()) {
                    meetingTimeButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }

                allowProceed = allowProceed && (!meetingEndingTime.isEmpty());
                if (meetingEndingTime.isEmpty()) {
                    meetingTimeButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }

                if (allowProceed) {
                    HashMap<String, Object> eventHashMap = new HashMap<>();
                    eventHashMap.put("attendeeEmail", attendeeEmailArrayList);
                    eventHashMap.put("topic", meetingTopic);
                    eventHashMap.put("notes", meetingNotes);
                    eventHashMap.put("date", meetingDate);
                    eventHashMap.put("startingTime", meetingStartingCalendarTime);
                    eventHashMap.put("endingTime", meetingEndingCalendarTime);
                    eventHashMap.put("timezoneId", timeZoneID);
                    eventHashMap.put("meetingDiarizationString", "");

                    SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("meetingCreatorLogin", currentUser);
                    contentValues.put("meetingTopic", meetingTopic);
                    contentValues.put("meetingNotes", meetingNotes);
                    contentValues.put("meetingDateTime", Utils.convertMeetingDateTimeToDatabaseDateTime(meetingDate, meetingStartingTime, meetingEndingTime));
                    contentValues.put("attendeeEmail", Utils.convertAttendeeArrayListToString(attendeeEmailArrayList));
                    contentValues.put("meetingDiarization", "");
                    sqLiteDatabase.insert("Meeting", null, contentValues);

                    String encodedMeetingString = Utils.encodingMeetingString(eventHashMap);
                    try {
                        String type = "QR Code";
                        meetingQRCodeBitmap = Utils.CreateImage(encodedMeetingString, type);
                    } catch (WriterException we) {
                        we.printStackTrace();
                    }

                    String[] PERMISSIONS = {
                            "android.permission.READ_EXTERNAL_STORAGE",
                            "android.permission.WRITE_EXTERNAL_STORAGE"};
                    int permission = ContextCompat.checkSelfPermission(ScheduleMeetingActivity.this,
                            "android.permission.WRITE_EXTERNAL_STORAGE");
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ScheduleMeetingActivity.this, PERMISSIONS, 1);
                    }
                    Utils.saveBitmap(meetingQRCodeBitmap, "meeting", ".PNG");

                    executor = ContextCompat.getMainExecutor(ScheduleMeetingActivity.this);
                    biometricPrompt = new BiometricPrompt(ScheduleMeetingActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            if (addToCalendar) {
                                Intent intent = Utils.addEventToSystemCalendar(eventHashMap);
                                ScheduleMeetingActivity.this.startActivityForResult(intent, 1000);
                            } else if (sendViaEmail) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("message/frc822");
                                intent.putExtra(Intent.EXTRA_EMAIL, attendeeEmailArrayList.toArray(new String[attendeeEmailArrayList.size()]));
                                intent.putExtra(Intent.EXTRA_SUBJECT, meetingTopic);
                                intent.putExtra(Intent.EXTRA_TEXT, Utils.emailComposer("", meetingTopic, meetingNotes, meetingDate, meetingStartingTime, meetingEndingTime, timeZoneDisplayName, useQRCode));
                                if (useQRCode) {
                                    String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), meetingQRCodeBitmap, "Meeting", null);
                                    Uri bitmapUri = Uri.parse(bitmapPath);
                                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                                }
                                switch (emailMedium) {
                                    case "gmail": intent.setPackage("com.google.android.gm"); break;
                                    case "outlook": intent.setPackage("com.microsoft.office.outlook"); break;
                                    case "chrome": {String urlStringGmail = "http://www.gmail.com"; intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStringGmail)); intent.setPackage("com.android.chrome"); break;}
                                    default: {String urlStringDefault = "http://www.gmail.com"; intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStringDefault)); break;}
                                }
                                ScheduleMeetingActivity.this.startActivityForResult(intent, 2000);
                            } else {
                                Intent intentFinish = new Intent(ScheduleMeetingActivity.this, HomeActivity.class);
                                ScheduleMeetingActivity.this.startActivity(intentFinish);
                                ScheduleMeetingActivity.this.finish();
                            }
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
                    if (addToCalendar || sendViaEmail) {
                        biometricPrompt.authenticate(promptInfo);
                    }
                    else {
                        Intent intent = new Intent (ScheduleMeetingActivity.this, HomeActivity.class);
                        ScheduleMeetingActivity.this.startActivity(intent);
                        ScheduleMeetingActivity.this.finish();
                    }
                }
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1000) {
            if (sendViaEmail) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/frc822");
                intent.putExtra(Intent.EXTRA_EMAIL, attendeeEmailArrayList.toArray(new String [attendeeEmailArrayList.size()]));
                intent.putExtra(Intent.EXTRA_SUBJECT, meetingTopic);
                intent.putExtra(Intent.EXTRA_TEXT, Utils.emailComposer("", meetingTopic, meetingNotes, meetingDate, meetingStartingTime, meetingEndingTime, timeZoneDisplayName, useQRCode));
                if (useQRCode) {
                    String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), meetingQRCodeBitmap, "Meeting", null);
                    Uri bitmapUri = Uri.parse(bitmapPath);
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                }
                switch (emailMedium) {
                    case "gmail": intent.setPackage("com.google.android.gm"); break;
                    case "outlook": intent.setPackage("com.microsoft.office.outlook"); break;
                    case "chrome": {String urlStringGmail = "http://www.gmail.com"; intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStringGmail)); intent.setPackage("com.android.chrome"); break;}
                    default: {String urlStringDefault = "http://www.gmail.com"; intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStringDefault)); break;}
                }
                ScheduleMeetingActivity.this.startActivityForResult(intent, 2000);
            }
            else {
                Intent intentFinish = new Intent(ScheduleMeetingActivity.this, HomeActivity.class);
                ScheduleMeetingActivity.this.startActivity(intentFinish);
                ScheduleMeetingActivity.this.finish();
            }
        }
        if(requestCode == 2000) {
            Intent intentFinish = new Intent(ScheduleMeetingActivity.this, HomeActivity.class);
            ScheduleMeetingActivity.this.startActivity(intentFinish);
            ScheduleMeetingActivity.this.finish();
        }
    }

    // Press exit key twice within 2 seconds to exit the app
    // Prevent mis-touch the exit key
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Discard draft");
            builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(ScheduleMeetingActivity.this, HomeActivity.class);
                    ScheduleMeetingActivity.this.startActivity(intent);
                    ScheduleMeetingActivity.this.finish();
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
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onStart() { super.onStart(); }

    @Override
    public void onRestart() { super.onRestart(); }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onPause() { super.onPause(); }

    @Override
    public void onStop() { super.onStop(); }

    @Override
    public void onDestroy() { super.onDestroy(); }

}
