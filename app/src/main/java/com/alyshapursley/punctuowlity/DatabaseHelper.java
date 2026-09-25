package com.alyshapursley.punctuowlity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PunctuOwlityDB.db";
    private static final int DATABASE_VERSION = 4;

    private static final String USER_TABLE = "users";
    private static final String USER_ID = "id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";

    private static final String EVENT_TABLE = "events";
    private static final String EVENT_ID = "id";
    private static final String EVENT_TITLE = "title";
    private static final String EVENT_DATE = "date";
    private static final String EVENT_TIME = "time";
    private static final String EVENT_REMINDER = "reminder_enabled";
    private static final String EVENT_CATEGORY = "category";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE " + USER_TABLE + " (" +
                        USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        USERNAME + " TEXT UNIQUE NOT NULL, " +
                        PASSWORD + " TEXT NOT NULL, " +
                        FIRST_NAME + " TEXT NOT NULL DEFAULT '', " +
                        LAST_NAME + " TEXT NOT NULL DEFAULT '', " +
                        EMAIL + " TEXT NOT NULL DEFAULT '', " +
                        PHONE + " TEXT NOT NULL DEFAULT '')"
        );

        database.execSQL(
                "CREATE TABLE " + EVENT_TABLE + " (" +
                        EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        EVENT_TITLE + " TEXT NOT NULL, " +
                        EVENT_DATE + " TEXT NOT NULL, " +
                        EVENT_TIME + " TEXT NOT NULL, " +
                        EVENT_REMINDER + " INTEGER NOT NULL DEFAULT 0, " +
                        EVENT_CATEGORY + " TEXT NOT NULL DEFAULT 'general')"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL(
                    "ALTER TABLE " + EVENT_TABLE +
                            " ADD COLUMN " + EVENT_REMINDER + " INTEGER NOT NULL DEFAULT 0"
            );
        }
        if (oldVersion < 3) {
            database.execSQL(
                    "ALTER TABLE " + EVENT_TABLE +
                            " ADD COLUMN " + EVENT_CATEGORY + " TEXT NOT NULL DEFAULT 'general'"
            );
        }
        if (oldVersion < 4) {
            database.execSQL(
                    "ALTER TABLE " + USER_TABLE +
                            " ADD COLUMN " + FIRST_NAME + " TEXT NOT NULL DEFAULT ''"
            );
            database.execSQL(
                    "ALTER TABLE " + USER_TABLE +
                            " ADD COLUMN " + LAST_NAME + " TEXT NOT NULL DEFAULT ''"
            );
            database.execSQL(
                    "ALTER TABLE " + USER_TABLE +
                            " ADD COLUMN " + EMAIL + " TEXT NOT NULL DEFAULT ''"
            );
            database.execSQL(
                    "ALTER TABLE " + USER_TABLE +
                            " ADD COLUMN " + PHONE + " TEXT NOT NULL DEFAULT ''"
            );
        }
    }

    boolean insertUser(
            String firstName,
            String lastName,
            String email,
            String phone,
            String username,
            String password
    ) {
        ContentValues values = new ContentValues();
        values.put(FIRST_NAME, firstName);
        values.put(LAST_NAME, lastName);
        values.put(EMAIL, email);
        values.put(PHONE, phone);
        values.put(USERNAME, username);
        values.put(PASSWORD, PasswordHasher.hash(password));
        return getWritableDatabase().insert(USER_TABLE, null, values) != -1;
    }

    boolean checkUser(String username, String password) {
        SQLiteDatabase database = getWritableDatabase();
        try (Cursor cursor = database.query(
                USER_TABLE,
                new String[]{USER_ID, PASSWORD},
                USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return false;
            }

            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD));
            boolean valid = PasswordHasher.verify(password, storedPassword);
            if (valid && PasswordHasher.isLegacyValue(storedPassword)) {
                ContentValues values = new ContentValues();
                values.put(PASSWORD, PasswordHasher.hash(password));
                database.update(
                        USER_TABLE,
                        values,
                        USER_ID + " = ?",
                        new String[]{String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(USER_ID)))}
                );
            }
            return valid;
        }
    }

    String[] getUserProfile(String username) {
        try (Cursor cursor = getReadableDatabase().query(
                USER_TABLE,
                new String[]{FIRST_NAME, LAST_NAME, EMAIL, PHONE, USERNAME},
                USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new String[]{
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
            };
        }
    }

    boolean updateUserProfile(
            String originalUsername,
            String firstName,
            String lastName,
            String email,
            String phone,
            String username
    ) {
        ContentValues values = new ContentValues();
        values.put(FIRST_NAME, firstName);
        values.put(LAST_NAME, lastName);
        values.put(EMAIL, email);
        values.put(PHONE, phone);
        values.put(USERNAME, username);
        try {
            return getWritableDatabase().update(
                    USER_TABLE,
                    values,
                    USERNAME + " = ?",
                    new String[]{originalUsername}
            ) > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    long insertEvent(
            String title,
            String date,
            String time,
            boolean reminderEnabled,
            String category
    ) {
        return getWritableDatabase().insert(
                EVENT_TABLE,
                null,
                eventValues(title, date, time, reminderEnabled, category)
        );
    }

    boolean updateEvent(
            int eventId,
            String title,
            String date,
            String time,
            boolean reminderEnabled,
            String category
    ) {
        return getWritableDatabase().update(
                EVENT_TABLE,
                eventValues(title, date, time, reminderEnabled, category),
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        ) > 0;
    }

    boolean deleteEvent(int eventId) {
        return getWritableDatabase().delete(
                EVENT_TABLE,
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        ) > 0;
    }

    List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                EVENT_TABLE, null, null, null, null, null, null
        )) {
            while (cursor.moveToNext()) {
                events.add(readEvent(cursor));
            }
        }
        return events;
    }

    Event getEventById(int eventId) {
        try (Cursor cursor = getReadableDatabase().query(
                EVENT_TABLE,
                null,
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? readEvent(cursor) : null;
        }
    }

    private ContentValues eventValues(
            String title,
            String date,
            String time,
            boolean reminderEnabled,
            String category
    ) {
        ContentValues values = new ContentValues();
        values.put(EVENT_TITLE, title);
        values.put(EVENT_DATE, date);
        values.put(EVENT_TIME, time);
        values.put(EVENT_REMINDER, reminderEnabled ? 1 : 0);
        values.put(EVENT_CATEGORY, category);
        return values;
    }

    private Event readEvent(Cursor cursor) {
        return new Event(
                cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_REMINDER)) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_CATEGORY))
        );
    }
}
