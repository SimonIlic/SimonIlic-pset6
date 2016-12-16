package mprog.simon.urlshortnr;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Created by Simon on 14-12-2016.
 */

public class LookupURLAsyncTask extends AsyncTask<String, Void, String> {
    public LookupURLAsyncTask.AsyncResponse delegate = null;
    private Activity mActivity;

    public LookupURLAsyncTask(Activity activity, LookupURLAsyncTask.AsyncResponse delegate){
        mActivity = activity;
        this.delegate = delegate;
    }

    // Interface to relay data back to LookupURLActivity
    public interface AsyncResponse {
        void processFinish(JSONObject output);
    }

    protected void onPreExecute() {
        Toast.makeText(mActivity.getApplicationContext(),
                "Looking up URL", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(String... args) {

        // api URL including app specific api key
        String api_url = "https://www.googleapis.com/urlshortener/v1/url?" +
                "key=AIzaSyDy0G2e5RPk0FwiZ8d4KBEpe8bggh3G3Uk";

        // add goo.gl url to http GET request
        api_url += "&shortUrl=" + args[0];

        URL url = null;
        try {
            // create url
            url = new URL(api_url);

            // open connection
            url = new URL(api_url);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            // get HTTP response code
            int responseCode = http.getResponseCode();
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
            return "ERROR looking up URL";
        }
    }

    protected void onPostExecute(String result) {
        // A space can never be in a correct link, therefor it must indicate an error
        // show error message to user if search failed
        if (result.contains("ERROR ")) {
            Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
            return;
        }

        // JSON-ify the result string
        // and delegate to LookupURLActivity
        JSONObject resultJSON = null;
        try {
            resultJSON = new JSONObject(result);
            delegate.processFinish(resultJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
