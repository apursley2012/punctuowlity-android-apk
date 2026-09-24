package com.alyshapursley.punctuowlity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventTest {
    @Test
    public void formatsValidEventDate() {
        Event event = new Event(1, "Birthday", "09/21/2026", "1:30 PM", true, "birthday");

        assertEquals("MON", event.getDayOfWeek());
        assertEquals("21", event.getDateDay());
    }

    @Test
    public void handlesInvalidEventDateWithoutCrashing() {
        Event event = new Event(1, "Invalid", "not-a-date", "1:30 PM", false, "general");

        assertEquals("---", event.getDayOfWeek());
        assertEquals("00", event.getDateDay());
    }
}
