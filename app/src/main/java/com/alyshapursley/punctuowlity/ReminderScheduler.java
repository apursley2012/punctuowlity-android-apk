package com.alyshapursley.punctuowlity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ReminderScheduler {
    static final String EXTRA_EVENT_TITLE = "event_title";
    private static final String EVENT_DATE_TIME_FORMAT = "MM/dd/yyyy h:mm a";

    private ReminderScheduler() {
    }

    static boolean schedule(Context context, Event event) {
        if (!event.isReminderEnabled()) {
            cancel(context, event.getId());
            return true;
        }

        long triggerAtMillis = parseTriggerTime(event.getDate(), event.getTime());
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return false;
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        PendingIntent pendingIntent = createPendingIntent(context, event.getId(), event.getTitle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        return true;
    }

    static void cancel(Context context, int eventId) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createPendingIntent(context, eventId, ""));
        }
    }

    static long parseTriggerTime(String date, String time) {
        SimpleDateFormat formatter =
                new SimpleDateFormat(EVENT_DATE_TIME_FORMAT, Locale.US);
        formatter.setLenient(false);

        try {
            Date parsed = formatter.parse(date + " " + normalizeTime(time));
            return parsed == null ? -1 : parsed.getTime();
        } catch (ParseException exception) {
            return -1;
        }
    }

    private static String normalizeTime(String time) {
        return time.trim()
                .toUpperCase(Locale.US)
                .replaceAll("\\s+", " ")
                .replaceAll("(?<=\\d)(AM|PM)$", " $1");
    }

    private static PendingIntent createPendingIntent(
            Context context,
            int eventId,
            String eventTitle
    ) {
        Intent intent = new Intent(context, SmsReminderReceiver.class);
        intent.putExtra(EXTRA_EVENT_TITLE, eventTitle);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, eventId, intent, flags);
    }
}
