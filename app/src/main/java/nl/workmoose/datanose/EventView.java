package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
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

    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int TEACHER = 4;
    private final static int ANIMATION_SPEED = 250;
    private String title;
    private String type;
    private String location;
    private String teacher;
    private String beginTime;
    private String endTime;
    private Context context;
    public Boolean expanded;
    private ScheduleActivity scheduleActivity;
    private ScheduleFragment scheduleFragment;
    private float deltaX;
    private float deltaY;
    private float factorX;
    private float factorY;
    private View rootView;

    /**
     * Inflates the layout from event_layout.xml to this view
     * @param context: activity where this view is placed in (ScheduleActivity)
     * @param scheduleFragment: fragment where this view is located in
     */
    public EventView(Context context, ScheduleFragment scheduleFragment) {
        super(context);

        // Inflate layout from XML file
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.event_layout, this, false);
        addView(rootView);
        this.context = context;

        this.scheduleActivity = (ScheduleActivity) context;
        this.scheduleFragment = scheduleFragment;
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
        String name = data.get(NAME);

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
        this.teacher = data.get(TEACHER);
        this.beginTime = "" + (Integer.parseInt(data.get(BEGIN_TIME).substring(9, 13)) + offSet);
        this.endTime = "" + (Integer.parseInt(data.get(END_TIME).substring(9, 13)) + offSet);

        // Call the setters to set the texts to the TextViews
        setTitle();
        setType();
        setLocation();

        // Set OnClickListener, if the event is pressed the event should animate
        // to the center of the screen and show DetailEventActivity
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToCenter();
            }
        });
    }

    public void setColor() {
        LayerDrawable backgroundDrawable = (LayerDrawable) rootView.getBackground();

        final GradientDrawable shape = (GradientDrawable)
                backgroundDrawable.findDrawableByLayerId(R.id.event_background_color);

        if (type.equalsIgnoreCase("tentamen") || type.equalsIgnoreCase("hertentamen")) {
            shape.setColor(context.getResources().getColor(R.color.lavender));
        } else {
            shape.setColor(context.getResources().getColor(R.color.green));
        }
    }

    /**
     * Calculates the current x and y position of the event, and the x and y position
     * of the point where it needs to go (to the center of the screen).
     * Then calculates the width and the height of the current and target position.
     * When all the calculations are completed, the animation is started.
     */
    private void animateToCenter() {

        // Notify ScheduleFragment that THIS event has been clicked on.
        scheduleFragment.expandedEvent = this;

        // Get elements from xml
        ScrollView scrollView = (ScrollView) getParent().getParent().getParent();
        RelativeLayout scheduleView = (RelativeLayout) getParent();

        // Get position of this view
        int leftOffset = scheduleView.getLeft();
        int scrollOffset = scrollView.getScrollY();

        // There is no good way (that I have found) to calculate the height of the actionbar
        // plus the notification bar. This is an estimate in dp, 48 for actionbar and 20 for
        // the notification bar
        int actionBarOffset = dpToPx(48+20);

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
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());

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
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                // After the animation has ended, start another alpha animation to hide
                // the fact that not on every screen the position is perfect
                // (actionBarOffset was a guess, remember?)
                AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
                alpha.setFillAfter(true);
                alpha.setDuration(100);
                eventView.startAnimation(alpha);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        // Set data to intent for the DetailEventActivity
        Intent eventDetailIntent = new Intent(scheduleActivity, DetailEventActivity.class);
        eventDetailIntent.putExtra("title", title);
        eventDetailIntent.putExtra("type", type);
        eventDetailIntent.putExtra("location", location);
        eventDetailIntent.putExtra("teacher", teacher);
        eventDetailIntent.putExtra("beginTime", beginTime);
        eventDetailIntent.putExtra("endTime", endTime);

        // Start animation and DetailEventActivity
        this.bringToFront();
        this.startAnimation(animationSet);

        // startActivityForResult calls onActivityResult in ScheduleFragment
        // Which is needed to animate this event back to its place
        scheduleFragment.startActivityForResult(eventDetailIntent, 0);
    }

    /**
     * Animates the view back from the middle of the screen back to its original place
     */
    public void animateBack() {
        // Set the alpha back to VISIBLE
        setAlpha(1f);

        // Create new animationset
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());

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

        // Start animation
        this.bringToFront();
        this.startAnimation(animationSet);
    }

    /**
     * Converts the given value of dp in pixels
     * @param dp: the size in dp
     * @return: the given value of dp in pixels
     */
    private int dpToPx(float dp) {
        // Convert dp into pixels
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }
}
