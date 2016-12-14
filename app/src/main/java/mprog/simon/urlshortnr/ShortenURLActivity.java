package mprog.simon.urlshortnr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class ShortenURLActivity extends AppCompatActivity implements ShortenURLAsyncTask.AsyncResponse {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shorten_url);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // go to sign in activity
                Intent intent = new Intent(ShortenURLActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shorten_url, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                return true;

            case R.id.action_search:
                // go to look up activity
                Intent intent = new Intent(this, LookupURLActivity.class);
                startActivity(intent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //this override the implemented method from AsyncResponse
    @Override
    public void processFinish(JSONObject output){
        String shortUrl = null;
        try {
            shortUrl = output.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView urlView = (TextView) findViewById(R.id.shortUrlTV);
        urlView.setText(shortUrl);
        urlView.setVisibility(View.VISIBLE);
    }

    public void shortenUrl(View view) {
        // Hide url display
        TextView urlView = (TextView) findViewById(R.id.shortUrlTV);
        urlView.setVisibility(View.INVISIBLE);

        // get url to shorten
        EditText urlTextView = (EditText) findViewById(R.id.urlEditText);
        String url = urlTextView.getText().toString();

        // do not start async short url lookup if query is not a valid url
        if (!isValidUrl(url)) {
            Toast.makeText(this, "Please provide a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // create asyncTask class instance and execute
        ShortenURLAsyncTask asyncTask = new ShortenURLAsyncTask(this, this);
        asyncTask.execute(url);
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
