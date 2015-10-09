package nl.workmoose.datanose;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * Custon view that is being used to show the events in the ScheduleActivity.
 * The user can set the data to this view by calling setEventData(). The given data only
 * effects the text shown of the view, not the width, height or margins etc.
 */
public class EventView extends RelativeLayout {

    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int ANIMATION_SPEED = 250;
    private final static int ANIMATION_SPEED_BUTTON = 100;
    private String title;
    private String type;
    private String location;
    private Context context;
    private float deltaX;
    private float deltaY;
    private float factorX;
    private float factorY;
    private View rootView;
    private ArrayList<String> data;
    private int offSet;

    /**
     * Inflates the layout from event_layout.xml to this view
     * @param context: activity where this view is placed in (ScheduleActivity)
     */
    public EventView(Context context) {
        super(context);
        // Inflate layout from XML file
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.event_layout, this, false);
        addView(rootView);

        this.context = context;
    }

    /**
     * Sets the title for this view
     */
    private void setTitle() {
        TextView tv = (TextView) rootView.findViewById(R.id.title);
        if (title.startsWith(" ")) {
            // Happens once in a while that the title start with a space.
            // This should be removed
            title = title.replaceFirst(" ", "");
        }
        tv.setText(title);
    }

    /**
     * Sets the type for this event (Hoorcollege, Werkcollege etc.)
     */
    private void setType() {
        TextView tv = (TextView) rootView.findViewById(R.id.type);
        tv.setText(type);
    }

    /**
     * Sets the location for this event
     */
    private void setLocation() {
        TextView tv = (TextView) rootView.findViewById(R.id.location);
        tv.setText(location);
    }

    /**
     * This function sets the data for this view. Calls setTitle, setType
     * and setLocation to customize the text for the given data
     * @param data: the data associated to this view
     * @param offSet: the timeOffset for this timezone
     */
    public void setEventData(ArrayList<String> data, int offSet) {
        this.data = data;
        this.offSet = offSet;
        String name = data.get(NAME);

        final EventView eventView = this;

        // Split the string because this string containt the title and the type
        ArrayList<String> nameList = new ArrayList(Arrays.asList(name.split(" ")));
        String classType = nameList.remove(nameList.size() - 1);
        String className = "";

        // Paste the strings in the nameList together
        for (String s : nameList) {
            className += " " + s;
        }

        // Set values for event
        this.title = className;
        this.type = classType;
        this.location = data.get(LOCATION);

        // Call the setters to set the texts to the TextViews
        setTitle();
        setType();
        setLocation();

        this.setOnTouchListener(getTouchListener(eventView));
    }

    private void animateView() {
        this.findViewById(R.id.eventContainer).setBackgroundResource(R.drawable.detail_event_background);
        setColor();
        final EventDetailView eventDetailView = new EventDetailView(context);
        eventDetailView.setData(data, offSet);
        eventDetailView.setVisibility(INVISIBLE);

        // Get elements from xml
        ScrollView scrollView = (ScrollView) getParent().getParent().getParent();
        final RelativeLayout scheduleFragment = (RelativeLayout) scrollView.getParent();
        RelativeLayout scheduleView = (RelativeLayout) getParent();

        // Get position of this view
        int leftOffset = scheduleView.getLeft();
        int scrollOffset = scrollView.getScrollY();

        // There is no good way (that I have found) to calculate the height of the actionbar
        // plus the notification bar. This is an estimate in dp, 48 for actionbar and 20 for
        // the notification bar
        int actionBarOffset = dpToPx(40);

        // Get middle of current event in px
        float middleX = ((this.getLeft() + this.getRight()) / 2) + leftOffset;
        float middleY = ((this.getTop() + this.getBottom()) / 2) - scrollOffset + actionBarOffset;

        // Get width and height of screen
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;

        // Get the target middle coordinates
        float screenMiddleX = screenWidth / 2;
        float screenMiddleY = screenHeight / 2;

        // Calculate the difference
        deltaX = screenMiddleX - middleX;
        deltaY = screenMiddleY - middleY;

        // Create animationset
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new OvershootInterpolator());

        // Set translate animation to view
        TranslateAnimation translate = new TranslateAnimation(0, deltaX, 0, deltaY);
        translate.setDuration(ANIMATION_SPEED);

        factorX = (screenWidth - dpToPx(20)) / getWidth();
        factorY = (float) dpToPx(180) / getHeight();

        // Set scale animation to view
        ScaleAnimation scale = new ScaleAnimation(
                1, factorX,
                1, factorY,
                getWidth() / 2, getHeight() / 2);
        scale.setDuration(ANIMATION_SPEED);

        // Add the views to the animationset
        animationSet.addAnimation(scale);
        animationSet.addAnimation(translate);

        final EventView eventView = this;

        // Set animation listener
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                eventView.findViewById(R.id.textContainer).setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                eventDetailView.setVisibility(VISIBLE);
                AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
                alpha.setFillAfter(true);
                alpha.setDuration(100);
                eventView.startAnimation(alpha);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        scheduleFragment.addView(eventDetailView);
        eventView.bringToFront();
        eventView.startAnimation(animationSet);

        eventDetailView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBack(eventDetailView, scheduleFragment);
            }
        });
    }

    public void animateBack(final EventDetailView eventDetailView, final RelativeLayout scheduleFragment) {
        final EventView eventView = this;
        // Create new animationset
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new OvershootInterpolator());

        // Translate the exact opposite way as the previous translate animation
        TranslateAnimation translate = new TranslateAnimation(deltaX, 0, deltaY, 0);
        translate.setDuration(ANIMATION_SPEED);

        // Scale the exact opposite as the previous scale animation
        ScaleAnimation scale = new ScaleAnimation(
                factorX, 1,
                factorY, 1,
                getWidth() / 2, getHeight() / 2);
        scale.setDuration(ANIMATION_SPEED);

        // Add animations to set
        animationSet.addAnimation(scale);
        animationSet.addAnimation(translate);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Set text visible
                eventView.findViewById(R.id.textContainer).setVisibility(VISIBLE);

                // Set OnClickListener back on the view
                eventView.setOnTouchListener(getTouchListener(eventView));
                eventView.findViewById(R.id.eventContainer).setBackgroundResource(R.drawable.event_background);
                setColor();

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Start animation
        this.bringToFront();
        scheduleFragment.removeView(eventDetailView);
        eventView.setAlpha(1f);
        this.startAnimation(animationSet);
    }

    public void setColor() {
        LayerDrawable backgroundDrawable = (LayerDrawable) rootView.getBackground();

        final GradientDrawable shape = (GradientDrawable)
                backgroundDrawable.findDrawableByLayerId(R.id.event_background_color);

        if (type.equalsIgnoreCase("tentamen") ||
                type.equalsIgnoreCase("hertentamen") ||
                type.equalsIgnoreCase("tussentoets")) {
            shape.setColor(context.getResources().getColor(R.color.exam_color));
        } else {
            shape.setColor(context.getResources().getColor(R.color.green));
        }
    }

    /**
     * Converts the given value of dp in pixels
     * @param dp: the size in dp
     * @return int: the given value of dp in pixels
     */
    private int dpToPx(float dp) {
        // Convert dp into pixels
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }

    private OnTouchListener getTouchListener(final EventView eventView) {
        return new OnTouchListener() {
            float x = 0;
            float y = 0;
            boolean pressed = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    x = event.getX();
                    y = event.getY();
                    pressed = true;
                    // Scale the exact opposite as the previous scale animation
                    ScaleAnimation scale = new ScaleAnimation(1, 0.95f, 1, 0.95f, getWidth() / 2, getHeight() / 2);
                    scale.setDuration(ANIMATION_SPEED_BUTTON);
                    scale.setFillAfter(true);
                    eventView.startAnimation(scale);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP && pressed) {
                    eventView.setOnTouchListener(null);
                    animateView();
                    pressed = false;
                    return true;
                } else {
                    float new_x = event.getX();
                    float new_y = event.getY();

                    // If you move your finger too far
                    if ((Math.abs(x - new_x) > dpToPx(10) || Math.abs(y - new_y) > dpToPx(10)) && pressed) {
                        pressed = false;
                        ScaleAnimation scale = new ScaleAnimation(0.95f, 1, 0.95f, 1, getWidth() / 2, getHeight() / 2);
                        scale.setDuration(ANIMATION_SPEED_BUTTON);
                        scale.setFillAfter(true);
                        eventView.startAnimation(scale);
                    }
                }
                return true;
            }
        };
    }
}
