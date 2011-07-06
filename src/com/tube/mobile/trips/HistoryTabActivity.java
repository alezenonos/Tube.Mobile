package com.tube.mobile.trips;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.tuberater.R;
import com.tuberater.data.DatabaseManager;

public class HistoryTabActivity extends ListActivity {
	
	public final static String selectedItem = "sel_item";
	public final static String selectedTrip = "sel_trip";
	public final static String selectedOrigin = "sel_orig";
	
	/*
	 * Shows all the user's historical trips and allows the user to add more trips
	 */
	
	private final static String LOG_TAG = "--HistoryTabActivity--";
	
	private SimpleAdapter adapter;
	private final List<HashMap<String, String>> data;
	private DatabaseManager dbManager;
	
	private final static int GET_TRIP = 0;
	private final static int REROUTE_TRIP = 1;

	private final static String tripId = "trip", tripRating = "ratingValue", tripDate = "date", ratingId = "ratingId";
	private final static String[] from = new String[] {tripId, tripRating, tripDate, ratingId};
	private final static int[] to = new int[] {R.id.tripName, R.id.ratingText, R.id.dateText};
	
	public HistoryTabActivity()
	{	
		data = new ArrayList<HashMap<String, String>>();		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		dbManager = new DatabaseManager(this);
		loadHistory();
		
		setContentView(R.layout.historylayout);
		adapter = new SimpleAdapter(this, data, R.layout.list_item_with_icon, from, to);
		
		final ListView lv= getListView();
		lv.setAdapter(adapter);
		
		((Button) findViewById(R.id.newTripButton)).setOnClickListener(
				new Button.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						getNewTrip();
					}
				}
		);
		registerForContextMenu(lv);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dbManager.close();
	}
	
	@Override
    protected void onResume()
	{
        super.onResume();
        updateScreen();
    }
	
	private void updateScreen()
	{
		loadHistory();
		adapter.notifyDataSetChanged();
		if (data.size() != 0)
		{
			(findViewById(R.id.noTripsText)).setVisibility(View.GONE);
		}
		else (findViewById(R.id.noTripsText)).setVisibility(View.VISIBLE);
	}
	
	/*
	 * ===== CONTEXT MENU =====
	 */
	
	private final static int MODIFY_RATING = 0;
	private final static int DELETE = 1;
	private final static int CANCEL = 2;
	private final static int MODIFY_DEST = 3;
	private final static int ADD_RETURN = 4;
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Options");
		menu.add(0, MODIFY_RATING, 0, "Change rating");
		menu.add(0, MODIFY_DEST, 1, "Change destination");
		menu.add(0, ADD_RETURN, 2, "Add return trip");
		menu.add(0, DELETE, 3, "Delete trip");
		menu.add(0, CANCEL, 4, "Cancel");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		int selectedItem = ((AdapterContextMenuInfo) menuItem.getMenuInfo()).position;
		switch (menuItem.getItemId())
		{
		case MODIFY_RATING:
			reRateTrip(selectedItem);
			return true;
			
		case MODIFY_DEST:
			newDestination(selectedItem);
			return true;

		case DELETE:
			deleteRating(selectedItem);
			return true;
			
		case ADD_RETURN:
			addReturnTrip(selectedItem);
			return true;
			
		case CANCEL:
			return true;

		default:
			return super.onContextItemSelected(menuItem);
		}
	}
	
	/*
	 * ===== Trips =====
	 */
	
	private void loadHistory()
	{
		data.clear();
		Cursor result = dbManager.getAllTrips(DatabaseManager.ALL);
		if (result != null)
		{
			if (result.getCount() > 0)
			{
				int origin_index = result.getColumnIndex(DatabaseManager.origin);
				int destination_index = result.getColumnIndex(DatabaseManager.destination);
				int id_index = result.getColumnIndex(DatabaseManager.tripKey);
				int modality_index = result.getColumnIndex(DatabaseManager.modality);
				
				HashMap<Integer, String> tripKeys = new HashMap<Integer, String>();
				result.moveToFirst();
				while (!result.isAfterLast())
				{
					Integer key = result.getInt(id_index);
					String mode = result.getString(modality_index);
					String entry = result.getString(origin_index)+" to "+result.getString(destination_index);
					
					Log.d(LOG_TAG, key+", "+mode+", "+entry);
					if (!mode.equals(DatabaseManager.TUBE_ENTRY))
					{
						entry = mode+", "+entry;
					}
					tripKeys.put(key, entry);
					result.moveToNext();
				}
				close(result);
				loadRatings(tripKeys);
			}
			close(result);
		}
	}
	
	private void close(Cursor r)
	{
		r.close();
		dbManager.close();
	}
	
	private void getNewTrip()
	{
		startActivityForResult(new Intent(this, FavoritesTabActivity.class), GET_TRIP);
	}
	
	private void newDestination(int tripIndex)
	{
		int ratingKey = (new Integer(data.get(tripIndex).get(ratingId))).intValue();
		boolean bus = dbManager.isBus(ratingKey);
		
		Intent intent;
		if (bus)
		{
			intent = new Intent(this, BusLocatorActivity.class);
			intent.putExtra(selectedItem, ratingKey);
			startActivityForResult(intent, REROUTE_TRIP);
		}
		else {
			intent = new Intent(this, SelectStationActivity.class);
			intent.putExtra(selectedItem, tripIndex);
			intent.putExtra(selectedTrip, ratingKey);
			intent.putExtra(selectedOrigin, data.get(tripIndex).get(tripId).split(" to ")[0]);
			startActivityForResult(intent, REROUTE_TRIP);
		}
	}
	
	private void addReturnTrip(int tripIndex)
	{
		int ratingKey = (new Integer(data.get(tripIndex).get(ratingId))).intValue();
		int tripKey = dbManager.getTripKey(ratingKey);
		Log.d(LOG_TAG, "Updating rating: "+ratingKey+", trip "+tripKey);
		
		Cursor tripDetails = dbManager.getTripDetails(tripKey);
		tripDetails.moveToFirst();
		String newDest = tripDetails.getString(tripDetails.getColumnIndex(DatabaseManager.origin));
		String newOrig = tripDetails.getString(tripDetails.getColumnIndex(DatabaseManager.destination));
		String mode = tripDetails.getString(tripDetails.getColumnIndex(DatabaseManager.modality));
		
		double miles = tripDetails.getDouble(tripDetails.getColumnIndex(DatabaseManager.milesValue));
		int points = tripDetails.getInt(tripDetails.getColumnIndex(DatabaseManager.pointsValue));
		
		tripKey = dbManager.insertNewTrip(newOrig, newDest, mode);
		if (points != DatabaseManager.UNKNOWN)
		{
			dbManager.updateTripDetails(tripKey, miles, points);
		}
		
		Intent intent = new Intent(this, TripActivity.class);
		intent.putExtra(FavoritesTab.SELECTED_TRIP, tripKey);
		startActivity(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			switch(requestCode)
			{
			case GET_TRIP:
				Intent intent = new Intent(this, TripActivity.class);
				intent.putExtra(FavoritesTab.SELECTED_TRIP, data.getIntExtra(FavoritesTab.SELECTED_TRIP, 0));
				startActivity(intent);
				break;
				
			case REROUTE_TRIP:
				loadHistory();
				this.adapter.notifyDataSetChanged();
				break;
			}
		}
	}
	
	/*
	 * ===== Ratings ===== 
	 */
	
	private void loadRatings(HashMap<Integer, String> tripKeys)
	{
		Cursor result = dbManager.getRecentRatings(LOG_TAG+".loadRatings()");
		if (result != null)
		{
			if (result.getCount() > 0)
			{
				int id_index = result.getColumnIndex(DatabaseManager.tripKey);
				int rating_index = result.getColumnIndex(DatabaseManager.ratingValue);
				int ratingId_index = result.getColumnIndex(DatabaseManager.ratingKey);
				int date_index = result.getColumnIndex(DatabaseManager.timeStamp);
				
				final SimpleDateFormat reader = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				final SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy (HH:mm)");
				
				result.moveToFirst();
				while (!result.isAfterLast())
				{
					HashMap<String, String> map = new HashMap<String, String>();
					String trip = tripKeys.get(result.getInt(id_index));
					Log.d(LOG_TAG, result.getInt(id_index)+", "+trip);
					if (trip != null)
					{
						try
						{
							int rating = result.getInt(rating_index);
							map.put(tripId, trip);
							map.put(tripRating, (new Integer(rating)).toString());
							map.put(ratingId, ""+result.getInt(ratingId_index));
							map.put(tripDate, formatter.format(reader.parse(result.getString(date_index))));
							data.add(map);
						} catch (ParseException e) { }
					}
					result.moveToNext();
				}
			}
			close(result);
		}
	}
	
	private void reRateTrip(int selectedTrip)
	{
		final HashMap<String, String> ratingData = data.get(selectedTrip);
		final int ratingKey = (new Integer(ratingData.get(ratingId))).intValue();
		reRateTrip(selectedTrip, ratingKey);
	}
	
	public void setRating(int selectedTrip, int newValue)
	{
		final HashMap<String, String> ratingData = data.get(selectedTrip);
		ratingData.put(tripRating, ""+newValue);
		adapter.notifyDataSetChanged();
	}
	
	private void reRateTrip(final int trip, final int ratingKey)
	{
		final RaterDialog rater = new RaterDialog(this, dbManager, trip, ratingKey);
		rater.create().show();
	}
	
	private void deleteRating(int selectedRating)
	{
		final HashMap<String, String> ratingData = data.get(selectedRating);
		final int ratingKey = (new Integer(ratingData.get(ratingId))).intValue();
		
		final DatabaseManager dbManager = new DatabaseManager(this);
		dbManager.deleteRating(ratingKey);
		dbManager.close();
		
		data.remove(selectedRating);
		this.updateScreen();
		Toast.makeText(getApplicationContext(), "Rating deleted!",Toast.LENGTH_LONG).show();
	}
}
