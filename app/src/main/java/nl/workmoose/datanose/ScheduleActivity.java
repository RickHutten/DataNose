package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Calendar;


public class ScheduleActivity extends ActionBarActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int BEGIN_TIME = 0;
    private static final int END_TIME = 1;
    private static final int NAME = 2;
    private static final int LOCATION = 3;
    private static final int TEACHER = 4;
    private static final int UID = 5;


    private ViewPager viewPager;
    private ArrayList<ArrayList<String>> eventList;
    public int academicYear;
    private SharedPreferences sharedPref;
    private int currentAcademicDay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        ParseIcs parseIcs = new ParseIcs(this);
        eventList = (ArrayList<ArrayList<String>>) parseIcs.readFile();

        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("signed_in", true).apply();

        /*
         * http://nl.wikipedia.org/wiki/NEN_2772
         * De norm schrijft voor dat de eerste week van het jaar die week is die vier of meer dagen in dat jaar heeft.
         * Vuistregels om de weeknummering van een jaar te bepalen:
         * 1 februari valt altijd in week 5
         * 4 januari valt altijd in week 1
         * 28 december valt altijd in de laatste week van het jaar
         */

        // Setup calendar
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setFirstDayOfWeek(Calendar.MONDAY);
        calendarNow.setMinimalDaysInFirstWeek(4);
        //calendarNow.set(2015, Calendar.MAY, 24); // Testing
        long millis = calendarNow.getTimeInMillis();


        // Calculate academic week
        int week = calendarNow.get(Calendar.WEEK_OF_YEAR);
        final int year = calendarNow.get(Calendar.YEAR); // Current year
        academicYear = year; // Academic year, may change in code below

        if (week >= 36) {
            // The week is in the current academic year
            week -= 36;
        } else {
            // The year we live in is not the academic year
            // OR we are in the same year, but in de first week of the next (like 31 dec)
            if (calendarNow.get(Calendar.MONTH) != Calendar.DECEMBER) {
                // If week is in the new calendar year
                academicYear = year - 1;
            }
            // Calculate the academic week
            Calendar lastWeek = Calendar.getInstance();
            lastWeek.setFirstDayOfWeek(Calendar.MONDAY);
            lastWeek.setMinimalDaysInFirstWeek(4);
            lastWeek.set(academicYear, Calendar.DECEMBER, 28);
            int totalWeeksInYear = lastWeek.get(Calendar.WEEK_OF_YEAR);
            week = week + (totalWeeksInYear - 36);
        }
        // 'week' is now the current academic week
        // 'year' is just the current year
        int currentDayInWeek = calendarNow.get(Calendar.DAY_OF_WEEK); // sun = 1, mon = 2, .., sat = 7
        currentDayInWeek -= 2;

        if (currentDayInWeek < 0) { currentDayInWeek += 7; }  // mon = 0, tue = 2, .., sun = 6
        currentAcademicDay = week * 7 + currentDayInWeek; // Day of the academic year, FINALLY :P
        //Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(currentAcademicDay);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                break;
            case R.id.sign_out:
                sharedPref.edit().putBoolean("signed_in", false).commit();
                // Exit current activity, go back to LoginActivity
                this.finish();
                overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
                break;
            case R.id.to_today:
                viewPager.setCurrentItem(currentAcademicDay, true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

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
}
