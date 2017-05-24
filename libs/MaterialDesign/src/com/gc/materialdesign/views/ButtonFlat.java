package com.gc.materialdesign.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.gc.materialdesign.R;

public class ButtonFlat extends ButtonRectangle {

    public ButtonFlat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onInitDefaultValues() {
        textButton = new TextView(getContext());
        minHeight = 36;
        minWidth = 88;
        rippleSpeed = 12f;
        defaultTextColor = Color.parseColor("#1E88E5");
        backgroundResId = R.drawable.background_transparent;
        rippleColor = Color.parseColor("#88DDDDDD");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (x != -1) {

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            if (rippleColor == null) {
                paint.setColor(Color.parseColor("#88DDDDDD"));
            } else {
                paint.setColor(rippleColor);
            }
            canvas.drawCircle(x, y, radius, paint);
            if (radius > getHeight() / rippleSize)
                radius += rippleSpeed;
            if (radius >= getWidth()) {
                x = -1;
                y = -1;
                radius = getHeight() / rippleSize;
                if (isEnabled() && clickAfterRipple && onClickListener != null) {
                    onClickListener.onClick(this);
                }
            }
        }
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        if (!settedRippleColor) {
            rippleColor = Color.parseColor("#88DDDDDD");
        }
    }
}
