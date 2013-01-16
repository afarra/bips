package com.group057;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.group057.DeviceListActivity;
import com.group057.bipsmainactivity.R;

public class BipsMainActivity extends Activity {
    // Debugging
    private static final String TAG = "BipsMainActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    
    // used to check and enable bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    
    // UI Layout Views
    private Button mStartServiceButton;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        setContentView(R.layout.activity_bips_main);
        
        // Ask for BT enable
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        
        
    }

    public void onStart()
    {
        super.onStart();
        if (D)
            Log.e(TAG, "++ ON START ++");
        
        // Initialize the service button with a listener that for click events
        mStartServiceButton = (Button) findViewById(R.id.enable_projection_button);
        mStartServiceButton.setOnClickListener(new OnClickListener() {
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
                // Bind Bips
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
