package nl.workmoose.datanose.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import nl.workmoose.datanose.R;
import nl.workmoose.datanose.activity.ScheduleActivity;
import nl.workmoose.datanose.fragment.WeekScheduleFragment;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Custom view that is being used to show the events in the ScheduleActivity.
 * The user can set the data to this view by calling setEventData(). The given data only
 * effects the text shown of the view, not the width, height or margins etc.
 */
public class EventView extends RelativeLayout {

    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int ANIMATION_SPEED = 175;
    private final static int ANIMATION_SPEED_BUTTON = 100;
    private final EventView eventView = this;
    private final Context context;
    private final View rootView;
    private final RelativeLayout pagerParent;
    private final ScheduleActivity activity;
    private String title;
    private String type;
    private String location;
    private float deltaX;
    private float deltaY;
    private float factorX;
    private float factorY;
    private ArrayList<String> data;
    private int offSet;
    private WeekScheduleFragment fragment;

    /**
     * Inflates the layout from event_layout.xml to this view
     *
     * @param context: activity where this view is placed in (ScheduleActivity)
     */
    public EventView(Context context) {
        super(context);
        // Inflate layout from XML file
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.event_layout, this, false);
        addView(rootView);
        activity = (ScheduleActivity) context;
        pagerParent = (RelativeLayout) activity.findViewById(R.id.pagerParent);
        this.context = context;

