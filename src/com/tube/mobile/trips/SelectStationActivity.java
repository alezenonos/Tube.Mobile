package com.tube.mobile.trips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

public class SelectStationActivity extends ListActivity {
	
	/*
	 * When changing the destination of trip
	 */
	
	public final static int LOCATE_LINE_REPORT = 1;
	public final static int LOCATE_STATION_REPORT = 2;
	
	public final static String SELECTED_STATION = "selectedTrip";
	
	private SimpleAdapter adapter;
	private final List<HashMap<String, String>> data;
	private DatabaseManager dbManager;
	private String origin;
	private int ratingIndex, itemIndex;
	
	private final static String tripId = "trip", tripCount = "count";
	private final static String[] from = new String[] {tripId, tripCount};
	private final static int[] to = new int[] {R.id.tripName, R.id.dateText};
	
	public SelectStationActivity()
	{	
		data = new ArrayList<HashMap<String, String>>();		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		dbManager = new DatabaseManager(this);
		
		Intent intent = getIntent();
		origin = intent.getStringExtra(HistoryTabActivity.selectedOrigin);
		ratingIndex = intent.getIntExtra(HistoryTabActivity.selectedTrip, 0);
		itemIndex = intent.getIntExtra(HistoryTabActivity.selectedItem, 0);
		loadHistory(origin);
		
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
				submit(data.get((int)selectedPosition).get(tripId));
			}
		});
		
		if (data.size() != 0)
		{
			(findViewById(R.id.noTripsText)).setVisibility(View.GONE);
		}
		else (findViewById(R.id.noTripsText)).setVisibility(View.VISIBLE);
		
		Button b = (Button) findViewById(R.id.newTripButton);
		b.setText("Pick another station...");
		b.setOnClickListener(
				new Button.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						addNewStation();
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
	
	private void submit(String d)
	{
		dbManager.reRoute(ratingIndex, d);
		
		final Intent result = this.getIntent();
		result.putExtra(SELECTED_STATION, d);
		result.putExtra(HistoryTabActivity.selectedTrip, ratingIndex);
		result.putExtra(HistoryTabActivity.selectedItem, itemIndex);
		this.setResult(RESULT_OK, result);
		finish();
	}
	
	private void submit(String o, String d)
	{
		dbManager.insertNewTrip(o, d, DatabaseManager.TUBE_ENTRY);
		dbManager.reRoute(ratingIndex, d);
		
		final Intent result = this.getIntent();
		result.putExtra(SELECTED_STATION, d);
		result.putExtra(HistoryTabActivity.selectedTrip, ratingIndex);
		result.putExtra(HistoryTabActivity.selectedItem, itemIndex);
		this.setResult(RESULT_OK, result);
		finish();
	}
	
	/*
	 * ==== Load data 
	 */
	
	private void close(Cursor r)
	{
		r.close();
		dbManager.close();
	}
	
	private class Station
	{
		private final String name;
		private int count;
		
		public Station(String n)
		{
			name = n;
			count = 1;
		}
	}
	
	private void add(String station, ArrayList<Station> stations)
	{
		for (Station s : stations)
		{
			if (s.name.equals(station))
			{
				s.count++;
				return;
			}
		}
		stations.add(new Station(station));
	}
	
	private void sort(ArrayList<Station> stations)
	{
		for (int i=0; i<stations.size(); i++)
		{
			Station a = stations.get(i);
			int best = i;
			int count = a.count;
			
			for (int j=(i+1); j<stations.size(); j++)
			{
				Station b = stations.get(j);
				int c = b.count;
				if (c > count)
				{
					best = j;
					a = b;
				}
			}
			
			if (best != i)
			{
				stations.set(best, stations.get(i));
				stations.set(i, a);
			}
		}
	}
	
	private void loadHistory(String origin)
	{
		data.clear();
		Cursor result = dbManager.getAllTrips(DatabaseManager.TUBE);
		if (result != null)
		{
			if (result.getCount() > 0)
			{
				int origin_index = result.getColumnIndex(DatabaseManager.origin);
				int destination_index = result.getColumnIndex(DatabaseManager.destination);
				ArrayList<Station> stations = new ArrayList<Station>();
				result.moveToFirst();
				while (!result.isAfterLast())
				{	
					add(result.getString(origin_index), stations);
					add(result.getString(destination_index), stations);
					result.moveToNext();
				}
				sort(stations);
				for (Station s : stations)
				{
					if (!s.name.equals(origin))
					{
						HashMap<String, String> map = new HashMap<String, String>();
						map.put(tripId, s.name);
						
						int count = s.count;
						map.put(tripCount, "You have used this station "+count+" time(s).");
						data.add(map);
					}
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
        		startActivityForResult((new Intent(this, TubeLocatorActivity.class))
        				.putExtra(TubeLocatorActivity.RETURN_TYPE, LOCATE_STATION_REPORT)
        				.putExtra(TubeLocatorActivity.SELECTED_LINE_ID, data.getIntExtra(TubeLocatorActivity.SELECTED_LINE_ID, -1)), LOCATE_STATION_REPORT);
        		break;
        		
        	case LOCATE_STATION_REPORT:
        		String destination = data.getStringExtra(TubeLocatorActivity.SELECTED_STATION);
        		if (!destination.equals(origin)) submit(origin, destination);
        		else {
        			Toast.makeText(getApplicationContext(), "ERROR: New destination can not be "+origin+"!",Toast.LENGTH_LONG).show();
        		}
        		break;
        	}
    	}
    }
	
	private void addNewStation()
	{
		final Intent intent = new Intent(this, TubeLocatorActivity.class);
        intent.putExtra(TubeLocatorActivity.RETURN_TYPE, LOCATE_LINE_REPORT);
        startActivityForResult(intent, LOCATE_LINE_REPORT);
	}
}
