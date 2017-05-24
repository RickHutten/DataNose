package nl.workmoose.datanose;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import nl.workmoose.datanose.activity.ScheduleActivity;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * <p>
 * Parses the file that has already been downloaded. The information is
 * saved in an ArrayList.
 */
public abstract class ParseIcs {

    final private static String FILE_NAME = "WARNING: DO NOT OPEN, VERY DANGEROUS FILE";

    /**
     * The actual parsing. It reads the file and puts the information in an
     * Arraylist. Call this function from outside to get the wanted data.
     *
     * @return ArrayList: ArrayList containing the data from the iCalendar file
     */
    public static ArrayList<ArrayList<String>> readFile(Context context) {
        // To time this function
        long startTime = System.currentTimeMillis();

        Log.i("ParseIcs", "Parsing file...");

        // Create variable to store single events in
        ArrayList<String> event = new ArrayList<>();

        // Create variable to store all the events in
        ArrayList<ArrayList<String>> eventList = new ArrayList<>();

        try {
            // Get ready to read file
            FileInputStream fis = context.openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            // Initialise variables
            String line;
            String begin = "";
            String end = "";
            String name = "";
            String location = "";
            String teacher = "";
            String uid = "";

            // The actual file reading, it reads line by line.
            while ((line = br.readLine()) != null) {
                if (line.startsWith("BEGIN:VEVENT")) {
                    // Begin event, clear previous values
                    event = new ArrayList<>();
                    begin = "";
                    end = "";
                    name = "";
                    location = "";
                    teacher = "";
                    uid = "";
                }
                else if (line.startsWith("DTSTART")) {
                    // Start time of event
                    begin = line.split(":")[1];
                    event.add(begin);
                }
                else if (line.startsWith("DTEND")) {
                    // End time of event
                    end = line.split(":")[1];
                    event.add(end);
                }
                else if (line.startsWith("SUMMARY")) {
                    // Name of class
                    name = line.split(":")[1];
                    event.add(name);
                }
                else if (line.startsWith("LOCATION")) {
                    // Location of the event
                    try {
                        location = line.split(":")[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // No location is given
                        location = " ";
                    }
                    event.add(location);
                }
                else if (line.startsWith("DESCRIPTION")) {
                    // Teacher of the class
                    teacher = line.split(":")[1];
                    event.add(teacher);
                }
                else if (line.startsWith("UID")) {
                    // Unique identifier of the event
                    uid = line.split(":")[1];
                    event.add(uid);
                }
                else if (line.equals("END:VEVENT")) {
                    // End of event
                    if (begin.equals("") || end.equals("") || name.equals("") || location.equals("") ||
                            teacher.equals("") || uid.equals("")) {
                        // If a variable is not in the calendar item
                        Log.i("ParseIcs", "Parsing went wrong :(");
                    } else {
                        // Put values in eventList if everything is correct
                        // Use a copy of the event, because if you clear the event,
                        // the value in eventList will be cleared also
                        eventList.add(event);
                    }
                }
            }
        } catch (Exception e) {
            // Parsing went wrong, go back to LoginActivity
            e.printStackTrace();
            Log.i("ParseIcs", "Error parsing file");
            ((ScheduleActivity) context).finish();
            ((ScheduleActivity) context).overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
        }

        // Log the time parsing took
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        Log.i("ParseIcs", "Done parsing");
        Log.i("ParseIcs", "Parsed in " + duration + " milliseconds");

        // Return all the events
        return eventList;
    }
}
