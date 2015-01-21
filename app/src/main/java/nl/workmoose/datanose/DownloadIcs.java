package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.gc.materialdesign.widgets.SnackBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class DownloadIcs extends AsyncTask<String, Void, String> {

    final private static String SHARED_PREF = "prefs";
    final private static String URL_STRING = "https://datanose.nl/";
    final private static String EXTENSION = ".ics";
    final private static String FILE_NAME = "WARNING: DO NOT OPEN, VERY DANGEROUS FILE";
    final private static int READ_TIMEOUT = 15000;
    final private static int CONN_TIMEOUT = 20000;

    private Context context;
    private String studentId;

    public DownloadIcs(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        String msg;  // Param that will be passed on to onPostExecute
        studentId = params[0];  // The param that is given using .execute(param)
        String urlString = URL_STRING + studentId + EXTENSION;
        System.out.println("Downloading file for student: " + studentId);

        if (hasInternetConnection(context)) {
            try {
                // Try to download file
                downloadFromUrl(urlString);
                msg = "File downloaded"; // Temporary, if download successfull
            } catch (SocketTimeoutException e) {
                msg = context.getString(R.string.timeout);
            } catch (FileNotFoundException e) {
                msg = context.getString(R.string.file_not_fount);
            } catch (ConnectException e) {
                msg = context.getString(R.string.connection_error);
            } catch (UnknownHostException e) {
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

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        System.out.println("downloadIcs result: " + result);
        if (result.equals("File downloaded")) {
            // Save correct student ID to sharedPreferences
            SharedPreferences s = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
            s.edit().putString("studentId", studentId).apply();
            System.out.println("Currently logged in: " + studentId);

            //  Start Schedul1eActivity
            Intent i = new Intent(context, ScheduleActivity.class);
            LoginActivity currentActivity = (LoginActivity) context;
            currentActivity.startActivity(i);
            currentActivity.overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);

        } else {
            // There was an error during download. Show the error to the user
            new SnackBar((Activity) context, result).show();
            LoginActivity loginActivity = (LoginActivity) context;
            loginActivity.backToBeginning();
        }
    }

    private boolean hasInternetConnection(Context context) {
        // Check whether the user has an internet connection
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void downloadFromUrl(String urlString) throws IOException {
        // Completes without errors if a new .ics file is successfully downloaded
        // (if the function is completed without errors)
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Start the connection
            conn.connect();

            System.out.println("Downloading new XML file");

            is = conn.getInputStream();

            // Make new file
            File file = new File(context.getFilesDir(), FILE_NAME);
            FileOutputStream fileOutput = new FileOutputStream(file);

            byte[] buffer = new byte[1024]; // 1024 is a magic number, don't know how/why it works
            int bufferLength;

            // Write to file
            while ((bufferLength = is.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.close();
        } finally {
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            if (is != null) {
                is.close();
            }
        }
        // The file is downloaded successfully
    }
}

