package com.gc.materialdesign.widgets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.gc.materialdesign.R;
import com.gc.materialdesign.views.ButtonFlat;

public class Dialog extends android.app.Dialog {

    private Context context;
    private View view;
    private View backView;
    private String message;
    private TextView messageTextView;
    private String title;
    private TextView titleTextView;
    private String cancelText;
    private String acceptText;
    private ButtonFlat buttonAccept;
    private ButtonFlat buttonCancel;
    private View.OnClickListener onAcceptButtonClickListener;
    private View.OnClickListener onCancelButtonClickListener;


    public Dialog(Context context, String title, String message, String cancelText, String acceptText) {
        super(context, android.R.style.Theme_Translucent);
        this.context = context;  // init Context
        this.message = message;
        this.title = title;
        this.cancelText = cancelText;
        this.acceptText = acceptText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        view = findViewById(R.id.contentDialog);
        backView = findViewById(R.id.dialog_rootView);

        this.titleTextView = (TextView) findViewById(R.id.title);
        setTitle(title);

        this.messageTextView = (TextView) findViewById(R.id.message);
        setMessage(message);

        this.buttonAccept = (ButtonFlat) findViewById(R.id.button_accept);
        buttonAccept.setText(acceptText);
        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (onAcceptButtonClickListener != null)
                    onAcceptButtonClickListener.onClick(v);
            }
        });
        this.buttonCancel = (ButtonFlat) findViewById(R.id.button_cancel);
        buttonCancel.setText(cancelText);
        buttonCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
                if (onCancelButtonClickListener != null)
                    onCancelButtonClickListener.onClick(v);
            }
        });
    }

    @Override
    public void show() {
        super.show();
        // set dialog enter animations
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.dialog_main_show_amination));
        backView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.dialog_root_show_amin));
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        messageTextView.setText(message);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        if (title == null)
            titleTextView.setVisibility(View.GONE);
        else {
            titleTextView.setVisibility(View.VISIBLE);
            titleTextView.setText(title);
        }
    }

    public void setOnAcceptButtonClickListener(
            View.OnClickListener onAcceptButtonClickListener) {
        this.onAcceptButtonClickListener = onAcceptButtonClickListener;
        if (buttonAccept != null)
            buttonAccept.setOnClickListener(onAcceptButtonClickListener);
    }

    public void setOnCancelButtonClickListener(
            View.OnClickListener onCancelButtonClickListener) {
        this.onCancelButtonClickListener = onCancelButtonClickListener;
        if (buttonCancel != null)
            buttonCancel.setOnClickListener(onAcceptButtonClickListener);
    }

    @Override
    public void dismiss() {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.dialog_main_hide_amination);
        anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Dialog.super.dismiss();
            }
        });
        Animation backAnim = AnimationUtils.loadAnimation(context, R.anim.dialog_root_hide_amin);
        view.startAnimation(anim);
        backView.startAnimation(backAnim);
    }
}
