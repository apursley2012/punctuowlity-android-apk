package com.alyshapursley.punctuowlity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class AddEventActivity extends AppCompatActivity {
    static final String EXTRA_EVENT_ID = "event_id";

    private DatabaseHelper databaseHelper;
    private EditText titleInput;
    private EditText dateInput;
    private EditText timeInput;
    private Switch reminderSwitch;
    private Spinner categorySpinner;
    private int eventId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseHelper = new DatabaseHelper(this);
        titleInput = findViewById(R.id.edit_event_title);
        dateInput = findViewById(R.id.edit_event_date);
        timeInput = findViewById(R.id.edit_event_time);
        dateInput.setFocusable(false);
        dateInput.setOnClickListener(view -> showDatePicker());
        timeInput.setFocusable(false);
        timeInput.setOnClickListener(view -> showTimePicker());
        reminderSwitch = findViewById(R.id.switch_alert);
        categorySpinner = findViewById(R.id.spinner_category);
        Button saveButton = findViewById(R.id.button_save);

        findViewById(R.id.button_back).setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> saveEvent());

        loadEventForEditing();
    }

    private void loadEventForEditing() {
        eventId = getIntent().getIntExtra(EXTRA_EVENT_ID, -1);
        if (eventId == -1) {
            return;
        }

        Event event = databaseHelper.getEventById(eventId);
        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleInput.setText(event.getTitle());
        dateInput.setText(event.getDate());
        timeInput.setText(event.getTime());
        reminderSwitch.setChecked(event.isReminderEnabled());
        String[] categories = getResources().getStringArray(R.array.event_categories_values);
        for (int index = 0; index < categories.length; index++) {
            if (categories[index].equals(event.getCategory())) {
                categorySpinner.setSelection(index);
                break;
            }
        }
    }

    private void saveEvent() {
        String title = titleInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        boolean reminderEnabled = reminderSwitch.isChecked();
        String[] categoryValues = getResources().getStringArray(R.array.event_categories_values);
        String category = categoryValues[categorySpinner.getSelectedItemPosition()];

        if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        long eventTime = ReminderScheduler.parseTriggerTime(date, time);
        if (eventTime < 0) {
            Toast.makeText(
                    this,
                    "Use MM/DD/YYYY for the date and a time such as 1:30 PM",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (reminderEnabled && eventTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Reminder time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved;
        if (eventId == -1) {
            long insertedId = databaseHelper.insertEvent(title, date, time, reminderEnabled, category);
            saved = insertedId != -1;
            if (saved) {
                eventId = (int) insertedId;
            }
        } else {
            ReminderScheduler.cancel(this, eventId);
            saved = databaseHelper.updateEvent(eventId, title, date, time, reminderEnabled, category);
        }

        if (!saved) {
            Toast.makeText(this, "Unable to save event", Toast.LENGTH_SHORT).show();
            return;
        }

        Event savedEvent = databaseHelper.getEventById(eventId);
        if (savedEvent != null && reminderEnabled && !ReminderScheduler.schedule(this, savedEvent)) {
            Toast.makeText(
                    this,
                    "Event saved, but the reminder could not be scheduled",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    getIntent().hasExtra(EXTRA_EVENT_ID) ? "Event Updated" : "Event Added",
                    Toast.LENGTH_SHORT
            ).show();
        }

        finish();
    }

    private void showDatePicker() {
        Calendar calendar = calendarFromValue(dateInput.getText().toString(), "MM/dd/yyyy");
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    dateInput.setText(new SimpleDateFormat("MM/dd/yyyy", Locale.US)
                            .format(selected.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        Calendar calendar = calendarFromValue(timeInput.getText().toString(), "h:mm a");
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    timeInput.setText(new SimpleDateFormat("h:mm a", Locale.US)
                            .format(selected.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    private Calendar calendarFromValue(String value, String format) {
        Calendar calendar = Calendar.getInstance();
        if (value == null || value.trim().isEmpty()) {
            return calendar;
        }
        try {
            calendar.setTime(new SimpleDateFormat(format, Locale.US).parse(value.trim()));
        } catch (ParseException ignored) {
            // Existing invalid values fall back to the current date or time.
        }
        return calendar;
    }
}
