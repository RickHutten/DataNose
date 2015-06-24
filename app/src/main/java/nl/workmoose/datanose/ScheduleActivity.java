package nl.workmoose.datanose;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.gc.materialdesign.widgets.SnackBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * Activity to hold the ViewPager in which the schedule is loaded.
 */
 public class ScheduleActivity extends ActionBarActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int BEGIN_TIME = 0;
    private static final long REFRESH_INTERVAL = 1000*60*60*24; // Refresh interval in milliseconds
    private static final long MAX_REFRESH_TIME = 1000*60*1; // 1 minute
    private static final long MAX_SYNC_TIME = 1000*60*5; // 5 minutes

    private ViewPager viewPager;
    private ArrayList<ArrayList<String>> eventList;
    public int academicYear;
    private SharedPreferences sharedPref;
    private int currentAcademicDay;
    private ScheduleActivity scheduleActivity;

    /**
     * Calls ParseIcs to parse the file. Than calculates the day of the year and sets
     * the ViewPager to the correct day.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        scheduleActivity = this;

        // Parse the file downloaded
        ParseIcs parseIcs = new ParseIcs(this);
        eventList = parseIcs.readFile();

        // Set that the user is signed in
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("signedIn", true).apply();

        // Setup calendar
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setFirstDayOfWeek(Calendar.MONDAY);
        calendarNow.setMinimalDaysInFirstWeek(4);

        // This line fixes a bug in the calendar API.
        // If this is not done, on sundays, the calendar will think it is NEXT week sunday
        calendarNow.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR));

        // Calculate current academic day
        currentAcademicDay = calculateAcademicDay(calendarNow);

        System.out.println("In daylight saving: " + TimeZone.getDefault().inDaylightTime( new Date() ));

        if (calendarNow.get(Calendar.HOUR_OF_DAY) == 0 && !TimeZone.getDefault().inDaylightTime(new Date())) {
            // This fixes another bug if the time is between 00:00 and 01:00 o'clock
            // when not in daylight saving
            currentAcademicDay++;
        }

        //Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(currentAcademicDay);

        checkForNewVersion(this, false);
    }

    /**
     * Calculate the academic dat given a calendar object
     * @param calendar: calendar object of day to calculate
     * @return int: integer that represents the academic day
     */
    private int calculateAcademicDay(Calendar calendar) {

        // Calculate academic week
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        // Get academic year
        academicYear = getAcademicYear();

        // Week 36 is the first week of the academic year
        if (week >= 36) {
            // The week is in the current academic year
            week -= 36;
        } else {
            // Calculate the academic week
            Calendar lastWeekOfYear = Calendar.getInstance();
            lastWeekOfYear.setFirstDayOfWeek(Calendar.MONDAY);
            lastWeekOfYear.setMinimalDaysInFirstWeek(4);

            // December 28th always is in the last week
            lastWeekOfYear.set(academicYear, Calendar.DECEMBER, 28);
            int totalWeeksInYear = lastWeekOfYear.get(Calendar.WEEK_OF_YEAR);
            week = week + (totalWeeksInYear - 36);
        }
        // 'week' is now the current academic week
        // 'year' is just the current year
        int currentDayInWeek = calendar.get(Calendar.DAY_OF_WEEK); // sun = 1, mon = 2, .., sat = 7
        currentDayInWeek -= 2;
        System.out.println("Academic week: " + week );

        // Reformat the day
        if (currentDayInWeek < 0) { currentDayInWeek += 7; }  // mon = 0, tue = 2, .., sun = 6
        return week * 7 + currentDayInWeek; // Day of the academic year, FINALLY :P
    }

    /**
     * Calculates the current academic year
     * @return int: int representing the academic year
     */
    private int getAcademicYear() {

        // Create calendar object
        Calendar rightNow = Calendar.getInstance();
        rightNow.setFirstDayOfWeek(Calendar.MONDAY);
        rightNow.setMinimalDaysInFirstWeek(4);

        // Get current week
        int week = rightNow.get(Calendar.WEEK_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR); // Current year

        // Week 36 is the first week of the academic year
        if (week < 36) {
            // The year we live in is not the academic year
            // OR we are in the same year, but in de first week of the next (like 31 dec)
            if (rightNow.get(Calendar.MONTH) != Calendar.DECEMBER) {
                // If week is in the new calendar year
                year -= 1;
            }
        }
        return year;
    }

    /**
     * Shows the DatePickerDialog. Called when the user presses the
     * corresponding item in the actionbar
     */
    private void showDatePickerDialog() {

        // Create date picker listener
        CalendarDatePickerDialog.OnDateSetListener dateSetListener = new CalendarDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
                // Set date from user input.
                Calendar date = Calendar.getInstance();
                date.setFirstDayOfWeek(Calendar.MONDAY);
                date.setMinimalDaysInFirstWeek(4);
                date.set(Calendar.YEAR, year);
                date.set(Calendar.MONTH, monthOfYear);
                date.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                // Check if date is in this academic year:
                // Make calendar instance of the first academic day
                Calendar firstAcademicDay = Calendar.getInstance();
                firstAcademicDay.setFirstDayOfWeek(Calendar.MONDAY);
                firstAcademicDay.setMinimalDaysInFirstWeek(4);
                firstAcademicDay.set(Calendar.YEAR, academicYear);
                firstAcademicDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                firstAcademicDay.set(Calendar.WEEK_OF_YEAR, 36);

                // Make calendar instance of the last academix day
                Calendar lastAcademicDay = Calendar.getInstance();
                lastAcademicDay.setFirstDayOfWeek(Calendar.MONDAY);
                lastAcademicDay.setMinimalDaysInFirstWeek(4);
                lastAcademicDay.set(Calendar.YEAR, academicYear);
                lastAcademicDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                lastAcademicDay.set(Calendar.WEEK_OF_YEAR, 36);
                lastAcademicDay.add(Calendar.DAY_OF_YEAR, 363);

                // The date is not in the current academic year
                if (date.before(firstAcademicDay) || date.after(lastAcademicDay)) {
                    new SnackBar(scheduleActivity, getResources().getString(R.string.date_not_in_year)).show();
                    return;
                }

                // Set the ViewPager to the selected day
                int academicDay = calculateAcademicDay(date);
                viewPager.setCurrentItem(academicDay, true);
            }
        };

        // Show date picker dialog.
        CalendarDatePickerDialog dialog = new CalendarDatePickerDialog();

        // Set listener
        dialog.setOnDateSetListener(dateSetListener);

        // Monday is day nr. 2
        dialog.setFirstDayOfWeek(2);

        // Set the year range
        dialog.setYearRange(academicYear, academicYear+1);

        // Show dialog
        dialog.show(getSupportFragmentManager(), null);
    }

    /**
     * Returns the events that occur on the date given the time in milliseconds.
     * Loops through the whole events list
     * @param milliseconds: date given in milliseconds of where you want the events from
     * @return ArrayList: ArrayList containing the events on the given date
     */
    public ArrayList<ArrayList<String>> getEventsOnDate(long milliseconds) {
        ArrayList<ArrayList<String>> events = new ArrayList<>();

        // Set calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setTimeInMillis(milliseconds);

        // Get values from calendar
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Januari is 0
        int year = calendar.get(Calendar.YEAR);
        String dayStr = "" + day;
        String monthStr = "" + month;
        String yearStr = "" + year;

        // Formatting date
        if (day < 10) {
            dayStr = "0" + dayStr;
        }
        if (month < 10) {
            monthStr = "0" + monthStr;
        }

        // Loop through the eventList to check if events have the same date
        // if so, add to the ArrayList events, which is then returned
        for (ArrayList<String> event : eventList) {
            if (event.get(BEGIN_TIME).startsWith(yearStr + monthStr + dayStr)) {
                events.add(event);
            }
        }
        // Return the list containing the events on the given date
        return events;
    }

    /**
     * Checks if the app needs to refresh and refreshes accordingly by calling DownloadIcs.execute()
     * @param forceRefresh: Whether to force the app to refresh or not
     */
    public void checkForNewVersion(Context context, Boolean forceRefresh) {
        sharedPref = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);

        if (checkIfSyncing()) {
            System.out.println("Already syncing");
            return;
        }
        if (!sharedPref.getBoolean("signedIn", false)) {
            System.out.println("User signed out, don't do anything");
            return;
        }

        sharedPref.edit().putBoolean("refreshing", true).apply();
        if (sharedPref.contains("lastDownloaded")) {
            // Get last saved iCal date
            long lastDownloaded = sharedPref.getLong("lastDownloaded", 0);

            // Get the current time in millis
            long nowMillis = Calendar.getInstance().getTimeInMillis();

            if (nowMillis - lastDownloaded > REFRESH_INTERVAL || forceRefresh) {
                // Refresh if the iCal is downloaded more than REFRESH_INTERVAL ago or forceRefresh
                sharedPref.edit().putLong("lastRefresh", Calendar.getInstance().getTimeInMillis()).apply();

                // Get the current signed in student ID
                String studentId = sharedPref.getString("studentId", "");

                try {
                    // Get the refreshing container
                    View refreshingContainer = findViewById(R.id.refreshContainer);
                    refreshingContainer.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    // When called from the receiver
                }

                if (sharedPref.getBoolean("syncSaved", false)) {
                    // Firstly, remove the old items in the current calendar
                    // and set the events for the new iCal file
                    System.out.println("Deleting items from calendar for sync...");
                    context.startService(new Intent(context, SyncCalendarService.class));
                }

                // Make final variables needed for the handler
                final String studentId_copy = studentId;
                final Context contextCopy = context;

                // Download the iCalendar file again. DownloadIcs will create
                // a new scheduleActivity. But wait like half a second to give
                // the parseIcs some time to finish before downloading the new file
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Downloading new iCalendar file.");
                        DownloadIcs downloadIcs = new DownloadIcs(contextCopy);
                        downloadIcs.execute(studentId_copy);
                    }
                }, 500); // Half a second before run() is exucuted (500 milliseconds)

            } else {
                // The iCal is downloaded less than REFRESH_INTERVAL ago or it is forceRefresh == true
                System.out.println("Don't download new iCal file.");
                sharedPref.edit().putBoolean("refreshing", false).apply();
            }
        } else {
            // If sharedPreferences doesn't contain "lastDownloaded",
            // but this isn't ever supposed to happen.
            sharedPref.edit().putBoolean("refreshing", false).apply();
        }
    }

    /**
     * Alters a drawable so that an icon in the actionbar shows the current day of the month
     * as a number in the center of the image
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);

        // Set the correct number in the today menu button
        setTodayButton(menu);

        return true;
    }

    private void setTodayButton(Menu menu) {
        // Get menu item that needs to be edited
        MenuItem toTodayMenu = menu.findItem(R.id.to_today);
        Drawable iconDrawable = toTodayMenu.getIcon();
        Bitmap iconBitmap = ((BitmapDrawable) iconDrawable).getBitmap(); // Converts drawable into bitmap
        iconBitmap = iconBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Set paint for the text
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.black));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dpToPx(30));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);

        // Get the day of the month
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Draw on the image
        new Canvas(iconBitmap).drawText(""+dayOfMonth, iconBitmap.getWidth()/2, (2*iconBitmap.getHeight())/3, paint);
        Drawable newIcon = new BitmapDrawable(getResources(), iconBitmap);

        // Set the new icon to the button
        toTodayMenu.setIcon(newIcon);
    }

    public void setAlarm(Context context) {
        System.out.println("Setting alarm");
        // Set the alarm to start at approx 14:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long repeatTime = AlarmManager.INTERVAL_DAY;

        long timeNow = System.currentTimeMillis();
        long nextAlarmTime = calendar.getTimeInMillis();
        if (timeNow < nextAlarmTime) {
            // The alarm has yet to go off
            // No need to change the alarm time
            System.out.println("The alarm has yet to go off");
        } else {
            // The alarm should have already gone off
            // change the nextAlarmTime
            System.out.println("The alarm should have already gone off");
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setRepeating(AlarmManager.RTC, nextAlarmTime,
                repeatTime, alarmIntent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Set the alarm
        setAlarm(this);
    }

    /**
     * The user logs out, the events in the calendar have to be deleted
     */
    private void signOut() {
        Boolean synced = sharedPref.getBoolean("syncSaved", false);

        if (synced) {
            // Delete the items from the users agenda
            sharedPref.edit().putBoolean("syncSaved", false).apply();
            System.out.println("Deleting items from calendar...");
            startService(new Intent(getApplicationContext(), SyncCalendarService.class));
        } else {
            System.out.println("Agenda not synced, don't delete items.");
        }

        sharedPref.edit().putInt("agendaColor", getResources().getColor(R.color.green)).apply();
        sharedPref.edit().putBoolean("signedIn", false).apply();
        // Exit current activity, go back to LoginActivity
        this.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
    }

    /**
     * Checks if the app is syncing. If the app IS syncing but the sync has started too long ago,
     * something has gone wrong so this function corrects that as well.
     *
     * @return Returns boolean whether the app is syncing or not.
     */
    private boolean checkIfSyncing() {
        if (!sharedPref.getBoolean("isSyncing", false)) {
            // The app is not syncing
            return false;
        } else {
            // The app thinks it's syncing but it might not be
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeSinceLastCheck = timeNow - sharedPref.getLong("lastRefresh", timeNow);
            if (timeSinceLastCheck > MAX_SYNC_TIME) {
                // The app was syncing quite a while ago. Something is wrong.
                // Put the values back
                System.out.println("Syncing took too long! Resetting value in SharedPreferences");
                sharedPref.edit().putBoolean("isSyncing", false).apply();
                return false;
            } else {
                // The app was syncing not too long ago. It's probably alright
                System.out.println("Last sync was " + timeSinceLastCheck/1000 + " seconds ago.");
                return true;
            }
        }
    }

    /**
     * Checks if the app is refreshing. If the app IS refreshing but it has started too long ago,
     * something has gone wrong so this function corrects that as well.
     *
     * Note: If the users has synced the agenda, the refreshing is surpressed by isSyncing. But
     * if the user has not synced the agenda, this is the only check if the user is refreshing.
     *
     * @return Returns boolean whether the app is refreshing or not.
     */
    private boolean checkIfRefreshing() {
        if (!sharedPref.getBoolean("refreshing", false)) {
            // The app is not syncing
            return false;
        } else {
            // The app thinks it's syncing but it might not be
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeSinceLastCheck = timeNow - sharedPref.getLong("lastRefresh", timeNow);
            if (timeSinceLastCheck > MAX_REFRESH_TIME) {
                // The app was syncing quite a while ago. Something is wrong.
                // Put the values back
                System.out.println("Refreshing took too long! Resetting value in SharedPreferences");
                sharedPref.edit().putBoolean("refreshing", false).apply();
                return false;
            } else {
                // The app was syncing not too long ago. It's probably alright
                System.out.println("Last refresh was " + timeSinceLastCheck/1000 + " seconds ago.");
                return true;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Check if the system is syncing at the moment
                if (checkIfSyncing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_syncing)).show();
                } else if (checkIfRefreshing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_refreshing)).show();
                } else {
                    // If the system is not syncing at the moment, start SettingsActivity
                    Intent i = new Intent(this, SettingsActivity.class);
                    startActivity(i);
                }
                break;
            case R.id.sign_out:
                // Check if the system is syncing at the moment
                if (checkIfSyncing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_syncing)).show();
                } else if (checkIfRefreshing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_refreshing)).show();
                } else {
                    // If the system is not syncing at the moment, sign out
                    signOut();
                }
                break;
            case R.id.to_date:
                // Show the DatePickerDialog
                showDatePickerDialog();
                break;
            case R.id.to_today:
                // Set the ViewPager to today
                viewPager.setCurrentItem(currentAcademicDay, true);
                break;
            case R.id.refresh:
                // Check if the system is syncing at the moment
                if (checkIfSyncing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_syncing)).show();
                } else if (checkIfRefreshing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_refreshing)).show();
                } else {
                    // If the system is not syncing at the moment, force refresh the iCal file
                    checkForNewVersion(this, true);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Exit the app:
     * This is done by restarting the first activity (LoginActivity)
     * with flag FLAG_ACTIVITY_CLEAR_TOP to kill all other activities.
     * LoginActivity calls 'finish()' when EXIT == true
     */
    @Override
    public void onBackPressed() {
        // Set the event and start activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
    }

    /**
     * Refreshes if the application is resumed
     */
    @Override
    public void onResume() {
        super.onResume();
        checkForNewVersion(this, false);
    }

    /**
     * Convert the size of dp in pixels
     * @param dp: dp to convert into pixels
     * @return px: the size of dp converted into pixels
     */
    private int dpToPx(float dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }
}
