package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.CheckBox;
import com.gc.materialdesign.widgets.ColorSelector;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * Activity where the user can set the settings of the synchronization
 */
 public class SettingsActivity extends ActionBarActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int ANIMATION_DURATION = 500;

    private int agendaColor;
    private ButtonRectangle colorButton;
    private SharedPreferences sharedPref;
    private RelativeLayout fakeSnackBar;
    private ButtonFlat syncNowButton;
    private CheckBox syncCheckBox;
    private Boolean sync_saved;
    private Boolean settingsChanged = false;

    /**
     * Get views from resource xml and sets them to their current state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Get views from layout
        fakeSnackBar = (RelativeLayout) findViewById(R.id.fakeSnackBar);
        colorButton = (ButtonRectangle) findViewById(R.id.colorButton);
        syncNowButton = (ButtonFlat) findViewById(R.id.syncNowButton);
        syncCheckBox = (CheckBox) findViewById(R.id.syncCheckBox);

        // Get saved settings
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        agendaColor = sharedPref.getInt("agendaColor", getResources().getColor(R.color.green));
        sync_saved = sharedPref.getBoolean("syncSaved", false);

        // Set checkboxes to saved state
        syncCheckBox.setChecked(sync_saved);

        // Set the color background of the colorButton
        colorButton.setBackgroundColor(agendaColor);

        // Set the color of the text
        setButtonTextColor();

        // Set all the onClickListeners
        setOnClickListeners();
    }

    /**
     * Sets all the onClickListeners in the layout
     */
    private void setOnClickListeners() {
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the color picker so the user can select a color for the calendar items
                showColorPicker();
            }
        });

        syncNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide the snackbar and save the current settings
                hideFakeSnackBar();
                saveCurrentSettings();
            }
        });

        syncCheckBox.setOncheckListener(new CheckBox.OnCheckListener() {
            @Override
            public void onCheck(boolean isChecked) {
                // To determine if the snackbar should be showed
                if (isChecked != sync_saved) {
                    showFakeSnackBar();
                } else {
                    // Request to hide the snackbar, maybe the color has changed
                    requestHideFakeSnackBar();
                }
            }
        });
    }

    /**
     * Shows the ColorSelector so the user can select a color for the agenda items
     */
    private void showColorPicker() {
        // Make listener
        ColorSelector.OnColorSelectedListener colorSelectedListener = new ColorSelector.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                // Set the color to the given value
                agendaColor = color;

                // "Guess" if the text is still readable, otherwise set color to black/white
                setButtonTextColor();

                // Set the chosen color to the background of the button
                colorButton.setBackgroundColor(color);

                // If the color is not the same as the previous one, there has been a change
                if (color != sharedPref.getInt("agendaColor", getResources().getColor(R.color.green))) {
                    if (syncCheckBox.isChecked()) {
                        // Show the snackbar
                        showFakeSnackBar();
                    }
                }
            }
        };

        // Create new ColorSelector and add listener
        ColorSelector colorSelector = new ColorSelector(this, agendaColor, colorSelectedListener);

        // Show the ColorSelector
        colorSelector.show();
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
        int savedColor = sharedPref.getInt("agendaColor", getResources().getColor(R.color.green));
        if (agendaColor != savedColor) {
            // The colors are not the same
            if (!sync_saved && syncCheckBox.isChecked() == sync_saved){
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

    /**
     * Sets the color of the text of the button to black if the color is too bright
     * and the color to white otherwise
     */
    private void setButtonTextColor() {

        // Get the rgb vales of the given color to determine the brightness
        int r = Color.red(agendaColor);
        int g = Color.green(agendaColor);
        int b = Color.blue(agendaColor);

        // Calculate the brightness. This is called the HSP model
        int brightness = (int) Math.pow(0.299 * Math.pow(r, 2) + 0.587 * Math.pow(g, 2) + 0.114 * Math.pow(b, 2), 0.5);

        if (brightness > 220) {
            // If the color is very light, set the color of the text to black
            colorButton.setTextColor(getResources().getColor(R.color.black));
        } else {
            // Else set it to white
            colorButton.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (settingsChanged) {
            // The settings have changed, sync the timetable with the current settings
            System.out.println("Sync calendar...");
            startService(new Intent(getApplicationContext(), SyncCalendarService.class));
            settingsChanged = false;
            // Quit from the activity. The user sould not be able to see this page while
            // the app is syncing.
            this.finish();
        }
    }
}
