package com.app.eresearch.meetingtracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeakerDiarizationConfig;
import com.google.cloud.speech.v1.SpeechClient;

import java.io.FileInputStream;
import java.io.IOException;

import com.dnkilic.waveform.WaveView;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.WordInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;

public class RecordingActivity extends AppCompatActivity {

    private RelativeLayout grandRelativeLayout;
    private TextView meetingTopicTextView;
    private Chronometer timerChronometer;
    private Chip darkModeChip;
    private ImageView controlImageView;
    private MaterialButton finishButton;

    private static final int REQUEST_PERMISSION_RECORD_AUDIO = 101;
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 102;

    private boolean isMeetingProgressing = false;
    private boolean isDarkModeEnabled = false;

    private static final int SAMPLING_RATE = 16000;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    private static final String AUDIO_RECORDING_FILE_NAME = "recording.raw";

    private asynchronousRecognizeFile recognitionTask;

    private String meetingId = "";
    private String meetingTopic = "";

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        meetingId = getIntent().getStringExtra("meetingId");
        meetingTopic = getIntent().getStringExtra("meetingTopic");

        grandRelativeLayout = findViewById(R.id.activity_recording_grand_relativelayout);
        meetingTopicTextView = findViewById(R.id.activity_recording_meeting_topic_textview);
        timerChronometer = findViewById(R.id.activity_recording_timer_chronometer);
        darkModeChip = findViewById(R.id.activity_recording_darkmode_chip);
        controlImageView = findViewById(R.id.activity_recording_control_imageview);
        finishButton = findViewById(R.id.activity_recording_finish_button);

        meetingTopicTextView.setText(meetingTopic);

