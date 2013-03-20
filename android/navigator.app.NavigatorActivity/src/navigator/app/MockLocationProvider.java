package navigator.app;

import java.io.IOException;
import java.util.List;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class MockLocationProvider extends Thread {

    private List<String> data;

    private LocationManager locationManager;

    private String mocLocationProvider;

    private String LOG_TAG = "faren";

    public MockLocationProvider(LocationManager locationManager,
            String mocLocationProvider, List data) throws IOException {

        this.locationManager = locationManager;
        this.mocLocationProvider = mocLocationProvider;
        this.data = data;
    }

    @Override
    public void run() {
    	long elapsedTime = 0;
        for (String str : data) {

            try {
				this.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            // Set one position
            String[] parts = str.split(",");
            Double latitude = Double.valueOf(parts[0]);
            Double longitude = Double.valueOf(parts[1]);
            //Long time = Long.valueOf(parts[2])*1000;
            elapsedTime+=5000;
            //Double altitude = Double.valueOf(parts[2]);
            Location location = new Location(mocLocationProvider);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setTime(System.currentTimeMillis());
            //location.setAltitude(altitude);

            // set the time in the location. If the time on this location
            // matches the time on the one in the previous set call, it will be
            // ignored
            //location.setTime(System.currentTimeMillis());

            locationManager.setTestProviderLocation(mocLocationProvider,
                    location);
            Log.e(LOG_TAG, location.toString());
        }
    }
}
