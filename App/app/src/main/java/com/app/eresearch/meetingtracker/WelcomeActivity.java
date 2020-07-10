package com.app.eresearch.meetingtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

public class WelcomeActivity extends AppCompatActivity {

    private long backKeyPressedTime = 0;
    private SharedPreferences sharedPreferences = null;
    private boolean isFirstRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Detect whether the app is run for the first time
        // If run for the first time, the intent will go to RegisterActivity
        // Otherwise, the intent will go to the LoginActivity
        sharedPreferences = getSharedPreferences("com.app.eresearch.meetingtracker", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("FIRST_RUN", true)) {
            sharedPreferences.edit().putBoolean("FIRST_RUN", false).commit();
            isFirstRun = true;
        }

        // Direct to Register Activity 5 seconds after the activity is created
        // Destroy this activity when it redirects
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                if (isFirstRun) {
                    intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
                }
                else {
                    intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                }
                WelcomeActivity.this.startActivity(intent);
                WelcomeActivity.this.finish();
            }
        }, 3000);
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
