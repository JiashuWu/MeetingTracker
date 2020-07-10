package com.app.eresearch.meetingtracker;

import android.Manifest;
import android.app.Activity;
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
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
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

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class RegisterActivity extends Activity{

    private long backKeyPressedTime = 0;

    private EditText emailEditText, phoneEditText, passwordEditText, passwordConfirmationEditText;
    private TextInputLayout emailTextInputLayout, phoneTextInputLayout, passwordTextInputLayout, passwordConfirmationTextInputlayout;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;

    private String email = "";
    private String phone = "";
    private String password = "";
    private String passwordConfirmation = "";

    private static final int REQUEST_PERMISSION_ACCESS_NETWORK_STATE = 100;
    private static final int REQUEST_PERMISSION_ACCESS_WIFI_STATE = 101;
    private static final int REQUEST_PERMISSION_INTERNET = 102;
    private static final int REQUEST_NETWORK_SETTING = 103;

    private SharedPreferences sharedPreferences = null;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseHelper = new DatabaseHelper(this, DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);

        // If an app declares in its manifest that it needs a normal permission, the system automatically grants the app that permission at install time.
        // The system doesn't prompt the user to grant normal permissions, and users cannot revoke these permissions.

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
            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
            builder.setTitle("Network Unavailable");
            builder.setCancelable(false);
            builder.setMessage("Network unavailable. Please go to setting to turn the network on. ");
            builder.setPositiveButton("Go to Setting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent networkIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    RegisterActivity.this.startActivityForResult(networkIntent, REQUEST_NETWORK_SETTING);
                }
            });
            builder.create();
            builder.show();
        }

        emailTextInputLayout = findViewById(R.id.activity_register_email_textinputlayout);
        emailEditText = findViewById(R.id.activity_register_email_edittext);

        phoneTextInputLayout = findViewById(R.id.activity_register_phone_textinputlayout);
        phoneEditText = findViewById(R.id.activity_register_phone_edittext);

        passwordTextInputLayout = findViewById(R.id.activity_register_password_textinputlayout);
        passwordEditText = findViewById(R.id.activity_register_password_edittext);

        passwordConfirmationTextInputlayout = findViewById(R.id.activity_register_password_confirmation_textinputlayout);
        passwordConfirmationEditText = findViewById(R.id.activity_register_password_confirmation_edittext);

        registerButton = findViewById(R.id.activity_register_register_button);
        loginTextView = findViewById(R.id.activity_register_login_textview);

        progressBar = findViewById(R.id.activity_register_determinate_progressar);

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null) {
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

        // phoneEditText: Set helper text for phone if it's length is invalid
        phoneEditText.addTextChangedListener(new TextWatcher() {
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
                String phoneEntered = phoneEditText.getText().toString();
                if (phoneEntered == null || (phoneEntered != null && phoneEntered.length() == 0)) {
                    phoneTextInputLayout.setHelperText("Please fill in your phone number (optional)");
                    phoneTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                }
                else if (!Utils.checkPhone(phoneEntered)) {
                    phoneTextInputLayout.setHelperText("Phone length invalid");
                    phoneTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                }
                else {
                    phoneTextInputLayout.setHelperText("Phone number valid");
                    phoneTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.pass)));
                }
            }
        });

        // passwordEditText: Set helper texts for password edit text based on password strength
        passwordEditText.addTextChangedListener(new TextWatcher() {
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
                String passwordEntered = passwordEditText.getText().toString();
                if (passwordEntered == null || (passwordEntered != null && passwordEntered.length() == 0)) {
                    passwordTextInputLayout.setHelperText("Please fill in your password");
                    passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                }
                else if (passwordEntered != null && passwordEntered.length() != 0) {
                    String passwordEnteringStrength = Utils.calculatePasswordStrength(passwordEntered);
                    passwordTextInputLayout.setHelperText("Password strength: " + passwordEnteringStrength);
                    if (passwordEnteringStrength.equals("Weak")) {
                        passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.passwordWeak)));
                    }
                    else if (passwordEnteringStrength.equals("Moderate")) {
                        passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.passwordModerate)));
                    }
                    else if (passwordEnteringStrength.equals("Strong")) {
                        passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.passwordStrong)));
                    }
                }
            }
        });

        // passwordConfirmationEditText: Set helper text for password confirmation edit text based on whether password matches
        passwordConfirmationEditText.addTextChangedListener(new TextWatcher() {
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
                String passwordEntered = passwordEditText.getText().toString();
                String passwordConfirmationEntered = passwordConfirmationEditText.getText().toString();
                if (passwordConfirmationEntered == null || (passwordConfirmationEntered != null && passwordConfirmationEntered.length() == 0)) {
                    passwordConfirmationTextInputlayout.setHelperText("Please confirm your password");
                    passwordConfirmationTextInputlayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorBlack)));
                }
                else if (passwordConfirmationEntered != null && passwordConfirmationEntered.length() != 0) {
                    if (!Utils.checkSamePassword(passwordConfirmationEntered, passwordEntered)) {
                        passwordConfirmationTextInputlayout.setHelperText("Password mismatched");
                        passwordConfirmationTextInputlayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    }
                    else {
                        passwordConfirmationTextInputlayout.setHelperText("Password matched");
                        passwordConfirmationTextInputlayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.pass)));
                    }
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = String.valueOf(emailEditText.getText());
                phone = String.valueOf(phoneEditText.getText());
                password = String.valueOf(passwordEditText.getText());
                passwordConfirmation = String.valueOf(passwordConfirmationEditText.getText());

                boolean proceedRegistration = true;

                // Check email
                if (!Utils.checkEmail(email)) {
                    emailTextInputLayout.setHelperText("Email address invalid");
                    emailTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedRegistration = false;
                }

                // Check phone
                if (phone != null && phone.length() != 0 && !Utils.checkPhone(phone)) {
                    phoneTextInputLayout.setHelperText("Phone number invalid");
                    phoneTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedRegistration = false;
                }

                // Check password empty
                if (!Utils.checkPasswordEmpty(password)) {
                    passwordTextInputLayout.setHelperText("Password cannot be empty");
                    passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedRegistration = false;
                }

                // Check password confirmation empty
                if (!Utils.checkPasswordEmpty(passwordConfirmation)) {
                    passwordConfirmationTextInputlayout.setHelperText("Password cannot be empty");
                    passwordConfirmationTextInputlayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedRegistration = false;
                }

                // Check password mismatching
                if (!Utils.checkSamePassword(password, passwordConfirmation)) {
                    passwordTextInputLayout.setHelperText("Password mismatched");
                    passwordConfirmationTextInputlayout.setHelperText("Password mismatched");
                    passwordTextInputLayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    passwordConfirmationTextInputlayout.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.error)));
                    proceedRegistration = false;
                }

                if (proceedRegistration) {
                    progressBar.setVisibility(View.VISIBLE);

                    CountDownTimer countDownTimer = new CountDownTimer(30000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            // Should do nothing here
                        }
                        public void onFinish() {
                            Toast.makeText(RegisterActivity.this,"Network Unstable, Please Try Again. ", Toast.LENGTH_LONG).show();
                            //Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            //RegisterActivity.this.startActivity(intent);
                            //RegisterActivity.this.finish();
                        }
                    };
                    countDownTimer.start();

                    SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                    String query = "SELECT * FROM User";
                    Cursor cursor = sqLiteDatabase.rawQuery(query, new String [] {});
                    boolean alreadyRegietered = false;
                    while (cursor.moveToNext()) {
                        if (email.equals(cursor.getString(0))) {
                            alreadyRegietered = true;
                            break;
                        }
                    }
                    countDownTimer.cancel();
                    if (alreadyRegietered) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                        builder.setTitle("Registration Error");
                        builder.setCancelable(false);
                        builder.setMessage("This username has already been registered. ");
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressBar.setVisibility(View.INVISIBLE);
                                emailEditText.setText("");
                                phoneEditText.setText("");
                                passwordEditText.setText("");
                                passwordConfirmationEditText.setText("");
                            }
                        });
                        builder.create();
                        builder.show();
                    }
                    else {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("username", email);
                        contentValues.put("password", passwordConfirmation);
                        contentValues.put("phone", phone);
                        sqLiteDatabase.insert("User", null, contentValues);
                        Toast.makeText(RegisterActivity.this,"Successfully Registered", Toast.LENGTH_LONG).show();
                        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
                        sharedPreferences.edit().putString("LOGIN", email).commit();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        RegisterActivity.this.startActivity(intent);
                        RegisterActivity.this.finish();
                    }
                }
            }
        });

        // if the registration completes, then guide the user to the login page.
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                RegisterActivity.this.startActivity(intent);
                RegisterActivity.this.finish();
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
