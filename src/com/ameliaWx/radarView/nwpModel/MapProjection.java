package com.ameliaWx.radarView.nwpModel;

import com.ameliaWx.radarView.PointD;

public interface MapProjection {
	public PointD projectLatLonToIJ(double longitude, double latitude);
	public PointD projectLatLonToIJ(PointD p);
	public boolean inDomain(double longitude, double latitude);
	public boolean inDomain(PointD p);

	default double sin(double theta) {
		return (Math.sin(Math.toRadians(theta)));
	}

	default double cos(double theta) {
		return (Math.cos(Math.toRadians(theta)));
	}

	default double tan(double theta) {
		return (Math.tan(Math.toRadians(theta)));
	}

	default double sec(double theta) {
		return 1 / cos(theta);
	}

	default double csc(double theta) {
		return 1 / sin(theta);
	}

	default double cot(double theta) {
		return 1 / tan(theta);
	}
}
