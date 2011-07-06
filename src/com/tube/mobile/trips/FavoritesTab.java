package com.tube.mobile.trips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.tuberater.R;
import com.tuberater.data.DatabaseManager;

public class FavoritesTab extends ListActivity {
	
	/*
	 * A tab that shows a list of historical trips, ranked by how many times that trip was taken
	 * This tab will either load tube trips or bus trips
	 */
	
	private final static String LOG_TAG = "FavoritesTab";
	
	public final static String TYPE = "type";
	private int modality;
	
	public final static int LOCATE_LINE_REPORT = 1;
	public final static int LOCATE_STATION_REPORT = 2;
	public final static int ADD_BUS_TRIP = 3;
	
	public final static int ORIGIN_INTENT = 1;
	public final static int DESTINATION_INTENT = 2;
	
	public final static String SELECTED_TRIP = "selectedTrip", SELECTED_MODE = "selectedMode";
	
	private SimpleAdapter adapter;
	private final List<HashMap<String, String>> data;
	private DatabaseManager dbManager;
	
	private String origin, destination, origin_line, destination_line;
	
	private final static String tripId = "trip", tripCount = "count", tripKey = "key";
	private final static String[] from = new String[] {tripId, tripCount, tripKey};
	private final static int[] to = new int[] {R.id.tripName, R.id.dateText};
	
