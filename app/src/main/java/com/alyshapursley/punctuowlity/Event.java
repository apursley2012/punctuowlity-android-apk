package com.alyshapursley.punctuowlity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Event {
    private static final String DATE_FORMAT = "MM/dd/yyyy";

    private final int id;
    private final String title;
    private final String date;
    private final String time;
    private final boolean reminderEnabled;
    private final String category;

    public Event(
            int id,
            String title,
            String date,
            String time,
            boolean reminderEnabled,
            String category
    ) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.reminderEnabled = reminderEnabled;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public String getCategory() {
        return category;
    }

    public String getDayOfWeek() {
        Date parsedDate = parseDate();
        if (parsedDate == null) {
            return "---";
        }
        return new SimpleDateFormat("EEE", Locale.US).format(parsedDate).toUpperCase(Locale.US);
    }

    public String getDateDay() {
        Date parsedDate = parseDate();
        if (parsedDate == null) {
            return "00";
        }
        return new SimpleDateFormat("dd", Locale.US).format(parsedDate);
    }

    private Date parseDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        formatter.setLenient(false);
        try {
            return formatter.parse(date);
        } catch (ParseException exception) {
            return null;
        }
    }
}
