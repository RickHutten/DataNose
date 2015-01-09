package nl.workmoose.datanose;

import android.app.Activity;
import android.content.Context;
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

public class DownloadXml extends AsyncTask<String, Void, String> {

    private Context context;

    public DownloadXml(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        String msg;
        String urlString = params[0];
        String fileName = params[1];
        System.out.println(urlString);
        if (hasInternetConnection(context)) {
            try {
                downloadFromUrl(urlString, fileName);
                msg = "File downloaded";
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                msg = context.getResources().getString(R.string.timeout);
            } catch (FileNotFoundException e) {
                msg = context.getResources().getString(R.string.webpage_doesnt_exist);
            } catch (ConnectException e) {
                msg = context.getResources().getString(R.string.connection_error);
            } catch (UnknownHostException e) {
                msg = context.getResources().getString(R.string.unknown_host);
            } catch (IOException e) {
                e.printStackTrace();
                msg = context.getResources().getString(R.string.unknown_error);
            }
        } else {
            msg = context.getResources().getString(R.string.no_internet_connection);
        }
        return msg;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result.equals(context.getResources().getString(R.string.no_internet_connection))) {
            SnackBar snackbar = new SnackBar((Activity) context, result);
            snackbar.show();
            LoginActivity loginActivity = (LoginActivity) context;
            loginActivity.backToBeginning();
        } else {
            SnackBar snackbar = new SnackBar((Activity) context, result);
            snackbar.show();
            LoginActivity loginActivity = (LoginActivity) context;
            loginActivity.backToBeginning();
        }
    }

    private boolean hasInternetConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean downloadFromUrl(String urlString, String fileName) throws IOException {
        // returns true if a new XML file is downloaded
        // (if the function is completed whithout errors)
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(20000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Start the connection
            conn.connect();

            System.out.println("Downloading new XML file");

            is = conn.getInputStream();

            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream fileOutput = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bufferLength;

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
        return true;
    }

}

