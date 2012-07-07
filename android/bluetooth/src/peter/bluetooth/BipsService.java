package peter.bluetooth;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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

	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to unregister a client, to stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	static final int MSG_UNREGISTER_CLIENT = 2;

	/**
	 * Command to service to set a new value. This can be sent to the service to
	 * supply a new value, and will be sent by the service to any registered
	 * clients with the new value.
	 */
	static final int MSG_SET_VALUE = 3;

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 4;
	public static final int MESSAGE_READ = 5;
	public static final int MESSAGE_WRITE = 6;
	public static final int MESSAGE_DEVICE_NAME = 7;
	public static final int MESSAGE_TOAST = 8;

	// Messages from DeviceListActivity
	public static final int REQUEST_CONNECT_DEVICE_SECURE = 9;

	// Debug message to send an image over BT
	public static final int DEBUG_SEND_IMAGE = 10;

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
	/** Holds last value set by a client. */
	int mValue = 0;

	/** The message priority queue **/
	// use index as priority (high = 0, low = 4). Then you can use the resulting
	// queue
	ArrayList<BipsImage>[] mImageQueue = (ArrayList<BipsImage>[]) new ArrayList[5];
	
	/** Important types and such for the scheduler **/
	boolean mIsDestroyed = false;
	Timer mTimer = new Timer();
	TimerTask mSetIdle = new TimerTask() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mStatus = BipsStatus.IDLE;
		}
	};
	Thread messageMonitoring;
	
	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
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

					break;
				case BluetoothChatService.STATE_CONNECTING:
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: (CONNECTING) "
								+ msg.arg1);

					break;
				case BluetoothChatService.STATE_LISTEN:
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
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				Intent data = (Intent) msg.obj;
				connectDevice(data, true);
				break;
			case DEBUG_SEND_IMAGE:
				// When DeviceListActivity returns with a device to connect
				BipsImage image = new BipsImage(msg.getData().getByteArray(
						API_IMAGE_PIXELS), msg.getData().getByte(
						API_IMAGE_PRIORITY), msg.getData().getInt(
						API_IMAGE_TIME), msg.getData().getInt(API_IMAGE_ID));
				sendImage(image);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Display a notification about us starting.
		showNotification();

		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");

		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);

		// flag the service as destroyed for threads to detect and stop themselves
		mIsDestroyed = true;
		
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
		return mMessenger.getBinder();
	}

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
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TestActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		noti.setLatestEventInfo(this, getText(R.string.remote_service_label),
				text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
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
			mChatService.write(byteTime);
			mChatService.write(message.pixelArray);

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

		// initialize the priority queues
		for (int i = 0; i < mImageQueue.length; i++) {
			mImageQueue[i] = new ArrayList<BipsImage>();
		}

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mMessenger);

		Intent deviceIntent = new Intent(this, DeviceListActivity.class);
		deviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(deviceIntent);
		
		// A separate thread for queue monitoring is created and then started.
		messageMonitoring = performOnBackgroundThread(new Runnable(){
			public void run() {
				while (!mIsDestroyed) {
					BipsImage currentImage = null;
					int delay = 1000;

					// Only perform the algorithm when the service is free
					if (mStatus != BipsStatus.BUSY) {
						Log.i(TAG, "Monitoring cycle");
						mStatus = BipsStatus.BUSY;

						// find the next image to service
						for (int i = 0; i < mImageQueue.length; ++i) {
							if (!mImageQueue[i].isEmpty()) {
								// Pop off the top of the queue to project
								currentImage = mImageQueue[i].remove(0);
								break;
							}
						}

						if (currentImage != null) {
							delay = currentImage.time;
							sendImage(currentImage);
						}
						// Set the service back to idle to repeat the algorithm
						//mTimer.schedule(mSetIdle, delay);
					}
				}
			}
		});
		messageMonitoring.setDaemon(true);
		messageMonitoring.start();
	}

	public static Thread performOnBackgroundThread(final Runnable runnable) {
	    final Thread t = new Thread() {
	        @Override
	        public void run() {
	            try {
	                runnable.run();
	            } finally {

	            }
	        }
	    };
	    t.start();
	    return t;
	}

}

// This class is for providing a structure for the message data between phone
// and
// Arduino.
// image is a byte array, since the image is supposedly 20x8 pixels,
// a single byte will represent a column of pixels
class BipsImage {
	byte[] pixelArray; // the array where each byte represents a column (default
						// 20 columns/horizontal pixels)
	byte priority; // the priority level from 0-4
	int time; // the length of time to project the image in seconds
	int id; // may be used to identify the requesting application

	BipsImage(byte[] imageByteArray, byte iPriority, int iTime, int iId) {
		pixelArray = imageByteArray;
		priority = iPriority;
		time = iTime;
		id = iId;
	}
}
