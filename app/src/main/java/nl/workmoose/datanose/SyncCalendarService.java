package nl.workmoose.datanose;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
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
    private int notifId;
    private long calendarId = -1;
    private int agendaColor;
    private SharedPreferences sharedPref;

    public SyncCalendarService() {}

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
        // Create a new thread to run the syncing off the UI thread
        System.out.println("Flags: " + flags);
        notifId = 1;
        final Context context = this;

        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("isSyncing", true).apply();

        // Make new thread to run service in background, prevent the UI thread to freeze
        final Thread t = new Thread() {
            @Override
            public void run() {
                // Set nofitication to keep task runnen even when application is closed
                if (Build.VERSION.SDK_INT < 16) {
                    startForeground(notifId, new Notification.Builder(context).getNotification());
                } else {
                    startForeground(notifId, new Notification.Builder(context).build());
                }

                // Starts the syncing process
                startSync();
            }
        };
        t.start();

        return START_STICKY;
    }

    /**
     * Make an notification.builder object
     * @return: notification.builder object
     */
    private Notification.Builder buildNotification(){
        CharSequence title = getText(R.string.app_name);

        // Make new notification
        if (isVisible() && !sharedPref.getBoolean("refreshing", false)) {
            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText("Updating calendar")
                    .setSmallIcon(R.drawable.datanose_logo_notification_small);
        } else {
            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText("Deleting calendar")
                    .setSmallIcon(R.drawable.datanose_logo_notification_small);
        }
    }

    /**
     * Updates the notification
     * @param maxProgress: maximum progress
     * @param progress: the current progress
     */
    private void updateNotification(int maxProgress, int progress) {
        // This function updates the notification
        Notification.Builder notificationBuilder = buildNotification();
        notificationBuilder.setProgress(maxProgress, progress, false);

        Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = notificationBuilder.getNotification();
        } else {
            notification = notificationBuilder.build();
        }

        // Notify the user
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifId, notification);
    }

    private void setNotificationWaitForDownload() {
        CharSequence title = getText(R.string.app_name);
        Notification.Builder notificationBuilder;
        // Make new notification
        notificationBuilder = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(getText(R.string.wait_for_schedule))
                .setSmallIcon(R.drawable.datanose_logo_notification_small);

        // This function updates the notification
        notificationBuilder.setProgress(0, 100, true);

        Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = notificationBuilder.getNotification();
        } else {
            notification = notificationBuilder.build();
        }

        // Notify the user
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifId, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Auto generated
        return null;
    }

    /**
     * Start the syncing, parse the iCalendar file and set the events
     */
    private void startSync() {
        try {
            // Parse the file
            ParseIcs parseIcs = new ParseIcs(getApplicationContext());
            ArrayList<ArrayList<String>> eventList = parseIcs.readFile();

            if (readAllCalendars().contains(ACCOUNT_NAME)) {
                // Calendar already exsists, don't create new calendar
                System.out.println("Calendar already exsists, don't create another");

            } else {
                // Calendar does not already exsists
                System.out.println("Creating new calendar");

                // Create a new calendar
                createNewCalendar();
            }

            if (calendarId == -1) {
                calendarId = getCalendarId();
                agendaColor = getColor();
            }

            // A calendar is present at this point.
            // Set the events to the calendar.
            setEvents(eventList);

            System.out.println("Sync result: SYNC OK");
        } catch (Exception e) {

            // Something went wrong in setEvents()
            e.printStackTrace();
            System.out.println("Sync result: SYNC ERROR");
        }

        if (sharedPref.getBoolean("refreshing", false)) {
            // If this sync operation is called for refreshing

            // Change the entry for "refreshing" so that the next time it will skip this step
            sharedPref.edit().putBoolean("refreshing", false).apply();

            if (sharedPref.getBoolean("isDownloading", false)) {
                setNotificationWaitForDownload();
            }

            while (sharedPref.getBoolean("isDownloading", false)) {
                try{

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // This is necessary
                }
            }
            // Call the syncing process again, now it will read the new downloaded file
            startSync();
        }

        sharedPref.edit().putBoolean("isSyncing", false).apply();

        // Stop service, otherwise it will repeat itself
        stopSelf();
    }

    /**
     * Create a new calendar called "DataNose"
     */
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

    /**
     * Set the events to the calendar
     * @param eventList: all the events
     */
    private void setEvents(ArrayList<ArrayList<String>> eventList) {

        // Loop through data and update every event
        int count = 0;
        for (ArrayList<String> event : eventList) {
            count++;

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
            beginTime.set(Calendar.SECOND, 0);
            beginTime.set(Calendar.MILLISECOND, 0);
            long startMillis = beginTime.getTimeInMillis();

            // Calendar instance for end time
            Calendar endTime = Calendar.getInstance();
            endTime.set(endYear, endMonth, endDay, endHour, endMinute);
            endTime.setTimeZone(TimeZone.getTimeZone("UTC"));
            endTime.set(Calendar.SECOND, 0);
            endTime.set(Calendar.MILLISECOND, 0);
            long endMillis = endTime.getTimeInMillis();

            if (isVisible() && !sharedPref.getBoolean("refreshing", false)) {
                // Update or insert the event
                System.out.println("Adding/Updating event: " + count + " of " + eventList.size());
            } else {
                // System is removing the events
                System.out.println("Deleting event: " + count + " of " + eventList.size());
            }

            if (count % 10 == 0) {
                updateNotification(eventList.size(), count);
            }
            addEvent(startMillis, endMillis, title, location, description, id);
        }
    }

    /**
     * Add a single event using the given parameters
     * @param start: Start time
     * @param stop: Stop time
     * @param title: title of the event
     * @param location: Location of the event
     * @param description: Description of the event
     * @param id: Unique identifier of the event
     */
    private void addEvent(long start, long stop, String title, String location,
                             String description, long id) {
        // Vreate ContentResolver to put in values for the event
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();

        if (isVisible() && !sharedPref.getBoolean("refreshing", false)) {
            // Set event data
            values.put(CalendarContract.Events.DTSTART, start);
            values.put(CalendarContract.Events.DTEND, stop);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.CALENDAR_ID, getCalendarId());
            values.put(CalendarContract.Events.EVENT_COLOR, getColor());
            values.put(CalendarContract.Events.EVENT_LOCATION, location);
            values.put(CalendarContract.Events._ID, id);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Amsterdam");

        } else {
            // Remove the event
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
        try {
            // This try/catch is the way to go, don't fuck this up
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            long eventId = Long.parseLong(uri.getLastPathSegment());
        } catch (Exception e) {
            // The ID already exists, update the event
            Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            getContentResolver().update(updateUri, values, null, null);
        }
    }

    /**
     * Gets all the calendars registered on this device
     * @return: list of all the calendars
     */
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

    /**
     * Gets the id of the DataNose calendar
     * @return: id
     */
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
                    calCursor.close();
                    return id;
                }
            } while (calCursor.moveToNext());
        }
        calCursor.close();
        return -1;
    }

    /**
     * Returns the saved color for the agenda items
     * @return: int color
     */
    private int getColor() {
        // Returns the color value saved in sharedPreferences
        return sharedPref.getInt("agendaColor", getResources().getColor(R.color.green));
    }

    /**
     * returns whether the items should be invisible or visible
     * @return: boolean isVisible
     */
    private Boolean isVisible() {
        // Returns the boolean whether the user wants to sync his/her account
        return sharedPref.getBoolean("syncSaved", true);
    }
}
