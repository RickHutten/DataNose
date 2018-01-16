package nl.workmoose.datanose.fragment;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import nl.workmoose.datanose.R;
import nl.workmoose.datanose.activity.ScheduleActivity;
import nl.workmoose.datanose.view.EventView;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * BaseFragment containing the schedule. Every fragment represents a single day.
 */

abstract public class BaseFragment extends Fragment {

    final static int DP_HOUR_HEIGHT = 60; // Height of 1 hour in dp
    final static int DP_HOUR_WIDTH = 50; // Width of the hour bar in dp

    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int MAX_ITEMS = 6; // Max items at the same time per event

    private int position;
    private int academicYear;
    private ArrayList<ArrayList<String>> events;
    private long currentDayInMillis;

    ScheduleActivity scheduleActivity;
    RelativeLayout scheduleView;
    ViewGroup rootView;
    private Fragment childFragment;

    /**
     * Initialises all this that need to be done for all fragments extending BaseFragment
     *
     * @param childFragment the fragment that extends BaseFragment
     */
    void init(final Fragment childFragment) {
        this.childFragment = childFragment;

        Bundle bundle = getArguments();
        position = bundle.getInt("position"); // Also the current day of the academic year

        // Get the parent activity and academic year
        scheduleActivity = (ScheduleActivity) getActivity();
        academicYear = scheduleActivity.academicYear;

        // Get events for this page
        currentDayInMillis = calculateCurrentMillis();
        events = scheduleActivity.getEventsOnDate(currentDayInMillis);

        // Get scheduleView from the xml resource file
        scheduleView = (RelativeLayout) rootView.findViewById(R.id.scheduleView);
        scheduleView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Gets called after layout has been done but before display.
                if (Build.VERSION.SDK_INT < 16) {
                    // Depricated after SDK 16
                    //noinspection deprecation
                    scheduleView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    // New function for SDK >= 16
                    scheduleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                // This is called here because now the width and height of the
                // scheduleView are known. This is necessary to be able to draw
                // the events on the right position
                if (childFragment instanceof DayScheduleFragment) {
                    scrollTo();
                }
                drawEvents();
            }
        });

        // Set the date (in the top left corner)
        setTitleDate();

        // Set the left bar where the times are located
        // and adds horizontal lines every hour
        setEmptySchedule();
    }

    /**
     * Calculate the milliseconds representing this fragment
     *
     * @return long: time in millis of this fragment
     */
    private long calculateCurrentMillis() {
        // Make calendar instance of the first academic day
        Calendar firstAcademicDay = Calendar.getInstance();
        firstAcademicDay.setFirstDayOfWeek(Calendar.MONDAY);
        firstAcademicDay.setMinimalDaysInFirstWeek(4);
        firstAcademicDay.set(Calendar.YEAR, academicYear);
        firstAcademicDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        firstAcademicDay.set(Calendar.WEEK_OF_YEAR, 36);

        // Add days to first academic day depending on the position
        firstAcademicDay.add(Calendar.DAY_OF_MONTH, position);

        // Calculate the milliseconds of the current view
        return firstAcademicDay.getTimeInMillis();
    }

    /**
     * Scrolls to the first item of the day
     */
    private void scrollTo() {
        // If there is no event today
        if (events.size() == 0) {
            return;
        }
        // Get ScrollView
        ScrollView sv = (ScrollView) rootView.findViewById(R.id.scheduleScrollView);

        // Scroll to first event
        int hour = 100; // Yeah its not pretty, but it's the only thing that works that I can think of

        // Loop through every event and see which one is the first
        for (ArrayList<String> event : events) {
            int offSet = timeOffset();
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int beginHour = (int) Math.floor(beginTime / 100);
            if (beginHour < hour) {
                hour = beginHour;
            }
        }
        // Scroll to the first item
        sv.scrollTo(0, dpToPx((hour - 9) * DP_HOUR_HEIGHT));
    }

    /**
     * Convert UTC to local timezone, calculate the offSet correction for the timezone
     *
     * @return int: int hour offset * 100
     */
    private int timeOffset() {
        TimeZone thisTimeZone = TimeZone.getDefault();
        int offSet = thisTimeZone.getOffset(currentDayInMillis); // offSet is in milliseconds
        return (((offSet / 1000) / 60) / 60) * 100; // Here milliseconds is set to hours (*100 for formatting)
    }

    /**
     * Convert dp into pixels
     *
     * @param dp: value to convert into pixels
     * @return int: int of the number of pixels
     */
    int dpToPx(float dp) {
        if (!isAdded()) {
            // If fragment is not added to an activity, return dimension not converted
            // or the app may crash
            return (int) dp;
        }
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getActivity().getResources().getDisplayMetrics());
        return (int) px;
    }

    /**
     * Loops several times through the events to get an understanding
     * of the distribution of the events. Add all the events of today to the scheduleContainer.
     */
    private void drawEvents() {

        // Make list for every 5 minutes
        ArrayList<Integer> occupationList = new ArrayList<>(Collections.nCopies(16 * 12, 0));

        // Get timezone offset
        int offSet = timeOffset();

        // Making the occupationList
        // necessary for determining the width of the events
        ArrayList<ArrayList<String>> eventsThatDontFit = new ArrayList<>();
        for (ArrayList<String> event : events) {
            // Get begin and end time
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;

            int beginHour = (int) Math.floor(beginTime / 100);
            int endHour = (int) Math.ceil(endTime / 100);
            int beginMinute = beginTime % 100;
            int endMinute = endTime % 100;

            if (beginHour < 8 || endHour >= 23) {
                // Does not fit in schedule, skip event and delete it later
                eventsThatDontFit.add(event);
                continue;
            }

            for (int i = beginHour * 12 + beginMinute / 5; i < endHour * 12 + endMinute / 5; i++) {
                // Add 1 to every timeslot in the occupationList
                occupationList.set(i - 8 * 12, occupationList.get(i - 8 * 12) + 1);
            }
        }
        // Remove events that don't fit in the schedule
        events.removeAll(eventsThatDontFit);

        // Get the number of simultaneous events per event
        for (ArrayList<String> event : events) {
            // Get begin and end time
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;

            int beginHour = (int) Math.floor(beginTime / 100);
            int endHour = (int) Math.ceil(endTime / 100);
            int beginMinute = beginTime % 100;
            int endMinute = endTime % 100;
            int maxItems = 1;  // Max events per event

            // Set data to ArrayList
            for (int i = beginHour * 12 + beginMinute / 5; i < endHour * 12 + endMinute / 5; i++) {
                if (occupationList.get(i - 8 * 12) > maxItems) {
                    maxItems = occupationList.get(i - 8 * 12);
                }
            }
            // Add the number of events at the same time to the event
            event.add(Integer.toString(maxItems));
        }

        ArrayList<Boolean> columnOccupation;
        int column = 0; // The column we want to draw an event in

        // Draw the events to the scheduleView and delete an item if a spot is free to draw to
        // Try to draw as many items in the first column, then try the second (if there is one)
        // them the third, etc.
        while (events.size() != 0) {

            // Set the Boolean list for the current column
            columnOccupation = new ArrayList<>(Collections.nCopies(16 * 12, false));
            ArrayList<ArrayList<String>> removedEvents = new ArrayList<>();

            for (ArrayList<String> event : events) {
                // Check if the desired place is free
                int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
                int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;

                int beginHour = (int) Math.floor(beginTime / 100);
                int endHour = (int) Math.ceil(endTime / 100);
                int beginMinute = beginTime % 100;
                int endMinute = endTime % 100;

                // Set canPlaceEvent initially to true
                Boolean canPlaceEvent = true;

                // Check if place is already occupied
                for (int i = beginHour * 12 + beginMinute / 5; i < endHour * 12 + endMinute / 5; i++) {
                    if (columnOccupation.get(i - 8 * 12)) {
                        canPlaceEvent = false;
                        break;
                    }
                }
                // If we can place the event, draw it
                if (canPlaceEvent) {
                    // Set columnOccupation to false for the current event
                    for (int i = beginHour * 12 + beginMinute / 5; i < endHour * 12 + endMinute / 5; i++) {
                        columnOccupation.set(i - 8 * 12, true);
                    }

                    // Place the event
                    int length = (endHour * 60 + endMinute) - (beginHour * 60 + beginMinute);
                    int width = scheduleView.getWidth();
                    int itemWidth = width / Integer.parseInt(event.get(MAX_ITEMS));

                    // Create new EventView
                    EventView eventView = new EventView(scheduleActivity);

                    if (childFragment instanceof WeekScheduleFragment) {
                        try {
                            eventView.setFragment((WeekScheduleFragment) childFragment);
                        } catch (Error e) {
                            Log.w("WeekScheduleFragent", "Couldn't set fragment!");
                            e.printStackTrace();
                        }
                    }

                    // Set layoutparams
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                            itemWidth - dpToPx(5), dpToPx(length - 3));
                    lp.setMargins(itemWidth * column + dpToPx(2.5f),
                            dpToPx(1 + (getDpOffset() + (DP_HOUR_HEIGHT * (beginHour - 8)) + beginMinute)),
                            0, 0);

                    // Add layoutparams to EventView, and set data
                    eventView.setLayoutParams(lp);
                    eventView.setEventData(event, offSet);

                    // Add view to parent
                    scheduleView.addView(eventView);

                    eventView.setColor();

                    // Add to removed events
                    removedEvents.add(event);
                }
                // End of this event in the loop, check next event
            }
            // Remove the events that are drawn this pass in the while loop
            events.removeAll(removedEvents);
            removedEvents.clear();
            column++; // Go to the next column
        }
    }

    /**
     * Returns true or false whether this fragment represents today
     *
     * @return boolean: Boolean, true if today, false if not.
     */
    private Boolean today() {
        // The actual current date
        Calendar rightNow = Calendar.getInstance();
        rightNow.setFirstDayOfWeek(Calendar.MONDAY);
        rightNow.setMinimalDaysInFirstWeek(4);

        // The date of this page
        Calendar thisPage = Calendar.getInstance();
        thisPage.setFirstDayOfWeek(Calendar.MONDAY);
        thisPage.setMinimalDaysInFirstWeek(4);
        thisPage.setTimeInMillis(currentDayInMillis);

        // Return if the two are the same
        return rightNow.get(Calendar.DAY_OF_YEAR) == thisPage.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Sets the TextView's of the date (top left of the fragment)
     */
    private void setTitleDate() {
        // Set calendar
        Calendar thisPage = Calendar.getInstance();
        thisPage.setFirstDayOfWeek(Calendar.MONDAY);
        thisPage.setMinimalDaysInFirstWeek(4);
        thisPage.setTimeInMillis(currentDayInMillis);

        // Get TextViews for the title
        TextView monthView = (TextView) rootView.findViewById(R.id.month);
        TextView dayOfMonthView = (TextView) rootView.findViewById(R.id.dayOfMonth);
        TextView dayOfWeekView = (TextView) rootView.findViewById(R.id.dayOfWeek);

        // Get month, day of month and day of week in string format
        String dayOfMonth = "" + thisPage.get(Calendar.DAY_OF_MONTH);
        String[] month_array = getResources().getStringArray(R.array.months);
        String[] day_array = getResources().getStringArray(R.array.days);
        String month = month_array[thisPage.get(Calendar.MONTH)];
        String dayOfWeek = day_array[thisPage.get(Calendar.DAY_OF_WEEK) - 1];

        // Set the text
        monthView.setText(month);
        dayOfMonthView.setText(dayOfMonth);
        dayOfWeekView.setText(dayOfWeek);
    }

    /**
     * Draws the line at the current time
     */
    private void drawTimeLine() {
        // If this screen is not today, don't show line at current time
        if (!today()) {
            return;
        }

        if (childFragment instanceof WeekScheduleFragment) {
            // Color background of fragment
            RelativeLayout weekScheduleContainer = (RelativeLayout) rootView.findViewById(R.id.weekScheduleContainer);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                weekScheduleContainer.setBackgroundColor(getResources().getColor(R.color.light_gray, null));
            } else {
                //noinspection deprecation
                weekScheduleContainer.setBackgroundColor(getResources().getColor(R.color.light_gray));
            }
        }

        // It is today, make new calendar and get the current time
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (hour >= 23 || hour < 8) {
            // The timeLine falls out of the view
            return;
        }
        // Get the marginTop
        int lineMargin = (hour - 8) * DP_HOUR_HEIGHT + minute + getDpOffset();

        // Get the timeLine from resource xml
        RelativeLayout timeLine = (RelativeLayout) rootView.findViewById(R.id.timeLine);

        // Make layoutparams
        RelativeLayout.LayoutParams lpLine = (RelativeLayout.LayoutParams)timeLine.getLayoutParams();
        int timelineHeight = timeLine.getChildAt(0).getLayoutParams().height;
        lpLine.setMargins(lpLine.leftMargin, dpToPx(lineMargin)-timelineHeight/2, lpLine.rightMargin, lpLine.bottomMargin);

        // Set layoutparams and set visibility to VISIBLE
        timeLine.setLayoutParams(lpLine);
        timeLine.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Draw the timeLine at the current time
        drawTimeLine();
    }

    /**
     * Function that needs to be implemented by fragments extending BaseFragment
     *
     * @return dp size of the offset on top of the schedule
     */
    protected abstract int getDpOffset();

    /**
     * Sets the empty schedule, like you would see if you are free today.
     */
    protected abstract void setEmptySchedule();
}
