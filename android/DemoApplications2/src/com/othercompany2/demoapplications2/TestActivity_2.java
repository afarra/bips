package com.othercompany2.demoapplications2;

import android.app.Activity;
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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.group057.BipsService;
import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;

public class TestActivity_2 extends Activity {
	// Debugging
    private static final String TAG = "AltTestActivity";
    private static final boolean D = true;

    // Images to send
    private static final byte [] up = {0, 0, 0, 0, 0, 0, 0, (byte)0x20, (byte)0x40, (byte)0x80, (byte)0x40, (byte)0x20, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte [] down = {0, 0, 0, 0, 0, 0, 0, (byte)0x04, (byte)0x02, (byte)0x80, (byte)0x02, (byte)0x04, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte [] right = {0, 0, 0, 0, 0, 0, (byte)0x8, (byte)0x8, (byte)0x49, (byte)0x2a, (byte)0x1c, (byte)0x08, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte [] left = {0, 0, 0, 0, 0, 0, (byte)0x1c, (byte)0x2a, (byte)0x49, (byte)0x08, (byte)0x08, (byte)0x08, 0, 0, 0, 0, 0, 0, 0, 0};
    
    /** IRemoteService for communication with service (will replace Messenger) */
    IRemoteService mIRemoteService = null;
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	/** Some text view we are using to show state information. */
	static TextView mCallbackText;

    // Layout Views
    private Button mSendButton;
    private Button mSendImageButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // Set up the window layout
        setContentView(R.layout.test_activity);
    }
    
	@Override
	public void onStart()
	{
		super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        mCallbackText = (TextView) findViewById(R.id.text_view);
        
        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setText("Start BipsService");
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Bind Bips
            	doBindService();
            }
        });

        // Initialize the send image button with a listener that for click events
        mSendImageButton = (Button) findViewById(R.id.button_send_image);
        mSendImageButton.setText("Send Up");
        mSendImageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
/*        		Message msg = Message.obtain(null, BipsService.DEBUG_SEND_IMAGE);
        		Bundle bundle = new Bundle();
        		bundle.putInt(BipsService.API_IMAGE_TIME, 10);
        		bundle.putByteArray(BipsService.API_IMAGE_PIXELS, up);
        		bundle.putByte(BipsService.API_IMAGE_PRIORITY,(byte) 0);
        		bundle.putInt(BipsService.API_IMAGE_ID, 0);
        		msg.setData(bundle);
        		try {
					mService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		*/

                try {
        			mIRemoteService.imageRequestQueue(up, 5000, (byte) 0, Process.myPid());
        		} catch (RemoteException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
                
            }
        });
        
        // Initialize the Queue image button with a listener that for click events
        mSendImageButton = (Button) findViewById(R.id.button_send_left);
        mSendImageButton.setText("Queue Left");
        mSendImageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
/*        		Message msg = BipsService.createImageRequestMessage(left, 2000, (byte)3, mMessenger);
        		try {
					mService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
*/
                try {
        			mIRemoteService.imageRequestCancelCurrent(Process.myPid());
        		} catch (RemoteException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
                
            }
        });
	}
	
	@Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
    }
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
        if(D) Log.e(TAG, "+ ON DESTROY +");
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
	                mCallbackText.setText("BIPS Set Val: " + msg.arg1);
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
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
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
	            msg = Message.obtain(null,
	                    BipsService.MSG_SET_VALUE, this.hashCode(), 0);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }

	        // As part of the sample, tell the user what happened.
//	        Toast.makeText(this, R.string.remote_service_connected,
//	                Toast.LENGTH_SHORT).show();
	    }
	

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        mCallbackText.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
//	        Toast.makeText(this, R.string.remote_service_disconnected,
//	                Toast.LENGTH_SHORT).show();
	    }
	};*/
	

    
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
		// applications replace our component.
		if (!mIsBound){
			bindService(new Intent("com.group057.IRemoteService"), mConnection,
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
	
/*	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
		// applications replace our component.
		if (!mIsBound){
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
	}*/
	

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
