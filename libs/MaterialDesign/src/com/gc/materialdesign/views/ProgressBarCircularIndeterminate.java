package com.gc.materialdesign.views;

import com.gc.materialdesign.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

public class ProgressBarCircularIndeterminate extends CustomView {

	public ProgressBarCircularIndeterminate(Context context, AttributeSet attrs) {
		super(context, attrs);
		setAttributes(attrs);
	}

	@Override
	protected void onInitDefaultValues() {
		// TODO 自动生成的方法存根
		minWidth = 32;
		minHeight = 32;
		backgroundColor = Color.parseColor("#1E88E5");
	}
	
	@Override
	protected void setAttributes(AttributeSet attrs) {
		// TODO 自动生成的方法存根
		super.setAttributes(attrs);
		float size = 4;// default ring width
		String width = attrs.getAttributeValue(MATERIALDESIGNXML, "ringWidth");
		if (width != null) {
			size = Utils.dipOrDpToFloat(width);
		}
		ringWidth = size;
	}

	/**
	 * Make a dark color to ripple effect
	 * 
	 * @return
	 */
	protected int makePressColor() {
		int r = (this.backgroundColor >> 16) & 0xFF;
		int g = (this.backgroundColor >> 8) & 0xFF;
		int b = (this.backgroundColor >> 0) & 0xFF;
		// r = (r+90 > 245) ? 245 : r+90;
		// g = (g+90 > 245) ? 245 : g+90;
		// b = (b+90 > 245) ? 245 : b+90;
		return Color.argb(128, r, g, b);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (firstAnimationOver == false) {
            drawFirstAnimation(canvas);
        }
		if (cont > 0) {
            drawSecondAnimation(canvas);
        }
		invalidate();
	}

	private float radius1 = 0;
	private float radius2 = 0;
	private int cont = 0;
	private boolean firstAnimationOver = false;
	private float ringWidth = 4;

	/**
	 * Draw first animation of view
	 * 
	 * @param canvas
	 */
	private void drawFirstAnimation(Canvas canvas) {
		if (radius1 < getWidth() / 2) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			//paint.setColor(makePressColor());
            paint.setColor(backgroundColor);
			radius1 = (radius1 >= getWidth() / 2) ? (float) getWidth() / 2 : radius1 + Utils.dpToPx(1, getResources());
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius1, paint);
		} else {
			Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas temp = new Canvas(bitmap);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(makePressColor());
            paint.setColor(backgroundColor);
			temp.drawCircle(getWidth() / 2, getHeight() / 2, getHeight() / 2, paint);
			Paint transparentPaint = new Paint();
			transparentPaint.setAntiAlias(true);
			transparentPaint.setColor(getResources().getColor(android.R.color.transparent));
			transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			//if (cont >= 8000) {
			//	radius2 = (radius2 >= getWidth() / 2) ? (float) getWidth() / 2 : radius2 + Utils.dpToPx(1, getResources());
			//} else {
			radius2 = (radius2 >= getWidth() / 2 - Utils.dpToPx(ringWidth, getResources())) ?
						(float) getWidth() / 2 - Utils.dpToPx(ringWidth, getResources()) : radius2 + Utils.dpToPx(1, getResources());
			//}
			temp.drawCircle(getWidth() / 2, getHeight() / 2, radius2, transparentPaint);
			canvas.drawBitmap(bitmap, 0, 0, new Paint());
			if (radius2 >= getWidth() / 2 - Utils.dpToPx(ringWidth, getResources()))
				cont++;
			if (radius2 >= getWidth() / 2)
				firstAnimationOver = true;

		}
	}

	private int arcD = 360;
	private int arcO = 270;
    private int iteration = 0;
    private int wait = 0;

	/**
	 * Draw second animation of view
	 * 
	 * @param canvas
	 */
	private void drawSecondAnimation(Canvas canvas) {
        iteration++;

        int max = 270;
        if (Math.abs(arcD) >= max && iteration > 15) {
            wait++;
            if (wait > 2) {
                arcO = arcO - Math.abs(arcD);
                arcD = -arcD;
                wait = 0;
            }
        }
        int min = 24;
        if (Math.abs(arcD) <= min) {
            wait++;
            if (wait > 8) {
                arcO = arcO + arcD;
                arcD = -arcD;
                wait = 0;
            }
        }
		Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas temp = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(backgroundColor);
		temp.drawARGB(255, 255, 255, 255);
		//temp.drawArc(new RectF(0, 0, getWidth(), getHeight()), arcO, arcD, true, paint);
        temp.drawArc(new RectF(0, 0, getWidth(), getHeight()), arcO, arcD, true, paint);
		Paint transparentPaint = new Paint();
		transparentPaint.setAntiAlias(true);
		transparentPaint.setColor(getResources().getColor(android.R.color.white));
		//transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        temp.drawCircle(getWidth() / 2, getHeight() / 2,
                getWidth() / 2 - Utils.dpToPx(ringWidth, getResources()), transparentPaint);

		canvas.drawBitmap(bitmap, 0, 0, new Paint());
        if (wait == 0) {
            arcO += 11;
            arcD -= 9;
        }
        if (wait != 0) {
            arcO += 2;
        }
	}

	// Set color of background
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		if (isEnabled()) {
			beforeBackground = backgroundColor;
		}
		this.backgroundColor = color;
	}

    public void restartAnimation() {
        radius1 = 0;
        radius2 = 0;
        cont = 0;
        firstAnimationOver = false;

        ringWidth = 4;
        arcD = 360;
        arcO = 225;
        iteration = 0;
        wait = 0;
    }

	public void setRingWidth(float width) {
		ringWidth = width;
	}

}
