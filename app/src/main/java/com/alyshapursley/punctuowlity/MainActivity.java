package com.alyshapursley.punctuowlity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends AppCompatActivity {
    private static final String CATEGORY_ALL = "all";
    private static final String CATEGORY_BIRTHDAY = "birthday";
    private static final String CATEGORY_APPOINTMENT = "appointment";
    private static final String CATEGORY_TRIP = "trip";

    private DatabaseHelper databaseHelper;
    private GridLayout eventsGrid;
    private TextView emptyState;
    private EditText searchInput;
    private String activeCategory = CATEGORY_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getSharedPreferences(SmsReminderReceiver.PREFERENCES_NAME, MODE_PRIVATE)
                .getBoolean(LoginActivity.IS_LOGGED_IN, false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        findViewById(R.id.button_back).setOnClickListener(view -> finish());
        findViewById(R.id.button_menu).setOnClickListener(this::showAccountMenu);

        databaseHelper = new DatabaseHelper(this);
        eventsGrid = findViewById(R.id.events_grid);
        emptyState = findViewById(R.id.text_empty_state);
        searchInput = findViewById(R.id.edit_search);

        TextView dateHeading = findViewById(R.id.text_date_heading);
        dateHeading.setText(
                new SimpleDateFormat("MMMM yyyy", Locale.US).format(new Date()).toUpperCase(Locale.US)
        );

        findViewById(R.id.fab_add_event).setOnClickListener(
                view -> startActivity(new Intent(this, AddEventActivity.class))
        );

        configureSearch();
        configureCategoryTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void configureSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No action is required before the query changes.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                loadEvents();
            }

            @Override
            public void afterTextChanged(Editable text) {
                // Rendering is handled in onTextChanged.
            }
        });
    }

    private void configureCategoryTabs() {
        bindCategoryTab(R.id.tab_all, CATEGORY_ALL);
        bindCategoryTab(R.id.tab_birthdays, CATEGORY_BIRTHDAY);
        bindCategoryTab(R.id.tab_appointments, CATEGORY_APPOINTMENT);
        bindCategoryTab(R.id.tab_trips, CATEGORY_TRIP);
    }

    private void bindCategoryTab(int viewId, String category) {
        findViewById(viewId).setOnClickListener(view -> {
            activeCategory = category;
            loadEvents();
        });
    }

    private void loadEvents() {
        eventsGrid.removeAllViews();
        List<Event> events = databaseHelper.getAllEvents();
        events.sort(Comparator.comparingLong(this::eventSortTime));
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.US);
        int visibleCount = 0;

        for (Event event : events) {
            if (!matchesFilters(event, query)) {
                continue;
            }

            eventsGrid.addView(createEventCard(event));
            visibleCount++;
        }

        emptyState.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private long eventSortTime(Event event) {
        long timestamp = ReminderScheduler.parseTriggerTime(event.getDate(), event.getTime());
        return timestamp < 0 ? Long.MAX_VALUE : timestamp;
    }

    private boolean matchesFilters(Event event, String query) {
        boolean categoryMatches = CATEGORY_ALL.equals(activeCategory)
                || activeCategory.equals(event.getCategory());

        if (!categoryMatches) {
            return false;
        }

        if (query.isEmpty()) {
            return true;
        }

        String searchableText = (
                event.getTitle() + " " + event.getDate() + " " + event.getTime()
        ).toLowerCase(Locale.US);
        return searchableText.contains(query);
    }

    private CardView createEventCard(Event event) {
        CardView card = (CardView) getLayoutInflater().inflate(
                R.layout.event_card,
                eventsGrid,
                false
        );

        TextView dayText = card.findViewById(R.id.text_day);
        TextView dateText = card.findViewById(R.id.text_date);
        TextView titleText = card.findViewById(R.id.text_event_title);
        TextView timeText = card.findViewById(R.id.text_time);
        ImageView reminderIcon = card.findViewById(R.id.icon_alert_status);
        ImageButton editButton = card.findViewById(R.id.button_edit);
        ImageButton deleteButton = card.findViewById(R.id.button_delete);

        dayText.setText(event.getDayOfWeek());
        dateText.setText(event.getDateDay());
        titleText.setText(event.getTitle());
        timeText.setText(event.getTime());

        reminderIcon.setImageResource(
                event.isReminderEnabled() ? R.drawable.ic_alarm_on : R.drawable.ic_alarm_off
        );
        reminderIcon.setContentDescription(
                getString(event.isReminderEnabled() ? R.string.reminder_enabled : R.string.reminder_disabled)
        );

        editButton.setContentDescription(getString(R.string.edit_event_accessibility, event.getTitle()));
        deleteButton.setContentDescription(getString(R.string.delete_event_accessibility, event.getTitle()));

        editButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra(AddEventActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });

        deleteButton.setOnClickListener(view -> {
            ReminderScheduler.cancel(this, event.getId());
            if (databaseHelper.deleteEvent(event.getId())) {
                Toast.makeText(this, R.string.event_deleted, Toast.LENGTH_SHORT).show();
                loadEvents();
            } else {
                Toast.makeText(this, R.string.event_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });

        return card;
    }

    private void showAccountMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, R.string.my_account_settings, 0, R.string.my_account_settings);
        popupMenu.getMenu().add(0, R.string.log_out, 1, R.string.log_out);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.string.my_account_settings) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            if (item.getItemId() == R.string.log_out) {
                logOut();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void logOut() {
        getSharedPreferences(SmsReminderReceiver.PREFERENCES_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.IS_LOGGED_IN, false)
                .remove(LoginActivity.CURRENT_USERNAME)
                .apply();

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}
