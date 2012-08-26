package navigator.app;

import java.util.LinkedList;
import java.util.List;

import com.google.android.maps.GeoPoint;

public class Step {
	
	
	private int distance;
	private GeoPoint start;
	private GeoPoint end;
	private String instructions;
	private LinkedList<GeoPoint> checkPoints;
	private Arrow currentArrow;


	public static enum Arrow {NONE, LEFT, RIGHT}; // Left, Right *Demo Only*

	public Step() {
		this.distance = 0;
		this.instructions = "";
		this.start = new GeoPoint(0,0);
		this.end = new GeoPoint(0,0);
		this.currentArrow = Arrow.NONE;
		this.checkPoints = new LinkedList<GeoPoint>();
	}
	
	public Step(int d, String i, float lat1, float long1, float lat2, float long2, String polyline) {
		this.distance = d;
		this.instructions = i;
		this.start = new GeoPoint((int)(lat1*1E6), (int)(long1*1E6));
		this.end = new GeoPoint((int)(lat2*1E6), (int)(long2*1E6));
		
		if (instructions.toLowerCase().contains("left")) {
			this.currentArrow = Arrow.LEFT;
		} else if (instructions.toLowerCase().contains("right")) {
			this.currentArrow = Arrow.RIGHT;
		} else {
			this.currentArrow = Arrow.NONE;
		}
		
		this.checkPoints = (LinkedList<GeoPoint>)decodePoly(polyline);
	}
	
	
	public Arrow getCurrentArrow() {
		return currentArrow;
	}
	
	public float getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public GeoPoint getStart() {
		return start;
	}

	public void setStart(GeoPoint start) {
		this.start = start;
	}

	public GeoPoint getEnd() {
		return end;
	}

	public void setEnd(GeoPoint end) {
		this.end = end;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String toString() {
		String s = this.instructions.toString() + " - " + this.distance + "m";
		return s;
	}
	
	public String toString(float d) {
		String s = instructions.toString() + " - " + Math.round(d) + "m";
		return s;
	}
	
	private List<GeoPoint> decodePoly(String encoded) {

	    List<GeoPoint> poly = new LinkedList<GeoPoint>();
	    int index = 0, len = encoded.length();
	    int lat = 0, lng = 0;

	    while (index < len) {
	        int b, shift = 0, result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lat += dlat;

	        shift = 0;
	        result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lng += dlng;

	        GeoPoint p = new GeoPoint((int) (((double) lat / 1E5) * 1E6),
	             (int) (((double) lng / 1E5) * 1E6));
	        poly.add(p);
	    }

	    return poly;
	}
	
}
