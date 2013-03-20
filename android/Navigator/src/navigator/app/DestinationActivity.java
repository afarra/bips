package navigator.app;

import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;


public class DestinationActivity extends MapActivity{
	private GeoPoint myLocation = null;
	private Button ok;
	private MapView mapView;
	private Intent selectedDestination;
	private MapItemOverlay itemizedoverlay;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapview);
        
        ok = (Button)findViewById(R.id.setDirection);
        ok.setOnClickListener(setDestinationListener);
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        Bundle b = this.getIntent().getExtras();
        selectedDestination = new Intent(); 
        
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.map_marker);
        itemizedoverlay = new MapItemOverlay(drawable, this);
        int lat = (int)(b.getDouble("myLat")*1E6);
        int lng = (int)(b.getDouble("myLong")*1E6);
        
        myLocation = new GeoPoint(lat,lng);
        OverlayItem overlayitem = new OverlayItem(myLocation,"Your Location","");
        itemizedoverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedoverlay);
    
    }
    
    
    private OnClickListener setDestinationListener = new OnClickListener() {
        public void onClick(View v) {
        	//selectedDestination.putExtra("selectedColor", color);
            //set result with our selectedColor intent and RESULT_OK request code\
        	GeoPoint destination = itemizedoverlay.getDestination();
            
        	if( destination != null) {
	        	Bundle bundle = new Bundle();
	            bundle.putInt("destLat", destination.getLatitudeE6());
	            bundle.putInt("destLong", destination.getLongitudeE6());
	            
	            selectedDestination.putExtras(bundle);
	            
	            setResult(RESULT_OK, selectedDestination);
	            //then finish the activity
	            finish();
        	}
        }
     };
    
     @Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
