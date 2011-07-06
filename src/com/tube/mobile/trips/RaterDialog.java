package com.tube.mobile.trips;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.tuberater.R;
import com.tuberater.data.DatabaseManager;

public class RaterDialog extends AlertDialog.Builder {
	
	private final CharSequence[] items;
	
	protected RaterDialog(Context c)
	{
		super(c);
		setTitle("How is your trip going?");
		items = c.getResources().getTextArray(R.array.rating_values);
	}

	protected RaterDialog(final HistoryTabActivity caller, final DatabaseManager dbManager, final int trip, final int ratingKey)
	{
		this(caller);	
		setItems(items, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				int rating = items.length-item;
				dbManager.reRate(ratingKey, rating);
				dbManager.close();
				caller.setRating(trip, rating);
	    	}
		});
	}
	
	protected RaterDialog(final TripActivity caller)
	{
		this((Context)caller);
		setItems(items, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				int rating = items.length-item;
				caller.storeAndFinish(rating);
	    	}
		});
	}
}
