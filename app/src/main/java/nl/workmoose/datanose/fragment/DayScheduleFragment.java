package nl.workmoose.datanose.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.MessageFormat;

import nl.workmoose.datanose.R;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Fragment containing the schedule. Every fragment represents a single day.
 */
public class DayScheduleFragment extends BaseFragment {

    private final static int DP_OFFSET = 38; // Offset for the scrollview

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.day_schedule_fragment, container, false);

        init(this);

        // Return the inflated view
        return rootView;
    }

    /**
     * Sets the empty schedule, like you would see if you are free today.
     * Times from 9 to 21 o'clock and lines next to the hours
     */
    @Override
    protected void setEmptySchedule() {
        // Get container for the times
        LinearLayout timeHolder = rootView.findViewById(R.id.timeHolder);

        // Make textView's for the left container displaying the hours of the day (9:00, 10:00,..)
        for (int i = 8; i <= 23; i++) {
            // Create new TextView and set text
            TextView time = new TextView(scheduleActivity);
            time.setText(MessageFormat.format("{0}:00", i));

            // Make layoutparams
            time.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(DP_HOUR_WIDTH),
                    dpToPx(DP_HOUR_HEIGHT)));

            // Add view to parent
            timeHolder.addView(time);
        }
        // Make horizontal lines to separate the hours
        for (int i = 0; i <= 15; i++) {
            // Make a new line
            View horizontalLine = new View(scheduleActivity);

            // Make layoutparams
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            lp.setMargins(0, dpToPx(DP_OFFSET + i * DP_HOUR_HEIGHT), 0, 0);

            // Set layoutparams and color
            horizontalLine.setLayoutParams(lp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                horizontalLine.setBackgroundColor(getResources().getColor(R.color.gray, null));
            } else {
                //noinspection deprecation
                horizontalLine.setBackgroundColor(getResources().getColor(R.color.gray));
            }

            // Add view to parent
            scheduleView.addView(horizontalLine);
        }
    }

    @Override
    protected int getDpOffset() {
        return DP_OFFSET;
    }
}