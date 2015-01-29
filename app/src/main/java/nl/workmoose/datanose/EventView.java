package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
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
    private ScheduleActivity scheduleActivity;
    public Boolean expanded;
    private ScheduleFragment scheduleFragment;
    private float deltaX;
    private float deltaY;
    private float factorX;
    private float factorY;

    private View rootView;

    public EventView(Context context, ScheduleFragment scheduleFragment) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.event_layout, this, true);
        this.context = context;
        this.scheduleActivity = (ScheduleActivity) context;
        this.scheduleFragment = scheduleFragment;
    }

    private void setTitle() {
        TextView tv = (TextView) rootView.findViewById(R.id.title);
        if (title.startsWith(" ")) {
            // Happens once in a while
            title = title.replaceFirst(" ", "");
        }
        tv.setText(title);
    }

    private void setType() {
        TextView tv = (TextView) rootView.findViewById(R.id.type);
        tv.setText(type);
    }

    private void setLocation() {
        TextView tv = (TextView) rootView.findViewById(R.id.location);
        tv.setText(location);
    }

    public void setEventData(ArrayList<String> data, int offSet) {
        String name = data.get(NAME);
        ArrayList<String> nameList = new ArrayList(Arrays.asList(name.split(" ")));
        String classType = nameList.remove(nameList.size() - 1);
        String className = "";
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

        setTitle();
        setType();
        setLocation();
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToCenter();
            }
        });
    }

    private void animateToCenter() {
        scheduleFragment.expandedEvent = this;
        ScrollView scrollView = (ScrollView) getParent().getParent().getParent();
        RelativeLayout scheduleView = (RelativeLayout) getParent();
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

        float screenMiddleX = screenWidth / 2;
        float screenMiddleY = screenHeight / 2;

        deltaX = screenMiddleX - middleX;
        deltaY = screenMiddleY - middleY;

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());

        TranslateAnimation translate = new TranslateAnimation(0, deltaX, 0, deltaY);
        translate.setDuration(ANIMATION_SPEED);
        factorX = (screenWidth - dpToPx(20)) / getWidth();
        factorY = (float) dpToPx(180) / getHeight();

        ScaleAnimation scale = new ScaleAnimation(
                1, factorX,
                1, factorY,
                getWidth() / 2, getHeight() / 2);
        scale.setDuration(ANIMATION_SPEED);

        animationSet.addAnimation(scale);
        animationSet.addAnimation(translate);

        final EventView eventView = this;
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
                alpha.setFillAfter(true);
                alpha.setDuration(100);
                eventView.startAnimation(alpha);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

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

        scheduleFragment.startActivityForResult(eventDetailIntent, 0);

    }

    public void animateBack() {
        setAlpha(1f);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());

        TranslateAnimation translate = new TranslateAnimation(deltaX, 0, deltaY, 0);
        translate.setDuration(ANIMATION_SPEED);

        ScaleAnimation scale = new ScaleAnimation(
                factorX, 1,
                factorY, 1,
                getWidth() / 2, getHeight() / 2);
        scale.setDuration(ANIMATION_SPEED);

        animationSet.addAnimation(scale);
        animationSet.addAnimation(translate);

        this.bringToFront();
        this.startAnimation(animationSet);
    }

    private int dpToPx(float dp) {
        // Convert dp into pixels
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
        return (int) px;
    }
}
