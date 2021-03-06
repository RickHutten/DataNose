package nl.workmoose.datanose.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import nl.workmoose.datanose.R;

public class EventDetailView extends RelativeLayout {

    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int TEACHER = 4;

    public EventDetailView(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.detail_event_layout, this, false);
        addView(rootView);
    }

    public void setData(ArrayList<String> data, int offSet) {

        // Get information of the event which started this activity
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
        String title = className;
        String location = data.get(LOCATION);
        String teacher = data.get(TEACHER);
        String beginTime = "" + (Integer.parseInt(data.get(BEGIN_TIME).substring(9, 13)) + offSet);
        String endTime = "" + (Integer.parseInt(data.get(END_TIME).substring(9, 13)) + offSet);

        // Format time strings (eg. 1200 -> 12:00)
        beginTime = new StringBuilder(beginTime).insert(beginTime.length() - 2, ":").toString();
        endTime = new StringBuilder(endTime).insert(endTime.length() - 2, ":").toString();

        // Teacher string frequently is in parenthesis, if so, remove them
        if (teacher.startsWith(" (") && teacher.endsWith(")")) {
            teacher = teacher.substring(2, teacher.length() - 1);
        }

        // Get views from layout
        View detailContainer = findViewById(R.id.detailContainer);
        TextView titleText = findViewById(R.id.title);
        TextView typeText = findViewById(R.id.typeDetail);
        TextView timeText = findViewById(R.id.time);
        TextView teacherText = findViewById(R.id.teacher);
        TextView locationText = findViewById(R.id.locationDetail);

        // Set texts of the views
        titleText.setText(title);
        typeText.setText(classType);
        timeText.setText(beginTime + " - " + endTime);
        teacherText.setText(teacher);
        locationText.setText(location);

        // Set the background color of the view
        LayerDrawable backgroundDrawable = (LayerDrawable) detailContainer.getBackground();

        final GradientDrawable shape = (GradientDrawable)
                backgroundDrawable.findDrawableByLayerId(R.id.event_background_color);

        if (classType.equalsIgnoreCase("tentamen") ||
                classType.equalsIgnoreCase("toets") ||
                classType.equalsIgnoreCase("hertentamen") ||
                classType.equalsIgnoreCase("deeltoets") ||
                classType.equalsIgnoreCase("tussentoets")) {
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
}
