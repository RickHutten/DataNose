package nl.workmoose.datanose;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * Parses the file that has already been downloaded. The information is
 * saved in an ArrayList.
 */
 public class ParseIcs {

    final private static String FILE_NAME = "WARNING: DO NOT OPEN, VERY DANGEROUS FILE";
    private Context context;

    public ParseIcs(Context context) {
        this.context = context;
    }

    /**
     * The actual parsing. It reads the file and puts the information in an
     * Arraylist. Call this function from outside to get the wanted data.
     * @return: ArrayList containing the data from the iCalendar file
     */
    public ArrayList<ArrayList<String>> readFile() {
        // To time this function
        long startTime = System.currentTimeMillis();

        System.out.println("Parsing file...");

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
                if (line.equals("BEGIN:VEVENT")){
                    // Begin event, clear previous values
                    begin = "";
                    end = "";
                    name = "";
                    location = "";
                    teacher = "";
                    uid = "";
                }
                if (line.startsWith("DTSTART")) {
                    // Start time of event
                    begin = line.split(":")[1];
                    event.add(begin);
                }
                if (line.startsWith("DTEND")) {
                    // End time of event
                    end = line.split(":")[1];
                    event.add(end);
                }
                if (line.startsWith("SUMMARY")) {
                    // Name of class
                    name = line.split(":")[1];
                    event.add(name);
                }
                if (line.startsWith("LOCATION")) {
                    // Location of the event
                    try {
                        location = line.split(":")[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // No location is given
                        location = " ";
                    }
                    event.add(location);
                }
                if (line.startsWith("DESCRIPTION")) {
                    // Teacher of the class
                    teacher = line.split(":")[1];
                    event.add(teacher);
                }
                if (line.startsWith("UID")) {
                    // Unique identifier of the event
                    uid = line.split(":")[1];
                    event.add(uid);
                }
                if (line.equals("END:VEVENT")) {
                    // End of event
                    if (begin.equals("")||end.equals("")||name.equals("")||location.equals("")||
                            teacher.equals("")||uid.equals("")) {
                        // If a variable is not in the calendar item
                        System.out.println("Parsing went wrong :(");
                    } else {
                        // Put values in eventList if everything is correct
                        // Use a copy of the event, because if you clear the event,
                        // the value in eventList will be cleared also
                        eventList.add((ArrayList<String>) event.clone());
                        event.clear();
                    }
                }
            }
        } catch (Exception e) {
            // Parsing went wrong, go back to LoginActivity
            e.printStackTrace();
            System.out.println("Error parsing file");
            ((ScheduleActivity) context).finish();
            ((ScheduleActivity) context).overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
        }

        // Log the time parsing took
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        System.out.println("Done parsing");
        System.out.println("Parsed in " + duration + " milliseconds");

        // Return all the events
        return eventList;
    }
}
