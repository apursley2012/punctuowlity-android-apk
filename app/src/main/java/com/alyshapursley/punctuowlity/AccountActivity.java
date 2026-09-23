package com.alyshapursley.punctuowlity;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class AccountActivity extends AppCompatActivity {

	private DatabaseHelper databaseHelper;
	private EditText firstNameInput;
	private EditText lastNameInput;
	private EditText emailInput;
	private EditText phoneInput;
	private EditText usernameInput;
	private Switch smsSwitch;
	private String originalUsername;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account);

		databaseHelper = new DatabaseHelper(this);
		firstNameInput = findViewById(R.id.edit_first_name);
		lastNameInput = findViewById(R.id.edit_last_name);
		emailInput = findViewById(R.id.edit_email);
		phoneInput = findViewById(R.id.edit_phone);
		usernameInput = findViewById(R.id.edit_username);
		smsSwitch = findViewById(R.id.switch_sms);

		originalUsername = getSharedPreferences(
			SmsReminderReceiver.PREFERENCES_NAME,
			MODE_PRIVATE
		).getString(LoginActivity.CURRENT_USERNAME, "");

		String[] profile = databaseHelper.getUserProfile(originalUsername);
		if (profile == null) {
			finish();
			return;
		}

		firstNameInput.setText(profile[0]);
		lastNameInput.setText(profile[1]);
		emailInput.setText(profile[2]);
		phoneInput.setText(profile[3]);
		usernameInput.setText(profile[4]);
		smsSwitch.setChecked(
			getSharedPreferences(
				SmsReminderReceiver.PREFERENCES_NAME,
				MODE_PRIVATE
			).getBoolean(SmsReminderReceiver.SMS_ENABLED, false)
		);

		findViewById(R.id.button_back).setOnClickListener((view) -> finish());
		findViewById(R.id.button_save).setOnClickListener((view) -> saveProfile());
	}

	private void saveProfile() {
		String firstName = firstNameInput.getText().toString().trim();
		String lastName = lastNameInput.getText().toString().trim();
		String email = emailInput.getText().toString().trim();
		String phone = phoneInput.getText().toString().trim();
		String username = usernameInput.getText().toString().trim();

		if (
			firstName.isEmpty() ||
			lastName.isEmpty() ||
			email.isEmpty() ||
			username.isEmpty()
		) {
			showMessage(R.string.error_required_account_fields, Toast.LENGTH_SHORT);
			return;
		}
		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			showMessage(R.string.error_invalid_email, Toast.LENGTH_SHORT);
			return;
		}
		if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
			showMessage(R.string.error_invalid_phone, Toast.LENGTH_SHORT);
			return;
		}
		if (
			!databaseHelper.updateUserProfile(
				originalUsername,
				firstName,
				lastName,
				email,
				phone,
				username
			)
		) {
			showMessage(R.string.error_account_update, Toast.LENGTH_LONG);
			return;
		}

		boolean smsEnabled = smsSwitch.isChecked() && !phone.isEmpty();
		getSharedPreferences(SmsReminderReceiver.PREFERENCES_NAME, MODE_PRIVATE)
			.edit()
			.putString(LoginActivity.CURRENT_USERNAME, username)
			.putString(SmsReminderReceiver.SMS_PHONE_NUMBER, phone)
			.putBoolean(SmsReminderReceiver.SMS_ENABLED, smsEnabled)
			.apply();

		originalUsername = username;
		showMessage(R.string.account_updated, Toast.LENGTH_SHORT);
	}

	private void showMessage(int messageResource, int duration) {
		Toast.makeText(this, messageResource, duration).show();
	}
}
