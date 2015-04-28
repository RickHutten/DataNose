package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.View;

import com.gc.materialdesign.widgets.Dialog;
import com.gc.materialdesign.widgets.SnackBar;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Rick Hutten
 * rick.hutten@gmail.com
 * 10189939
 */
 public class StudentIdChecker extends AsyncTask<String, Void, String> {

    private Context context;
    private String studentId;

    public StudentIdChecker(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        String msg;
        studentId = params[0];
        if (hasInternetConnection(context)) {
            String urlStart =
                    "http://content.datanose.nl/Timetable.svc/GetCoursesByStudent?id=";
            String urlString = urlStart + studentId;
            if (validURL(urlString)) {
                msg = "Correct";
            } else {
                // Something went wrong
                // Check whether ODATA stream is available
                urlString = "http://content.datanose.nl/Timetable.svc";
                if (validURL(urlString)) {
                    // The stream is available, so the previous studentId is incorrect
                    msg = context.getString(R.string.invalid_student_id);
                } else {
                    msg = "ODATA error";
                }
            }
        } else {
            msg = context.getString(R.string.no_internet_connection);
        }
        return msg;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        System.out.println("idChecker result: " + result);
        if (result.equals("Correct")) {
            DownloadIcs downloadIcs = new DownloadIcs(context);
            downloadIcs.execute(studentId);
        } else if (result.equals("ODATA error")) {
            // Called when ODATA is down, show dialog if the user wants to continue
            // whithout checking the given student ID.
            showDialog();
        } else {
            new SnackBar((LoginActivity) context, result).show();
            LoginActivity loginActivity = (LoginActivity) context;
            loginActivity.backToBeginning();
        }
    }

    private void showDialog() {
        Dialog dialog = new Dialog(context, context.getString(R.string.odata_down_title),
                context.getString(R.string.odata_down_message),
                context.getString(R.string.button_cancel),
                context.getString(R.string.button_continue));

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Set listener for the continue button
        dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadIcs downloadIcs = new DownloadIcs(context);
                downloadIcs.execute(studentId);
            }
        });

        // Set listener for the cancel button
        dialog.setOnCancelButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginActivity loginActivity = (LoginActivity) context;
                loginActivity.backToBeginning();
            }
        });

        // Tell the LoginActivity that this is the dialog
        ((LoginActivity) context).dialog = dialog;

        // Show the dialog
        dialog.show();
    }

    private Boolean validURL(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            System.out.println("Checking URL: " + urlString);
            System.out.println("Response code: " + conn.getResponseCode());

            // Response code "307" if for the url "http://content.datanose.nl/Timetable.svc"
            return (conn.getResponseCode() == HttpURLConnection.HTTP_OK || conn.getResponseCode() == 307);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean hasInternetConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
