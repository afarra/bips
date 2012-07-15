package navigator.app;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NavigatorActivity extends Activity{
	
	private String toAddress;
	private Button setDestination;
	private Button getDirections;
	private EditText directions;
	private List<Step> steps;
	private LocationManager locationManager;
	private LocationProvider locationProvider;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
	private boolean running = false;
	private Location currentBestLocation = null;
	private static final int DESINATION_SELECTION=0;
	private GeoPoint to = null;
	private Thread turnByturn;
	private Thread retrieveDirections;
	
	private String directionsString = "";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setDestination = (Button)findViewById(R.id.swapMapView);
        setDestination.setOnClickListener(swapMapView);
        getDirections = (Button)findViewById(R.id.getDirections);
        getDirections.setOnClickListener(getDirectionsListener);
        directions = (EditText)findViewById(R.id.directions);
        directions.setKeyListener(null);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        	Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        	startActivity(settingsIntent);
        }
   

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
            	if (isBetterLocation(location, currentBestLocation)) {
            		currentBestLocation = location;
            	}
            	              
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        
    }
    
    public void onDestory() {
    	turnByturn.stop();
    }


    /** Determines whether one Location reading is better than the current Location fix
      * @param location  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
      */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    
    private void startApplication() {
    	Step s = ((LinkedList<Step>)this.steps).pop();
    	while(running) {
    		
    		double distanceLat = s.getEnd().getLatitudeE6() - currentBestLocation.getLatitude()*1E6;
    		double distanceLong = s.getEnd().getLongitudeE6() - currentBestLocation.getLongitude()*1E6;
    		
    		if(distanceLat < 10 && distanceLat > -10 && distanceLong > 10 && distanceLong < -10) {
    			directions.setText(s.getInstructions());
    		}
    		
    	}
    	
    }
    
    private void updateDirections() {
    	directions.setText(directionsString);
    }
    
    private OnClickListener swapMapView = new OnClickListener() {
    	public void onClick(View v) {
    		
            if (currentBestLocation == null) {
                return ;
            }
            
            Bundle bundle = new Bundle();
            bundle.putDouble("myLat", currentBestLocation.getLatitude());
            bundle.putDouble("myLong", currentBestLocation.getLongitude());
            
    	    //first create new intent to call ColorSelectorActivity 
    	    Intent request = new Intent(NavigatorActivity.this, DestinationActivity.class);
    	    request.putExtras(bundle);
    	    /*then start new ColorSelectorActivity and waiting for its result 
    	    here we use COLOR_SELECTOR unic code because we might have lot of 
    	    activity call from this activity for ignoring complict of that
    	    we use unic request code for each activity*/   
    	    startActivityForResult(request, DESINATION_SELECTION); 
    	}
    };
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case DESINATION_SELECTION:
    		if(resultCode == RESULT_OK){
    			Bundle b = data.getExtras();
    			to = new GeoPoint(b.getInt("destLat"),b.getInt("destLong"));
    		}
    	}
    }
    
    private OnClickListener getDirectionsListener = new OnClickListener() {
        public void onClick(View v) {
        	if(currentBestLocation != null && to != null) {
        		
        		//retrieveDirections thread should be stopped if it exists
        		if(retrieveDirections != null && retrieveDirections.isAlive()) {
        			retrieveDirections.stop();
        		}
        		
                retrieveDirections = new Thread (new Runnable() {
                	public void run() {
                		RetrieveDirections();
                	}
                });
        		
        		retrieveDirections.start();
        	} else if (currentBestLocation == null) {
        		directions.setText("Could not find your current location");
        	} else if (to == null) {
        		directions.setText("Please select a destination");
        	}
        }
    };
    
    
	
	//GeoPoint from = new GeoPoint(43465850, -80540030);
	//GeoPoint to = new GeoPoint(43473800, -80555680);
    private void RetrieveDirections() {
    	GeoPoint from = new GeoPoint((int)(currentBestLocation.getLatitude()*1E6),(int)(currentBestLocation.getLongitude()*1E6));
    	
		String url = makeUrl(from,to);	
		steps = new LinkedList<Step>();
		//get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();
			//parse the input and also register a private class for call backs
			sp.parse(url, new StepsParserCallBacks(steps));
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch (IOException ie) {
			ie.printStackTrace();
		}
		
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < steps.size(); i++) {
			sb.append("Step"+ (i+1)+":\n");
			sb.append(steps.get(i).toString());
			sb.append("\n");
		}
		directionsString = sb.toString();
		
    	runOnUiThread(new Runnable(){
			public void run() {
				updateDirections();
			}
    	});
    	 
        running = true;
        
        
        //turnByturn thread should be stopped if it exists
        if(turnByturn != null && turnByturn.isAlive()) {
        	turnByturn.stop();
        }
        
        turnByturn = new Thread(new Runnable() {
			public void run() {
				startApplication();
			}
        });
        
        turnByturn.start();
    }
    
    
    private String makeUrl(GeoPoint src, GeoPoint dest) {

        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/xml?");
        urlString.append("origin=");// from
        urlString.append(Double.toString((double) src.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) src.getLongitudeE6() / 1.0E6));
        urlString.append("&destination=");// to
        urlString.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) dest.getLongitudeE6() / 1.0E6));
        urlString.append("&sensor=true");

        System.out.println(urlString.toString());
        //Log.d("xxx", "URL=" + urlString.toString());
        return urlString.toString();
    }
    
}


class StepsParserCallBacks extends DefaultHandler {
	private List<Step> lst;
	
	private String tmpVal;
	private String tmpVal2;
	
	private int distance;
	
	private float lng;
	private float lat;
	
	private float lng1;
	private float lat1;
	
	private float lng2;
	private float lat2;
	
	private String instructions;
	private boolean instr = false;
	
	
	public StepsParserCallBacks(List<Step> lst) {
		this.lst = lst;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("html_instructions")) {
			this.instr = true;
			this.instructions = "";
		} else if (qName.equalsIgnoreCase("distance")) {
			this.distance = -1;
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		this.tmpVal = new String(ch, start, length);
		if(instr) {
			this.instructions += this.tmpVal;
		}
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			//(int d, String i, float lat1, float long1, float lat2, float long2)
		if (qName.equalsIgnoreCase("step")) {
			lst.add(new Step(this.distance, this.instructions, this.lat1, this.lng1, this.lat2, this.lng2));
		} else if (qName.equalsIgnoreCase("start_location")) {
			this.lng1 = this.lng;
			this.lat1 = this.lat;
		} else if (qName.equalsIgnoreCase("end_location")) {
			this.lng2 = this.lng;
			this.lat2 = this.lat;
		} else if (qName.equalsIgnoreCase("lat")) {
			this.lat = Float.parseFloat(this.tmpVal);
		} else if (qName.equalsIgnoreCase("lng")) {
			this.lng = Float.parseFloat(this.tmpVal);
		} else if (qName.equalsIgnoreCase("value") && this.distance == -1) {
			this.distance = Integer.parseInt(this.tmpVal);
		} else if (qName.equalsIgnoreCase("b")) {
			this.instructions+= this.tmpVal;
		} else if (qName.equalsIgnoreCase("div")) {
			this.instructions+= this.tmpVal;
		} else if (qName.equalsIgnoreCase("html_instructions")) {
			this.instr = false;
		}
		
		} catch (StringIndexOutOfBoundsException e) {
			System.out.println("String Index out of bounds");
			System.out.println("  tmpVal = " + this.tmpVal + " qname = " + qName);
			e.printStackTrace();
		}
	}
}