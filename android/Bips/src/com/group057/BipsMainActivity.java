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
import android.util.SparseArray;
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

enum BipsPriority {Highest, High, Medium, Low, Lowest};

// Used to populate the ListView of the activity
class BipsClientListRowInfo 
{
    Drawable appIcon;
    String appLabel;
    String appPackage;
    int appPriority;
    
    public BipsClientListRowInfo(Drawable i, String l, String pkg, int p) {
        appIcon = i;
        appLabel = l;
        appPackage = pkg;
        appPriority = p;
    }
}

// Maps the BipsClientListRowInfo to the Views in the ListView rows
class BipsClientAdapter extends ArrayAdapter<BipsClientListRowInfo>
{
    // Priority number to string map
    private static final SparseArray<String> bipsMapPriority;
    static
    {
        bipsMapPriority = new SparseArray<String>();
        bipsMapPriority.append(0, BipsPriority.Highest.toString());
        bipsMapPriority.append(1, BipsPriority.High.toString());
        bipsMapPriority.append(2, BipsPriority.Medium.toString());
        bipsMapPriority.append(3, BipsPriority.Low.toString());
        bipsMapPriority.append(4, BipsPriority.Lowest.toString());
    }
    
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
                bipsMapPriority.get(clientInfo.appPriority));
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

    // used to check and enable bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    
    // UI Layout Views
    private Button mChooseDeviceButton;
    private Button mBluetoothButton;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.v(TAG, "+++ ON CREATE +++");
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        

        // Initialize the service button with a listener that for click events
        mChooseDeviceButton = new Button(this);
        mBluetoothButton = new Button(this);
        mChooseDeviceButton.setEnabled(false);
        if (mBluetoothAdapter == null)
        {
            mBluetoothButton.setEnabled(false);
            
        }
        
        mChooseDeviceButton.setText(R.string.choose_device_button_text);
        mBluetoothButton.setText(R.string.enable_bt_button_text);
        
        ListView lv = getListView();
        lv.addHeaderView(mBluetoothButton);
        lv.addHeaderView(mChooseDeviceButton);
        
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.v(TAG, "++ ON START ++");

        mChooseDeviceButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // If the BT adapter is enabled, the user may choose the BT
                // adapter of the BIPS unit
                if (mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                    startActivity(enableIntent);
                }
                
                
            }
        });
        
        mBluetoothButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
                if (mBluetoothAdapter != null)
                {
                    if (!mBluetoothAdapter.isEnabled())
                    {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableIntent);
                        updateBluetoothButtonText();
                    }
                    else
                    {
                        mBluetoothAdapter.disable();
                        // Change text here because the adapter does not disable fast enough
                        mBluetoothButton.setText(R.string.enable_bt_button_text);
                        mChooseDeviceButton.setEnabled(false);
                    }
                    
                }
            }
        });

    }
    
    // If Bluetooth is enabled, the button is able to disable it, 
    // otherwise it is able to request enabling BT. This is strictly
    // for enabling and changing the text of the button.
    void updateBluetoothButtonText()
    {
        if (mBluetoothAdapter != null)
        {
            if (mBluetoothAdapter.isEnabled())
            {
                mBluetoothButton.setText(R.string.disable_bt_button_text);
            }
            else
            {
                mBluetoothButton.setText(R.string.enable_bt_button_text);
            }
        }
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        if (D) Log.v(TAG, "+ ON RESUME +");
        
        // Update button functionality
        
        // Bluetooth button set text appropriately
        updateBluetoothButtonText();
        
        // Don't let the user select a device if Bluetooth is off
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            mChooseDeviceButton.setEnabled(true);
        }
        else
        {
            mChooseDeviceButton.setEnabled(false);
        }
        
        // Update the list (will update since the activity resumes after 
        // priority selection

        // Get the preferences for application priority and load it
        // into the UI as a listview
        SharedPreferences settings = getSharedPreferences(BipsService.BIPS_PREFS, 0);

        ArrayList<BipsClientListRowInfo> bipsClients = new ArrayList<BipsClientListRowInfo>();
        
        // used to retrieve app name and icon from the package name
        PackageManager pk = getPackageManager();
        
        for (String s : settings.getAll().keySet())
        {
            try {
                bipsClients.add(new BipsClientListRowInfo(pk.getApplicationIcon(s),
                        (String) pk.getApplicationLabel(pk.getApplicationInfo(s, 0)), 
                        s, 
                        settings.getInt(s, 4)));
            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
                
        BipsClientAdapter bipsAdapter = new BipsClientAdapter(this, 
                R.layout.bound_app_info, bipsClients);

        ListView lv = getListView();
        lv.setClickable(true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // getting values from selected ListItem
                Intent intent = new Intent(getApplicationContext(), PriorityListActivity.class);
                
                BipsClientListRowInfo item = (BipsClientListRowInfo) parent.getItemAtPosition(position);
                intent.putExtra("packageName", item.appPackage);
                
                startActivity(intent);

            }
        });
        
        lv.setAdapter(bipsAdapter);
    }
    
    
    
}
