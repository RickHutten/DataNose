package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

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
            if (validStudentNumber(studentId)) {
                msg = "Correct";
            } else {
                msg = context.getString(R.string.invalid_student_id);
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
        } else {
            new SnackBar((Activity) context, result).show();
            LoginActivity loginActivity = (LoginActivity) context;
            loginActivity.backToBeginning();
        }
    }

    private Boolean validStudentNumber(String studentId) {
        final String CHECK_URL =
                "http://content.datanose.nl/Timetable.svc/GetCoursesByStudent?id=";
        try {
            URL url = new URL(CHECK_URL + studentId);
            HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
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
