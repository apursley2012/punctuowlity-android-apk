package com.alyshapursley.punctuowlity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class LoginActivity extends AppCompatActivity {
    static final String CURRENT_USERNAME = "current_username";
    static final String IS_LOGGED_IN = "is_logged_in";

    private DatabaseHelper databaseHelper;
    private EditText usernameInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);
        usernameInput = findViewById(R.id.edit_username);
        passwordInput = findViewById(R.id.edit_password);

        findViewById(R.id.button_back).setOnClickListener(view -> finish());
        findViewById(R.id.button_sign_up).setOnClickListener(
                view -> startActivity(new Intent(this, SignupActivity.class))
        );
        findViewById(R.id.button_login).setOnClickListener(view -> logIn());
    }

    private void logIn() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!databaseHelper.checkUser(username, password)) {
            Toast.makeText(this, R.string.error_invalid_login, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(SmsReminderReceiver.PREFERENCES_NAME, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(IS_LOGGED_IN, true)
                .putString(CURRENT_USERNAME, username)
                .apply();

        String[] profile = databaseHelper.getUserProfile(username);
        if (profile != null) {
            preferences.edit()
                    .putString(SmsReminderReceiver.SMS_PHONE_NUMBER, profile[3])
                    .apply();
        }

        boolean smsChoiceAlreadyMade = preferences.getBoolean(
                SmsReminderReceiver.SMS_PREFERENCE_SET,
                false
        );
        startActivity(new Intent(
                this,
                smsChoiceAlreadyMade ? MainActivity.class : SmsPermissionActivity.class
        ));
        finish();
    }
}
