import android.app.Activity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;

import com.example.callalert.R;


public class CallAlert extends Activity{
	// Debug var
	private static final boolean D = true;
	private static final String TAG = "CallAlertApp";
	
	PhoneStateListener callState;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
        requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.call_alert);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");
		
		callState = new PhoneStateListener() {
			@Override 
			public void onCallStateChanged(int state, String incomingNumber)
			{
				if (state == TelephonyManager.CALL_STATE_RINGING)
				{
					
				}
			}
		};
	}
}
