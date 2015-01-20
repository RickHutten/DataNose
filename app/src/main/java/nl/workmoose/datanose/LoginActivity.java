package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.widgets.SnackBar;


public class LoginActivity extends ActionBarActivity {

    private static final String SHARED_PREF = "prefs";
    private static final int ANIMATION_DURATION = 500;

    private Boolean enableBack = true; // To enable/disable the back button
    private EditText idInput;
    private Boolean created = true;  // Flag if onResume is called when creating the activity
    private ProgressBarCircularIndeterminate progressBar;
    private ButtonFlat okButton;
    private View inputContainer;
    private int screen_height;
    private final Context context = this;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Special case when onBackpressed is called, quit the app (not just this activity)
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return; // So the code quits and doesn't run ScheduleActivity again
        }
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        Boolean signedIn = sharedPref.getBoolean("signed_in", false);
        if (signedIn) {
            if (sharedPref.contains("studentId")) {
                String studentId = sharedPref.getString("studentId", "");
                if (!studentId.equals("")) {
                    Intent i = new Intent(context, ScheduleActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);
                }
            }
        }
        setContentView(R.layout.activity_login);
        // Get the height of the screen in px
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screen_height = size.y;

        // Assign progressBar
        progressBar = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBar);

        // Make animation and add to inputContainer
        Animation slideIn = new TranslateAnimation(0, 0, screen_height, 0);
        slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
        slideIn.setFillAfter(true);
        slideIn.setDuration(ANIMATION_DURATION);
        View inputContainer = findViewById(R.id.inputContainer);
        inputContainer.setAnimation(slideIn);

        // Add onKeyListener to EditText so the user can press enter to proceed
        idInput = (EditText) findViewById(R.id.idInput);
        idInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the user presses on enter
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    idEntered();
                    return true;
                }
                return false;
            }
        });

        // Set onClickListener to ok button
        okButton = (ButtonFlat) findViewById(R.id.okButton);
        okButton.setTextSize(25f);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idEntered();
            }
        });
    }

    private void idEntered() {
        // Get the input of the EditText
        String studentId = idInput.getText().toString();
        if (studentId.equals("")) {
            String message = getResources().getString(R.string.enter_student_id);
            new SnackBar(this, message).show();
            return;
        }

        // Start slide down animation of the inputContainer
        inputContainer = findViewById(R.id.inputContainer);
        Animation slideOut = new TranslateAnimation(0, 0, 0, screen_height);
        slideOut.setDuration(ANIMATION_DURATION);
        slideOut.setInterpolator(new AccelerateDecelerateInterpolator());
        slideOut.setFillAfter(true);
        idInput.setEnabled(false);
        inputContainer.setAnimation(slideOut);

        // Disable button and start progressBar
        okButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.restartAnimation();


        /*
         *http://nl.wikipedia.org/wiki/NEN_2772
         *De norm schrijft voor dat de eerste week van het jaar die week is die vier of meer dagen in dat jaar heeft.
         *Vuistregels om de weeknummering van een jaar te bepalen:
         *1 februari valt altijd in week 5
         *4 januari valt altijd in week 1
         *28 december valt altijd in de laatste week van het jaar
         */
//        Calendar calendar = Calendar.getInstance();
//        calendar.setFirstDayOfWeek(Calendar.MONDAY);
//        calendar.setMinimalDaysInFirstWeek(4);
//
//        // week: 0 for the first week of the academic year, 1 for the next, ..
//        // year: the academic year, so not always the current year
//        // the first week of the acatemic year is ALWAYS week 36
//        int week = calendar.get(Calendar.WEEK_OF_YEAR);
//        int year = calendar.get(Calendar.YEAR);
//        if (week >= 36) {
//            week -= 36;
//        } else {
//            if (calendar.get(Calendar.DAY_OF_MONTH) < 15) {
//                // If week is in the new calendar year
//                year -= 1;
//            }
//            // Calculate the academic week
//            Calendar tempCal = Calendar.getInstance();
//            tempCal.setFirstDayOfWeek(Calendar.MONDAY);
//            tempCal.setMinimalDaysInFirstWeek(4);
//            tempCal.set(year, Calendar.DECEMBER, 28);
//            int totalWeeksInYear = tempCal.get(Calendar.WEEK_OF_YEAR);
//            week = week + (totalWeeksInYear - 36);
//        }

        // Get last signed in information
        if (sharedPref.contains("studentId")) {
                if (!sharedPref.getString("studentId", "-1").equals(studentId)) {
                // If the input student ID is different from the previous one
                System.out.println("New student ID, check new ID");
                StudentIdChecker idChecker = new StudentIdChecker(this);
                idChecker.execute(studentId);
            } else {
                System.out.println("Same student ID as before, don't download file");
                // The student number is the same as before
                // Dont download but continue
                enableBack = false;
                // Delay the start of ScheduleActivity for aesthetic reasons
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Enable backbutton
                        enableBack = true;
                        //  Start ScheduleActivity
                        Intent i = new Intent(context, ScheduleActivity.class);
                        LoginActivity currentActivity = (LoginActivity) context;
                        currentActivity.startActivity(i);
                        currentActivity.overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);

                    }
                }, ANIMATION_DURATION);
            }
        } else {
            // No previous sign in detected
            System.out.println("First run, download file");
            StudentIdChecker idChecker = new StudentIdChecker(this);
            idChecker.execute(studentId);
        }
    }

    public void backToBeginning() {
        // Sets all the settings to defaults
        // slide in inputContainer and set button and EditText enabled
        progressBar.setVisibility(View.INVISIBLE);
        inputContainer = findViewById(R.id.inputContainer);
        Animation slideIn = new TranslateAnimation(0, 0, screen_height, 0);
        slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
        slideIn.setDuration(ANIMATION_DURATION);
        slideIn.setFillAfter(true);
        inputContainer.setAnimation(slideIn);
        okButton.setEnabled(true);
        idInput.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!created) {  // Only run when activity is not created
            backToBeginning();
        }
        created = false;
    }

    @Override
    public void onBackPressed() {
        if (enableBack) {
            super.onBackPressed();
        }
    }
}
