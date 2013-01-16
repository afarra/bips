package com.example.callalert;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;

public class CallAlert extends Activity{
	// Debug var
	private static final boolean D = true;
	private static final String TAG = "CallAlertApp";
	
    TelephonyManager mTM;

	/** Messenger for communicating with service. */
    IRemoteService mIRemoteService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	
    // Buttons
	Button mStartServiceButton;
	


    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Toast.makeText(CallAlert.this, "CALL_STATE_RINGING",
                            Toast.LENGTH_SHORT).show();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Toast.makeText(CallAlert.this, "CALL_STATE_OFFHOOK",
                            Toast.LENGTH_SHORT).show();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Toast.makeText(CallAlert.this, "CALL_STATE_IDLE",
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(CallAlert.this, "default",
                            Toast.LENGTH_SHORT).show();
                    Log.i("Default", "Unknown phone state=" + state);
                }
            } catch (Exception e) {
                Log.i("Exception", "PhoneStateListener() e = " + e);
            }
        }
    };
	@Override 
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

        // Get the local Bluetooth adapter
        // mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
		// Set up the window layout
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.call_alert);
        mTM = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mTM.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	}


	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");
		
		
		// Initialize the service button with a listener that for click events
		mStartServiceButton = (Button) findViewById(R.id.start_service);
		mStartServiceButton.setOnClickListener(
			new OnClickListener() {
				public void onClick(View v) {
						// Bind Bips
						doBindService();
				}
			} 
		);
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (D)
			Log.e(TAG, "+ ON DESTROY +");
		doUnbindService();
	}


	/**
	 * Target we publish for clients to send messages to Incoming Handler.
	 */
	final Handler mHandler = new Handler();
	final Messenger mMessenger = new Messenger(mHandler);


	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    // Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // Following the example above for an AIDL interface,
	        // this gets an instance of the IRemoteInterface, which we can use to call on the service
	        mIRemoteService = IRemoteService.Stub.asInterface(service);
            Toast.makeText(getApplicationContext(), "Attached to BIPS", Toast.LENGTH_SHORT).show();

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				mIRemoteService.registerCallback(mCallback);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
	    }

	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className) {
	        Log.e(TAG, "Service has unexpectedly disconnected");
	        mIRemoteService = null;
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
		// applications replace our component.
		if (!mIsBound){
			bindService(new Intent(IRemoteService.class.getName()), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	void doUnbindService() {
		if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mIRemoteService != null) {
                try {
                    mIRemoteService.unregisterCallback(mCallback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
	}

    // ----------------------------------------------------------------------
    // Code showing how to deal with callbacks.
    // ----------------------------------------------------------------------

    /**
     * This implementation is used to receive callbacks from the remote
     * service.
     */
    private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {
    	public int getPid(){
            return Process.myPid();
        }
    	
    	/**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void valueChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(0, value, 0));
        }
    };
}