        // Set text to invisible if the eventView is too small
        ViewTreeObserver vto = rootView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (eventView.getWidth() < dpToPx(15)) {
                    rootView.findViewById(R.id.textContainer).setVisibility(INVISIBLE);
                }
            }
        });
    }

    /**
     * Sets the title for this view
     */
    private void setTitle() {
        TextView tv = (TextView) rootView.findViewById(R.id.title);
        tv.setText(title.trim());
    }

    /**
     * Sets the type for this event (Hoorcollege, Werkcollege etc.)
     */
    private void setType() {
        TextView tv = (TextView) rootView.findViewById(R.id.type);
        tv.setText(type.trim());
    }

    /**
     * Sets the location for this event
     */
    private void setLocation() {
        TextView tv = (TextView) rootView.findViewById(R.id.location);
        tv.setText(location.trim());
    }

    /**
     * This function sets the data for this view. Calls setTitle, setType
     * and setLocation to customize the text for the given data
     *
     * @param data:   the data associated to this view
     * @param offSet: the timeOffset for this timezone
     */
    public void setEventData(ArrayList<String> data, int offSet) {
        this.data = data;
        this.offSet = offSet;
        String name = data.get(NAME);

        // Split the string because this string containt the title and the type
        ArrayList<String> nameList = new ArrayList<>(Arrays.asList(name.split(" ")));
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

        this.setOnTouchListener(getTouchListener());
    }

    private void animateView() {
        this.findViewById(R.id.eventContainer).setBackgroundResource(R.drawable.detail_event_background);
        setColor();
        final EventDetailView eventDetailView = new EventDetailView(context);
        eventDetailView.setData(data, offSet);

        pagerParent.addView(eventDetailView);

        // Get elements from xml
        ScrollView scrollView = (ScrollView) getParent().getParent().getParent();

        RelativeLayout detailContainer = (RelativeLayout) eventDetailView.findViewById(R.id.detailContainer);
        ViewGroup.LayoutParams lp = ((View) detailContainer.getParent()).getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        ((View) detailContainer.getParent()).setLayoutParams(lp);
        // Get position of this view
        int scrollOffset = scrollView.getScrollY();

        // There is no good way (that I have found) to calculate the height of the actionbar
        int actionBarOffset = dpToPx(40);

        // Get middle of current event in px
        float middleY = ((this.getTop() + this.getBottom()) / 2) - scrollOffset + actionBarOffset;
        int[] coords = {0, 0};
        this.getLocationOnScreen(coords);
        float middleX = coords[0] + this.getWidth() / 2f;

        // Get width and height of screen
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final float screenWidth = metrics.widthPixels;
        final float screenHeight = metrics.heightPixels;

        // Get the target middle coordinates
        final float screenMiddleX = screenWidth / 2;
        float screenMiddleY = screenHeight / 2;

        // Calculate the difference
        deltaX = middleX - screenMiddleX;
        deltaY = middleY - screenMiddleY;

        factorX = (screenWidth - dpToPx(20)) / getWidth();
        factorY = (float) dpToPx(180) / getHeight();

        // Create animationset
        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);

        // Set animation listener
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                eventView.setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Add transparent view to capture all touches and animate back eventDetailView
                eventDetailView.findViewById(R.id.detailTextContainer).setVisibility(VISIBLE);
                final View captureTouchView = new View(activity);
                captureTouchView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                pagerParent.addView(captureTouchView);
                captureTouchView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pagerParent.removeView(captureTouchView);
                        animateBack(eventDetailView);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        ViewTreeObserver vto = eventDetailView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (eventDetailView.getTag() != "true") {
                    eventDetailView.setTag("true");

                    eventDetailView.setScaleX(1 / factorX);
                    eventDetailView.setScaleY(1 / factorY);
                    eventDetailView.setTranslationY(deltaY);
                    eventDetailView.setTranslationX(deltaX);
                    eventDetailView.findViewById(R.id.detailTextContainer).setVisibility(INVISIBLE);

                    eventDetailView.setVisibility(VISIBLE);

                    // Set translate animation to view
                    TranslateAnimation translate = new TranslateAnimation(0, -deltaX, 0, -deltaY);
                    translate.setDuration(ANIMATION_SPEED);
                    translate.setInterpolator(new AccelerateInterpolator());

                    // Set scale animation to view
                    ScaleAnimation scale = new ScaleAnimation(
                            1, factorX,
                            1, factorY,
                            (eventDetailView.getWidth() / 2), (eventDetailView.getHeight()) / 2);
                    scale.setDuration(ANIMATION_SPEED);
                    scale.setInterpolator(new DecelerateInterpolator());
                    scale.setStartOffset(ANIMATION_SPEED / 2);

                    // Add the views to the animationset
                    animationSet.addAnimation(translate);
                    animationSet.addAnimation(scale);
                }
            }
        });
        eventDetailView.startAnimation(animationSet);
    }

    private void animateBack(final EventDetailView eventDetailView) {

        final EventView eventView = this;
        // Create new animationset
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);

        // Translate the exact opposite way as the previous translate animation
        TranslateAnimation translate = new TranslateAnimation(-deltaX, 0, -deltaY, 0);
        translate.setDuration(ANIMATION_SPEED);
        translate.setInterpolator(new DecelerateInterpolator());
        translate.setStartOffset(ANIMATION_SPEED / 2);

        // Scale the exact opposite as the previous scale animation
        ScaleAnimation scale = new ScaleAnimation(
                factorX, 1,
                factorY, 1,
                eventDetailView.getWidth() / 2, eventDetailView.getHeight() / 2);
        scale.setDuration(ANIMATION_SPEED);
        scale.setInterpolator(new AccelerateInterpolator());

        // Add animations to set
        animationSet.addAnimation(translate);
        animationSet.addAnimation(scale);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                eventDetailView.findViewById(R.id.detailTextContainer).setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Set text visible
                eventView.setAlpha(1f);
                eventView.findViewById(R.id.textContainer).setVisibility(VISIBLE);
                // Cannot change hierarchy (removing view) in onAnimationEnd
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        pagerParent.removeView(eventDetailView);
                    }
                });
                // Set OnClickListener back on the view
                eventView.setOnTouchListener(getTouchListener());
                eventView.findViewById(R.id.eventContainer).setBackgroundResource(R.drawable.event_background);
                setColor();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Start animation
        eventDetailView.startAnimation(animationSet);
    }

    public void setColor() {
        LayerDrawable backgroundDrawable = (LayerDrawable) rootView.getBackground();

        final GradientDrawable shape = (GradientDrawable)
                backgroundDrawable.findDrawableByLayerId(R.id.event_background_color);

        if (type.equalsIgnoreCase("tentamen") ||
                type.equalsIgnoreCase("toets") ||
                type.equalsIgnoreCase("hertentamen") ||
                type.equalsIgnoreCase("deeltoets") ||
                type.equalsIgnoreCase("tussentoets")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shape.setColor(getResources().getColor(R.color.exam_color, null));
            } else {
                //noinspection deprecation
                shape.setColor(getResources().getColor(R.color.exam_color));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shape.setColor(getResources().getColor(R.color.green, null));
            } else {
                //noinspection deprecation
                shape.setColor(getResources().getColor(R.color.green));
            }
        }
    }

    /**
     * Converts the given value of dp in pixels
     *
     * @param dp: the size in dp
     * @return int: the given value of dp in pixels
     */
    private int dpToPx(float dp) {
        // Convert dp into pixels
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }

    /**
     * Makes an TouchListener for every EventView that is made. It animates the
     * eventview to the center.
     *
     * @return OnTouchListener: The touchlistener that
     */
    private OnTouchListener getTouchListener() {
        return new OnTouchListener() {
            float x = 0;
            float y = 0;
            boolean pressed = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Get x, y coords of original press
                    x = event.getX();
                    y = event.getY();
                    pressed = true;
                    // Scale the event to look like its pressed
                    ScaleAnimation scale = new ScaleAnimation(1, 0.95f, 1, 0.95f, getWidth() / 2, getHeight() / 2);
                    scale.setDuration(ANIMATION_SPEED_BUTTON);
                    scale.setFillAfter(true);
                    v.startAnimation(scale);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP && pressed) {
                    if (activity.isAnyEventPressed) {
                        // Scale item back to original size
                        ScaleAnimation scale = new ScaleAnimation(0.95f, 1, 0.95f, 1, getWidth() / 2, getHeight() / 2);
                        scale.setDuration(ANIMATION_SPEED_BUTTON);
                        scale.setFillAfter(true);
                        v.startAnimation(scale);
                        return true;
                    }
                    ScaleAnimation scale = new ScaleAnimation(0.95f, 1, 0.95f, 1, getWidth() / 2, getHeight() / 2);
                    scale.setDuration(ANIMATION_SPEED_BUTTON);
                    scale.setFillAfter(true);
                    v.startAnimation(scale);
                    v.setOnTouchListener(null); // Prevent item from being pressed quickly after

                    if (fragment != null) {
                        if (fragment.getView() != null) {
                            fragment.getView().bringToFront();
                        }
                    }
                    // Can only press if no other EventView is pressed
                    activity.isAnyEventPressed = true;
                    animateView(); // Animate view to center
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.isAnyEventPressed = false;
                        }
                    }, 500);
                    pressed = false;

                    return true;
                } else {
                    // This event is most likely a move action
                    float new_x = event.getX();
                    float new_y = event.getY();

                    // If you move your finger too far
                    if ((Math.abs(x - new_x) > dpToPx(10) || Math.abs(y - new_y) > dpToPx(10)) && pressed) {
                        // The item is no longer pressed
                        pressed = false;
                        // Scale item back to original size
                        ScaleAnimation scale = new ScaleAnimation(0.95f, 1, 0.95f, 1, getWidth() / 2, getHeight() / 2);
                        scale.setDuration(ANIMATION_SPEED_BUTTON);
                        scale.setFillAfter(true);
                        v.startAnimation(scale);
                    }
                }
                // End of if-else statements, return True in onTouch
                return true;
            }
        };
    }

    public void setFragment(WeekScheduleFragment fragment) {
        this.fragment = fragment;
    }

}
