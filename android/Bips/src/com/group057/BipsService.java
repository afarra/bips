package com.group057;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class BipsService extends Service {
	// Debugging
	private static final String TAG = "BipsService";
	private static final boolean D = true;

	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Key names received from the third party applications
	public static final String API_IMAGE_ID = "image_id";
	public static final String API_IMAGE_PRIORITY = "image_priority";
	public static final String API_IMAGE_TIME = "image_time";
	public static final String API_IMAGE_PIXELS = "image_pixels";

	// Preferences name
    static final String BIPS_PREFS = "BipsPreferences";
    static final String BT_PREFS = "BluetoothPreferences";
	static final String BT_DEVICE = "BTDeviceAddress";
    
	enum BipsStatus {
		IDLE, BUSY
	}

	// BipsService priority levels for image queues
	public static final byte API_LOWEST_PRIORITY = 4;
	public static final byte API_LOW_PRIORITY = 3;
	public static final byte API_MEDIUM_PRIORITY = 2;
	public static final byte API_HIGH_PRIORITY = 1;
	public static final byte API_HIGHEST_PRIORITY = 0;
	
	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	public static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to unregister a client, to stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	public static final int MSG_UNREGISTER_CLIENT = 2;

	/**
	 * Command to service to set a new value. This can be sent to the service to
	 * supply a new value, and will be sent by the service to any registered
	 * clients with the new value.
	 */
	public static final int MSG_SET_VALUE = 3;

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 4;
	public static final int MESSAGE_READ = 5;
	public static final int MESSAGE_WRITE = 6;
	public static final int MESSAGE_DEVICE_NAME = 7;
	public static final int MESSAGE_TOAST = 8;

	// Messages from DeviceListActivity
	public static final int REQUEST_CONNECT_DEVICE_SECURE = 9;

	// Messages from third party applications
	public static final int API_REQ_QUEUE_IMAGE = 10;				// Add an image into the queues for projection
	public static final int API_REQ_CANCEL_CURRENT_IMAGE = 11;		// Cancel the current projected image if it is from the requesting application
	public static final int API_REQ_CANCEL_ALL_IMAGES = 12;			// Cancel all the images in the queues from this application
	public static final int API_REQ_CANCEL_BY_PRIORITY = 13;		// Cancel all images in a certain priority queue
	

    static final int BIPS_IMAGE_WIDTH = 20;
    static final int BIPS_IMAGE_HEIGHT = 8;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
    
	/** For showing and hiding our notification. */
	NotificationManager mNM;
	/** Keeps track of all current registered clients. */
	ArrayList<IRemoteServiceCallback> mClientsAidl = new ArrayList<IRemoteServiceCallback>();
	/** Holds last value set by a client. */
	int mValue = 0;

	/** The message priority queue **/
	// use index as priority (high = 0, low = 4). Then you can use the resulting
	// queue. Not sure how to create an ArrayList of BipsImages without warning.
	ArrayList<BipsImage>[] mImageQueue = (ArrayList<BipsImage>[]) new ArrayList[API_LOWEST_PRIORITY + 1];
	
	// TODO: Get rid of the above warning
	ArrayList<ArrayList<Object>> mImageQueue2;
	
	/** Power manager to allow service to operate while the phone is locked **/
	PowerManager pm;
    PowerManager.WakeLock wl;

	 
	// The image currently being projected
	protected BipsImage mCurrentImage = null;
	
	/** Important types and such for the scheduler **/
	boolean mIsDestroyed = false;


	/**
	 * Handler of internal private messages and thread execution
	 */
	final Handler mHandler = new Handler();
	final Runnable rSetIdle = new Runnable() {
		public void run() {
			// Stop the service if nobody is using it.
			if (mClientsAidl.isEmpty())
			{
				stopSelf();
			}
			
			// Stop thread if the service is destroyed
			if (mIsDestroyed)
				return;
			 
			int delay = 0;

			mCurrentImage = null;
			
			// find the next image to service
			for (int i = 0; i < mImageQueue.length; ++i) {
				if (!mImageQueue[i].isEmpty()) {
					Log.i(TAG, "Next image: priority " + i);
					// Pop off the top of the queue to project
					mCurrentImage = mImageQueue[i].remove(0);
					break;
				}
			}

			if (mCurrentImage != null) {
				delay = mCurrentImage.time;
				sendImage(mCurrentImage);
				
				// Check for an image after the image time length elapses
				if (delay != 0)
				{
					mHandler.postDelayed(rSetIdle, delay);
				}
			}
		}
	};

	/**
	 * Target we publish for BTThread to send messages to IncomingHandler.
	 */
	private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + connectedDeviceName, Toast.LENGTH_SHORT).show();
                showNotification("Connected to " + connectedDeviceName);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	final Messenger mMessenger = new Messenger(mBtHandler);

	/**
	 * This listener will watch for DeviceListActivity to update the 
	 * bluetooth device and connect to the chosen device
	 */
	OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                String key) {
            if (key == BT_DEVICE)
            {
                SharedPreferences settings = getSharedPreferences(BT_PREFS, 0);
                String address = settings.getString(key, null);
                if (D) Log.v(TAG, "Device pref updated: " + address);
                bluetoothConnect(address);
            }
        }
    };
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (D) Log.e(TAG, "+++ ON CREATE +++");
			//android.os.Debug.waitForDebugger();
		
		
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Display a notification about us starting.
		showNotification(getText(R.string.remote_service_started));
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // Make the service listen for selected BT device to connect to
        SharedPreferences settings = getSharedPreferences(BT_PREFS, 0);
        settings.registerOnSharedPreferenceChangeListener(mListener);
        
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			// finish(); no available for Service
			stopSelf();
			return;
		}
		
		
		// initialize the priority queues
		for (int i = 0; i < mImageQueue.length; i++) {
			mImageQueue[i] = new ArrayList<BipsImage>();
		}
		
		
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mMessenger);
	          		
		// auto-connect to the bluesmirf
		if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled() && settings.contains(BT_DEVICE))
            {
                bluetoothConnect(settings.getString(BT_DEVICE, null));
            }
		}

		
		// acquire wakelock to prevent lost messages caused by sleeping phone
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wl.acquire();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "--- ON DESTROY ---");
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();

		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);

		// Stop the wake lock
		wl.release();
		
		// flag the service as destroyed for threads to detect and stop themselves
		mIsDestroyed = true;
		
		// TODO Remove mHandler callbacks
		
		// Tell the user we stopped.
		Toast.makeText(this, R.string.remote_service_stopped,
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		//return mMessenger.getBinder();
		return mBinder;
	}
	private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
	    
	    @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            
                return super.onTransact(code, data, reply, flags);
            
        }
	    
        public int getPid(){
            return Process.myPid();
        }
        public void basicTypes(int anInt, long aLong, boolean aBoolean,
            float aFloat, double aDouble, String aString) {
            // Does nothing
        }
        
        public void imageRequestQueue(byte[] image, int time, byte priority, String packageName) {
        	if (D) Log.i(TAG, "Queuing image: time " + time + " PID: " + packageName);
			// Add an image into the queues for projection
			BipsImage bImage = new BipsImage(image, priority, time, packageName);
			
			// get the priority of the application requesting image projection
			SharedPreferences settings = getSharedPreferences(BIPS_PREFS, 0);
			int sendingPriority = settings.getInt(packageName, API_MEDIUM_PRIORITY);
			if (D) Log.i(TAG, "  image priority " + sendingPriority);
			
			mImageQueue[sendingPriority].add(bImage);
			
			if (mCurrentImage == null)
			{
				mHandler.post(rSetIdle);
			}
        }
        
        public void imageRequestCancelCurrent(String packageName) {

			if (D)
				Log.i(TAG, "Cancel current image: PID " + packageName);
			// Cancel the current projected image if it is from the requesting application
			// This is done by setting the service monitoring status to IDLE
			if (mCurrentImage != null && packageName.equals(mCurrentImage.packageName))
			{
				// sending a preempting blank image
				BipsImage bImage = new BipsImage();
				mImageQueue[bImage.priority].add(0,bImage);
				mHandler.postAtFrontOfQueue(rSetIdle);
			}
        }
        
        public void imageRequestCancelAll(String packageName) {
            if (packageName != null)
            {
                if (D)
                    Log.i(TAG, "CancellAll: Canceling current image: PID " + packageName);
                // Cancel the current projected image if it is from the requesting application
                // This is done by setting the service monitoring status to IDLE
                if (mCurrentImage != null && packageName.equals(mCurrentImage.packageName))
                {
                    // sending a preempting blank image
                    BipsImage bImage = new BipsImage();
                    mImageQueue[bImage.priority].add(0,bImage);
                    mHandler.postAtFrontOfQueue(rSetIdle);
                }
                
                if (D)
                    Log.i(TAG, "Cancelling all queued images: PID " + packageName);
                // Cancel all the images in the queues from this application
                // get the priority of the application requesting image projection
                SharedPreferences settings = getSharedPreferences(BIPS_PREFS, 0);
                int sendingPriority = settings.getInt(packageName, API_MEDIUM_PRIORITY);
                
                for (int j = 0; j < mImageQueue[sendingPriority].size(); ++j)
                {
                    if (mImageQueue[sendingPriority].get(j).packageName.equals(packageName))
                    {
                        if (D)
                            Log.i(TAG, "Cancelled");
                        mImageQueue[sendingPriority].remove(j);
                        --j;    // decrement since the next index will be pushed forward to the just removed one
                    }
                }
                
            }
        }
        
        public void deviceChosenConnect(String address) {
        	// When DeviceListActivity returns with a device to connect
			// Get the BluetoothDevice object
            bluetoothConnect(address);
        }

        public void registerCallback(IRemoteServiceCallback client) {
			if (D)
				Log.i(TAG, "Added client callback: " + client.toString());
			mClientsAidl.add(client);
			
			
			// Add the client to preferences if they don't exist yet
            SharedPreferences settings = getSharedPreferences(BIPS_PREFS, 0);
            String clientPackage = null;
            
            // Request the client's package name for identifying it
            try 
            {
                clientPackage = client.getClientPackageName();
            }
            catch (RemoteException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            if (clientPackage != null)
            {
                // a new application is binding, add it to preferences
                if (!settings.contains(clientPackage))
                {
                    SharedPreferences.Editor editor = settings.edit();
    
                    editor.putInt(clientPackage, API_MEDIUM_PRIORITY);
    
                    editor.commit();
                    if (D) Log.v(TAG, "New client to priority prefs: " + clientPackage);
                }
            }
            
			
		}

        public void unregisterCallback(IRemoteServiceCallback client) {
        	if (D)
				Log.i(TAG, "Removed client callback: " + client.toString());
			mClientsAidl.remove(client);
		}
        
        public void valueSetClient(int value) {
			mValue = value;
			for (int i = mClientsAidl.size() - 1; i >= 0; i--) {
				try {
					mClientsAidl.get(i).valueChanged(mValue);
				} catch (RemoteException e) {
					// The client is dead. Remove it from the list;
					// we are going through the list from back to front
					// so this is safe to do inside the loop.
					mClientsAidl.remove(i);
				}
			}
        }
        @Override
        public void bitmapRequestQueue(Bitmap image, int time, byte priority, String packageName)
        {
            if (D) Log.i(TAG, "Queuing bitmap: time " + time + " PID: " + packageName);
            // Add an image into the queues for projection
            if (image.getHeight() == BIPS_IMAGE_HEIGHT && image.getWidth() == BIPS_IMAGE_WIDTH)
            {
                
                byte[] imageArray = bitmapToByteArray(image);
                imageRequestQueue(imageArray, time, priority, packageName);
            }
        }
    };

    /**
	 * Show a notification while this service is running.
	 */
	private void showNotification(CharSequence text) {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification

		// Set the icon, scrolling text and timestamp
		Notification noti = new Notification(R.drawable.ic_launcher, 
		        text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, com.group057.BipsMainActivity.class), Notification.FLAG_ONGOING_EVENT);
		noti.flags = Notification.FLAG_ONGOING_EVENT;
		// Set the info for the views that show in the notification panel.
		noti.setLatestEventInfo(this, getText(R.string.remote_service_label),
				text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
//        startForeground(R.string.remote_service_started, noti);
        mNM.notify(R.string.remote_service_started, noti);
	}
	
	/**
	 * Sends an image.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendImage(BipsImage message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message != null) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] byteTime = ByteBuffer.allocate(4).putInt(message.time)
					.array();
			mChatService.write(message.pixelArray);
			mChatService.write(byteTime);
			// still need to write the priority, time, etc.
		}
	}

//
//	public static Thread performOnBackgroundThread(final Runnable runnable) {
//	    final Thread t = new Thread() {
//	    	public void run(){
//	            try {
//	                runnable.run();
//	            } finally {
//
//	            }
//	        }
//	    };
//	    t.setDaemon(true);
//	    t.start();
//	    return t;
//	}
	
    public byte[] bitmapToByteArray(Bitmap b) {
        byte[] imageArray = new byte[BIPS_IMAGE_WIDTH];

        for (int i = 0; i < b.getWidth(); ++i) {
            // start off the column as full black
            byte column = (byte) 0xff;

            for (int j = 0; j < b.getHeight(); ++j) {
                // update the column for each pixel, where the XOR will
                // be used to designate the bit/pixel as blank
                // and the OR will be used to indicate a displaying signal

                // minus 1 from the height to shift to the MSB (first iteration)
                // minus j to shift to somewherein the middle
                byte position = (byte) (1 << b.getHeight() - j - 1);
                if (b.getPixel(i, j) == -1) {
                    column = (byte) (position ^ column);
                } else {
                    column = (byte) (position | column);
                }
            }
            imageArray[i] = column;
        }

        return imageArray;
    }

    public void bluetoothConnect(String address) {
        // When DeviceListActivity returns with a device to connect
        // Get the BluetoothDevice object
        if (address != null)
        {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            showNotification(getText(com.group057.R.string.selected_bt_device) + device.getName());

            // Attempt to connect to the device
            mChatService.connect(device, true);
            
        }
        else
        {
            if (D) Log.e(TAG, "Tried to BT connect to null address");
        }
    }
    


	/** 
	 * This call takes in the fields required for projecting the image to formulate a Message that 
	 * can be sent to BIPS Service to queue up for laser projection
	 * 
	 * @param image The 20x8 pixel image array to project
	 * @param time Duration in milliseconds to project the image
	 * @param priority The priority of the message which is a byte value from 0-4 (highest to lowest)
	 * @param replyTo The Application's Messenger class for communication with BIPS
	 * @return A message object for the application to send to the BIPS service. Will be null if input
	 * parameters are bad
	 */
	public static Message createImageRequestMessage(byte[] image, int time, byte priority, Messenger replyTo)
	{
		// Error check
		if (image == null || 
				time <= 0 || 
				priority < API_HIGHEST_PRIORITY || 
				priority > API_LOWEST_PRIORITY || 
				replyTo == null)
		{
			return null;
		}
		
		
		Message msg = Message.obtain(null, BipsService.API_REQ_QUEUE_IMAGE);
		Bundle bundle = new Bundle();
		bundle.putInt(BipsService.API_IMAGE_TIME, time);
		bundle.putByteArray(BipsService.API_IMAGE_PIXELS, image);
		bundle.putByte(BipsService.API_IMAGE_PRIORITY, priority);
		msg.replyTo = replyTo;
		msg.setData(bundle);
		return msg;
	}
	

	/** 
	 * This call will request to cancel the currently projecting image if the requesting application
	 * is the one which requested it originally.
	 * 
	 * @param replyTo The Application's Messenger class for communication with BIPS
	 * @return A message object for the application to send to the BIPS service. Will be null if input
	 * parameters are bad
	 */
	public static Message cancelCurrentImageRequestMessage(Messenger replyTo)
	{
		// Error check
		if (replyTo == null)
		{
			return null;
		}
		
		Message msg = Message.obtain(null, BipsService.API_REQ_CANCEL_CURRENT_IMAGE);
		msg.replyTo = replyTo;
		return msg;
	}
	

	/** 
	 * This call will remove all images queued up by the requesting application.
	 * 
	 * @param replyTo The Application's Messenger class for communication with BIPS
	 * @return A message object for the application to send to the BIPS service. Will be null if input
	 * parameters are bad
	 */
	public static Message cancelAllImagesRequestMessage(Messenger replyTo)
	{
		// Error check
		if (replyTo == null)
		{
			return null;
		}
		
		Message msg = Message.obtain(null, BipsService.API_REQ_CANCEL_ALL_IMAGES);
		msg.replyTo = replyTo;
		return msg;
	}
	

	/** 
	 * This call will search through the input priority queue and remove all requests from the 
	 * messaging application.
	 * 
	 * @param priority The priority of the message which is a byte value from 0-4 (highest to lowest)
	 * @param replyTo The Application's Messenger class for communication with BIPS
	 * @return A message object for the application to send to the BIPS service. Will be null if input
	 * parameters are bad
	 */
	public static Message cancelImageByPriorityRequestMessage(byte priority, Messenger replyTo)
	{
		// Error check
		if (priority < API_HIGHEST_PRIORITY || 
			priority > API_LOWEST_PRIORITY || 
			replyTo == null)
		{
			return null;
		}
		
		Message msg = Message.obtain(null, BipsService.API_REQ_CANCEL_ALL_IMAGES);
		Bundle bundle = new Bundle();
		bundle.putByte(BipsService.API_IMAGE_PRIORITY, priority);
		msg.replyTo = replyTo;
		msg.setData(bundle);
		return msg;
	}
}

// This class is for providing a structure for the message data between phone
// and
// Arduino.
// image is a byte array, since the image is supposedly 20x8 pixels,
// a single byte will represent a column of pixels
class BipsImage {
	byte[] pixelArray = new byte[20]; // the array where each byte represents a column (default
						// 20 columns/horizontal pixels)
	byte priority; // the priority level from 0-4
	int time; // the length of time to project the image in seconds
	
	Messenger requester; // may be used to identify the requesting application
	String packageName; 
	
	BipsImage(){
		priority = 0;
		time = 0;
		requester = null;
	}
	BipsImage(byte[] imageByteArray, byte iPriority, int iTime, Messenger iId) {
		pixelArray = imageByteArray;
		priority = iPriority;
		time = iTime;
		requester = iId;
	}

	BipsImage(byte[] imageByteArray, byte iPriority, int iTime, String packageName) {
		pixelArray = imageByteArray;
		priority = iPriority;
		time = iTime;
		this.packageName = packageName;
	}
}
