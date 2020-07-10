package com.app.eresearch.meetingtracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Timer;

public class LoginActivity extends Activity {

    private TextInputLayout emailTextInputLayout, passwordTextInputLayout;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;

    private long backKeyPressedTime = 0;

    private String email = "";
    private String password = "";

    FirebaseAuth firebaseAuth;

    private static final int REQUEST_PERMISSION_ACCESS_NETWORK_STATE = 100;
    private static final int REQUEST_PERMISSION_ACCESS_WIFI_STATE = 101;
    private static final int REQUEST_PERMISSION_INTERNET = 102;
    private static final int REQUEST_NETWORK_SETTING = 103;

    private SharedPreferences sharedPreferences = null;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        // Check whether network permission is granted, if not, ask for permission
        // As these are normal permission, the granting will not trigger a dialog
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_PERMISSION_ACCESS_NETWORK_STATE);
        }

        // Check whether WI-FI permission is granted, if not, ask for permission
        // As these are normal permission, the granting will not trigger a dialog
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_PERMISSION_ACCESS_WIFI_STATE);
        }

        // Check whether Internet permission is granted, if not, ask for permission
        // As these are normal permission, the granting will not trigger a dialog
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_PERMISSION_INTERNET);
        }

        // Check whether network access is available, if not, direct to setting
        ConnectivityManager connectivityManager = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if ((connectivityManager.getActiveNetworkInfo() == null) || (connectivityManager.getActiveNetworkInfo() != null && !connectivityManager.getActiveNetworkInfo().isConnected())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("Network Unavailable");
            builder.setCancelable(false);
            builder.setMessage("Network unavailable. Please go to setting to turn the network on. ");
            builder.setPositiveButton("Go to Setting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent networkIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    LoginActivity.this.startActivityForResult(networkIntent, REQUEST_NETWORK_SETTING);
                }
            });
            builder.create();
            builder.show();
        }

        emailTextInputLayout = findViewById(R.id.activity_login_email_textinputlayout);
        emailEditText = findViewById(R.id.activity_login_email_edittext);

        passwordTextInputLayout = findViewById(R.id.activity_login_password_textinputlayout);
        passwordEditText = findViewById(R.id.activity_login_password_edittext);

        loginButton = findViewById(R.id.activity_login_login_button);
        registerTextView = findViewById(R.id.activity_login_register_textview);

        progressBar = findViewById(R.id.activity_login_determinate_progressar);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.signOut();
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

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ConnectivityManager connectivityManager = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                boolean isNetworkAvailable = true;
                if ((connectivityManager.getActiveNetworkInfo() == null) || (connectivityManager.getActiveNetworkInfo() != null && !connectivityManager.getActiveNetworkInfo().isConnected())) {
                    isNetworkAvailable = false;
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setTitle("Network Unavailable");
                    builder.setCancelable(false);
                    builder.setMessage("Network unavailable. Please go to setting to turn the network on. ");
                    builder.setPositiveButton("Go to Setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent networkIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            LoginActivity.this.startActivityForResult(networkIntent, REQUEST_NETWORK_SETTING);
                        }
                    });
                    builder.create();
                    builder.show();
                }

                email = emailEditText.getText().toString();
                password = passwordEditText.getText().toString();

                boolean proceedLogin = true;

                // Check email
                if (!Utils.checkEmail(email)) {
                    emailTextInputLayout.setHelperText("Email address invalid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedLogin = false;
                }

                // Check if the password is typed in.
                if(!Utils.checkPasswordEmpty(password)){
                    passwordTextInputLayout.setHelperText("Password cannot be empty");
                    passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedLogin = false;
                }

                if (proceedLogin && isNetworkAvailable) {
                    //authenticate user
                    progressBar.setVisibility(View.VISIBLE);
                    CountDownTimer countDownTimer = new CountDownTimer(30000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            // Should do nothing here
                        }
                        public void onFinish() {
                            Toast.makeText(LoginActivity.this,"Log-in Timeout, Please Try Again. ", Toast.LENGTH_LONG).show();
                        }
                    };
                    countDownTimer.start();

                    SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                    String query = "SELECT * FROM User";
                    Cursor cursor = sqLiteDatabase.rawQuery(query, new String [] {});
                    boolean foundRegistration = false;
                    boolean passwordCorrect = false;
                    while (cursor.moveToNext()) {
                        if (email.equals(cursor.getString(0))) {
                            foundRegistration = true;
                            if (password.equals(cursor.getString(1))) {
                                passwordCorrect = true;
                            }
                        }
                    }
                    countDownTimer.cancel();
                    if (!foundRegistration) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Log-in Error");
                        builder.setCancelable(false);
                        builder.setMessage("This username is not registered. ");
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressBar.setVisibility(View.INVISIBLE);
                                emailEditText.setText("");
                                passwordEditText.setText("");
                            }
                        });
                        builder.create();
                        builder.show();
                    }
                    else if (!passwordCorrect) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Log-in Error");
                        builder.setCancelable(false);
                        builder.setMessage("Incorrect Password. ");
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressBar.setVisibility(View.INVISIBLE);
                                emailEditText.setText("");
                            }
                        });
                        builder.create();
                        builder.show();
                    }
                    else {
                        Toast.makeText(LoginActivity.this,"Logged in successfully", Toast.LENGTH_LONG).show();
                        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
                        sharedPreferences.edit().putString("LOGIN", email).commit();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        LoginActivity.this.startActivity(intent);
                        LoginActivity.this.finish();
                    }
                }
            }
        });

        // If the user have not registered yet, guide the user to the registration activity.
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(intent);
                LoginActivity.this.finish();
            }
        });
    }

    // Press exit key twice within 2 seconds to exit the app
    // Prevent mis-touch the exit key
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent)
    {
        if (keyCode == keyEvent.KEYCODE_BACK)
        {
            long currentTime = System.currentTimeMillis();
            System.out.println(currentTime);
            System.out.println(backKeyPressedTime);
            if ((currentTime - backKeyPressedTime) > 2000)
            {
                Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
                backKeyPressedTime = currentTime;
            }
            else
            {
                System.exit(0);
            }
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
