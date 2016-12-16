package mprog.simon.urlshortnr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class ShortenURLActivity extends AppCompatActivity implements ShortenURLAsyncTask.AsyncResponse {

    // init firebase variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;

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


        // Initialize Firebase Auth and Database Reference
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Display saved username
        // get username as String from shared prefs
        final SharedPreferences sharedPref = getSharedPreferences(
                "mprog.simon.simonilic_pset6_sharedprefs", MODE_PRIVATE);
        String username = sharedPref.getString("username", "");

        // set welcome message with saved name
        TextView welcomeTV = (TextView) findViewById(R.id.welcomeMessage);
        welcomeTV.setText(getString(R.string.welcome_user_fmt, username));
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
            case R.id.action_search: {
                // go to look up activity
                Intent intent = new Intent(this, LookupURLActivity.class);
                startActivity(intent);

                return true;
            }
            case R.id.action_history: {
                // go to history/tracker activity
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);

                return true;
            }
            case R.id.action_sign_out: {
                mFirebaseAuth.signOut();
                // update mFirebasUser to ensure proper logic
                mFirebaseUser = mFirebaseAuth.getCurrentUser();
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /** this override the implemented method from ShortenURLAsyncResponse **/
    @Override
    public void processFinish(JSONObject output){
        // get the shortened URL from the received output JSON
        String shortUrl = "";
        try {
            shortUrl = output.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // display shortened URL
        TextView urlView = (TextView) findViewById(R.id.shortUrlTV);
        urlView.setText(shortUrl);
        urlView.setVisibility(View.VISIBLE);

        // if user is logged in write to database
        if (mFirebaseUser == null) {
            Toast.makeText(this,
                    "Log in with google to save this link in your history",
                    Toast.LENGTH_LONG).show();
        }
        else {
            // get user firebase id
            String userId = mFirebaseUser.getUid();
            // get unique id part of goo.gl url
            String urlId = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

            // write short url to database
            mDatabase.child("users").child(userId).child("links").child(urlId)
                    .child("id").setValue(shortUrl);
        }
    }

    public void shortenUrl(View view) {
        // hide URL display
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
