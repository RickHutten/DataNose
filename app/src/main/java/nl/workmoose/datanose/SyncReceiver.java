package nl.workmoose.datanose;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class SyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            // Check for new version, called from alarm
            Log.i("SyncReceiver", "Check for new version");
            ScheduleActivity scheduleActivity = new ScheduleActivity();
            scheduleActivity.checkForNewVersion(context, false);
        } else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm on boot
            Log.i("SyncReceiver", "Setting alarm on boot");
            ScheduleActivity scheduleActivity = new ScheduleActivity();
            scheduleActivity.setAlarm(context);
        }
    }
}
