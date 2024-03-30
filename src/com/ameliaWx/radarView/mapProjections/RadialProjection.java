package com.ameliaWx.radarView.mapProjections;

import com.ameliaWx.utils.general.PointF;

import com.ameliaWx.radarView.mapProjections.MapProjection;

public class RadialProjection implements MapProjection {
	private int na; // number of azimuths
	private int nr; // number of ranges
	private int da; // difference between azimuth gates per 

	@Override
	public PointF projectLatLonToIJ(double longitude, double latitude) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PointF projectLatLonToIJ(PointF p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean inDomain(double longitude, double latitude) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean inDomain(PointF p) {
		// TODO Auto-generated method stub
		return false;
	}

}
