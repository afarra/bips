package com.group057;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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

	// Status values for mStatus
	private static BipsStatus mStatus = BipsStatus.IDLE;

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
	
	// Debug message to send an image over BT
	public static final int DEBUG_SEND_IMAGE = 20;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	// Buffer taking in the serial data and forming into a chat line
	private String readBufString = null;

	/** For showing and hiding our notification. */
	NotificationManager mNM;
	/** Keeps track of all current registered clients. */
	ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	ArrayList<IRemoteServiceCallback> mClientsAidl = new ArrayList<IRemoteServiceCallback>();
	/** Holds last value set by a client. */
	int mValue = 0;

	/** The message priority queue **/
	// use index as priority (high = 0, low = 4). Then you can use the resulting
	// queue
	ArrayList<BipsImage>[] mImageQueue = (ArrayList<BipsImage>[]) new ArrayList[5];
	
	// The image currently being projected
	protected BipsImage mCurrentImage = null;
	
	/** Important types and such for the scheduler **/
	boolean mIsDestroyed = false;
	
	Thread messageMonitoring;
	
	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			BipsImage image;
			Intent data;
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				if (D)
					Log.i(TAG, "Added client: " + msg.replyTo);
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				if (D)
					Log.i(TAG, "Removed client: " + msg.replyTo);
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_VALUE:
				mValue = msg.arg1;
				for (int i = mClients.size() - 1; i >= 0; i--) {
					try {
						mClients.get(i).send(
								Message.obtain(null, MSG_SET_VALUE, mValue, 0));
					} catch (RemoteException e) {
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						mClients.remove(i);
					}
				}
				break;
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: (CONNECTED) "
								+ msg.arg1);
					mHandler.sendMessage(mHandler.obtainMessage(BipsService.MSG_SET_VALUE, msg.arg1, 0));
					break;
				case BluetoothChatService.STATE_CONNECTING:
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: (CONNECTING) "
								+ msg.arg1);

					break;
				case BluetoothChatService.STATE_LISTEN:
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: (LISTENING) " + msg.arg1);
					mHandler.sendMessage(mHandler.obtainMessage(BipsService.MSG_SET_VALUE, msg.arg1, 0));
					showDisconnectNotification();

					break;
				case BluetoothChatService.STATE_NONE:
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: (NONE) " + msg.arg1);
						
					break;
				}
				break;
			case MESSAGE_WRITE: // doesn't do anything
				// byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				// String writeMessage = new String(writeBuf);
				break;
			case MESSAGE_READ: // doesn't do anything
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				// String readMessage = new String(readBuf, 0, msg.arg1);
				readBufString = new String(readBuf);
				if (D)
					Log.i(TAG, "readBuf: " + readBufString);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				/*Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();*/
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				
				break;
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				data = (Intent) msg.obj;
				connectDevice(data, true);
				break;	
			case API_REQ_QUEUE_IMAGE:
				if (D)
					Log.i(TAG, "Queuing image: time " + msg.getData().getInt(API_IMAGE_TIME) + 
							" messenger: " + msg.replyTo.toString());
				// Add an image into the queues for projection
				image = new BipsImage(msg.getData().getByteArray(
						API_IMAGE_PIXELS), msg.getData().getByte(
						API_IMAGE_PRIORITY), msg.getData().getInt(
						API_IMAGE_TIME), msg.replyTo);
				
				mImageQueue[image.priority].add(image);		
				
				break;
			case API_REQ_CANCEL_CURRENT_IMAGE:
				if (D)
					Log.i(TAG, "Cancel current image: " + msg.replyTo.toString());
				// Cancel the current projected image if it is from the requesting application
				// This is done by setting the service monitoring status to IDLE
				if (mCurrentImage != null && msg.replyTo.equals(mCurrentImage.requester))
				{
					mHandler.post(rSetIdle);
				}
				break;
			case API_REQ_CANCEL_ALL_IMAGES:
				if (D)
					Log.i(TAG, "Cancelling all images: " + msg.replyTo.toString());
				// Cancel all the images in the queues from this application
				// Search through all priority queues for images with matching messenger and remove
				for( int i = 0; i < mImageQueue.length; ++i)
				{
					if (D)
						Log.i(TAG, "Searching Priority queue " + i + "(count: " + mImageQueue[i].size() + ")");
					for (int j = 0; j < mImageQueue[i].size(); ++j)
					{
						if (mImageQueue[i].get(j).requester.equals(msg.replyTo))
						{
							if (D)
								Log.i(TAG, "Cancelled");
							mImageQueue[i].remove(j);
							// decrement since the next index will be pushed forward to the just removed one
							// TODO could be change to a loop that iterates backwards instead to remove this line
							--j;	
						}
					}
				}
				break;
			case API_REQ_CANCEL_BY_PRIORITY:
				if (D)
					Log.i(TAG, "Priority Cancel: " + msg.replyTo.toString() + " priority " + 
							msg.getData().getByte(API_IMAGE_PRIORITY));
				// Cancel all images in a certain priority queue
				// Search through all priority queues for images with matching messenger 
				// and priority and remove
				for( int i = 0; i < mImageQueue.length; ++i)
				{
					for (int j = 0; j < mImageQueue[i].size(); ++j)
					{
						if (mImageQueue[i].get(j).requester.equals(msg.replyTo) && 
								mImageQueue[i].get(j).priority == msg.getData().getByte(BipsService.API_IMAGE_PRIORITY))
						{
							mImageQueue[i].remove(j);
						}
					}
				}
				break;
			case DEBUG_SEND_IMAGE:
				// Debug mode, send an image straight to Arduino. Skip queues.
				if (D)
				{
					// construct a BipsImage from the message data bundle
					image = new BipsImage(msg.getData().getByteArray(
							API_IMAGE_PIXELS), msg.getData().getByte(
							API_IMAGE_PRIORITY), msg.getData().getInt(
							API_IMAGE_TIME), msg.replyTo);
					sendImage(image);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Handler of internal private messages and thread execution
	 */
	final Handler mHandler = new IncomingHandler();
	final Runnable rSetIdle = new Runnable() {
		public void run() {
			Log.i(TAG, "Algostart");
			mStatus = BipsStatus.IDLE;
			// Stop the service if nobody is using it.
			if (mClients.isEmpty())
			{
				stopSelf();
			}
			
			// Stop thread if the service is destroyed
			if (mIsDestroyed)
				return;
			 
			int delay = 0;	// by default checks the queues once per second

			mStatus = BipsStatus.BUSY;
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
				
				// Set the service back to idle to repeat the algorithm
				mHandler.postDelayed(rSetIdle, delay);
			}
		}
	};

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(mHandler);

	@Override
	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Display a notification about us starting.
		showNotification();

		if (D){
			Log.e(TAG, "+++ ON CREATE +++");
			//android.os.Debug.waitForDebugger();
		}
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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

		if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {

        		if (D){
        			Log.e(TAG, "Service starting BT threads");
        		}
        		
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
		
		// Get the paired device.
		Intent deviceIntent = new Intent(this, DeviceListActivity.class);
		deviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(deviceIntent);
		
		
		setupChat();
		/* This is handled by 3rd party now
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(enableIntent);
			// Otherwise, setup the chat session
		}
		

		// TODO: This does not properly get launched if BT needed to be enabled.
		if (mBluetoothAdapter.isEnabled()) {
			// Let them choose the device to pair with if BT enabled.
			if (mChatService == null)
				setupChat();
		}
		*/
		
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "--- ON DESTROY ---");
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)

		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);

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
        public int getPid(){
            return Process.myPid();
        }
        public void basicTypes(int anInt, long aLong, boolean aBoolean,
            float aFloat, double aDouble, String aString) {
            // Does nothing
        }
        
        public void imageRequestQueue(byte[] image, int time, byte priority, int pid) {
        	if (D)
				Log.i(TAG, "Queuing image: time " + time + " PID: " + pid);
			// Add an image into the queues for projection
			BipsImage bImage = new BipsImage(image, priority, time, pid);
			
			mImageQueue[bImage.priority].add(bImage);
			
			if (mCurrentImage == null)
			{
				mHandler.post(rSetIdle);
			}
        }
        
        public void imageRequestCancelCurrent(int pid) {

			if (D)
				Log.i(TAG, "Cancel current image: PID " + pid);
			// Cancel the current projected image if it is from the requesting application
			// This is done by setting the service monitoring status to IDLE
			if (mCurrentImage != null && pid == mCurrentImage.pid)
			{
				// TODO Doesn't work
				//mHandler.post(rSetIdle);
			}
        }
        
        public void imageRequestCancelAll(int pid) {
        	if (D)
				Log.i(TAG, "Cancelling all images: PID " + pid);
			// Cancel all the images in the queues from this application
			// Search through all priority queues for images with matching messenger and remove
			for( int i = 0; i < mImageQueue.length; ++i)
			{
				for (int j = 0; j < mImageQueue[i].size(); ++j)
				{
					if (mImageQueue[i].get(j).pid == pid)
					{
						if (D)
							Log.i(TAG, "Cancelled");
						BipsImage image = mImageQueue[i].remove(j);
						image = null;
						--j;	// decrement since the next index will be pushed forward to the just removed one
					}
				}
			}
        }
        
        public void deviceChosenConnect(String address) {
        	// When DeviceListActivity returns with a device to connect
			// Get the BluetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mChatService.connect(device, true);
        }

        public void registerCallback(IRemoteServiceCallback client) {
			if (D)
				Log.i(TAG, "Added client callback: " + client.toString());
			mClientsAidl.add(client);
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
    };

    /**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// Set the icon, scrolling text and timestamp
		Notification noti = new Notification(R.drawable.ic_launcher, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TestActivity.class), 0);*/
		noti.flags = Notification.FLAG_ONGOING_EVENT;
		// Set the info for the views that show in the notification panel.
		noti.setLatestEventInfo(this, getText(R.string.remote_service_label),
				text, null);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.remote_service_started, noti);
	}
	
	/**
	 * Show a notification of BT disconnection
	 */
	private void showDisconnectNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.not_connected);

		// Set the icon, scrolling text and timestamp
		Notification noti = new Notification(R.drawable.ic_launcher, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, DeviceListActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		noti.flags = Notification.FLAG_ONGOING_EVENT;
		// Set the info for the views that show in the notification panel.
		noti.setLatestEventInfo(this, getText(R.string.remote_service_label),
				text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
		mNM.cancel(R.string.remote_service_started);
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

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		/*
		// initialize the priority queues
		for (int i = 0; i < mImageQueue.length; i++) {
			mImageQueue[i] = new ArrayList<BipsImage>();
		}

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mMessenger);

		Intent deviceIntent = new Intent(this, DeviceListActivity.class);
		deviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(deviceIntent);*/
		
		
		mHandler.post(rSetIdle);
		// A separate thread for queue monitoring is created and then started.
		//messageMonitoring = performOnBackgroundThread(rSetIdle);
	}

	public static Thread performOnBackgroundThread(final Runnable runnable) {
	    final Thread t = new Thread() {
	    	public void run(){
	            try {
	                runnable.run();
	            } finally {

	            }
	        }
	    };
	    t.setDaemon(true);
	    t.start();
	    return t;
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
	int pid; 
	
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

	BipsImage(byte[] imageByteArray, byte iPriority, int iTime, int iId) {
		pixelArray = imageByteArray;
		priority = iPriority;
		time = iTime;
		pid = iId;
	}
}
