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
                    System.out.println("Currently logged in: " + studentId);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy in LoginActivity");
    }
}
