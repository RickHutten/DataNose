package nl.workmoose.datanose;

import android.content.Context;

public class DataDownloadManager {

    final private static String SCHEDULE_PART_1 =
            "http://content.datanose.nl/Timetable.svc/GetActivitiesByStudent?id=";
    final private static String SCHEDULE_PART_2 = "&week=";
    final private static String SCHEDULE_PART_3 = "&acyear=";
    Context context;

    public DataDownloadManager(Context context) {
        this.context = context;
    }

    public void downloadWeekXML(String studentId, int week, int year) {
        String urlString =
                SCHEDULE_PART_1 + studentId + SCHEDULE_PART_2 + week + SCHEDULE_PART_3 + year;
        DownloadXml downloadXml = new DownloadXml(context);
        downloadXml.execute(urlString, week + " " + year);
    }

    public void downloadYearXML(String studentId, int year) {

    }
}
