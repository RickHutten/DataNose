package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.gc.materialdesign.widgets.SnackBar;

import java.util.ArrayList;
import java.util.Calendar;


public class ScheduleActivity extends ActionBarActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int BEGIN_TIME = 0;

    private ViewPager viewPager;
    private ArrayList<ArrayList<String>> eventList;
    public int academicYear;
    private SharedPreferences sharedPref;
    private int currentAcademicDay;
    private ScheduleActivity scheduleActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        scheduleActivity = this;
        ParseIcs parseIcs = new ParseIcs(this);
        eventList = parseIcs.readFile();

        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("signed_in", true).apply();

        // Setup calendar
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setFirstDayOfWeek(Calendar.MONDAY);
        calendarNow.setMinimalDaysInFirstWeek(4);

        currentAcademicDay = calculateAcademicDay(calendarNow);

        //Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(currentAcademicDay);
    }

    private int calculateAcademicDay(Calendar calendar) {
        /*
         * http://nl.wikipedia.org/wiki/NEN_2772
         * De norm schrijft voor dat de eerste week van het jaar die week is die vier of meer dagen in dat jaar heeft.
         * Vuistregels om de weeknummering van een jaar te bepalen:
         * 4 januari valt altijd in week 1
         * 28 december valt altijd in de laatste week van het jaar
         */
        // Calculate academic week
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        academicYear = getAcademicYear();

        if (week >= 36) {
            // The week is in the current academic year
            week -= 36;
        } else {
            // Calculate the academic week
            Calendar lastWeekOfYear = Calendar.getInstance();
            lastWeekOfYear.setFirstDayOfWeek(Calendar.MONDAY);
            lastWeekOfYear.setMinimalDaysInFirstWeek(4);
            lastWeekOfYear.set(academicYear, Calendar.DECEMBER, 28);
            int totalWeeksInYear = lastWeekOfYear.get(Calendar.WEEK_OF_YEAR);
            week = week + (totalWeeksInYear - 36);
        }
        // 'week' is now the current academic week
        // 'year' is just the current year
        int currentDayInWeek = calendar.get(Calendar.DAY_OF_WEEK); // sun = 1, mon = 2, .., sat = 7
        currentDayInWeek -= 2;
        System.out.println("Academic week: " + week );
        if (currentDayInWeek < 0) { currentDayInWeek += 7; }  // mon = 0, tue = 2, .., sun = 6
        return week * 7 + currentDayInWeek; // Day of the academic year, FINALLY :P
    }

    private int getAcademicYear() {
        // Returns the academic year
        Calendar rightNow = Calendar.getInstance();
        rightNow.setFirstDayOfWeek(Calendar.MONDAY);
        rightNow.setMinimalDaysInFirstWeek(4);
        int week = rightNow.get(Calendar.WEEK_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR); // Current year
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

    private void showDatePickerDialog() {
        // Create date picker listener.
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

                if (date.before(firstAcademicDay) || date.after(lastAcademicDay)) {
                    // The date is not in the current academic year
                    new SnackBar(scheduleActivity, getResources().getString(R.string.date_not_in_year)).show();
                    return;
                }

                int academicDay = calculateAcademicDay(date);
                viewPager.setCurrentItem(academicDay, true);
            }
        };
        // Show date picker dialog.
        CalendarDatePickerDialog dialog = new CalendarDatePickerDialog();
        dialog.setOnDateSetListener(dateSetListener);
        dialog.setFirstDayOfWeek(2);
        dialog.setYearRange(academicYear, academicYear+1);

        dialog.show(getSupportFragmentManager(), null);
    }

    public ArrayList<ArrayList<String>> getEventsOnDate(long milliseconds) {
        ArrayList<ArrayList<String>> events = new ArrayList<>();

        // Set calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setTimeInMillis(milliseconds);

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
        for (ArrayList<String> event : eventList) {
            if (event.get(BEGIN_TIME).startsWith(yearStr + monthStr + dayStr)) {
                events.add(event);
            }
        }
        return events;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);
        MenuItem toTodayMenu = menu.findItem(R.id.to_today);
        Drawable iconDrawable = toTodayMenu.getIcon();
        Bitmap iconBitmap = ((BitmapDrawable) iconDrawable).getBitmap(); // Converts drawable into bitmap
        iconBitmap = iconBitmap.copy(Bitmap.Config.ARGB_4444, true);

        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.black));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dpToPx(30));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);

        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        new Canvas(iconBitmap).drawText(""+dayOfMonth, iconBitmap.getWidth()/2, (2*iconBitmap.getHeight())/3, paint);
        Drawable newIcon = new BitmapDrawable(getResources(), iconBitmap);
        toTodayMenu.setIcon(newIcon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.sign_out:
                // The user logs out, the events in the calendar have to be deleted
                sharedPref.edit().putBoolean("sync_saved", false).commit();
                sharedPref.edit().putInt("agendaColor", getResources().getColor(R.color.green)).commit();
                System.out.println("Deleting items from calendar...");
                startService(new Intent(getApplicationContext(), SyncCalendarService.class));

                sharedPref.edit().putBoolean("signed_in", false).commit();
                // Exit current activity, go back to LoginActivity
                this.finish();
                overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
                break;
            case R.id.to_date:
                showDatePickerDialog();
                break;
            case R.id.to_today:
                viewPager.setCurrentItem(currentAcademicDay, true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Exit the app:
        // This is done by restarting the first activity (LoginActivity)
        // with flag FLAG_ACTIVITY_CLEAR_TOP to kill all other activities.
        // LoginActivity calls 'finish()' when EXIT == true
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
    }

    private int dpToPx(float dp) {
        // Convert dp into pixels
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }

    /**
     * PagerAdapter for the ViewPager
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Create new Fragment and give arguments
            ScheduleFragment scheduleFragment = new ScheduleFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);

            scheduleFragment.setArguments(bundle);
            return scheduleFragment;
        }

        @Override
        public int getCount() {
            return 365;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy in ScheduleActivity");
    }
}
