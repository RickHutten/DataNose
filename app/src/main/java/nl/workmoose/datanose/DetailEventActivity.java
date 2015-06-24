package nl.workmoose.datanose;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * This class shows detailed information of an event. It is only called when the user
 * clicks on an event. This is an activity with a transparent background so it doesn't
 * look like the app went to a different activity.
 */
 public class DetailEventActivity extends Activity {

    // The whole layout has a onclick listener so wherever
    // the user clicks, the activity is finished
    View.OnClickListener dismissListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            overridePendingTransition(R.anim.do_nothing, R.anim.shrink_fade_out_center);
        }
    };

    /**
     * Show the activity. Set all the values to the TextViews and set an
     * OnClickListener to dismiss the activity when the user touches the screen
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_event_layout);

        // Get information of the event which started this activity
        String title = getIntent().getStringExtra("title");
        String type = getIntent().getStringExtra("type");
        String location = getIntent().getStringExtra("location");
        String teacher = getIntent().getStringExtra("teacher");
        String beginTime = getIntent().getStringExtra("beginTime");
        String endTime = getIntent().getStringExtra("endTime");

        // Format time strings (eg. 1200 -> 12:00)
        beginTime = new StringBuilder(beginTime).insert(beginTime.length()-2, ":").toString();
        endTime = new StringBuilder(endTime).insert(endTime.length()-2, ":").toString();

        // Teacher string frequently is in parenthesis, if so, remove them
        if (teacher.startsWith(" (") && teacher.endsWith(")")) {
            teacher = teacher.substring(2, teacher.length()-1);
        }

        // Set dismissListener to parent
        RelativeLayout uberParent = (RelativeLayout) findViewById(R.id.uberParent);
        uberParent.setOnClickListener(dismissListener);

        // Get views from layout
        View detailContainer = findViewById(R.id.detailContainer);
        TextView titleText = (TextView) findViewById(R.id.title);
        TextView typeText = (TextView) findViewById(R.id.type);
        TextView timeText = (TextView) findViewById(R.id.time);
        TextView teacherText = (TextView) findViewById(R.id.teacher);
        TextView locationText = (TextView) findViewById(R.id.location);

        // Set texts of the views
        titleText.setText(title);
        typeText.setText(type);
        timeText.setText(beginTime + " - " + endTime);
        teacherText.setText(teacher);
        locationText.setText(location);

        // Set the background color of the view
        LayerDrawable backgroundDrawable = (LayerDrawable) detailContainer.getBackground();

        final GradientDrawable shape = (GradientDrawable)
                backgroundDrawable.findDrawableByLayerId(R.id.event_background_color);

        if (type.equalsIgnoreCase("tentamen") ||
                type.equalsIgnoreCase("hertentamen") ||
                type.equalsIgnoreCase("tussentoets")) {
            shape.setColor(getResources().getColor(R.color.exam_color));
        } else {
            shape.setColor(getResources().getColor(R.color.green));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.shrink_fade_out_center);
    }
}
