package nl.workmoose.datanose.activity;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.gc.materialdesign.widgets.Dialog;
import com.gc.materialdesign.widgets.SnackBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nl.workmoose.datanose.DayPagerAdapter;
import nl.workmoose.datanose.DownloadIcs;
import nl.workmoose.datanose.ParseIcs;
import nl.workmoose.datanose.R;
import nl.workmoose.datanose.SyncCalendarService;
import nl.workmoose.datanose.SyncReceiver;
import nl.workmoose.datanose.WeekPagerAdapter;
import nl.workmoose.datanose.view.ListeningScrollView;

import static java.util.Calendar.DECEMBER;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Activity to hold the ViewPager in which the schedule is loaded.
 */
public class ScheduleActivity extends AppCompatActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int BEGIN_TIME = 0;
    private static final long REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // Refresh interval in milliseconds
    private static final long MAX_REFRESH_TIME = 1000 * 60; // 1 minute
    private static final long MAX_SYNC_TIME = 1000 * 60 * 5; // 5 minutes
    private final static long DP_HOUR_HEIGHT = 60; // Height of 1 hour in dp
    private final static int DP_HOUR_WIDTH = 50; // Width of the hour bar in dp
    public int academicYear;
    public boolean isAnyEventPressed = false;
    public ViewPager viewPager;
    private ActionBar actionBar;
    private Menu menu;
    private ArrayList<ArrayList<String>> eventList;
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

        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }

        // Parse the file downloaded
        eventList = ParseIcs.readFile(this);
        if (eventList.size() < 1) {
            new SnackBar(this, getResources().getString(R.string.empty_schedule)).show();
        }

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

        Log.i("ScheduleActivity", "In daylight saving: " + TimeZone.getDefault().inDaylightTime(new Date()));

        //Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setPageMargin(-1);  // Visual bug fix
        PagerAdapter pagerAdapter;

        if (sharedPref.getString("mode", "day").equalsIgnoreCase("day")) {
            pagerAdapter = new DayPagerAdapter(getSupportFragmentManager());
        } else {
            // Mode is week
            pagerAdapter = new WeekPagerAdapter((getSupportFragmentManager()));
            RelativeLayout sideContainer = (RelativeLayout) findViewById(R.id.side_container);
            ((ViewGroup) findViewById(R.id.pagerParent)).bringChildToFront(sideContainer);
            sideContainer.setVisibility(View.VISIBLE);
            if (actionBar != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    //noinspection deprecation
                    actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background_week));
                } else {
                    actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background_week, null));
                }
            }
        }
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(currentAcademicDay);

        setActivityTimeHolder();

        ListeningScrollView scrollView = (ListeningScrollView) findViewById(R.id.timeHolderScrollView);
        scrollView.setOnScrollChangedListener(new ListeningScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView view, int x, int y, int oldx, int oldy) {
                ((WeekPagerAdapter) viewPager.getAdapter()).scrollTo(y);
            }
        });
    }

    public void setTimeHolderScroll(int y) {
        findViewById(R.id.timeHolderScrollView).setScrollY(y);
    }

    private void setActivityTimeHolder() {
        // Get container for the times
        LinearLayout timeHolder = (LinearLayout) findViewById(R.id.activityTimeHolder);

        // Make textView's for the left container displaying the hours of the day (9:00, 10:00,..)
        for (int i = 8; i <= 23; i++) {
            // Create new TextView and set text
            TextView time = new TextView(scheduleActivity);
            time.setText(i + ":00");

            // Make layoutparams
            time.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(DP_HOUR_WIDTH),
                    dpToPx(DP_HOUR_HEIGHT)));

            // Add view to parent
            timeHolder.addView(time);
        }
    }

    /**
     * Calculate the academic day given a calendar object
     *
     * @param calendar: calendar object of day to calculate
     * @return int: integer that represents the academic day
     */
    private int calculateAcademicDay(Calendar calendar) {

        // Calculate academic week
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        // Get academic year
        academicYear = getAcademicYear();

        // Week 36 is the first week of the academic year
        if (week >= 32) {
            // The week is in the current academic year
            week -= 36;
        } else {
            // Calculate the academic week
            Calendar lastWeekOfYear = Calendar.getInstance();
            lastWeekOfYear.setFirstDayOfWeek(Calendar.MONDAY);
            lastWeekOfYear.setMinimalDaysInFirstWeek(4);
            lastWeekOfYear.set(academicYear, DECEMBER, 28);  // December 28th always is in the last week

            int totalWeeksInYear = lastWeekOfYear.get(Calendar.WEEK_OF_YEAR);
            week = week + (totalWeeksInYear - 36);
        }
        // 'week' is now the current academic week
        // 'year' is just the current year
        int currentDayInWeek = calendar.get(Calendar.DAY_OF_WEEK); // sun = 1, mon = 2, .., sat = 7
        currentDayInWeek -= 2;
        Log.i("ScheduleActivity", "Academic week: " + week);

        // Reformat the day
        if (currentDayInWeek < 0) {
            currentDayInWeek += 7;
        }  // mon = 0, tue = 1, .., sun = 6
        // If in week 32 - 36, week number is negative. Show first day of 36th week
        if (week < 0) {
            return 0;
        }
        return week * 7 + currentDayInWeek; // Day of the academic year, FINALLY :P
    }

    /**
     * Calculates the current academic year
     *
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

        // Week 36 is the first week of the academic year, but switch to next academic year
        // on week 32
        if (week < 32) {
            // The year we live in is not the academic year
            // OR we are in the same year, but in de first week of the next year (like 31 dec)
            if (rightNow.get(Calendar.DAY_OF_MONTH) < 15) {
                // If week is not in the current academic year
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
        CalendarDatePickerDialogFragment.OnDateSetListener dateSetListener = new CalendarDatePickerDialogFragment.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
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
        CalendarDatePickerDialogFragment dialog = new CalendarDatePickerDialogFragment();
        dialog.setThemeLight();

        // Set listener
        dialog.setOnDateSetListener(dateSetListener);

        // Monday is day nr. 2
        dialog.setFirstDayOfWeek(Calendar.MONDAY);

        // Set the year range
        dialog.setDateRange(new MonthAdapter.CalendarDay(academicYear, Calendar.JANUARY, 1),
                new MonthAdapter.CalendarDay(academicYear + 1, DECEMBER, 31));

        // Show dialog
        dialog.show(getSupportFragmentManager(), null);
    }

    /**
     * Returns the events that occur on the date given the time in milliseconds.
     * Loops through the whole events list
     *
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
     *
     * @param forceRefresh: Whether to force the app to refresh or not
     */
    public void checkForNewVersion(Context context, Boolean forceRefresh) {
        sharedPref = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);

        if (checkIfSyncing()) {
            Log.i("ScheduleActivity", "Already syncing");
            return;
        }
        if (!sharedPref.getBoolean("signedIn", false)) {
            Log.i("ScheduleActivity", "User signed out, don't do anything");
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
                    Log.i("ScheduleActivity", "Deleting items from calendar for sync...");
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
                        Log.i("ScheduleActivity", "Downloading new iCalendar file.");
                        DownloadIcs downloadIcs = new DownloadIcs(contextCopy);
                        downloadIcs.execute(studentId_copy);
                    }
                }, 500); // Half a second before run() is exucuted (500 milliseconds)

            } else {
                // The iCal is downloaded less than REFRESH_INTERVAL ago or it is forceRefresh == true
                Log.i("ScheduleActivity", "Don't download new iCal file.");
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
        this.menu = menu;
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
        paint.setTextSize(dpToPx(12));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);

        // Get the day of the month
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Draw on the image
        new Canvas(iconBitmap).drawText("" + dayOfMonth, iconBitmap.getWidth() / 2, (3/4f * iconBitmap.getHeight()), paint);
        Drawable newIcon = new BitmapDrawable(getResources(), iconBitmap);

        // Set the new icon to the button
        toTodayMenu.setIcon(newIcon);
    }

    public void setAlarm(Context context) {
        Log.i("ScheduleActivity", "Setting alarm");
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
            Log.i("ScheduleActivity", "The alarm has yet to go off");
        } else {
            // The alarm should have already gone off
            // change the nextAlarmTime
            Log.i("ScheduleActivity", "The alarm should have already gone off");
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setInexactRepeating(AlarmManager.RTC, nextAlarmTime,
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
            Log.i("ScheduleActivity", "Deleting items from calendar...");
            startService(new Intent(getApplicationContext(), SyncCalendarService.class));
        } else {
            Log.i("ScheduleActivity", "Agenda not synced, don't delete items.");
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
                Log.i("ScheduleActivity", "Syncing took too long! Resetting value in SharedPreferences");
                sharedPref.edit().putBoolean("isSyncing", false).apply();
                return false;
            } else {
                // The app was syncing not too long ago. It's probably alright
                Log.i("ScheduleActivity", "Last sync was " + timeSinceLastCheck / 1000 + " seconds ago.");
                return true;
            }
        }
    }

    /**
     * Checks if the app is refreshing. If the app IS refreshing but it has started too long ago,
     * something has gone wrong so this function corrects that as well.
     * <p>
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
                Log.i("ScheduleActivity", "Refreshing took too long! Resetting value in SharedPreferences");
                sharedPref.edit().putBoolean("refreshing", false).apply();
                return false;
            } else {
                // The app was syncing not too long ago. It's probably alright
                Log.i("ScheduleActivity", "Last refresh was " + timeSinceLastCheck / 1000 + " seconds ago.");
                return true;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
            case R.id.action_show_week:
                sharedPref.edit().putString("mode", "week").apply();
                RelativeLayout sideContainer = (RelativeLayout) findViewById(R.id.side_container);
                ((ViewGroup) findViewById(R.id.pagerParent)).bringChildToFront(sideContainer);
                sideContainer.setVisibility(View.VISIBLE);
                viewPager.invalidate();
                viewPager.requestLayout();

                // Switch to week view
                actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background_week));
                PagerAdapter WeekAdapter = new WeekPagerAdapter(getSupportFragmentManager());
                int current_item = viewPager.getCurrentItem();
                viewPager.setAdapter(WeekAdapter);
                viewPager.setCurrentItem(current_item);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        item.setVisible(false);  // Set this menu to invisible
                        menu.findItem(R.id.action_show_day).setVisible(true);
                    }
                }, 500);
                break;
            case R.id.action_show_day:
                sharedPref.edit().putString("mode", "day").apply();
                RelativeLayout mySideContainer = (RelativeLayout) findViewById(R.id.side_container);
                mySideContainer.setVisibility(View.GONE);
                viewPager.invalidate();
                viewPager.requestLayout();

                // Switch to day view
                actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background_day));
                PagerAdapter DayAdapter = new DayPagerAdapter(getSupportFragmentManager());
                int current_item_week = viewPager.getCurrentItem();
                viewPager.setAdapter(DayAdapter);
                viewPager.setCurrentItem(current_item_week);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        item.setVisible(false);  // Set this menu to invisible
                        menu.findItem(R.id.action_show_week).setVisible(true);
                    }
                }, 500);
                break;
            case R.id.sign_out:
                // Check if the system is syncing at the moment
                if (checkIfSyncing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_syncing)).show();
                } else if (checkIfRefreshing()) {
                    new SnackBar(this, getResources().getString(R.string.busy_refreshing)).show();
                } else {
                    // If the system is not syncing at the moment
                    // Ask the user is he/she really wants to sign out if calendar is synced
                    if (sharedPref.getBoolean("syncSaved", false)) {
                        askToSignOut();
                    } else {
                        signOut();
                    }
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
                ((ViewGroup) findViewById(R.id.pagerParent)).bringChildToFront(findViewById(R.id.refreshContainer));
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
     * Asks the user if he/she really wants to sign out
     * because the calendar is synced.
     */
    private void askToSignOut() {
        Log.i("ScheduleActivity", "Asking user corfirmation to sign out.");
        final Dialog dialog = new Dialog(this, getString(R.string.ask_sign_out_title),
                getString(R.string.ask_sign_out_question),
                getString(R.string.button_cancel),
                getString(R.string.sign_out));

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        // Set listener for the continue button
        dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // Sign out when the dismiss animation of the dialog is finished
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        signOut();
                    }
                }, 250);
            }
        });

        // Set listener for the cancel button
        dialog.setOnCancelButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
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
     *
     * @param dp: dp to convert into pixels
     * @return px: the size of dp converted into pixels
     */
    private int dpToPx(float dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (sharedPref.getString("mode", "day").equalsIgnoreCase("day")) {
            MenuItem showWeek = menu.findItem(R.id.action_show_week);
            showWeek.setVisible(true);
        } else {
            MenuItem showDay = menu.findItem(R.id.action_show_day);
            showDay.setVisible(true);
        }
        return true;
    }
}
