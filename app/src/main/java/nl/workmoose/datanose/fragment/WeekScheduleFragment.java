package nl.workmoose.datanose.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import nl.workmoose.datanose.R;
import nl.workmoose.datanose.WeekPagerAdapter;
import nl.workmoose.datanose.activity.ScheduleActivity;
import nl.workmoose.datanose.view.ListeningScrollView;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Fragment containing the schedule. Every fragment represents a single day.
 */
public class WeekScheduleFragment extends BaseFragment {

    private final static int DP_OFFSET = 50; // Offset for the scrollview

    private WeekPagerAdapter adapter;
    private ListeningScrollView sv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.week_schedule_fragment, container, false);
        rootView.setClipChildren(false);
        rootView.setClipToPadding(false);

        init(this);

        sv = (ListeningScrollView) rootView.findViewById(R.id.scheduleScrollView);

        sv.setOnScrollChangedListener(new ListeningScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int y) {
                int scroll = sv.getScrollY(); //for verticalScrollView
                adapter.scrollTo(scroll);
                scheduleActivity.setTimeHolderScroll(scroll);
            }
        });

        // Return the inflated view
        return rootView;
    }

    @Override
    protected int getDpOffset() {
        return DP_OFFSET;
    }

    /**
     * Sets the empty schedule, like you would see if you are free today.
     * Times from 9 to 23 o'clock and lines next to the hours
     */
    @Override
    protected void setEmptySchedule() {
        // Make horizontal lines to separate the hours
        for (int i = 0; i <= 16; i++) {
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

    public void setAdapter(WeekPagerAdapter adapter) {
        this.adapter = adapter;
    }

    public void setScroll(int y) {
        sv.setScrollY(y);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set the scroll of the fragment: wait for it to load before setting scroll.
        if (rootView == null) {
            rootView = (ViewGroup) getView();
        }
        if (rootView == null) {
            throw new NullPointerException("rootView is null, wtf");
        }
        if (sv == null) {
            sv = (ListeningScrollView) rootView.findViewById(R.id.scheduleScrollView);
        }
        if (sv == null) {
            throw new NullPointerException("scrollView is null, wtf");
        }
        ViewTreeObserver vto = sv.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (sv != null && adapter != null) {
                    sv.setScrollY(adapter.getScrollY());
                } else if (sv == null) {
                    throw new NullPointerException("scrollView is null");
                } else if (adapter == null) {
                    throw new NullPointerException("adapter is null");
                }
            }
        });
        if (adapter == null) {
            adapter = (WeekPagerAdapter) ((ScheduleActivity) getActivity()).viewPager.getAdapter();
        }
        adapter.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.unregister(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (rootView == null) {
            rootView = (ViewGroup) getView();
        }

        if (rootView != null) {
            rootView.setClipChildren(false);
            rootView.setClipToPadding(false);
        }
    }
}