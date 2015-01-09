package nl.workmoose.datanose;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.widgets.SnackBar;

import java.io.File;
import java.util.Calendar;


public class LoginActivity extends ActionBarActivity {

    final private static String SHARED_PREF = "prefs";
    final private static String URL_PART_1 =
            "http://content.datanose.nl/Timetable.svc/GetActivitiesByStudent?id=";
    final private static String URL_PART_2 = "&week=";
    final private static String URL_PART_3 = "&acyear=";

    EditText idInput;
    String studentId;
    Bundle savedInstanceState;
    ProgressBarCircularIndeterminate progressBar;
    ButtonFlat okButton;
    View inputContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_login);

        okButton = (ButtonFlat) findViewById(R.id.okButton);
        progressBar = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBar);
        idInput = (EditText) findViewById(R.id.idInput);
        idInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    idEntered();
                    return true;
                }
                return false;
            }
        });

        okButton.setTextSize(25f);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idEntered();
            }
        });
    }

    private void idEntered() {
        studentId = idInput.getText().toString();
        if (studentId.equals("")) {
            String message = getResources().getString(R.string.enter_student_id);
            SnackBar snackbar = new SnackBar(this, message);
            snackbar.show();
            return;
        }
        inputContainer = findViewById(R.id.inputContainer);
        Animation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.setFillAfter(true);
        idInput.setEnabled(false);
        inputContainer.setAnimation(fadeOut);

        okButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.restartAnimation();

        studentId = idInput.getText().toString();

        /*
        http://nl.wikipedia.org/wiki/NEN_2772
        De norm schrijft voor dat de eerste week van het jaar die week is die vier of meer dagen in dat jaar heeft.
        Vuistregels om de weeknummering van een jaar te bepalen:
        1 februari valt altijd in week 5
        4 januari valt altijd in week 1
        28 december valt altijd in de laatste week van het jaar
        */
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);

        // week: 0 for the first week of the academic year, 1 for the next, ..
        // year: the academic year, so not always the current year
        // the first week of the acatemic year is ALWAYS week 36
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        if (week >= 36) {
            week -= 36;
        } else {
            if (calendar.get(Calendar.DAY_OF_MONTH) < 15) {
                // If week is in the new calendar year
                year -= 1;
            }
            // Calculate the academic week
            Calendar tempCal = Calendar.getInstance();
            tempCal.setFirstDayOfWeek(Calendar.MONDAY);
            tempCal.setMinimalDaysInFirstWeek(4);
            tempCal.set(year, Calendar.DECEMBER, 28);
            int totalWeeksInYear = tempCal.get(Calendar.WEEK_OF_YEAR);
            week = week + (totalWeeksInYear - 36);
        }

        SharedPreferences s = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        if (s.contains("studentId")) {
            if (!s.getString("studentId", "-1").equals(studentId)) {
                // Clear directory if a new student number is entered
                for(File file: getFilesDir().listFiles()) file.delete();
            }
        }

        DataDownloadManager dataDownloadManager = new DataDownloadManager(this);
        dataDownloadManager.downloadWeekXML(studentId, week, year);
    }

    public void backToBeginning() {
        progressBar.setVisibility(View.INVISIBLE);
        inputContainer = findViewById(R.id.inputContainer);
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.setFillAfter(true);
        inputContainer.setAnimation(fadeIn);
        okButton.setEnabled(true);
        idInput.setEnabled(true);
    }
}
