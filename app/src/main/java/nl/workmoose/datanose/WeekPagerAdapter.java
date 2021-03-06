package nl.workmoose.datanose;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import nl.workmoose.datanose.fragment.WeekScheduleFragment;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * PagerAdapter for the ViewPager.
 */
public class WeekPagerAdapter extends FragmentStatePagerAdapter {

    private final ArrayList<WeekScheduleFragment> activeFragments = new ArrayList<>();
    private int scrollY = 0;

    public WeekPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void scrollTo(int y) {
        this.scrollY = y;
        for (WeekScheduleFragment fragment : activeFragments) {
            fragment.setScroll(y);
        }
    }

    public int getScrollY() {
        return scrollY;
    }

    public void register(WeekScheduleFragment fragment) {
        activeFragments.add(fragment);
    }

    public void unregister(WeekScheduleFragment fragment) {
        activeFragments.remove(fragment);
    }

    @Override
    public Fragment getItem(int position) {
        // Create new Fragment and give arguments
        WeekScheduleFragment scheduleFragment = new WeekScheduleFragment();

        // Set adapter
        scheduleFragment.setAdapter(this);
        // Give the fragment the position it is in
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        scheduleFragment.setArguments(bundle);
        return scheduleFragment;
    }

    @Override
    public int getCount() {
        // The number of days in a year
        return 364;
    }

    @Override
    public float getPageWidth(int position) {
        return 1 / 5f;
    }
}
