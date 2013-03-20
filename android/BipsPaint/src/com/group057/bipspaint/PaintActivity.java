package com.group057.bipspaint;

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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;

public class PaintActivity extends Activity {
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(mThumbIds[position]);
            return imageView;
        }

        // references to our images
        private Integer[] mThumbIds = {
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black,
                R.drawable.white, R.drawable.black
        };
    }
  
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.paint, menu);
        return true;
    }

    // Debug var
    private static final boolean D = true;
    private static final String TAG = "BipsPaintApp";

    private static final int BIPS_IMAGE_WIDTH = 20;

    GridView mGridView = null;
    
    /** Messenger for communicating with service. */
    IRemoteService mIRemoteService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.v(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_paint);
        
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(PaintActivity.this, "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        
        doBindService();
        
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D)Log.v(TAG, "++ ON START ++");

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
        if (D) Log.v(TAG, "+ ON DESTROY +");
        
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
            // this gets an instance of the IRemoteInterface, which we can use
            // to call on the service
            mIRemoteService = IRemoteService.Stub.asInterface(service);
            Toast.makeText(getApplicationContext(), "Attached to BIPS",
                    Toast.LENGTH_SHORT).show();

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
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if (!mIsBound) {
            bindService(new Intent(IRemoteService.class.getName()),
                    mConnection, Context.BIND_AUTO_CREATE);
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
     * This implementation is used to receive callbacks from the remote service.
     */
    private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {
        public int getPid() {
            return Process.myPid();
        }

        /**
         * This is called by the remote service regularly to tell us about new
         * values. Note that IPC calls are dispatched through a thread pool
         * running in each process, so the code executing here will NOT be
         * running in our main thread like most other things -- so, to update
         * the UI, we need to use a Handler to hop over there.
         */
        public void valueChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(0, value, 0));
        }

        /**
         * This is called by the service to find out what applications are
         * binding and using the projector to allow the user to assign priority
         * to which applications they prefer to see from the projector over
         * other apps.
         */
        public String getClientPackageName() {
            return getPackageName();
        }
    };
}
