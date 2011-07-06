package com.tube.mobile.trips;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tuberater.R;
import com.tuberater.data.DatabaseManager;

public class BusLocatorActivity extends Activity {
	
	/*
	 * An activity for the user to input a bus trip
	 * The user inputs:
	 * 1. The bus number
	 * 2. The trip origin
	 * 3. The trip destination
	 */
	
	public final static String ORIGIN = "origin", 
		DESTINATION = "destination",
		BUS_NUMBER = "bus_number";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.locate_bus_layout);

        ((Button) findViewById(R.id.submitTripButton)).setOnClickListener(
        		new OnClickListener()
        		{
        			@Override
        			public void onClick(View v)
        			{
        				submit();
        			}
        		}
        );
        
        int ratingKey = getIntent().getIntExtra(HistoryTabActivity.selectedItem, -1);
        if (ratingKey != -1)
        {
        	DatabaseManager dbManager = new DatabaseManager(this);
        	int key = dbManager.getTripKey(ratingKey);
        	
        	Cursor trip = dbManager.getTripDetails(key);
        	trip.moveToFirst();
        	String busNumber = trip.getString(trip.getColumnIndex(DatabaseManager.modality));
        	String origin = trip.getString(trip.getColumnIndex(DatabaseManager.origin));
        	String destination = trip.getString(trip.getColumnIndex(DatabaseManager.destination));
        	trip.close();
        	dbManager.close();
        	
        	((EditText) findViewById(R.id.busNumberText)).setText(busNumber);
        	((EditText) findViewById(R.id.busOriginText)).setText(origin);
        	((EditText) findViewById(R.id.busDestinationText)).setText(destination);
        	((TextView) findViewById(R.id.titleText)).setText("Change your trip:");
        }
	}
	
	private String getBusNumber()
	{
		String input = ((EditText) findViewById(R.id.busNumberText)).getText().toString();
		if (input.length() > 0)
		{
			/*
			 * Bus numbers (in London):
			 * - can have first character be a letter
			 * - are then a number less than 999
			 * - e.g., N134, H12, N29, 309
			 */
			String number = input;
			char first = input.charAt(0);
			if (!Character.isDigit(first))
			{
				number = number.substring(1, input.length());
			}
			try {
				int value = Integer.parseInt(number);
				if (value < 1000)
				{
					return input;
				} 
			}
			catch(NumberFormatException e) { }
			return null;
		}
		else return null;
	}
	
	private void submit()
	{
		String busNumber = getBusNumber();
		if (busNumber != null)
		{
			final Intent result = this.getIntent();
			result.putExtra(BUS_NUMBER, busNumber);
			result.putExtra(ORIGIN, ((EditText) findViewById(R.id.busOriginText)).getText().toString());
			result.putExtra(DESTINATION, ((EditText) findViewById(R.id.busDestinationText)).getText().toString());
			this.setResult(RESULT_OK, result);
			finish();
		}
		else {
			MessageDialog message = new MessageDialog(this, "Please input/fix your bus number!", "Ok", MessageDialog.DONT_FINISH_ON_CLICK);
			message.create().show();
		}
	}
}
