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
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * This file creates an calendar to write the events in. It only creates a calendar if there hasn't
 * one been created already. It loads the events into the calendar.
 * If a calendar already exsists, it updates your events
 */
public class SyncCalendarService extends Service {

    private static final String SHARED_PREF = "prefs";
    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int TEACHER = 4;
    private final static int UID = 5;
    private static final String ACCOUNT_NAME = "DataNose";
    private final int notifId = 1;
    private long calendarId = -1;
    private int agendaColor;
    private SharedPreferences sharedPref;

    public SyncCalendarService() {
    }

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
        // Create a new thread to run the syncing off the UI thread
        Log.i("SyncCalendarService", "Flags: " + flags);
        final Context context = this;

        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("isSyncing", true).apply();
        sharedPref.edit().putLong("lastRefresh", Calendar.getInstance().getTimeInMillis()).apply();

        // Make new thread to run service in background, prevent the UI thread to freeze
        final Thread t = new Thread() {
            @Override
            public void run() {
                // Set nofitication to keep task runnen even when application is closed
                if (Build.VERSION.SDK_INT < JELLY_BEAN) {
                    //noinspection deprecation
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
     * Make an notification.builder object.
     *
     * @return The notification.builder object.
     */
    private Notification.Builder buildNotification() {
        CharSequence title = getText(R.string.app_name);

        // Make new notification
        if (isVisible() && !sharedPref.getBoolean("refreshing", false)) {
            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText("Updating calendar")
                    .setSmallIcon(R.drawable.datanose_logo_notification);
        } else {
            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText("Deleting calendar")
                    .setSmallIcon(R.drawable.datanose_logo_notification);
        }
    }

    /**
     * Updates the notification.
     *
     * @param maxProgress: maximum progress.
     * @param progress:    the current progress.
     */
    private void updateNotification(int maxProgress, int progress) {
        // This function updates the notification
        Notification.Builder notificationBuilder = buildNotification();
        notificationBuilder.setProgress(maxProgress, progress, false);

        Notification notification;
        if (Build.VERSION.SDK_INT < JELLY_BEAN) {
            //noinspection deprecation
            notification = notificationBuilder.getNotification();
        } else {
            notification = notificationBuilder.build();
        }

        // Notify the user
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifId, notification);
    }

    /**
     * Sets an notification if the calendar is deleted from the phone but the downloading is not
     * yet finished.
     */
    private void setNotificationWaitForDownload() {
        CharSequence title = getText(R.string.app_name);
        Notification.Builder notificationBuilder;
        // Make new notification
        notificationBuilder = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(getText(R.string.wait_for_schedule))
                .setSmallIcon(R.drawable.datanose_logo_notification);

        // This function updates the notification
        notificationBuilder.setProgress(0, 100, true);

        Notification notification;
        if (Build.VERSION.SDK_INT < JELLY_BEAN) {
            //noinspection deprecation
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
            ArrayList<ArrayList<String>> eventList = ParseIcs.readFile(getApplicationContext());

            if (readAllCalendars().contains(ACCOUNT_NAME)) {
                // Calendar already exsists, don't create new calendar
                Log.i("SyncCalendarService", "Calendar already exsists, don't create another");

            } else {
                // Calendar does not already exsists
                Log.i("SyncCalendarService", "Creating new calendar");

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

            Log.i("SyncCalendarService", "Sync result: SYNC OK");
        } catch (Exception e) {

            // Something went wrong in setEvents()
            e.printStackTrace();
            Log.i("SyncCalendarService", "Sync result: SYNC ERROR");
        }

        if (sharedPref.getBoolean("refreshing", false)) {
            // If this sync operation is called for refreshing

            // Change the entry for "refreshing" so that the next time it will skip this step
            sharedPref.edit().putBoolean("refreshing", false).apply();

            if (sharedPref.getBoolean("isDownloading", false)) {
                setNotificationWaitForDownload();
            }
            if (sharedPref.getBoolean("isDownloading", false)) {
                for (int i = 0; i < 30; i++) {
                    if (!sharedPref.getBoolean("isDownloading", false)) {
                        // If it stopped downloading
                        break;
                    }

                    // Sleep for max 30 seconds
                    try {
                        // Sleep for 1 second and check again
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // This is necessary
                    }
                }
            }
            // Call the syncing process again, now it will read the new downloaded file
            startSync();
        }

        // Tell the system that it is done syncing
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            values.put(CalendarContract.Calendars.CALENDAR_COLOR, getResources().getColor(R.color.green, null));
        } else {
            //noinspection deprecation
            values.put(CalendarContract.Calendars.CALENDAR_COLOR, getResources().getColor(R.color.green));
        }
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
     *
     * @param eventList: all the events
     */
    private void setEvents(ArrayList<ArrayList<String>> eventList) {

        // Loop through data and update every event
        int count = 0;
        for (ArrayList<String> event : eventList) {

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
                Log.i("SyncCalendarService", "Adding/Updating event: " + count + " of " + eventList.size());
            } else {
                // System is removing the events
                Log.i("SyncCalendarService", "Deleting event: " + count + " of " + eventList.size());
            }

            if (count % 5 == 0) {
                updateNotification(eventList.size(), count);
            }
            addEvent(startMillis, endMillis, title, location, description, id);
            count++;
        }
    }

    /**
     * Add a single event using the given parameters
     *
     * @param start:       Start time
     * @param stop:        Stop time
     * @param title:       title of the event
     * @param location:    Location of the event
     * @param description: Description of the event
     * @param id:          Unique identifier of the event
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
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.EVENT_COLOR, agendaColor);
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
            values.put(CalendarContract.Events.TITLE, "");
            values.put(CalendarContract.Events.DESCRIPTION, "");
            values.put(CalendarContract.Events.EVENT_COLOR, -1);
            values.put(CalendarContract.Events.EVENT_LOCATION, "");
        }
        try {
            // This try/catch is the way to go, don't fuck this up
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            long eventId = Long.parseLong(uri.getLastPathSegment()); // This can throw an exception
        } catch (Exception e) {
            // The ID already exists, update the event
            Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            getContentResolver().update(updateUri, values, null, null);
        }
    }

    /**
     * Gets all the calendars registered on this device
     *
     * @return calendars: list of all the calendars
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
     * Gets the id of the DataNose calendar.
     *
     * @return The id of the calendar. -1 if 'DataNose' calendar doesn't exist.
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
     * Returns the saved color for the agenda items.
     *
     * @return Returns the value of the color saved by the user.
     */
    private int getColor() {
        // Returns the color value saved in sharedPreferences
        return sharedPref.getInt("agendaColor", getResources().getColor(R.color.green));
    }

    /**
     * Returns whether the items should be invisible or visible.
     *
     * @return Return boolean if the agenda is visible or not.
     */
    private Boolean isVisible() {
        // Returns the boolean whether the user wants to sync his/her account
        return sharedPref.getBoolean("syncSaved", true);
    }
}
