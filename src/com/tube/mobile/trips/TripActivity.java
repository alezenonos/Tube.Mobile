package com.tube.mobile.trips;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tuberater.R;
import com.tuberater.TubeActivity;
import com.tuberater.data.DatabaseManager;

public class TripActivity extends Activity {
	
	private final static String LOG_TAG = "-- TripActivity --";
	private DatabaseManager dbManager;
	
	private String origin, destination, mode;
	private int tripCount, tripPoints, tripKey;
	private double tripMiles;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        dbManager = new DatabaseManager(this);
        setContentView(R.layout.checkinlayout);
        
        ((Button) findViewById(R.id.newTripButton)).setOnClickListener(
				new Button.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						rateTrip();
					}
				}
		);
        
        final Intent intent = getIntent();
        tripKey = intent.getIntExtra(FavoritesTab.SELECTED_TRIP, 0);
        
        loadData();
        if (mode.equals(DatabaseManager.TUBE_ENTRY))
        {
        	((TextView) findViewById(R.id.checkInTitle)).setText(origin+" to "+destination);
        }
        else ((TextView) findViewById(R.id.checkInTitle)).setText(mode+", "+origin+" to "+destination);
        
        setText((TextView) findViewById(R.id.checkInCountValue), tripCount);
		setText((TextView) findViewById(R.id.pointsValue), tripPoints);
		setText((TextView) findViewById(R.id.milesValue), tripMiles);
    }
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dbManager.close();
	}
	
	/*
	 * ===== Load data from the database
	 */
	private void loadData()
	{
		Cursor result = dbManager.getTripDetails(tripKey);
		if (result != null) 
		{
			if (result.getCount() == 1)
			{
				result.moveToFirst();
				mode = result.getString(result.getColumnIndex(DatabaseManager.modality));
				origin = result.getString(result.getColumnIndex(DatabaseManager.origin));
				destination = result.getString(result.getColumnIndex(DatabaseManager.destination));
				
				tripCount = result.getInt(result.getColumnIndex(DatabaseManager.tripCount));
				tripPoints = result.getInt(result.getColumnIndex(DatabaseManager.pointsValue));
				tripMiles = result.getDouble(result.getColumnIndex(DatabaseManager.milesValue));
				
				Log.d(LOG_TAG, "Trip Key = "+tripKey+", Trip count = "+tripCount);
			}
			else
			{
				Log.d("---DB ERROR---", "Trip History Result length is: "+result.getCount());
				throw new NullPointerException(); // shit way of handling error
			}
		}
		else
		{
			Log.d("---DB ERROR---", "Trip History Result is null");
			handleProblem();
		}
		close(result);
	}
	
	private void close(Cursor r)
	{
		r.close();
		dbManager.close();
	}
	
	private void handleProblem()
	{
		tripCount = -1;
	}
	
	private void setText(TextView t, int value)
	{
		if (value != DatabaseManager.UNKNOWN)
		{
			t.setText(""+value);
		}
		else {
			t.setText("(?)");
		}
	}
	
	private void setText(TextView t, double value)
	{
		if (value != DatabaseManager.UNKNOWN)
		{
			t.setText(Double.valueOf((new DecimalFormat("#.##")).format(value)).toString());
		}
		else {
			t.setText("(?)");
		}
	}
	
	private void rateTrip()
	{
		final RaterDialog rater = new RaterDialog(this);
		rater.create().show();
	}
	
	public void storeAndFinish(final int rating)
	{
		Log.d(LOG_TAG, tripKey+" trip => Adding rating: "+rating);
		dbManager.addNewRating(tripKey, rating);
		dbManager.close();

		Intent i = new Intent();
	    i.setAction(TubeActivity.SEND_DATA_BROADCAST);
	    sendBroadcast(i);
	    
		MessageDialog message = new MessageDialog(this, "Thanks!", "Done", MessageDialog.FINISH_ON_CLICK);
		message.create().show();		
	}
}
