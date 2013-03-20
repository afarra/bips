package com.group057;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PriorityListActivity extends ListActivity {
    // Debug var
    private static final boolean D = true;
    private static final String TAG = "PriorityListActivity";

    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.v(TAG, "+ ON CREATE +");
        
        String[] priorities = {"Highest", "High", "Medium", "Low", "Lowest"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, priorities);
        
        setListAdapter(adapter);
        ListView lv = getListView();
        
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // getting values from selected ListItem
                String updateApp = getIntent().getStringExtra("packageName");
                
                SharedPreferences settings = getSharedPreferences(BipsService.BIPS_PREFS, 0);
                SharedPreferences.Editor editor = settings.edit();
                
                editor.putInt(updateApp, position);
                editor.apply();
                
                if (D) Log.v(TAG, "Changed priority " + updateApp + " to " + position);
                finish();
            }
        });
    }
}
