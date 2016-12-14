package mprog.simon.urlshortnr;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * An AsyncTask Class to fetch a shortened URL from the goo.gl API
 */

public class ShortenURLAsyncTask extends AsyncTask<String, Void, String> {

    public AsyncResponse delegate = null;
    private Activity mActivity;

    public ShortenURLAsyncTask(Activity activity, AsyncResponse delegate){
        mActivity = activity;
        this.delegate = delegate;
    }

    // Interface to relay data back to ShortenURLActivity
    public interface AsyncResponse {
        void processFinish(JSONObject output);
    }

    protected void onPreExecute() {
        Toast.makeText(mActivity.getApplicationContext(),
                "Shortening URL", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(String... args) {

        // api URL including app specific api key
        String api_url = "https://www.googleapis.com/urlshortener/v1/url?" +
                "key=AIzaSyDy0G2e5RPk0FwiZ8d4KBEpe8bggh3G3Uk";

        URL url = null;
        try {
            // create url
            url = new URL(api_url);

            // open connection
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            // create POST request body
            // body JSON as string (inserting the searched URL, removing space chars)
            String outString = "{\"longUrl\":\"" + args[0] + "\"}";
            // convert outstring to byte array
            byte[] out = outString.getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }

            int responseCode = http.getResponseCode();
            //Toast.makeText(mActivity, Integer.toString(responseCode), Toast.LENGTH_SHORT).show();


            // handle response if OK
            if (responseCode == 200) {
                try {
                    // get json data as string
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();

                } finally {
                    http.disconnect();
                }
            }
            // if response is bad, disconnect and relay error message
            else {
                http.disconnect();
                Log.e("ERROR", Integer.toString(responseCode));
                return "ERROR " + Integer.toString(responseCode);
            }
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return "ERROR shortening URL";
        }
    }

    protected void onPostExecute(String result) {
        // A space can never be in a correct link, therefor it must indicate an error
        // show error message to user if shortening the link failed
        if (result.contains("ERROR ")) {
            Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
            return;
        }

        // JSON-ify the result string
        // and delegate to ShortenURLActivity
        JSONObject resultJSON = null;
        try {
            resultJSON = new JSONObject(result);
            delegate.processFinish(resultJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
