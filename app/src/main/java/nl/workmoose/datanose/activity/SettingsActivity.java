package nl.workmoose.datanose.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import nl.workmoose.datanose.R;
import nl.workmoose.datanose.SyncCalendarService;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Activity where the user can set the settings of the synchronization
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int ANIMATION_DURATION = 500;

    private int agendaColor;
    private SharedPreferences sharedPref;
    private RelativeLayout fakeSnackBar;
    private Button syncNowButton;
    private CheckBox syncCheckBox;
    private Boolean sync_saved;
    private SettingsActivity activity;
    private Boolean settingsChanged = false;

    /**
     * Get views from resource xml and sets them to their current state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        activity = this;

        // Get views from layout
        fakeSnackBar = findViewById(R.id.fakeSnackBar);
        syncNowButton = findViewById(R.id.syncNowButton);
        syncCheckBox = findViewById(R.id.syncCheckBox);

        // Get saved settings
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        agendaColor = sharedPref.getInt("agendaColor", getResources().getColor(R.color.green, null));
        sync_saved = sharedPref.getBoolean("syncSaved", false);

        TextView studentIdView = findViewById(R.id.student_id);
        String studentId = sharedPref.getString("studentId", "");
        String message = String.format("%s %s", getResources().getText(R.string.settings_student_id), studentId);
        studentIdView.setText(message);

        // Set checkboxes to saved state
        syncCheckBox.setChecked(sync_saved);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0f);
        }

        // Set all the onClickListeners
        setOnClickListeners();
    }

    /**
     * Sets all the onClickListeners in the layout
     */
    private void setOnClickListeners() {
        syncNowButton.setOnClickListener(v -> {
            // Hide the snackbar and save the current settings
            hideFakeSnackBar();
            saveCurrentSettings();
        });

        syncCheckBox.setOnClickListener(v -> {
            boolean isChecked = syncCheckBox.isChecked();

            if (isChecked) {
                if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

                    String[] permissions = new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
                    requestPermissions(permissions, 1);
                }
            }

            // To determine if the snackbar should be showed
            if (isChecked != sync_saved) {
                showFakeSnackBar();
            } else {
                // Request to hide the snackbar, maybe the color has changed
                requestHideFakeSnackBar();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int i : grantResults) {
                if (i == PackageManager.PERMISSION_DENIED) {
                    syncCheckBox.setChecked(false);
                    syncCheckBox.callOnClick();
                }
            }
        }
    }

    /**
     * Shows the snackbar, translate it into the screen
     */
    private void showFakeSnackBar() {
        // Shows the fake SnackBar to ask the user to sync the calendar
        if (fakeSnackBar.getVisibility() == View.VISIBLE) {
            return;
        }
        // Make animation
        Animation translate = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1,
                Animation.RELATIVE_TO_SELF, 0);
        translate.setDuration(ANIMATION_DURATION);
        translate.setInterpolator(new AccelerateDecelerateInterpolator());

        // Set button enabled
        syncNowButton.setEnabled(true);

        // Set visibility and start animation
        fakeSnackBar.setVisibility(View.VISIBLE);
        fakeSnackBar.startAnimation(translate);
    }

    /**
     * Hides the snackbar, translate the snackbar out of the screen
     */
    private void hideFakeSnackBar() {
        // Make new animation
        Animation translate = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        translate.setDuration(ANIMATION_DURATION);

        // Set animation listener
        translate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Set visibility to INVISIBLE if the animation has ended
                fakeSnackBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        translate.setInterpolator(new AccelerateDecelerateInterpolator());

        // Set the button to disabled and start animation
        syncNowButton.setEnabled(false);
        fakeSnackBar.startAnimation(translate);
    }

    /**
     * Checks if the snackbar can be hidden or not.
     * Only hide the fakeSnackBar if all the values are the same as the saved ones
     */
    private void requestHideFakeSnackBar() {
        if (syncCheckBox.isChecked() != sync_saved) {
            // Not the same values
            return;
        }
        int savedColor;
        savedColor = sharedPref.getInt("agendaColor", getResources().getColor(R.color.green, null));
        if (agendaColor != savedColor) {
            // The colors are not the same
            if (!sync_saved && syncCheckBox.isChecked() == sync_saved) {
                // If you changed the color and you don't want to sync
                hideFakeSnackBar();
            }
            return;
        }
        // All the values are the same, hide fakeSnackBar
        hideFakeSnackBar();
    }

    /**
     * Set the values for this current setting
     */
    private void saveCurrentSettings() {
        sync_saved = syncCheckBox.isChecked();
        settingsChanged = true;

        // Save to sharedPreferences
        sharedPref.edit().putBoolean("syncSaved", sync_saved).apply();
        sharedPref.edit().putInt("agendaColor", agendaColor).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (settingsChanged) {
            // The settings have changed, sync the timetable with the current settings
            Log.i("SettingsActivity", "Sync calendar...");
            startService(new Intent(getApplicationContext(), SyncCalendarService.class));
            settingsChanged = false;
            // Quit from the activity. The user sould not be able to see this page while
            // the app is syncing.
            this.finish();
        }
    }
}
