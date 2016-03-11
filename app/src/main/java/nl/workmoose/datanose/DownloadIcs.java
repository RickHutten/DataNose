package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.gc.materialdesign.widgets.SnackBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 *
 * This class downloads the iCalendar file for the given student ID.
 * The file being downloaded is from 'https://datanose.nl/' + studentID + '.ics'.
 */
 public class DownloadIcs extends AsyncTask<String, Void, String> {

    final private static String SHARED_PREF = "prefs";
    final private static String URL_STRING = "https://datanose.nl/";
    final private static String EXTENSION = ".ics";
    final private static String FILE_NAME = "WARNING: DO NOT OPEN, VERY DANGEROUS FILE";
    final private static int READ_TIMEOUT = 25000;
    final private static int CONN_TIMEOUT = 25000;

    private Context context;
    private String studentId;
    private SharedPreferences sharedPref;

    public DownloadIcs(Context context) {
        this.context = context;
    }

    /**
     *
     * @param params: Only contains exactly 1 argument: the student ID
     * @return String: Message to the user if something went wrong, or "File downloaded"
     * if everything went good.
     */
    @Override
    protected String doInBackground(String... params) {
        String msg;  // Param that will be passed on to onPostExecute
        studentId = params[0];  // The param that is given using .execute(param)
        String urlString = URL_STRING + studentId + EXTENSION;
        Log.i("DownloadIcs", "Downloading file for student: " + studentId);
        sharedPref = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("isDownloading", true).apply();

        if (hasInternetConnection(context)) {
            try {
                // Try to download file
                Log.i("DownloadIcs", urlString);
                downloadFromUrl(urlString);
                msg = "File downloaded";
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                msg = context.getString(R.string.timeout);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                msg = context.getString(R.string.file_not_fount);
            } catch (ConnectException e) {
                e.printStackTrace();
                msg = context.getString(R.string.connection_error);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                msg = context.getString(R.string.unknown_host);
            } catch (IOException e) {
                e.printStackTrace();
                msg = context.getString(R.string.unknown_error);
            }
        } else {
            msg = context.getString(R.string.no_internet_connection);
        }
        // Return the message to onPostExecute
        return msg;
    }

    /**
     * If file is downloaded succesfully, start ScheduleActivity.
     * If file is not downloaded succesfully, notify the user and restart LoginActivity.
     * @param result: result string returned from doInBackGround.
     */
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.i("DownloadIcs", "Result: " + result);
        sharedPref.edit().putBoolean("isDownloading", false).apply();


        if (result.equals("File downloaded")) {
            // Get calendar instance for this time
            Calendar now = Calendar.getInstance();

            // Save correct student ID to sharedPreferences
            sharedPref.edit().putString("studentId", studentId).apply();
            Log.i("DownloadIcs", "Currently logged in: " + studentId);

            // Save the time the last iCal was downloaded
            sharedPref.edit().putLong("lastDownloaded", now.getTimeInMillis()).apply();

            try{
                //  Start ScheduleActivity
                Activity currentActivity = (Activity) context;
                Log.i("DownloadIcs", "Done downloading, start new ScheduleActivity");

                if (!sharedPref.getBoolean("syncSaved", false)) {
                    // If the file is only being downloaded without syncing, set refreshing to false
                    sharedPref.edit().putBoolean("refreshing", false).apply();
                }

                Intent intent = new Intent(context, ScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);
            } catch (Exception e) {
                // When called from receiver
                Log.i("DownloadIcs", "Done downloading");
                if (!sharedPref.getBoolean("syncSaved", false)) {
                    // If the file is only being downloaded without syncing, set refreshing to false
                    sharedPref.edit().putBoolean("refreshing", false).apply();
                }
            }
        } else {

            // There was an error during download
            // Show the user what error occurred
            try {
                new SnackBar((Activity) context, result).show();
            } catch (Exception e) {
                // Called from the receiver (alarm)
                // TODO: notify the user that the schedule is not updated
            }
            try {
                // Try casting it to LoginActivity
                LoginActivity loginActivity = (LoginActivity) context;
                loginActivity.backToBeginning();
            } catch (ClassCastException e) {
                // The calling Activity is not LoginActivity but ScheduleActivity
                ScheduleActivity scheduleActivity = (ScheduleActivity) context;
                sharedPref.edit().putBoolean("refreshing", false).apply();

                // Hide the refreshing container
                View refreshingContainer = scheduleActivity.findViewById(R.id.refreshContainer);
                refreshingContainer.setVisibility(View.INVISIBLE);

            }
        }
    }

    /**
     * Check whether the user has an internet connection
     * @param context: activity context to call getSystemService
     * @return boolean: Boolean, true if the user has a connection, false if not.
     */
    private boolean hasInternetConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Download the file from the given string. Completes without errors if a new .ics file
     * is successfully downloaded else throws error, which is catched by doInBackground
     * and the error will be shown to the user
     * @param urlString: The url of the iCalendar file
     */
    private void downloadFromUrl(String urlString) throws IOException {
        // Set up the connection to the site
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set read- and connection timeout and shit
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONN_TIMEOUT);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        Log.i("DownloadIcs", "Downloading new XML file");

        // Make new file
        File file = new File(context.getFilesDir(), FILE_NAME);
        FileOutputStream fileOutput = new FileOutputStream(file);

        // Get content from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        // Write to file
        String line;
        while ((line = reader.readLine()) != null) {
            fileOutput.write((line + "\n").getBytes());
        }
        reader.close();

        // Close the fileOutput
        fileOutput.close();

        // The file is downloaded successfully
    }
}