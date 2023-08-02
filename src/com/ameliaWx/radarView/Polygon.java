package com.ameliaWx.radarView;

import java.util.ArrayList;

public class Polygon {
	public ArrayList<PointD> points;
	public double ulLon = -1024.0;
	public double ulLat = -1024.0;
	public double lrLon = -1024.0;
	public double lrLat = -1024.0;
	
	public Polygon(ArrayList<PointD> points) {
		this.points = points;
		
		for(int i = 0; i < points.size(); i++) {
			double lon = points.get(i).getY();
			double lat = points.get(i).getX();
			
			if(lon < ulLon) ulLon = lon;
			if(lat > ulLat) ulLat = lat;
			if(lon > lrLon) lrLon = lon;
			if(lat < lrLat) lrLat = lat;
		}
	}
}
