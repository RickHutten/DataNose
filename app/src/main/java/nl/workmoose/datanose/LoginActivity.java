package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Handler;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.widgets.SnackBar;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * First activity where the user can put in his/her student ID, calls StudentIdChecker
 * and (possibly) downloads the iCalendar file before the next activity is called.
 * If the user was already signed in, it skips this acitivity and goes directly
 * to ScheduleActivity
 */
 public class LoginActivity extends Activity {

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

    /**
     * If the activity is called with the intent "EXIT" = true, the event will quit. This happens
     * when the user want to quit the app in ScheduleActivity. He/She doesn't go back to this
     * activity because that will cause him/her to log out, or press the backbutton twice.
     * "So, why won't you call finish() when you start ScheduleActivity?"
     * - "Because than I am not possible to the the transition animation used to go back to this
     * activity when the user signs out.".
     *
     * Checks whether the user is aleady signed in. Calls ScheduleActivity if so.
     *
     * Slides in the inputContainer on start and shows the keyboard.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Special case when onBackpressed is called, quit the app (not just this activity)
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return; // So the code quits and doesn't run ScheduleActivity again
        }
        // Check whether the user is signed in or not
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        Boolean signedIn = sharedPref.getBoolean("signed_in", false);
        if (signedIn) {
            // If the user is signed in, go to ScheduleActivity
            if (sharedPref.contains("studentId")) {
                String studentId = sharedPref.getString("studentId", "");
                if (!studentId.equals("")) {
                    System.out.println("Currently logged in: " + studentId);
                    Intent i = new Intent(context, ScheduleActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);
                }
            }
        }
        // The user is not signed in, set layout of activity_login.xml
        setContentView(R.layout.activity_login);

        // Get the height of the screen in px, needed for animation
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

        // Set animation listener
        slideIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(idInput, InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        // Set animation
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
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idEntered();
            }
        });
    }

    /**
     * Gets called when the user presses the "OK" button or presses enter.
     */
    private void idEntered() {
        // Get the input of the EditText
        String studentId = idInput.getText().toString();

        // If the input is empty, go back.
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
                System.out.println("Currently logged in: " + studentId);
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

    /**
     * Sets the activity in the state in which it was created
     */
    public void backToBeginning() {
        // Sets all the settings to defaults
        // slide in inputContainer and set button and EditText enabled
        progressBar.setVisibility(View.INVISIBLE);
        inputContainer = findViewById(R.id.inputContainer);

        // Make animation to slide in the inputContainer
        Animation slideIn = new TranslateAnimation(0, 0, screen_height, 0);
        slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
        slideIn.setDuration(ANIMATION_DURATION);
        slideIn.setFillAfter(true);

        // Set animation
        inputContainer.setAnimation(slideIn);

        // Set EditText and button enabled
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

    /**
     * Don't always let the user go back
     */
    @Override
    public void onBackPressed() {
        if (enableBack) {
            super.onBackPressed();
        }
    }
}
