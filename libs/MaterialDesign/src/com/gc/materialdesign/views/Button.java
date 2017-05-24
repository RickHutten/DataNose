package com.gc.materialdesign.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;

import com.gc.materialdesign.R;
import com.gc.materialdesign.utils.Utils;

public abstract class Button extends RippleView {

    public Button(Context context, AttributeSet attrs) {
        super(context, attrs);
        onInitAttributes(attrs);
    }

    @Override
    protected void onInitDefaultValues() {
        backgroundColor = Color.parseColor("#2196f3");
    }

    protected void onInitAttributes(AttributeSet attrs) {
        setAttributes(attrs);
    }

    public Bitmap makeCircle() {
        Bitmap output = Bitmap.createBitmap(
                getWidth() - Utils.dpToPx(6, getResources()),
                getHeight() - Utils.dpToPx(7, getResources()), Config.ARGB_8888);
        return makeCircleFromBitmap(output);
    }

    // Set color of background
    @Override
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        if (isEnabled()) {
            beforeBackground = backgroundColor;
        }
        try {
            LayerDrawable layer = (LayerDrawable) getBackground();
            GradientDrawable shape = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_bacground);
            shape.setColor(backgroundColor);
            if (!settedRippleColor) {
                rippleColor = makePressColor(255);
            }
        } catch (Exception ignored) {
        }
    }
}
