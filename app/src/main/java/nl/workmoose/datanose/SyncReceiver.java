package nl.workmoose.datanose;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class SyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            // Start downloading the iCal file again and refresh the agenda
            System.out.println("Syncing");
            ScheduleActivity scheduleActivity = new ScheduleActivity();
            scheduleActivity.checkForNewVersion(context, false);

        } else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm on boot
            System.out.println("Setting alarm on boot");
            ScheduleActivity scheduleActivity = new ScheduleActivity();
            scheduleActivity.setAlarm(context);
        }
    }
}
