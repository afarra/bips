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
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.group057.BipsService;
import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;
import com.group057.R;

public class TestActivity extends Activity {
	// Debugging
	private static final String TAG = "BIPSTestApp";
	private static final boolean D = true;

	// Images to send
	private static final byte[] up = { 0, 0, 0, 0, 0, 0, 0, (byte) 0x20,
		(byte) 0x60, (byte) 0xff, (byte) 0x60, (byte) 0x20, 0, 0, 0, 0, 0,
		0, 0, 0 };
	private static final byte[] down = { 0, 0, 0, 0, 0, 0, 0, (byte) 0x20,
			(byte) 0x30, (byte) 0xff, (byte) 0x30, (byte) 0x20, 0, 0, 0, 0, 0,
			0, 0, 0 };
	private static final byte[] right = { 0, 0, 0, 0, 0, 0, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0xf8, (byte) 0x70, (byte) 0x20, 0,
			0, 0, 0, 0, 0, 0, 0 };
	private static final byte[] left = { 0, 0, 0, 0, 0, 0, (byte) 0x20,
			(byte) 0x70, (byte) 0xf8, (byte) 0x20, (byte) 0x20, (byte) 0x20, 0,
			0, 0, 0, 0, 0, 0, 0 };
	private static final byte[] up_eight = { 0, 0, 0, 0, 0, 0, 0, (byte) 0x20,
		(byte) 0x40, (byte) 0xff, (byte) 0x40, (byte) 0x20, 0, 0, 0, 0, 0,
		0, 0, 0 };
	private static final byte[] down_eight = { 0, 0, 0, 0, 0, 0, 0, (byte) 0x04,
		(byte) 0x02, (byte) 0xff, (byte) 0x02, (byte) 0x04, 0, 0, 0, 0, 0,
		0, 0, 0 };
	private static final byte[] right_eight = { 0, 0, 0, 0, 0, 0, (byte) 0x08,
		(byte) 0x08, (byte) 0x49, (byte) 0x2a, (byte) 0x1c, (byte) 0x08, 0,
		0, 0, 0, 0, 0, 0, 0 };
	private static final byte[] left_eight = { 0, 0, 0, 0, 0, (byte) 0x08, (byte) 0x1c,
		(byte) 0x2a, (byte) 0x49, (byte) 0x08, (byte) 0x08, (byte) 0x08, 0,
		0, 0, 0, 0, 0, 0, 0 };


    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
    IRemoteService mIRemoteService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	/** Some text view we are using to show state information. */
	static TextView mCallbackText;
	// Layout Views
	private Button mSendButton;
	private Button mSendImageButton;
	private Button mCancelCurrentButton;
	private Button mCancelAllButton;
	private RadioGroup mImageRadio;
	private RadioGroup mPriorityRadio;
	byte[] mImageChosen;
	byte mPriorityChosen = (byte)0xffff;
	private EditText mDurationText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
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
		mPriorityRadio = (RadioGroup) findViewById(R.id.radioGroup2);
		mImageRadio = (RadioGroup) findViewById(R.id.radioGroup1);
		mDurationText = (EditText) findViewById(R.id.editText1);

		// Initialize the send image button with a listener that for click
		// events
		mSendImageButton = (Button) findViewById(R.id.button_send_image);
		mSendImageButton.setEnabled(false);
		mSendImageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPriorityChosen = getPriorityFromRadio(mPriorityRadio);
				mImageChosen = getImageFromRadio(mImageRadio);
				if (mImageChosen != null && mPriorityChosen >= 0
						&& mDurationText.getText() != null) {
/*
					Message msg = BipsService.createImageRequestMessage(
							mImageChosen, (int) Integer.parseInt(mDurationText
									.getText().toString()), mPriorityChosen,
							mMessenger);

					// send off the message
					try {
						mService.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
	                try {
	        			mIRemoteService.imageRequestQueue(mImageChosen, Integer.parseInt(mDurationText
								.getText().toString()), mPriorityChosen, Process.myPid());
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
				/*
				 * Message msg =
				 * BipsService.cancelCurrentImageRequestMessage(mMessenger); //
				 * send off the message try { mService.send(msg); } catch
				 * (RemoteException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */
				try {
					mIRemoteService.imageRequestCancelCurrent(Process.myPid());
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
/*
					Message msg = BipsService.cancelAllImagesRequestMessage(mMessenger);

					// send off the message
					try {
						mService.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
				
				try {
					mIRemoteService.imageRequestCancelAll(Process.myPid());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_service);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		        // If BT is not on, request that it be enabled.
		        // setupChat() will then be called during onActivityResult
		        if (!mBluetoothAdapter.isEnabled()) {
		            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		        // Otherwise, setup the chat session
		        } else {

					// Bind Bips
					doBindService();
		        }
		        
		        
				mSendImageButton.setEnabled(true);
				mCancelAllButton.setEnabled(true);
				mCancelCurrentButton.setEnabled(true);
			}
		});

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
	 * Handler of incoming messages from service.
	 */
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
	}

	/**
	 * Target we publish for clients to send messages to Incoming Handler.
	 */
	final IncomingHandler mHandler = new IncomingHandler();
	final Messenger mMessenger = new Messenger(mHandler);

	/**
	 * Class for interacting with the main interface of the service.
	 */

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
/*	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			mCallbackText.setText("Attached.");

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
						BipsService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);

				// Give it some value as an example.
				msg = Message.obtain(null, BipsService.MSG_SET_VALUE,
						this.hashCode(), 0);
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}

			// As part of the sample, tell the user what happened.
			// Toast.makeText(this, R.string.remote_service_connected,
			// Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mCallbackText.setText("Disconnected.");

			// As part of the sample, tell the user what happened.
			// Toast.makeText(this, R.string.remote_service_disconnected,
			// Toast.LENGTH_SHORT).show();
		}
	};
*/
	

    
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
	
	/*
	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		if (!mIsBound) {
			bindService(new Intent(this, BipsService.class), mConnection,
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
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							BipsService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
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
*/
	byte[] getImageFromRadio(RadioGroup group) {
		RadioButton temp = (RadioButton) findViewById(group
				.getCheckedRadioButtonId());
		if (temp.getText().toString().startsWith("U"))
			return up;
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
			return BipsService.API_HIGHEST_PRIORITY;
		if (temp.getText().toString().startsWith("Lowe"))
			return BipsService.API_LOWEST_PRIORITY;
		if (temp.getText().toString().startsWith("Low"))
			return BipsService.API_LOW_PRIORITY;
		if (temp.getText().toString().startsWith("High"))
			return BipsService.API_HIGH_PRIORITY;
		return BipsService.API_MEDIUM_PRIORITY;
	}
	

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                doBindService();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
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
            mHandler.sendMessage(mHandler.obtainMessage(BipsService.MSG_SET_VALUE, value, 0));
        }
    };
}

