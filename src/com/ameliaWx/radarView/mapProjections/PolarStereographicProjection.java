package com.ameliaWx.radarView.mapProjections;

import com.ameliaWx.utils.general.PointF;

public class PolarStereographicProjection implements MapProjection {
	private double projectionOriginLongitude;
	private double projectionOriginScaleFactor;
	private double dx;
	private double dy;
	private double offsetX;
	private double offsetY;
	private int nx;
	private int ny;
	
//	public static final PolarStereographicProjection HRRR_AK_PROJ = new PolarStereographicProjection(90, -135.0, 0.9330127, 3, 3, -1143, 451, 1299, 919);
	public static final PolarStereographicProjection hrrrAkProj = new PolarStereographicProjection(90, -135.0, 0.9330127, 3.01, 3.01, -1142, 447, 1299, 919);
	
	public PolarStereographicProjection(double projectionOriginLatitude, double projectionOriginLongitude,
			double projectionOriginScaleFactor, double dx, double dy, double offsetX, double offsetY, int nx, int ny) {
		super();
		this.projectionOriginLongitude = projectionOriginLongitude;
		this.projectionOriginScaleFactor = projectionOriginScaleFactor;
		this.dx = dx;
		this.dy = dy;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.nx = nx;
		this.ny = ny;
	}

	public PointF projectLatLonToIJ(double longitude, double latitude) {
		double zenithAngle = latitude + 90;
		double azimuth = longitude + projectionOriginLongitude;
		
		double rPlane = sin(zenithAngle)/(1 - cos(zenithAngle));
		double thPlane = (360 + 180 - azimuth) % 360.0;
		
		double xPlane = rPlane * cos(thPlane);
		double yPlane = rPlane * sin(thPlane);
		
		double x = xPlane * 111.32 * 100;
		double y = yPlane * 111.32 * 100;
		
		x /= dx;
		y /= dy;
		
		x /= projectionOriginScaleFactor;
		y /= projectionOriginScaleFactor;
		
		//System.out.println(new PointD(x, y));
		return new PointF(x - offsetX, y - offsetY);
	}

	public PointF projectLatLonToIJ(PointF p) {
		return projectLatLonToIJ(p.getY(), p.getX());
	}

	@Override
	public boolean inDomain(double i, double j) {
//		System.out.println(i + "\t" + (nx - 1) + "\t" + (i <= nx -1));
		if(i >= 0 && i <= nx - 1 && j >= 0 && j <= ny - 1) return true;
		
		return false;
	}

	@Override
	public boolean inDomain(PointF p) {
		return inDomain(p.getX(), p.getY());
	}
}