	public FavoritesTab()
	{	
		data = new ArrayList<HashMap<String, String>>();		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		dbManager = new DatabaseManager(this);
		
		modality = getIntent().getIntExtra(TYPE, DatabaseManager.UNKNOWN);
		loadHistory();
		
		setContentView(R.layout.historylayout);
		((TextView) findViewById(R.id.detailsText)).setText("Select from favourites:");
		((TextView) findViewById(R.id.noTripsText)).setText("You have no favourites. Add a new trip below!");
		
		adapter = new SimpleAdapter(this, data, R.layout.list_item_with_icon, from, to);
		final ListView lv= getListView();
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long selectedPosition)
			{
				int key = (new Integer(data.get((int)selectedPosition).get(tripKey))).intValue();
				submit(key);
			}
		});
		
		if (data.size() != 0)
		{
			(findViewById(R.id.noTripsText)).setVisibility(View.GONE);
		}
		else (findViewById(R.id.noTripsText)).setVisibility(View.VISIBLE);
		
		((Button) findViewById(R.id.newTripButton)).setOnClickListener(
				new Button.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						addNewTrip();
					}
				}
		);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dbManager.close();
	}
	
	/*
	 * ==== Set result and finish
	 */
	
	private void submit(int key)
	{
		Log.d(LOG_TAG+" ["+modality+"]", "Returning selection: "+key);
		final Intent result = this.getIntent();
		result.putExtra(SELECTED_TRIP, key);
		
		this.setResult(RESULT_OK, result);
		this.getParent().setResult(RESULT_OK, result); // since it's in a tab
		finish();
	}
	
	private void insertAndFinish(String o, String d, String type)
	{
		int key = dbManager.insertNewTrip(o, d, type);
		submit(key);
	}
	
	/*
	 * ==== Load data 
	 */
	
	private void close(Cursor r)
	{
		r.close();
		dbManager.close();
	}
	
	private void loadHistory()
	{
		data.clear();
		Cursor result = dbManager.getAllTrips(modality);
		if (result != null)
		{
			if (result.getCount() > 0)
			{
				int key = result.getColumnIndex(DatabaseManager.tripKey);
				int origin_index = result.getColumnIndex(DatabaseManager.origin);
				int destination_index = result.getColumnIndex(DatabaseManager.destination);
				int count_index = result.getColumnIndex(DatabaseManager.tripCount);
				int busNumber = result.getColumnIndex(DatabaseManager.modality);
				
				result.moveToFirst();
				while (!result.isAfterLast())
				{	
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(tripKey, ""+result.getInt(key));
					if (modality == DatabaseManager.TUBE)
					{
						map.put(tripId, result.getString(origin_index)+" to "+result.getString(destination_index));
					}
					else {
						map.put(tripId, result.getString(busNumber)+", "+result.getString(origin_index)+" to "+result.getString(destination_index));
					}
	
					
					int count = result.getInt(count_index);
					if (count != 1) map.put(tripCount, "You have rated this trip "+(new Integer(count).toString())+" times.");
					else map.put(tripCount, "You have rated this trip 1 time.");
					
					data.add(map);
					result.moveToNext();
				}
			}
			close(result);
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (resultCode == RESULT_OK)
    	{
    		switch(requestCode)
        	{
        	case LOCATE_LINE_REPORT:
        		final String line = data.getStringExtra(TubeLocatorActivity.SELECTED_LINE);
        		if (origin_line == null)
        		{
        			origin_line = line;
        			Toast.makeText(getApplicationContext(), "From: "+origin_line,Toast.LENGTH_SHORT).show();
        		}
        		else if (!origin_line.equals(line))
        		{
        			destination_line = line;
        			Toast.makeText(getApplicationContext(), "To: "+destination_line,Toast.LENGTH_SHORT).show();
        		}
        		startActivityForResult((new Intent(this, TubeLocatorActivity.class))
        				.putExtra(TubeLocatorActivity.RETURN_TYPE, LOCATE_STATION_REPORT)
        				.putExtra(TubeLocatorActivity.SELECTED_LINE_ID, data.getIntExtra(TubeLocatorActivity.SELECTED_LINE_ID, -1)), LOCATE_STATION_REPORT);
        		break;
        		
        	case LOCATE_STATION_REPORT:
        		if (origin == null)
        		{
        			origin = data.getStringExtra(TubeLocatorActivity.SELECTED_STATION);
        			Toast.makeText(getApplicationContext(), "From: "+origin,Toast.LENGTH_SHORT).show();
        			selectLine(false);
        		}
        		else if (destination == null)
        		{
        			destination = data.getStringExtra(TubeLocatorActivity.SELECTED_STATION);
        			if (origin.equals(destination))
        			{
        				Toast.makeText(getApplicationContext(), "Error: Origin is same as destination.",Toast.LENGTH_SHORT).show();
        				return;
        			}
        			else {
        				Toast.makeText(getApplicationContext(), "To: "+destination,Toast.LENGTH_SHORT).show();
        				String lines = origin_line;
        				if (destination_line != null) lines += ","+destination_line;
        				insertAndFinish(origin, destination, DatabaseManager.TUBE_ENTRY);
        			}
        		}
        		break;
        		
        	case ADD_BUS_TRIP:
        		origin = data.getStringExtra(BusLocatorActivity.ORIGIN);
        		destination = data.getStringExtra(BusLocatorActivity.DESTINATION);
        		String busNumber = data.getStringExtra(BusLocatorActivity.BUS_NUMBER);
        		insertAndFinish(origin, destination, busNumber);
        		break;
        	}
    	}
    }
	
	private void selectLine(boolean origin)
	{
		final Intent intent = new Intent(this, TubeLocatorActivity.class);
        intent.putExtra(TubeLocatorActivity.RETURN_TYPE, LOCATE_LINE_REPORT);
        if (origin)
        {
        	intent.putExtra(TubeLocatorActivity.OD_TYPE, ORIGIN_INTENT);
        }
        else intent.putExtra(TubeLocatorActivity.OD_TYPE, DESTINATION_INTENT);
        startActivityForResult(intent, LOCATE_LINE_REPORT);
	}
	
	private void addNewTrip()
	{
		if (modality == DatabaseManager.TUBE)
		{
			origin = null;
			destination = null;
			origin_line = null;
			destination_line = null;
			selectLine(true);
		}
		else {
			final Intent intent = new Intent(this, BusLocatorActivity.class);
	        startActivityForResult(intent, ADD_BUS_TRIP);
		}
	}
}
