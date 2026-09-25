package com.alyshapursley.punctuowlity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class SmsPermissionActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_REQUEST_CODE = 1001;
    private String phoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        String username = getSharedPreferences(
                SmsReminderReceiver.PREFERENCES_NAME,
                MODE_PRIVATE
        ).getString(LoginActivity.CURRENT_USERNAME, "");

        String[] profile = databaseHelper.getUserProfile(username);
        if (profile != null) {
            phoneNumber = profile[3];
        }

        findViewById(R.id.button_back).setOnClickListener(view -> finish());
        findViewById(R.id.button_allow).setOnClickListener(view -> requestSmsPermission());
        findViewById(R.id.button_deny).setOnClickListener(view -> savePreference(false));
    }

    private void requestSmsPermission() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(this, R.string.sms_phone_required, Toast.LENGTH_LONG).show();
            savePreference(false);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            savePreference(true);
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            savePreference(granted);
        }
    }

    private void savePreference(boolean enabled) {
        getSharedPreferences(SmsReminderReceiver.PREFERENCES_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(SmsReminderReceiver.SMS_PREFERENCE_SET, true)
                .putBoolean(SmsReminderReceiver.SMS_ENABLED, enabled)
                .putString(
                        SmsReminderReceiver.SMS_PHONE_NUMBER,
                        enabled && phoneNumber != null ? phoneNumber : ""
                )
                .apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
