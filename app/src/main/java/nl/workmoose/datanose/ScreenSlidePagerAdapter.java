package nl.workmoose.datanose;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * PagerAdapter for the ViewPager.
 */
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    public ScreenSlidePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // Create new Fragment and give arguments
        ScheduleFragment scheduleFragment = new ScheduleFragment();

        // Give the fragment the position it is in
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        scheduleFragment.setArguments(bundle);
        return scheduleFragment;
    }

    @Override
    public int getCount() {
        // The number of days in a year
        return 365;
    }
}
