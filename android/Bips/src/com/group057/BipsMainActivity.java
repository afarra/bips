package com.group057;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

enum BipsPriority {Highest, High, Medium, Low, Lowest};

class BipsClientListRowInfo 
{
    Drawable appIcon;
    String appLabel;
    int appPriority;
    
    public BipsClientListRowInfo(Drawable i, String l, int p) {
        appIcon = i;
        appLabel = l;
        appPriority = p;
    }
}

class BipsClientAdapter extends ArrayAdapter<BipsClientListRowInfo>
{
    Context context;
    int layoutResourceId;
    List<BipsClientListRowInfo> data = null;

    public BipsClientAdapter(Context context, int textViewResourceId,
            List<BipsClientListRowInfo> objects) {
        super(context, textViewResourceId, objects);
        this.layoutResourceId = textViewResourceId;
        this.context = context;
        this.data = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        View row = convertView;
        BipsClientHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new BipsClientHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.app_icon);
            holder.appLabel = (TextView)row.findViewById(R.id.app_label);
            holder.appPriority = (TextView)row.findViewById(R.id.app_priority);
            
            row.setTag(holder);
        }
        else
        {
            holder = (BipsClientHolder)row.getTag();
        }
        
        
        BipsClientListRowInfo clientInfo = data.get(position);
        holder.appLabel.setText(clientInfo.appLabel);
        holder.appPriority.setText("Projection priority: " + 
                (BipsPriority.High));
        holder.imgIcon.setImageDrawable(clientInfo.appIcon);
        
        return row;
    }
    
    static class BipsClientHolder
    {
        ImageView imgIcon;
        TextView appLabel;
        TextView appPriority;
    }

}

public class BipsMainActivity extends ListActivity {
    // Debugging
    private static final String TAG = "BipsMainActivity";
    private static final boolean D = true;

    // For user preferences of app priority
    private static final String BIPS_PREFS = "BipsPreferences";
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    
    // used to check and enable bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    
    // UI Layout Views
    private Button mChooseDeviceButton;
    private Button mBluetoothButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        setContentView(R.layout.activity_bips_main);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        SharedPreferences settings = getSharedPreferences(BIPS_PREFS, 0);
//        
//        ArrayList<String> stuff = new ArrayList<String>();
//        stuff.addAll(settings.getAll().keySet());
//        
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
//                R.layout.bound_app_info, stuff);

        ArrayList<BipsClientListRowInfo> bipsClients = new ArrayList<BipsClientListRowInfo>();
        
        
        PackageManager pk = getPackageManager();
        
        for (String s : settings.getAll().keySet())
        {
            try {
                bipsClients.add(new BipsClientListRowInfo(pk.getApplicationIcon(s), 
                        (String) pk.getApplicationLabel(pk.getApplicationInfo(s, 0)), settings.getInt(s, 4)));
            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
                
        BipsClientAdapter bipsAdapter = new BipsClientAdapter(this, 
                R.layout.bound_app_info, bipsClients);

        // selecting single ListView item
        ListView lv = getListView();
        
        // Initialize the service button with a listener that for click events
        mChooseDeviceButton = new Button(this);
        mBluetoothButton = new Button(this);
        
        mChooseDeviceButton.setText(R.string.choose_device_button_text);
        mBluetoothButton.setText(R.string.enable_bt_button_text);

        lv.addHeaderView(mBluetoothButton);
        lv.addHeaderView(mChooseDeviceButton);
        
        lv.setAdapter(bipsAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // getting values from selected ListItem
                Log.i(TAG,"clicked");

            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");
        
        mChooseDeviceButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // If BT is not on, request that it be enabled.
                // setupChat() will then be called during onActivityResult
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                // Otherwise, setup the chat session
                } else {

                    // Bind Bips
                    Intent enableIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                
                
            }
        });

    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so bind to the BIPS Android service
                //doBindService();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
