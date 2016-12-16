package mprog.simon.urlshortnr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class LookupURLActivity extends AppCompatActivity implements LookupURLAsyncTask.AsyncResponse{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup_url);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void copyUrlToClipboard(View view) {
        TextView urlTextView = (TextView) view;
        // return if no url was looked up
        if (urlTextView.getVisibility() == View.INVISIBLE) {
            return;
        }

        // get url from text view
        String url = urlTextView.getText().toString();

        // add url to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Short URL", url);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Copied URL to clipboard", Toast.LENGTH_SHORT).show();
    }

    public void lookupUrl(View view) {
        // Hide url display
        TextView urlView = (TextView) findViewById(R.id.longUrlTV);
        urlView.setVisibility(View.INVISIBLE);

        // get url to shorten
        EditText urlET = (EditText) findViewById(R.id.shortUrlEditText);
        String url = urlET.getText().toString();

        // do not start async short url lookup if query is not a valid url
        if (!isValidUrl(url)) {
            Toast.makeText(this, "Please provide a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // create asyncTask class instance and execute
        LookupURLAsyncTask asyncTask = new LookupURLAsyncTask(this, this);
        asyncTask.execute(url);
    }

    @Override
    public void processFinish(JSONObject output) {
        String longUrl = null;
        try {
            longUrl = output.getString("longUrl");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView urlView = (TextView) findViewById(R.id.longUrlTV);
        urlView.setText(longUrl);
        urlView.setVisibility(View.VISIBLE);
    }

    /** A helper function that validates a url on a very basic level **/
    public boolean isValidUrl(String urlString) {

        // try to form a url from the string, if succesful return true
        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }
}
