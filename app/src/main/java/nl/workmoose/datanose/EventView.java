package nl.workmoose.datanose;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class EventView extends RelativeLayout {

    private final static int BEGIN_TIME = 0;
    private final static int END_TIME = 1;
    private final static int NAME = 2;
    private final static int LOCATION = 3;
    private final static int TEACHER = 4;
    private final static int UID = 5;

    private View rootView;

    public EventView(Context context) {
        super(context);
        LayoutInflater  inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.event_layout, this, true);
    }

    public void setTitle(String title) {
        TextView tv = (TextView) rootView.findViewById(R.id.title);
        if (title.startsWith(" ")) {
            // Happens once in a while
            title = title.replaceFirst(" ", "");
        }
        tv.setText(title);
    }

    public void setType(String type) {
        TextView tv = (TextView) rootView.findViewById(R.id.type);
        tv.setText(type);
    }

    public void setLocation(String location) {
        TextView tv = (TextView) rootView.findViewById(R.id.location);
        tv.setText(location);
    }

    public void setEventData(ArrayList<String> data) {
        String name = data.get(NAME);
        ArrayList<String> nameList = new ArrayList(Arrays.asList(name.split(" ")));
        String type = nameList.remove(nameList.size() - 1);
        String title = "";
        for (String s : nameList) {
            title += " " + s;
        }
        setTitle(title);
        setType(type);
        setLocation(data.get(LOCATION));
    }
}
