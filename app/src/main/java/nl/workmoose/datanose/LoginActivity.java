package nl.workmoose.datanose;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;


public class LoginActivity extends ActionBarActivity {

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
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
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
            return;
        }
        System.out.println("Called!");
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
        System.out.println("Student id: " + studentId);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                backToBeginning();
            }
        }, 10000);
    }

    private void backToBeginning() {
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
