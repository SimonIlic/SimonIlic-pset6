package mprog.simon.urlshortnr;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;

/** History activity.
 * Allowing users to view their URL shorten history and link clicks
 *
 * Created by Simon Ilic
 **/
public class HistoryActivity extends AppCompatActivity implements LookupURLAsyncTask.AsyncResponse {
    private DatabaseReference mDatabase;
    private String mUserId;

    // intit variables
    private HistoryListArrayAdapter mAdapter;
    private Activity mActivity;
    private LookupURLAsyncTask.AsyncResponse mResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // allow for up navigation
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get context and response reference
        mActivity = this;
        mResponse = this;

        // Initialize Firebase Auth and Database Reference
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new LookupURLAsyncTask(this, this);

        // if user is logged in, continue
        if (mFirebaseUser == null) {
            TextView welcomeTV = (TextView) findViewById(R.id.historyWelcomeMessage);
            welcomeTV.setText(R.string.login_required_message);
        }
        else {
            mUserId = mFirebaseUser.getUid();
            createList();
        }

    }

    /** Creates the History list using HistoryListArrayAdapter**/
    private void createList() {
        // Set up ListView
        //empty arraylist to pass to adapter
        ArrayList<JSONObject> data = new ArrayList<>();

        // add a header to the data
        JSONObject header = null;
        try {
            header = new JSONObject("{\"id\":\"goo.gl/\",\"longUrl\":\"long url\",\"analytics\":{\"allTime\":{\"shortUrlClicks\":\"clicks\"}}}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data.add(header);

        final ListView listView = (ListView) findViewById(R.id.historyList);
        mAdapter = new HistoryListArrayAdapter(this, data, R.layout.history_list_item);
        listView.setAdapter(mAdapter);

        // Register ListView for context menu, implicitly defining item longclick listener
        registerForContextMenu(listView);

        mDatabase.child("users").child(mUserId).child("links").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // lookup url analytics and populate list on result
                LookupURLAsyncTask asyncTask = new LookupURLAsyncTask(mActivity, mResponse);
                asyncTask.execute((String) dataSnapshot.child("id").getValue());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                recreate();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /** context menu for long clicks **/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    /** handle context menu logic **/
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // do not allow for deletion/copying of header
        if (info.position == 0) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_copy_short_url:
                copyUrl(info.targetView, false);
                return true;
            case R.id.action_copy_long_url:
                copyUrl(info.targetView, true);
                return true;
            case R.id.action_delete:
                deleteUrl(info.targetView);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /** delete a url from users database **/
    private void deleteUrl(View targetView) {
        // get url id
        TextView idTV = (TextView) targetView.findViewById(R.id.idListTV);
        String id = idTV.getText().toString();

        // delete url from database
        mDatabase.child("users").child(mUserId).child("links")
                .child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChildren()) {
                            DataSnapshot firstChild = dataSnapshot.getChildren().iterator().next();
                            firstChild.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /** copy a url from the listview to the users clipboard **/
    private void copyUrl(View targetView, boolean targetIsLongUrl) {
        // get the long or short url from the corresponding textview
        if (targetIsLongUrl) {
            TextView longUrlTV = (TextView) targetView.findViewById(R.id.longUrlListTV);
            copyUrlToClipboard(longUrlTV, false);
        }
        else {
            TextView shortUrlTV = (TextView) targetView.findViewById(R.id.idListTV);
            copyUrlToClipboard(shortUrlTV, true);
        }
    }

    /** this overrides the implemented method from lookupURLAsyncResponse **/
    @Override
    public void processFinish(JSONObject output) {
        // check status of url lookup
        String status = "";
        try {
            status = output.getString("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // only append to listview if response was OK
        if (status.contentEquals("OK")) {
            mAdapter.add(output);
        }
    }

    /** A helper function that copies a text from a view to the phones clipboard **/
    public void copyUrlToClipboard(View view, boolean isShortUrlView) {
        TextView urlTextView = (TextView) view;

        // get url from text view
        String url = urlTextView.getText().toString();

        // if the url is a short url add the goo.gl domain to the textview text
        if (isShortUrlView) {
            url = "https://goo.gl/" + url;
        }

        // add url to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", url);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Copied URL to clipboard", Toast.LENGTH_SHORT).show();
    }
}