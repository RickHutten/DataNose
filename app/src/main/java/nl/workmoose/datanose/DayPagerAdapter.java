package nl.workmoose.datanose;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import nl.workmoose.datanose.fragment.DayScheduleFragment;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * PagerAdapter for the ViewPager.
 */
public class DayPagerAdapter extends FragmentStatePagerAdapter {


    public DayPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // Create new Fragment and give arguments
        DayScheduleFragment scheduleFragment = new DayScheduleFragment();

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
}
