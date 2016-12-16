package mprog.simon.urlshortnr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A custom ArrayAdapter that polpulates the history/tracker list based on
 * goo.gl JSON response
 *
 * Created by Simon Ilic
 *
 * loosely based on this tutorial:
 * http://www.vogella.com/tutorials/AndroidListView/article.html#listsactivity_layout
 **/
public class HistoryListArrayAdapter extends ArrayAdapter<JSONObject> {
    private final Context context;
    private final ArrayList<JSONObject> data;
    private final int layout;

    public HistoryListArrayAdapter(Context context, ArrayList<JSONObject> data, int layout) {
        super(context, -1, data);
        this.data = data;
        this.context = context;
        this.layout = layout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate row layout view
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(layout, parent, false);

        // get list tv's
        TextView idTV = (TextView) rowView.findViewById(R.id.idListTV);
        TextView longUrlTV = (TextView) rowView.findViewById(R.id.longUrlListTV);
        TextView clicksTV = (TextView) rowView.findViewById(R.id.clicksListTV);

        // unpack relevant JSON data
        JSONObject itemJSON = data.get(position);
        String shortUrl = "/0";
        String longUrl = "";
        String clicks = "";
        try {
            shortUrl = itemJSON.getString("id");
            longUrl = itemJSON.getString("longUrl");
            clicks = itemJSON.getJSONObject("analytics")
                    .getJSONObject("allTime")
                    .getString("shortUrlClicks");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // get just the id part of the short URL to display (unless its the header)
        String id;
        if (position != 0) {
            id = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);
        }
        else {
            id = shortUrl;
        }

        // set data to corresponding views
        idTV.setText(id);
        longUrlTV.setText(longUrl);
        clicksTV.setText(clicks);

        return rowView;
    }
}