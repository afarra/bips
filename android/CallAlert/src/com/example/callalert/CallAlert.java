package com.example.callalert;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.provider.ContactsContract;
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
	
	private static final int BIPS_IMAGE_WIDTH = 20;
    
    TelephonyManager mTM;

	/** Messenger for communicating with service. */
    IRemoteService mIRemoteService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	
    // Buttons
	Button mStartServiceButton;
	
	public Bitmap initialsToBitmap(String initials)
	{
        // Build an image of the initials
        Bitmap alphabetPic = BitmapFactory.decodeResource(getResources(), R.drawable.alphabet);
        Bitmap result = Bitmap.createBitmap(BIPS_IMAGE_WIDTH, alphabetPic.getHeight(), Bitmap.Config.ARGB_8888);
        int stride = 7; // each character will be tightly packed into arrays, 7 pixel width
        int[] pixConstruction = new int[BIPS_IMAGE_WIDTH * alphabetPic.getHeight()];
                                    
        // each character is 7 pixels wide, start getting pixels at an
        // offset character of the ascii code of the letter (starts at code 32)
        
        // Put the first letter to the front of the array 
        alphabetPic.getPixels(pixConstruction, 0, stride, 
                ((int)initials.toCharArray()[0] - 32) * 7, 0, 7, alphabetPic.getHeight());
        // Append the last name letter to the array
        alphabetPic.getPixels(pixConstruction, 7, 7, 
                ((int)initials.toCharArray()[1] - 32) * 7, 0, 7, alphabetPic.getHeight());
        
        result.setPixels(pixConstruction, 0, stride, 
                0, 0, BIPS_IMAGE_WIDTH, alphabetPic.getHeight());
        
        return result;
    }

    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            try 
            {
                switch (state) 
                {
                case TelephonyManager.CALL_STATE_RINGING:
                    
                    Bitmap initialsImage = null;
                    try 
                    {
                        
                        String name = null;
                        String[] nameSplit = null;
                        String initialsString = "";
                        
                        // want to get the name
                        String[] projection = new String[] {
                                ContactsContract.PhoneLookup.DISPLAY_NAME};

                        // encode the phone number and build the filter URI
                        Uri contactUri = Uri.withAppendedPath(
                                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                        Uri.encode(incomingNumber));

                        // query for data
                        Cursor cursor = getApplicationContext().getContentResolver().query(
                                contactUri, projection, null, null, null);

                        if (cursor.moveToFirst()) 
                        {
                            try 
                            {
                                // Get name from contacts database:
                                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                                
                                Log.v(TAG, "Name/number match: Contact name  = " + name);
                                
                                // retrieve initials by splitting the string
                                nameSplit = name.split("\\s");

                                // initials format will be JS (or only J  if no last name)
                                initialsString += nameSplit[0].toCharArray()[0]; 
                                if (nameSplit.length > 1)
                                {
                                    initialsString += nameSplit[nameSplit.length].toCharArray()[0];
                                }
                                else
                                {  
                                    // to prevent null error later when converting to bitmap
                                    initialsString += ' ';
                                }
                                
                                // prepare image of initials for projection
                                initialsImage = initialsToBitmap(initialsString);
                            }
                            catch(NullPointerException e)
                            {
                                Log.e(TAG, "Invalid contact name format. Not displaying");
                            }
                        }
                        else 
                        {
                            // contact not found or no CID
                            Log.v(TAG, "Name/number match: Contact Not Found @ " + incomingNumber);
                            

                        }
                        
                        // repeat calling graphic sequence 2 times
                        for (int i = 0; i < 2; i++)
                        {
                            Bitmap phonePic = BitmapFactory.decodeResource(getResources(), R.drawable.phone_call1);
                            mIRemoteService.bitmapRequestQueue(phonePic, 250, (byte)0, Process.myPid());
                            
                            phonePic = BitmapFactory.decodeResource(getResources(), R.drawable.phone_call2);
                            mIRemoteService.bitmapRequestQueue(phonePic, 250, (byte)0, Process.myPid());
                            
                            phonePic = BitmapFactory.decodeResource(getResources(), R.drawable.phone_call3);
                            mIRemoteService.bitmapRequestQueue(phonePic, 250, (byte)0, Process.myPid());
    
                            phonePic = BitmapFactory.decodeResource(getResources(), R.drawable.phone_call);
                            mIRemoteService.bitmapRequestQueue(phonePic, 2000, (byte)0, Process.myPid());
    
                            if (initialsImage != null)
                            {
                                mIRemoteService.bitmapRequestQueue(initialsImage, 5000, (byte)0, Process.myPid());
                            }
                        }
                        
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                default:
                    // Do nothing
                }
            } catch (Exception e) {
                e.printStackTrace();
                //Log.i("Exception", "PhoneStateListener() e = " + e.toString());
            }
        }
    };
	@Override 
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (D)
			Log.v(TAG, "+++ ON CREATE +++");
		
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
			Log.v(TAG, "++ ON START ++");
		
		
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
			Log.v(TAG, "+ ON RESUME +");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (D)
			Log.v(TAG, "+ ON DESTROY +");
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

        /**
         * This is called by the service to find out what applications are binding
         * and using the projector to allow the user to assign priority to 
         * which applications they prefer to see from the projector over other apps.
         */
        public String getClientPackageName()
        {
            return getPackageName();
        }
    };
}
