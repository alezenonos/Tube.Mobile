package com.tube.mobile.trips;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MessageDialog extends AlertDialog.Builder {
	
	public final static boolean FINISH_ON_CLICK = true;
	public final static boolean DONT_FINISH_ON_CLICK = false;

	protected MessageDialog(final Activity caller, final String message, final String buttonMessage, final boolean finish)
	{
		super(caller);
		setMessage(message);
		setCancelable(false);
	    setPositiveButton(buttonMessage, new DialogInterface.OnClickListener() 
	    {
	           public void onClick(DialogInterface dialog, int id)
	           {
	        	   if (finish)
	        	   {
	        		   caller.finish();
	        	   }
	           }
	    });
	}
}