        controlImageView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                boolean permissionOK = true;
                // Check necessary permissions, if all granted, start the voice recorder.
                if (!(isPermissionGranted(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
                    requestPermission(Manifest.permission.RECORD_AUDIO);
                    permissionOK = false;
                }
                if (!(isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                    requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    permissionOK = false;
                }

                if (permissionOK) {
                    // If the meeting hasn't started, start the meeting
                    if (!isMeetingProgressing) {
                        isMeetingProgressing = true;

                        controlImageView.setImageDrawable(getDrawable(R.drawable.ic_stop));

                        timerChronometer.setBase(SystemClock.elapsedRealtime());
                        timerChronometer.start();

                        Thread recordingThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                                byte audioData [] = new byte [BUFFER_SIZE];
                                AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
                                audioRecord.startRecording();

                                String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + AUDIO_RECORDING_FILE_NAME;

                                BufferedOutputStream bufferedOutputStream = null;

                                try {
                                    bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath));
                                }
                                catch (FileNotFoundException fnfe) {
                                    fnfe.printStackTrace();
                                }

                                while (isMeetingProgressing) {
                                    int status = audioRecord.read(audioData, 0, audioData.length);

                                    if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) {
                                        return;
                                    }

                                    try {
                                        bufferedOutputStream.write(audioData, 0, audioData.length);
                                    }
                                    catch (IOException ioe) {
                                        ioe.printStackTrace();
                                        return;
                                    }
                                }

                                try {
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (audioRecord != null) {
                                        audioRecord.stop();
                                        audioRecord.release();
                                    }

                                    isMeetingProgressing = false;
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        recordingThread.start();
                    }
                    // Otherwise, terminate the recording
                    else {
                        isMeetingProgressing = false;

                        timerChronometer.stop();

                        // Adjust buttons and icons
                        timerChronometer.setText("Meeting Ends");
                        controlImageView.setVisibility(View.GONE);
                        darkModeChip.setVisibility(View.GONE);
                        finishButton.setVisibility(View.VISIBLE);
                        finishButton.setText("Processing");

                        String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + AUDIO_RECORDING_FILE_NAME;

                        recognitionTask = new asynchronousRecognizeFile();
                        recognitionTask.execute(new String [] {filePath});

                        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(RecordingActivity.this);
                        builder.setTitle("Meeting Finished");
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent (RecordingActivity.this, HomeActivity.class);
                                RecordingActivity.this.startActivity(intent);
                                RecordingActivity.this.finish();
                            }
                        });
                        builder.setMessage("Meeting ends. The meeting participation analysis will be available the meeting detail page after a short processing time. ");
                        builder.setCancelable(false);
                        builder.create();
                        builder.show();
                    }
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        darkModeChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDarkModeEnabled) {
                    // Enable the dark mode
                    isDarkModeEnabled = true;
                    grandRelativeLayout.setBackgroundColor(getResources().getColor(R.color.colorDark));
                    darkModeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.colorDark)));
                    darkModeChip.setChipStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorWhite)));
                    darkModeChip.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorWhite)));
                    darkModeChip.setText("Light Mode");
                    darkModeChip.setChipIcon(getResources().getDrawable(R.drawable.ic_sky));
                    timerChronometer.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorWhite)));
                    meetingTopicTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorWhite)));
                    actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorBlack)));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setStatusBarColor(getResources().getColor(R.color.colorBlack));
                    }
                }
                else {
                    isDarkModeEnabled = false;
                    grandRelativeLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
                    darkModeChip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.transparent)));
                    darkModeChip.setChipStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                    darkModeChip.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                    darkModeChip.setText("Dark Mode");
                    darkModeChip.setChipIcon(getResources().getDrawable(R.drawable.ic_night));
                    timerChronometer.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                    meetingTopicTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                    actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
                    }
                }
            }
        });
    }

    // Check permission
    private int isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    // Request permission if not granted
    private void requestPermission(String permission) {
        if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION_RECORD_AUDIO);
        }
        else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private class asynchronousRecognizeFile extends AsyncTask<String, Void, Void>{

        @Override
        protected void onPreExecute () {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... fileName) {
            // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
            try {
                CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(getResources().openRawResource(R.raw.credential)));
                SpeechSettings speechSettings = SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {

                    Path path = Paths.get(fileName[0]);
                    byte [] byteData = Files.readAllBytes(path);
                    ByteString audioBytes = ByteString.copyFrom(byteData);

                    SpeakerDiarizationConfig speakerDiarizationConfig = SpeakerDiarizationConfig.newBuilder()
                            .setEnableSpeakerDiarization(true)
                            .setMinSpeakerCount(1)
                            .setMaxSpeakerCount(100)
                            .build();

                    // Configure request with local raw PCM audio
                    RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(16000)
                            .setDiarizationConfig(speakerDiarizationConfig)
                            .build();
                    RecognitionAudio recognitionAudio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

                    // Use non-blocking call for getting file transcription
                    OperationFuture <LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient.longRunningRecognizeAsync(recognitionConfig, recognitionAudio);

                    while (!response.isDone()) {
                        Thread.sleep(500);
                    }

                    SpeechRecognitionResult speechRecognitionResult = response.get().getResultsList().get(response.get().getResultsList().size() - 1);
                    ArrayList<String> speechRecognitionArrayList = new ArrayList<>();
                    SpeechRecognitionAlternative alternative = speechRecognitionResult.getAlternativesList().get(0);
                    for (int i = 0 ; i < alternative.getWordsCount() ; i++) {
                        WordInfo wordInfo = alternative.getWords(i);
                        String singleWordResult = wordInfo.getSpeakerTag() +
                                "#" + String.valueOf(wordInfo.getStartTime().getSeconds() + (double) wordInfo.getStartTime().getNanos() / 1000000000) +
                                "#" + String.valueOf(wordInfo.getEndTime().getSeconds() + (double) wordInfo.getEndTime().getNanos() / 1000000000);
                        speechRecognitionArrayList.add(singleWordResult);
                    }

                    String speechRecognitionResultString = Utils.convertDiarizationArrayListToString(speechRecognitionArrayList);

                    SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("meetingDiarization", speechRecognitionResultString);

                    sqLiteDatabase.update("Meeting", contentValues, "meetingId = ?", new String [] {meetingId});
                    String testQuery = "SELECT * FROM Meeting WHERE meetingId = ?";
                    Cursor cursor = sqLiteDatabase.rawQuery(testQuery, new String[]{meetingId});
                    while (cursor.moveToNext()) {
                        Log.d("TESTQUERY", cursor.getString(6));
                    }

                }
                catch (Exception e) { e.printStackTrace(); }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            if (!isMeetingProgressing) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RecordingActivity.this);
                builder.setTitle("Quit Meeting");
                builder.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent (RecordingActivity.this, HomeActivity.class);
                        RecordingActivity.this.startActivity(intent);
                        RecordingActivity.this.finish();
                    }
                });
                builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here.
                    }
                });
                builder.setMessage("Quit the meeting? ");
                builder.create();
                builder.show();
                return true;
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(RecordingActivity.this);
                builder.setTitle("Meeting On Progress");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here.
                    }
                });
                builder.setMessage("Meeting is on progress. Stop the meeting before quitting. ");
                builder.create();
                builder.show();
                return true;
            }

        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}

