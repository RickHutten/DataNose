package nl.workmoose.datanose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * Fragment containing the schedule. Every fragment represents a single day.
 */
 public class ScheduleFragment extends Fragment {

    private final static long MILLIS_IN_DAY = 86400000; // Milliseconds in a day
    private final static int DP_OFFSET = 38; // Offset for the scrollview
    private final static long DP_HOUR_HEIGHT = 60; // Height of 1 hour in dp
    private final static int DP_HOUR_WIDTH = 50; // Width of the hour bar in dp
    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int MAX_ITEMS = 6; // Max items at the same time per event

    private int position;
    private int academicYear;
    private ArrayList<ArrayList<String>> events;
    private long currentDayInMillis;
    private ScheduleActivity scheduleActivity;
    private RelativeLayout scheduleView;
    private ViewGroup rootView;
    public EventView expandedEvent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.schedule_fragment, container, false);
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
            public void onGlobalLayout()
            {
                // Gets called after layout has been done but before display.
                if (Build.VERSION.SDK_INT < 16) {
                    // Depricated after SDK 16
                    scheduleView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    // New function for SDK >= 16
                    scheduleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                // This is called here because now the width and height of the
                // scheduleView are known. This is necessary to be able to draw
                // the events on the right position
                scrollTo();
                drawEvents();
            }
        });

        // Set the date (in the top left corner)
        setTitleDate();

        // Set the left bar where the times are located
        // and adds horizontal lines every hour
        setEmptySchedule();

        // Return the inflated view
        return rootView;
    }

    /**
     * Sets the empty schedule, like you would see if you are free today.
     * Times from 9 to 21 o'clock and lines next to the hours
     */
    private void setEmptySchedule() {
        // Get container for the times
        LinearLayout timeHolder = (LinearLayout) rootView.findViewById(R.id.timeHolder);

        // Make textView's for the left container displaying the hours of the day (9:00, 10:00,..)
        for (int i=8; i <= 21; i++) {
            // Create new TextView and set text
            TextView time = new TextView(scheduleActivity);
            time.setText(i + ":00");

            // Make layoutparams
            time.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(DP_HOUR_WIDTH),
                    dpToPx(DP_HOUR_HEIGHT)));

            // Add view to parent
            timeHolder.addView(time);
        }
        // Make horizontal lines to separate the hours
        for (int i=0; i <= 13; i++) {
            // Make a new line
            View horizontalLine = new View(scheduleActivity);

            // Make layoutparams
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            lp.setMargins(0, dpToPx(DP_OFFSET + i * DP_HOUR_HEIGHT), 0, 0);

            // Set layoutparams and color
            horizontalLine.setLayoutParams(lp);
            horizontalLine.setBackgroundColor(getResources().getColor(R.color.gray));

            // Add view to parent
            scheduleView.addView(horizontalLine);
        }
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
     * Loops several times through the events to get an understanding
     * of the distribution of the events. Add all the events of today to the scheduleContainer.
     */
    private void drawEvents() {

        // Make list to calculate the
        ArrayList<Integer> occupationList = new ArrayList<>(Collections.nCopies(12, 0));

        // Get timezone offset
        int offSet = timeOffset();

        // Making the occupationList
        // necessary for determining the width of the events
        for (ArrayList<String> event : events) {
            // Loop through every event
            // Get begin and end time
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;
            int beginHour = (int) Math.floor(beginTime / 100);
            int endHour = (int) Math.ceil(endTime / 100);

            // Set data to ArrayList
            for (int i = beginHour; i < endHour; i++) {
                occupationList.set(i - 9, occupationList.get(i - 9) + 1);
            }
        }
        // Count the number of events at the same time
        int globalMaxWidth = 1; // The maximum number of events at the same time of the whole day
        for (ArrayList<String> event : events) {
            // Loop through every event.. again
            // Get begin and end time
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;
            int beginHour = (int) Math.floor(beginTime / 100);
            int endHour = (int) Math.ceil(endTime / 100);
            int maxItems = 1;

            // Set data to ArrayList
            for (int i = beginHour; i < endHour; i++) {
                if (occupationList.get(i - 9) > maxItems) {
                    maxItems = occupationList.get(i - 9);
                    if (maxItems > globalMaxWidth) {
                        globalMaxWidth = maxItems;
                    }
                }
            }
            // Add the number of events at the same time to the event
            event.add("" + maxItems);
        }
        // Calculate the width of the items per hour
        ArrayList<Integer> widthList = new ArrayList<>(Collections.nCopies(12, 0));

        // Last loop through the events to calculate the final width of the items
        for (ArrayList<String> event : events) {
            // Get begin and end time
            int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
            int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;
            int beginHour = (int) Math.floor(beginTime / 100);
            int endHour = (int) Math.ceil(endTime / 100);

            // Make final ArrayList
            for (int i = beginHour; i < endHour; i++) {
                if (Integer.parseInt(event.get(MAX_ITEMS)) > widthList.get(i - 9)) {
                    widthList.set(i - 9, Integer.parseInt(event.get(MAX_ITEMS)));
                }
            }
        }

        ArrayList<Boolean> columnOccupation;
        int column = 0; // The column we want to draw an event in

        // Draw the events to the scheduleView and delete an item if a spot is free to draw to
        // Try to draw as many items in the first column, then try the second (if there is one)
        // them the third, etc.
        while (events.size() != 0) {

            // Set the Boolean list for the current column
            columnOccupation = new ArrayList<>(Collections.nCopies(12, false));
            ArrayList<ArrayList<String>> removedEvents = new ArrayList<>();

            for (ArrayList<String> event : events) {
                // Check if the disired place is free
                int beginTime = Integer.parseInt(event.get(BEGIN_TIME).substring(9, 13)) + offSet;
                int endTime = Integer.parseInt(event.get(END_TIME).substring(9, 13)) + offSet;
                int beginHour = (int) Math.floor(beginTime / 100);
                int endHour = (int) Math.ceil(endTime / 100);

                // Set canPlaceEvent initially to true
                Boolean canPlaceEvent = true;

                // Check if place is already occupied
                for (int i = beginHour; i < endHour; i++) {
                    if (columnOccupation.get(i - 9)){
                        canPlaceEvent = false;
                        break;
                    }
                }
                // If we can place the event, draw it
                if (canPlaceEvent) {
                    // Set columnOccupation to false for the current event
                    for (int i = beginHour; i < endHour; i++) {
                        columnOccupation.set(i - 9, true);
                    }

                    // Place the event
                    long beginMinute = beginTime - beginHour*100L;
                    long endMinute = endTime - endHour*100L;
                    long length = (endHour*60L + endMinute) - (beginHour * 60L + beginMinute);
                    float width = scheduleView.getWidth();
                    int itemWidth = (int) width / widthList.get(beginHour - 9);

                    // Create new EventView
                    EventView eventView = new EventView(scheduleActivity, this);

                    // Set layoutparams
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                            itemWidth - dpToPx(5), dpToPx(length - 3));
                    lp.setMargins(itemWidth * column + dpToPx(2.5f),
                            dpToPx(1 + (DP_OFFSET + (DP_HOUR_HEIGHT * (beginHour - 8)) + beginMinute)),
                            0, 0);

                    // Add layoutparams to EventView, and set data
                    eventView.setLayoutParams(lp);
                    eventView.setEventData(event, offSet);

                    // Add view to parent
                    scheduleView.addView(eventView);

                    // Add to removed events
                    removedEvents.add(event);
                }
                // End of this event in the loop
            }
            // Remove the events that are drawn this pass in the while loop
            events.removeAll(removedEvents);
            removedEvents.clear();
            column++; // Go to the next column
        }
    }

    /**
     * Convert UTC to local timezone, calculate the offSet correction for the timezone
     * @return: int hour offset * 100
     */
    private int timeOffset() {
        TimeZone thisTimeZone = TimeZone.getDefault();
        int offSet = thisTimeZone.getOffset(currentDayInMillis); // offSet is in milliseconds
        return (((offSet / 1000) / 60) / 60) * 100; // Here milliseconds is set to hours (*100 for formatting)
    }

    /**
     * Draws the line at the current time
     */
    private void drawTimeLine() {

        // If this screen is not today, don't show line at current time
        if (!today()) {
            return;
        }

        // It is today, make new calendar and get the current time
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (hour >= 21 || hour < 8) {
            // The timeLine falls out of the view
            return;
        }
        // Get the marginTop
        long lineMargin = (hour - 8) * DP_HOUR_HEIGHT + minute + DP_OFFSET;

        // Get the timeLine from resource xml
        View timeLine = rootView.findViewById(R.id.timeLine);

        // Make layoutparams
        RelativeLayout.LayoutParams lpLine = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLine.setMargins(dpToPx(-12), dpToPx(lineMargin - 6), 0, 0);

        // Set layoutparams and set visibility to VISIBLE
        timeLine.setLayoutParams(lpLine);
        timeLine.setVisibility(View.VISIBLE);
    }

    /**
     * Returns true or false whether this fragment represents today
     * @return: Boolean, true if today, false if not.
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
     * Calculate the milliseconds representing this fragment
     * @return: time in millis of this fragment
     */
    private long calculateCurrentMillis() {
        // Make calendar instance of the first academic day
        Calendar firstAcademicDay = Calendar.getInstance();
        firstAcademicDay.setFirstDayOfWeek(Calendar.MONDAY);
        firstAcademicDay.setMinimalDaysInFirstWeek(4);
        firstAcademicDay.set(Calendar.YEAR, academicYear);
        firstAcademicDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        firstAcademicDay.set(Calendar.WEEK_OF_YEAR, 36);

        // Calculate the milliseconds of the current view
        long millisFirstDay = firstAcademicDay.getTimeInMillis();
        return (millisFirstDay + position * MILLIS_IN_DAY);
    }

    /**
     * Convert dp into pixels
     * @param dp: value to convert into pixels
     * @return: int of the number of pixels
     */
    private int dpToPx(float dp) {
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
     * Gets called when DetailEventActivity is ended. Tell the touched eventview to animate back
     * @param requestCode: not used
     * @param resultCode: not used
     * @param data: not used
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        expandedEvent.animateBack();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Draw the timeLine at the current time
        drawTimeLine();
    }
}