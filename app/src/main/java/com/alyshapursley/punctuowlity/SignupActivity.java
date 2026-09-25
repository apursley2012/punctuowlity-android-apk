package com.alyshapursley.punctuowlity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class SignupActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        databaseHelper = new DatabaseHelper(this);
        firstNameInput = findViewById(R.id.edit_first_name);
        lastNameInput = findViewById(R.id.edit_last_name);
        emailInput = findViewById(R.id.edit_email);
        phoneInput = findViewById(R.id.edit_phone);
        usernameInput = findViewById(R.id.edit_username);
        passwordInput = findViewById(R.id.edit_password);
        confirmPasswordInput = findViewById(R.id.edit_confirm_password);

        findViewById(R.id.button_back).setOnClickListener(view -> finish());
        findViewById(R.id.button_login).setOnClickListener(view -> finish());
        findViewById(R.id.button_create_account).setOnClickListener(view -> createAccount());
    }

    private void createAccount() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmation = confirmPasswordInput.getText().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || username.isEmpty() || password.isEmpty() || confirmation.isEmpty()) {
            Toast.makeText(this, R.string.error_required_signup_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, R.string.error_invalid_optional_phone, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmation)) {
            Toast.makeText(this, R.string.error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!databaseHelper.insertUser(firstName, lastName, email, phone, username, password)) {
            Toast.makeText(this, R.string.error_username_in_use, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
