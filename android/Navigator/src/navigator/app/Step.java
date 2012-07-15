package navigator.app;

import com.google.android.maps.GeoPoint;

public class Step {
	
	
	private int distance;
	private GeoPoint start;
	private GeoPoint end;
	private String instructions;

	public Step() {
		this.distance = 0;
		this.instructions = "";
		this.start = new GeoPoint(0,0);
		this.end = new GeoPoint(0,0);
	}
	
	public Step(int d, String i, float lat1, float long1, float lat2, float long2) {
		this.distance = d;
		this.instructions = i;
		this.start = new GeoPoint((int)(lat1*1E6), (int)(long1*1E6));
		this.end = new GeoPoint((int)(lat2*1E6), (int)(long2*1E6));
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
		String s = instructions.toString() + " - " + distance + "m";
		return s;
	}
	
}
