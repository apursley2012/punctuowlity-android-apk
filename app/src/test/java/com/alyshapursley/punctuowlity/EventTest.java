package com.alyshapursley.punctuowlity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventTest {
    @Test
    public void formatsValidEventDate() {
        Event event = new Event(
                1,
                "Birthday",
                "09/21/2026",
                "1:30 PM",
                true,
                "birthday"
        );

        assertEquals("MON", event.getDayOfWeek());
        assertEquals("21", event.getDateDay());
    }

    @Test
    public void handlesInvalidEventDateWithoutCrashing() {
        Event event = new Event(
                1,
                "Invalid",
                "not-a-date",
                "1:30 PM",
                false,
                "general"
        );

        assertEquals("---", event.getDayOfWeek());
        assertEquals("00", event.getDateDay());
    }

    @Test
    public void rejectsImpossibleCalendarDate() {
        Event event = new Event(
                1,
                "Invalid date",
                "02/30/2026",
                "1:30 PM",
                false,
                "general"
        );

        assertEquals("---", event.getDayOfWeek());
        assertEquals("00", event.getDateDay());
    }

    @Test
    public void preservesStoredEventValues() {
        Event event = new Event(
                7,
                "Appointment",
                "10/15/2026",
                "9:45 AM",
                true,
                "appointment"
        );

        assertEquals(7, event.getId());
        assertEquals("Appointment", event.getTitle());
        assertEquals("10/15/2026", event.getDate());
        assertEquals("9:45 AM", event.getTime());
        assertEquals(true, event.isReminderEnabled());
        assertEquals("appointment", event.getCategory());
    }
}
