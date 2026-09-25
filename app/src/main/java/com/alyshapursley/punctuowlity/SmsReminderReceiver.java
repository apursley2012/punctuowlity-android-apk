package com.alyshapursley.punctuowlity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

public final class SmsReminderReceiver extends BroadcastReceiver {
    static final String PREFERENCES_NAME = "punctuowlity_preferences";
    static final String SMS_ENABLED = "sms_enabled";
    static final String SMS_PHONE_NUMBER = "sms_phone_number";
    static final String SMS_PREFERENCE_SET = "sms_preference_set";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        boolean enabled = preferences.getBoolean(SMS_ENABLED, false);
        String phoneNumber = preferences.getString(SMS_PHONE_NUMBER, "");
        boolean permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;

        if (!enabled || !permissionGranted || phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return;
        }

        String eventTitle = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_TITLE);
        String reminderText = eventTitle == null || eventTitle.trim().isEmpty()
                ? context.getString(R.string.sms_reminder_default)
                : eventTitle;
        String message = context.getString(R.string.sms_reminder_message, reminderText);

        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
    }
}
