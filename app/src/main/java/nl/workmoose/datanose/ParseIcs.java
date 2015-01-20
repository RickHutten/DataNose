package nl.workmoose.datanose;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ParseIcs {

    private Context context;
    final private static String FILE_NAME = "WARNING: DO NOT OPEN, VERY DANGEROUS FILE";

    public ParseIcs(Context context) {
        this.context = context;
    }

    public ArrayList readFile() {
        long startTime = System.currentTimeMillis();
        System.out.println("Parsing file...");
        ArrayList<String> event = new ArrayList<>();
        ArrayList<ArrayList<String>> eventList = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            String line;
            String begin = "";
            String end = "";
            String name = "";
            String location = "";
            String teacher = "";
            String uid = "";

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
                        location = context.getResources().getString(R.string.location_unknown);
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
                        System.out.println("Parsing went wrong :(");
                    }
                    // Put values in eventList
                    eventList.add((ArrayList<String>) event.clone());
                    event.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        System.out.println("Done parsing");
        System.out.println("Parsed in " + duration + " milliseconds");

        return eventList;
    }
}
