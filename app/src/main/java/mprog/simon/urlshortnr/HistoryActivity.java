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

public class HistoryActivity extends AppCompatActivity implements LookupURLAsyncTask.AsyncResponse {
    // init firebase variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
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
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new LookupURLAsyncTask(this, this);

        // if user is logged in, continue
        if (mFirebaseUser == null) {
            TextView welcomeTV = (TextView) findViewById(R.id.historyWelcomeMessage);
            welcomeTV.setText(R.string.login_required_message);
        }
        else {
            mUserId = mFirebaseUser.getUid();

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

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            createList();
        }

    }

    private void createList() {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        // only inflate context menu if clicked item is not the header
        TextView idTV = (TextView) v.findViewById(R.id.idListTV);

        Toast.makeText(mActivity, idTV.getText().toString(), Toast.LENGTH_SHORT).show();
        if (idTV.getText().toString().contentEquals("goo.gl/")) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

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

    private void copyUrl(View targetView, boolean targetIsLongUrl) {
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
        // return if no url was looked up
        if (urlTextView.getVisibility() == View.INVISIBLE) {
            return;
        }

        // get url from text view
        String url = urlTextView.getText().toString();

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