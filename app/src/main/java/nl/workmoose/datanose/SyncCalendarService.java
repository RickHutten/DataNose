package nl.workmoose.datanose;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This file creates an calendar to write the events in. It only creates a calendar if there hasn't
 * one been created already. It loads the events into the calendar.
 * If a calendar already exsists, it updates your events
 */
public class SyncCalendarService extends Service {

    private static final String SHARED_PREF = "prefs";
    private static String ACCOUNT_NAME = "DataNose";
    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int TEACHER = 4;
    private final static int UID = 5;
    private Intent intent;

    public SyncCalendarService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a new thread to run the syncing off the UI thread
        final Thread t = new Thread() {
            @Override
            public void run() {
                // Starts the syncing process
                startSync();
            }
        };
        t.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Auto generated
        return null;
    }

    private void startSync() {
        try {
            ParseIcs parseIcs = new ParseIcs(getApplicationContext());
            ArrayList<ArrayList<String>> eventList = parseIcs.readFile();

            if (readAllCalendars().contains(ACCOUNT_NAME)) {

                // Calendar already exsists, don't create new calendar
                System.out.println("Calendar already exsists, don't create another");

                // If the calendar already exists, the events also do.
                // Update the calendar instead of inserting events
                setEvents(eventList, "update");

            } else {

                // Calendar does not already exsists
                System.out.println("Creating new calendar");

                // Create a new calendar and insert the events for the first time
                createNewCalendar();
                setEvents(eventList, "insert");
            }

            System.out.println("Sync result: SYNC OK");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Sync result: SYNC ERROR");
        }
        // Stop service
        stopSelf();
    }

    private void createNewCalendar() {

        // Create a new calendar values
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.NAME, ACCOUNT_NAME);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, ACCOUNT_NAME);
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, getResources().getColor(R.color.green));
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

        // Create calender builder values
        Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");

        // Make calendar
        getContentResolver().insert(builder.build(), values);
    }

    private void setEvents(ArrayList<ArrayList<String>> eventList, String mode) {

        // Loop through data and update every event
        int count = 0;
        for (ArrayList<String> event : eventList) {
            count++;
            if (mode.equals("insert")) {
                System.out.println("Inserting: " + count + " of " + eventList.size());
            } else if (mode.equals("update")) {
                System.out.println("Updating: " + count + " of " + eventList.size());
            }

            // Get values from event
            String begin = event.get(BEGIN_TIME);
            String end = event.get(END_TIME);
            String title = event.get(NAME);
            String location = event.get(LOCATION);
            String description = event.get(TEACHER);
            long id = Math.abs(Long.parseLong(event.get(UID)));

            // Get all the values for the calendar date
            int beginYear = Integer.parseInt(begin.substring(0, 4));
            int beginHour = Integer.parseInt(begin.substring(9, 11));
            int beginMinute = Integer.parseInt(begin.substring(11, 13));
            int beginMonth = Integer.parseInt(begin.substring(4, 6)) - 1;
            int beginDay = Integer.parseInt(begin.substring(6, 8));
            int endYear = Integer.parseInt(end.substring(0, 4));
            int endHour = Integer.parseInt(end.substring(9, 11));
            int endMinute = Integer.parseInt(end.substring(11, 13));
            int endMonth = Integer.parseInt(end.substring(4, 6)) - 1;
            int endDay = Integer.parseInt(end.substring(6, 8));

            // Calendar instance for begin time
            Calendar beginTime = Calendar.getInstance();
            beginTime.set(beginYear, beginMonth, beginDay, beginHour, beginMinute);
            beginTime.setTimeZone(TimeZone.getTimeZone("UTC"));
            long startMillis = beginTime.getTimeInMillis();

            // Calendar instance for end time
            Calendar endTime = Calendar.getInstance();
            endTime.set(endYear, endMonth, endDay, endHour, endMinute);
            endTime.setTimeZone(TimeZone.getTimeZone("UTC"));
            long endMillis = endTime.getTimeInMillis();

            if (mode.equals("insert")) {
                // Insert event
                insertEvent(startMillis, endMillis, title, location, description, id);
            } else if (mode.equals("update")) {
                // Insert event
                update(startMillis, endMillis, title, location, description, id);
            }
        }
    }

    private void insertEvent(long start, long stop, String title, String location,
                             String description, long id) {
        // Set event data
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, start);
        values.put(CalendarContract.Events.DTEND, stop);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, description);
        values.put(CalendarContract.Events.CALENDAR_ID, getCalendarId());
        values.put(CalendarContract.Events.EVENT_COLOR, getColor());
        values.put(CalendarContract.Events.EVENT_LOCATION, location);
        values.put(CalendarContract.Events._ID, id);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Amsterdam");

        // Insert to SQL database
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        // Get the event ID that is the last element in the Uri
        long eventId = Long.parseLong(uri.getLastPathSegment());

        // Check if everything is correct
        if (eventId != id) {
            System.out.println("De shit gaat niet goed: eventId, id: " + eventId + " " + id);
        }
    }

    public void update(long start, long stop, String title, String location,
                        String description, long id) {
        // Update the events with the current settings
        ContentValues values = new ContentValues();

        if (isVisible()) {
            // The new title for the event
            values.put(CalendarContract.Events.DTSTART, start);
            values.put(CalendarContract.Events.DTEND, stop);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.EVENT_COLOR, getColor());
            values.put(CalendarContract.Events.EVENT_LOCATION, location);
        } else {
            // I do not have the permission to set the visibility,
            // so that's why I set every value to null or -1
            // because now the event is not shown
            values.put(CalendarContract.Events.DTSTART, -1);
            values.put(CalendarContract.Events.DTEND, -1);
            values.put(CalendarContract.Events.TITLE, (String) null);
            values.put(CalendarContract.Events.DESCRIPTION, (String) null);
            values.put(CalendarContract.Events.EVENT_COLOR, -1);
            values.put(CalendarContract.Events.EVENT_LOCATION, (String) null);
        }
        Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        getContentResolver().update(updateUri, values, null, null);
    }

    private ArrayList<String> readAllCalendars() {
        // Returns an arraylist containing all the names of every calendar on this device
        ArrayList<String> calendars = new ArrayList<>();
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE};

        // Can't really write good comments because it's vague
        Cursor calCursor = getContentResolver().
                query(CalendarContract.Calendars.CONTENT_URI,
                        projection,
                        CalendarContract.Calendars.VISIBLE + " = 1 OR " +
                        CalendarContract.Calendars.VISIBLE + " = 0",
                        null,
                        CalendarContract.Calendars._ID + " ASC");
        if (calCursor.moveToFirst()) {
            do {
                String displayName = calCursor.getString(1);

                // Add calendar name to arraylist
                calendars.add(displayName);
            } while (calCursor.moveToNext());
        }
        calCursor.close();
        return calendars;
    }

    private long getCalendarId() {
        // Returns the calendar id
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE};

        // Can't really write good comments because it's vague
        Cursor calCursor = getContentResolver().
                query(CalendarContract.Calendars.CONTENT_URI,
                        projection,
                        CalendarContract.Calendars.VISIBLE + " = 1 OR " +
                        CalendarContract.Calendars.VISIBLE + " = 0",
                        null,
                        CalendarContract.Calendars._ID + " ASC");
        if (calCursor.moveToFirst()) {
            do {
                long id = calCursor.getLong(0);
                String displayName = calCursor.getString(1);
                if (displayName.equals(ACCOUNT_NAME)) {

                    // When we encounter the same name, it means that the calendar is already make
                    // and 'id' is the corresponding id
                    return id;
                }
            } while (calCursor.moveToNext());
        }
        calCursor.close();
        return -1;
    }

    private int getColor() {
        // Returns the color value saved in sharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getInt("agendaColor", getResources().getColor(R.color.green));
    }

    private Boolean isVisible() {
        // Returns the boolean whether the user wants to sync his/her account
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getBoolean("sync_saved", true);
    }
}
