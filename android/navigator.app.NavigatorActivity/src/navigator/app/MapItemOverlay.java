package navigator.app;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MapItemOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	private int destination = -1;
	
	
	public MapItemOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}
	
	public MapItemOverlay(Drawable defaultMarker, Context context) {
		  super(boundCenterBottom(defaultMarker));
		  mContext = context;
	}

	@Override
	protected OverlayItem createItem(int i) {
	  return mOverlays.get(i);
	}

	@Override
	public int size() {
	  return mOverlays.size();
	}
	
	public void addOverlay(OverlayItem overlay) {
	    mOverlays.add(overlay);
	    populate();
	}
	
	public void removeOverlay(int i) {
		mOverlays.remove(i);
	}
	
	public GeoPoint getDestination() {
		if(destination != -1){
			return mOverlays.get(destination).getPoint();
		}
		return null;
	}
	
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		 if(super.onTap(p, mapView)){
			  return true;
			 }
		 
		 if(destination!=-1) {
			 //destination already exists, remove it.
			 removeOverlay(destination);
		 }
		 
		 OverlayItem o = new OverlayItem(p,"Destination","");
		 addOverlay(o);
		 destination = mOverlays.indexOf(o);
		 return true;
	}
	
	
	/*
	 *  List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.map_marker);
        MapItemOverlay itemizedoverlay = new MapItemOverlay(drawable, this);

        myLocation = new GeoPoint((int)(b.getDouble("myLat")*1E6),(int)(b.getDouble("myLong")*1E6));
        OverlayItem overlayitem = new OverlayItem(myLocation,"Your Location","Test");
        itemizedoverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedoverlay);
	 * 
	 * 
	 * */
	 
	/*@Override
	protected boolean onTap(int index) {
	  OverlayItem item = mOverlays.get(index);
	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
	  dialog.setTitle(item.getTitle());
	  dialog.setMessage(item.getSnippet());
	  dialog.show();
	  return true;
	}*/

}
