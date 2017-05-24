package com.gc.materialdesign.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.gc.materialdesign.R;
import com.gc.materialdesign.views.ButtonFlat;

public class SnackBar extends Dialog {

    private String text;
    private float textSize = 18; // Roboto Regular 18sp
    private String buttonText;

    private Activity activity;
    private View view;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            dismiss();
            return false;
        }
    });
    private int backgroundSnackBar = Color.parseColor("#333333");
    private int buttonTextColor = Color.parseColor("#1E88E5");
    // Dismiss timer
    private Thread dismissTimer = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                int mTimer = 6 * 1000;
                Thread.sleep(mTimer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handler.sendMessage(new Message());
        }
    });

    // Only text
    public SnackBar(Activity activity, String text) {
        super(activity, android.R.style.Theme_Translucent);
        this.activity = activity;
        this.text = text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.snackbar);
        setCanceledOnTouchOutside(false);
        final View snackBarContainer = findViewById(R.id.snackbarContainer);
        snackBarContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                snackBarContainer.setOnClickListener(null);
            }
        });
        ((TextView) findViewById(R.id.text)).setText(text);
        ButtonFlat button = (ButtonFlat) findViewById(R.id.buttonflat);
        if (text == null) {
            button.setVisibility(View.GONE);
        } else {
            button.setText(buttonText);
            button.setTextColor(buttonTextColor);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        view = findViewById(R.id.snackbar);
        view.setBackgroundColor(backgroundSnackBar);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return activity.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void show() {
        super.show();
        view.setVisibility(View.VISIBLE);
        view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.snackbar_show_animation));
        dismissTimer.start();
    }

    @Override
    public void dismiss() {
        Animation anim = AnimationUtils.loadAnimation(activity, R.anim.snackbar_hide_animation);
        anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                SnackBar.super.dismiss();
            }
        });
        view.startAnimation(anim);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss();
        }
        return super.onKeyDown(keyCode, event);
    }
}
