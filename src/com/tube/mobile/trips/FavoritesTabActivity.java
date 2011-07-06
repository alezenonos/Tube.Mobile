package com.tube.mobile.trips;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.tuberater.R;
import com.tuberater.data.DatabaseManager;

public class FavoritesTabActivity extends TabActivity {
	
	/*
	 * Will display two tabs: one for historical tube trips, one for historical bus trips
	 */
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	Intent intent;
    	
    	final Resources res = getResources();
        
        final TabHost tabHost = getTabHost(); 
        TabSpec spec; 

        intent = new Intent().setClass(this, FavoritesTab.class);
        intent.putExtra(FavoritesTab.TYPE, DatabaseManager.TUBE);
        spec = tabHost.newTabSpec("rail_favorites").setIndicator("By Train", res.getDrawable(R.drawable.train))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        intent = new Intent().setClass(this, FavoritesTab.class);
        intent.putExtra(FavoritesTab.TYPE, DatabaseManager.BUS);
        spec = tabHost.newTabSpec("bus_favorites").setIndicator("By Bus", res.getDrawable(R.drawable.bus))
                      .setContent(intent);
        tabHost.addTab(spec);
        tabHost.setCurrentTab(0); 
    }
}