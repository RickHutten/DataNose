package nl.workmoose.datanose;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DetailEventActivity extends Activity {

    View.OnClickListener dismissListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_event_layout);

        String title = getIntent().getStringExtra("title");
        String type = getIntent().getStringExtra("type");
        String location = getIntent().getStringExtra("location");
        String teacher = getIntent().getStringExtra("teacher");
        String beginTime = getIntent().getStringExtra("beginTime");
        String endTime = getIntent().getStringExtra("endTime");

        // Format strings (eg. 1200 -> 12:00)
        beginTime = new StringBuilder(beginTime).insert(beginTime.length()-2, ":").toString();
        endTime = new StringBuilder(endTime).insert(endTime.length()-2, ":").toString();

        if (teacher.startsWith(" (") && teacher.endsWith(")")) {
            teacher = teacher.substring(2, teacher.length()-1);
        }

        dismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };

        RelativeLayout uberParent = (RelativeLayout) findViewById(R.id.uberParent);
        uberParent.setOnClickListener(dismissListener);

        TextView titleText = (TextView) findViewById(R.id.title);
        titleText.setText(title);

        TextView typeText = (TextView) findViewById(R.id.type);
        typeText.setText(type);

        TextView timeText = (TextView) findViewById(R.id.time);
        timeText.setText(beginTime + " - " + endTime);


        TextView teacherText = (TextView) findViewById(R.id.teacher);
        teacherText.setText(teacher);

        TextView locationText = (TextView) findViewById(R.id.location);
        locationText.setText(location);

    }
}
