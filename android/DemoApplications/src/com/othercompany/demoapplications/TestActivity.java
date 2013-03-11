package com.othercompany.demoapplications;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;

public class TestActivity extends Activity {
	// Debugging
	private static final String TAG = "BIPSTestApp";
	private static final boolean D = true;

	// Images to send
	private static final byte[] upWide = { 
		(byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x30, 
		(byte) 0x30, (byte) 0x30, (byte) 0x70, (byte) 0x70,
		(byte) 0x70, (byte) 0xf0, (byte) 0xf0, (byte) 0x70, 
		(byte) 0x70, (byte) 0x70, (byte) 0x30, (byte) 0x30, 
		(byte) 0x30, (byte) 0x10, (byte) 0x10, (byte) 0x10 };
	private static final byte[] down = { 
		(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xc0,
		(byte) 0xc0, (byte) 0xc0, (byte) 0xe0, (byte) 0xe0,
		(byte) 0xe0, (byte) 0xf0, (byte) 0xf0, (byte) 0xe0,
		(byte) 0xe0, (byte) 0xe0, (byte) 0xc0, (byte) 0xc0,
		(byte) 0xc0, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
	private static final byte[] right = { 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, 
		(byte) 0x60, (byte) 0x60, (byte) 0xf8, (byte) 0xf8, 
		(byte) 0xf8, (byte) 0x60, (byte) 0x60, (byte) 0x60};
	private static final byte[] left = { 
		(byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, 
		(byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60};
	
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
    IRemoteService mIRemoteService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	BluetoothAdapter mBtAdapter = null;

	/** Some text view we are using to show state information. */
	static TextView mCallbackText;
	// Layout Views
	private Button mStartServiceButton;
	private Button mSendImageButton;
	private Button mCancelCurrentButton;
    private Button mCancelAllButton;
    private Button mPeriodicButton;
	private RadioGroup mImageRadio;
	byte[] mImageChosen;
	private EditText mDurationText;
	private boolean mPeriodicFlag = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
        
		// Set up the window layout
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.test_activity2);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");
        
		mCallbackText = (TextView) findViewById(R.id.textView_callback);
		mImageRadio = (RadioGroup) findViewById(R.id.radioGroup1);
		mDurationText = (EditText) findViewById(R.id.editText1);

		// Initialize the send image button with a listener that for click
		// events
		mSendImageButton = (Button) findViewById(R.id.button_send_image);
		mSendImageButton.setEnabled(false);
		mSendImageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mImageChosen = getImageFromRadio(mImageRadio);
				if (mImageChosen != null && mDurationText.getText() != null) {

	                try {
	        			mIRemoteService.imageRequestQueue(mImageChosen, Integer.parseInt(mDurationText
								.getText().toString()), (byte)0, getPackageName());
	        		} catch (RemoteException e) {
	        			// TODO Auto-generated catch block
	        			e.printStackTrace();
	        		}
				}
			}
		});
		
		// Initialize the cancel image button with a listener that for click
		// events
		mCancelCurrentButton = (Button) findViewById(R.id.button_cancel_current);
		mCancelCurrentButton.setEnabled(false);
		mCancelCurrentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					mIRemoteService.imageRequestCancelCurrent(getPackageName());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        // Initialize the cancel all images button with a listener that for click
        // events
        mCancelAllButton = (Button) findViewById(R.id.button_cancel_all);
        mCancelAllButton.setEnabled(false);
        mCancelAllButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                
                try {
                    mIRemoteService.imageRequestCancelAll(getPackageName());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        });
        
        // Initialize the periodic requests button. Pressing the button toggles periodic
        // requests which last for 
        mPeriodicButton = (Button) findViewById(R.id.button_periodic_request);
        mPeriodicButton.setEnabled(false);
        mPeriodicButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
                mPeriodicFlag = !mPeriodicFlag;
                if (mPeriodicFlag)
                {
                    if (mPeriodicRequestThread == null || !mPeriodicRequestThread.isAlive())
                    {
                        mPeriodicRequestThread = new Thread(mPeriodicRequestProc);
                        mPeriodicRequestThread.start();
                        
                        mPeriodicButton.setText("Stop periodic requests");
                    }
                }
                else
                {
                    mPeriodicButton.setText("Start periodic requests");
                }
                
            }
        });

		// Initialize the service button with a listener that for click events
		mStartServiceButton = (Button) findViewById(R.id.button_service);
		mStartServiceButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		        /*// If BT is not on, request that it be enabled.
		        // setupChat() will then be called during onActivityResult
		        if (!mBluetoothAdapter.isEnabled()) {
		            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		        // Otherwise, setup the chat session
		        } else {

					// Bind Bips
					doBindService();
		        }*/
		        doBindService();
		        
				mSendImageButton.setEnabled(true);
				mCancelAllButton.setEnabled(true);
                mCancelCurrentButton.setEnabled(true);
                mPeriodicButton.setEnabled(true);
			}
		});
		
		// allow sending of images if bound
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter != null && !mBtAdapter.isDiscovering())
		{
            mSendImageButton.setEnabled(true);
            mCancelAllButton.setEnabled(true);
            mCancelCurrentButton.setEnabled(true);
            mPeriodicButton.setEnabled(true);
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");
		if (mPeriodicFlag)
		{
		    mPeriodicButton.setText("Stop periodic requests");
		}
		else
		{
		    mPeriodicButton.setText("Start periodic requests");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (D)
			Log.e(TAG, "+ ON DESTROY +");
		mPeriodicFlag = false;
		doUnbindService();
		
	}

	/**
	 * Handler of incoming messages from service.
	 
	static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BipsService.MSG_SET_VALUE:
				mCallbackText.setText("Received from service: " + msg.arg1);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}*/

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
	        mCallbackText.setText("Attached to BIPS.");

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
			mCallbackText.setText("Binding.");
		} else {
			mCallbackText.setText("Already Bound.");
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
            mCallbackText.setText("Unbinding.");
        }
	}
	
	byte[] getImageFromRadio(RadioGroup group) {
		RadioButton temp = (RadioButton) findViewById(group
				.getCheckedRadioButtonId());
		if (temp.getText().toString().startsWith("U"))
			return upWide;
		if (temp.getText().toString().startsWith("D"))
			return down;
		if (temp.getText().toString().startsWith("L"))
			return left;
		if (temp.getText().toString().startsWith("R"))
			return right;
		return null;
	}

	byte getPriorityFromRadio(RadioGroup group) {
		RadioButton temp = (RadioButton) findViewById(group
				.getCheckedRadioButtonId());
		if (temp.getText().toString().startsWith("Highe"))
			return 0;
		if (temp.getText().toString().startsWith("Lowe"))
			return 4;
		if (temp.getText().toString().startsWith("Low"))
			return 3;
		if (temp.getText().toString().startsWith("High"))
			return 1;
		return 2;
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
    
    Thread mPeriodicRequestThread = null;
    Runnable mPeriodicRequestProc = new Runnable()
    {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            while(mPeriodicFlag)
            {
                try {
                    mIRemoteService.imageRequestQueue(down, Integer.parseInt(mDurationText
                            .getText().toString()), (byte)0, getPackageName());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(Integer.parseInt(mDurationText
                            .getText().toString()));
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        
    };
}

