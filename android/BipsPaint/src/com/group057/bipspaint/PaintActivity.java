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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.group057.IRemoteService;
import com.group057.IRemoteServiceCallback;

public class PaintActivity extends Activity {
    
    int[][] pixelTrack = new int[BIPS_LASER_COUNT][BIPS_IMAGE_WIDTH];
    
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return mThumbIds[position];
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(40, 40));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(mThumbIds[position]);
            return imageView;
        }

        // references to our images
        private Integer[] mThumbIds = {
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke,
                R.drawable.whitestroke, R.drawable.whitestroke
            };
    }
  

    // Debug var
    private static final boolean D = true;
    private static final String TAG = "BipsPaintApp";

    private static final int BIPS_IMAGE_WIDTH = 20;
    private static final int BIPS_LASER_COUNT = 5;

    // UI
    Button mProjectButton = null;
    GridView mGridView = null;
    Button mReset = null;
    CheckBox mEraser = null;
    
    /** Messenger for communicating with service. */
    IRemoteService mIRemoteService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.v(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_paint);
        
        mGridView = (GridView) findViewById(R.id.gridview);
        mGridView.setAdapter(new ImageAdapter(this));

        mGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(PaintActivity.this, "" + position, Toast.LENGTH_SHORT).show();

                ImageView iv = (ImageView) v;
               
                // toggle the pixel colour
                if (!mEraser.isChecked())
                {
                    iv.setImageResource(R.drawable.blackstroke);
                    iv.setTag(R.drawable.blackstroke);
                    pixelTrack[position/BIPS_IMAGE_WIDTH][position % BIPS_IMAGE_WIDTH] = 1; 
                }
                else
                {
                    iv.setImageResource(R.drawable.whitestroke);
                    iv.setTag(R.drawable.whitestroke);
                    pixelTrack[position/BIPS_IMAGE_WIDTH][position % BIPS_IMAGE_WIDTH] = 0; 
                }
                
            }
            
        });

        mProjectButton = (Button) findViewById(R.id.project_image);
        mProjectButton.setEnabled(false);
        mProjectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
               

                // Bind Bips
                try {
                    mIRemoteService.imageRequestQueue(getGridPixels(), 15000, 
                            (byte) 0, getPackageName());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
//        mReset = (Button) findViewById(R.id.button1);
//        mReset.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//               
//                // set all squares white
//                for (int i = 0; i < BIPS_IMAGE_WIDTH * BIPS_LASER; i++)
//                {
//                    mGridView.
//                }
//                
//            }
//        });

        mEraser = (CheckBox) findViewById(R.id.checkBox1);
        doBindService();
        
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D)Log.v(TAG, "++ ON START ++");
        
        if (mIsBound)
        {
            mProjectButton.setEnabled(true);
        }

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

    byte[] getGridPixels()
    {
        byte[] image = new byte[BIPS_IMAGE_WIDTH];

        for (int i = 0; i < BIPS_IMAGE_WIDTH; i++)
        {
            for (int j = 0; j < BIPS_LASER_COUNT; j++)
            {
                byte pixel = (pixelTrack[j][i] == 1) ? (byte)0x01 : (byte)0x00; 
                        
                image[i] = (byte) ((byte)image[i] | (byte)(pixel<<j+1));
            }
        }

        return image;
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
