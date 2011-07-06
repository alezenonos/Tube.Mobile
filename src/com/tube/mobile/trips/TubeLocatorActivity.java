package com.tube.mobile.trips;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.tuberater.R;

public class TubeLocatorActivity extends ListActivity {
	
	public final static String RETURN_TYPE = "dt";
	public final static String OD_TYPE = "od";
	public final static String SELECTED_LINE = "sl";
	public final static String SELECTED_LINE_ID = "sli";
	public final static String SELECTED_STATION = "ss";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.locate_tube_layout);
		
		final Intent intent = getIntent();
		final int returnType = intent.getIntExtra(RETURN_TYPE, 0);
		
		if (returnType == FavoritesTab.LOCATE_LINE_REPORT)
		{
			if (intent.getIntExtra(OD_TYPE, 1) == FavoritesTab.ORIGIN_INTENT)
			{
				setTitle("Where are you travelling from?");
			}
			else {
				setTitle("Where are you travelling to?");
			}
			
			final String[] data = getResources().getStringArray(R.array.tube_lines);
			setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item_no_icon, data));
			(findViewById(R.id.searchField)).setVisibility(View.GONE);
		}
		else if (returnType == FavoritesTab.LOCATE_STATION_REPORT)
		{
			setTitle("Which station?");
			final String[] data = getResources().getStringArray(intent.getIntExtra(SELECTED_LINE_ID, -1));
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_no_icon, data);
			
			final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.autocompleteField);
			textView.setAdapter(adapter);
			textView.setHint("Find station...");
			
			setListAdapter(adapter);
		}
		
        final ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view, int position, long id)
          {
        	  CharSequence text = ((TextView) view).getText();
        	  submit(text.toString(), returnType);
          }
        });
	}
	
	private void submit(String line, final int type)
	{
		final Intent result = this.getIntent();
		if (type == FavoritesTab.LOCATE_LINE_REPORT)
		{
			line = line.replaceAll(" Line", "");
			final int line_id = getResources().getIdentifier("com.tuberater:array/"+line.replaceAll(" ", "_").toLowerCase()+"_line", null, null);
			result.putExtra(SELECTED_LINE_ID, line_id);
			result.putExtra(SELECTED_LINE, line);
		}
		else if (type == FavoritesTab.LOCATE_STATION_REPORT)
		{
			result.putExtra(SELECTED_STATION, line);
		}
		this.setResult(RESULT_OK, result);
		finish();
	}
}
