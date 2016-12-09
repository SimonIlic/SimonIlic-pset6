package mprog.simon.urlshortnr;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An AsyncTask Class to fetch a shortened URL from the goo.gl API
 */

public class ShortenURLAsyncTask extends AsyncTask<String, Void, String> {

    private AsyncResponse delegate = null;
    private Activity mActivity;

    public ShortenURLAsyncTask(Activity activity, AsyncResponse delegate){
        mActivity = activity;
        this.delegate = delegate;
    }

    // Interface to relay data back to ShortenURLActivity
    public interface AsyncResponse {
        void processFinish(String output);
    }

    protected void onPreExecute() {
        Toast.makeText(mActivity.getApplicationContext(),
                "Shortening URL", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(String... strings) {
        return null;
    }

    protected void onPostExecute(String result) {
        if (result == null) {
            return;
        }

        // JSON-ify the result string
        // handle error returns from omdb
        JSONObject movieInfo = null;
        try {
            movieInfo = new JSONObject(result);
            if (!movieInfo.getBoolean("Response")) {
                Toast.makeText(mActivity, movieInfo.getString("Error"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(mActivity.getApplicationContext(), "Fetched data",
                        Toast.LENGTH_SHORT).show();

                delegate.processFinish(result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
