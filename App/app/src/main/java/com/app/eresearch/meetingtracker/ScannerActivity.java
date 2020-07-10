package com.app.eresearch.meetingtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.google.zxing.Result;

import java.util.Calendar;
import java.util.HashMap;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import static android.Manifest.permission.CAMERA;

public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    private static final int REQUEST_CAMERA = 1;
    private ZXingScannerView mScannerView;

    private DatabaseHelper databaseHelper;

    private String currentUser = "";
    private SharedPreferences sharedPreferences = null;

    private boolean isFlashAvailable = false;
    private boolean isFlashOn = false;
    private boolean isBatteryHigh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        isFlashAvailable = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        int batteryLevel = getBatteryPercentage(getBaseContext());
        isBatteryHigh = (batteryLevel >= 10);

        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
        currentUser = sharedPreferences.getString("LOGIN", "");

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (checkPermission()) {
                Toast.makeText(getApplicationContext(), "Ready to scan", Toast.LENGTH_LONG).show();

            } else {
                requestPermission();
            }
        }
    }

    private boolean checkPermission() {
        return ( ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA ) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted){
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can scan the QR code", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access and camera", Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{CAMERA},
                                                            REQUEST_CAMERA);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (checkPermission()) {
                if(mScannerView == null) {
                    mScannerView = new ZXingScannerView(this);
                    setContentView(mScannerView);
                }
                mScannerView.setResultHandler(this);
                mScannerView.startCamera();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {

        final String result = rawResult.getText();

        HashMap<String, Object> eventHashMap = Utils.decodingMeetingString(result);
        String meetingContent = "";
        if ((Boolean) eventHashMap.get("successfullyRecognized")) {
            meetingContent = "Meeting Topic: " + eventHashMap.get("topic") + "\n\n" +
                     "Meeting Notes: " + eventHashMap.get("notes") + "\n\n" +
                     "Meeting Starting Time: " + Utils.convertCalendarToString((Calendar) eventHashMap.get("startingTime")) + "\n\n" +
                     "Meeting Ending Time: " + Utils.convertCalendarToString((Calendar) eventHashMap.get("endingTime")) + "\n\n" +
                     "Attendee Emails: " + eventHashMap.get("attendeeEmailString");
        }
        else {
            meetingContent = "QR code not recognized. This QR code is not a valid meeting QR code. ";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if ((Boolean) eventHashMap.get("successfullyRecognized")) {
            builder.setTitle("Add meeting");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("meetingCreatorLogin", currentUser);
                    contentValues.put("meetingTopic", String.valueOf(eventHashMap.get("topic")));
                    contentValues.put("meetingNotes", String.valueOf(eventHashMap.get("notes")));
                    contentValues.put("meetingDateTime", Utils.convertMeetingDateTimeToDatabaseDateTime(String.valueOf(eventHashMap.get("date")), Utils.convertCalendarToTime((Calendar) eventHashMap.get("startingTime")), Utils.convertCalendarToTime((Calendar) eventHashMap.get("endingTime"))));
                    contentValues.put("attendeeEmail", String.valueOf(eventHashMap.get("attendeeEmailString")));
                    contentValues.put("meetingDiarization", String.valueOf(eventHashMap.get("meetingDiarizationString")));
                    sqLiteDatabase.insert("Meeting", null, contentValues);
                    mScannerView.resumeCameraPreview(ScannerActivity.this);
                    Intent intent = new Intent(ScannerActivity.this, HomeActivity.class);
                    ScannerActivity.this.startActivity(intent);
                    ScannerActivity.this.finish();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(ScannerActivity.this, HomeActivity.class);
                    ScannerActivity.this.startActivity(intent);
                    ScannerActivity.this.finish();
                }
            });
            builder.setMessage(meetingContent);
        }
        else {
            builder.setTitle("QR code invalid");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(ScannerActivity.this, HomeActivity.class);
                    ScannerActivity.this.startActivity(intent);
                    ScannerActivity.this.finish();
                }
            });
            builder.setMessage(meetingContent);
        }
        AlertDialog alert1 = builder.create();
        alert1.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            Toast.makeText(getApplicationContext(), "No meeting added through scanning", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ScannerActivity.this, HomeActivity.class);
            ScannerActivity.this.startActivity(intent);
            ScannerActivity.this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isFlashAvailable && isBatteryHigh) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_scanner, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isFlashOn) {
            switch (item.getItemId()) {
                case R.id.action_scanner_flash: mScannerView.setFlash(false); isFlashOn = false; break;
            }
        }
        else {
            switch (item.getItemId()) {
                case R.id.action_scanner_flash: mScannerView.setFlash(true); isFlashOn = true; break;
            }
        }
        return true;
    }

    public static int getBatteryPercentage(Context context) {

        if (Build.VERSION.SDK_INT >= 21) {

            BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        } else {

            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            double batteryPct = level / (double) scale;

            return (int) (batteryPct * 100);
        }
    }
}
