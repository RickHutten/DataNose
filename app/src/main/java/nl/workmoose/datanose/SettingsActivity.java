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
 * Created by Rick on 27-1-2015.
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
        sync_saved = sharedPref.getBoolean("sync_saved", false);

        // Set checkboxes to saved state
        syncCheckBox.setChecked(sync_saved);

        // Set the color background of the colorButton
        colorButton.setBackgroundColor(agendaColor);

        // Set all the onClickListeners
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        // Sets all the onClickListeners in the layout
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });

        syncNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFakeSnackBar();
                saveCurrentSettings();
            }
        });

        syncCheckBox.setOncheckListener(new CheckBox.OnCheckListener() {
            @Override
            public void onCheck(boolean isChecked) {
                if (isChecked != sync_saved) {
                    showFakeSnackBar();
                } else {
                    requestHideFakeSnackBar();
                }
            }
        });
    }

    private void showColorPicker() {
        ColorSelector.OnColorSelectedListener colorSelectedListener = new ColorSelector.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                agendaColor = color;
                setButtonTextColor();
                colorButton.setBackgroundColor(color);
                if (color != sharedPref.getInt("agendaColor", getResources().getColor(R.color.green))) {
                    showFakeSnackBar();
                }
            }
        };

        ColorSelector colorSelector = new ColorSelector(this, agendaColor, colorSelectedListener);
        colorSelector.show();
    }

    private void showFakeSnackBar() {
        // Shows the fake SnackBar to ask the user to sync the calendar
        if (fakeSnackBar.getVisibility() == View.VISIBLE) {
            return;
        }
        Animation translate = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1,
                Animation.RELATIVE_TO_SELF, 0);
        translate.setDuration(ANIMATION_DURATION);
        translate.setInterpolator(new AccelerateDecelerateInterpolator());

        syncNowButton.setEnabled(true);
        fakeSnackBar.setVisibility(View.VISIBLE);
        fakeSnackBar.startAnimation(translate);

    }

    private void hideFakeSnackBar() {
        // Hides the fakeSnackBar
        Animation translate = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        translate.setDuration(ANIMATION_DURATION);
        translate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fakeSnackBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        translate.setInterpolator(new AccelerateDecelerateInterpolator());

        syncNowButton.setEnabled(false);
        fakeSnackBar.startAnimation(translate);
    }

    private void requestHideFakeSnackBar() {
        // Only hide the fakeSnackBar if all the values are the same as the saved ones
        if (syncCheckBox.isChecked() != sync_saved) {
            return;
        }
        if (agendaColor != sharedPref.getInt("agendaColor", getResources().getColor(R.color.green))) {
            return;
        }
        // All the values are the same, hide fakeSnackBar
        hideFakeSnackBar();
    }

    private void saveCurrentSettings() {
        // Set the values for this current setting
        sync_saved = syncCheckBox.isChecked();
        sharedPref.edit().putBoolean("sync_saved", sync_saved).apply();
        sharedPref.edit().putInt("agendaColor", agendaColor).apply();

        // Set boolean that the settings have changed in this activity
        settingsChanged = true;
    }

    private void setButtonTextColor() {
        // Sets the color of the text of the button to black if the color is too bright
        // and the color to white otherwise

        // Get the saturation and value of the given color to determine the brightness of the color
        float[] hsv = new float[3];
        Color.colorToHSV(agendaColor, hsv);
        float s = hsv[1];
        float v = hsv[2];

        if (s <= 0.20 && v >= 0.90) {
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
    protected void onDestroy() {
        super.onDestroy();
        if (settingsChanged) {
            // The settings have changed, sync the timetable with the current settings
            System.out.println("Sync calendar...");
            startService(new Intent(this, SyncCalendarService.class));
        }
    }
}
