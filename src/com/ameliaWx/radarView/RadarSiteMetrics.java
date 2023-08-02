package com.ameliaWx.radarView;

public class RadarSiteMetrics {
	public double coneOfSilenceSize; // km
	public double rangeGateResolution; // km
	public int numAzimuthalGates;
	
	public static final RadarSiteMetrics NEXRAD_METRICS = new RadarSiteMetrics(1.875, 0.25, 720);
	
	public RadarSiteMetrics(double coneOfSilenceSize, double rangeGateResolution, int numAzimuthalGates) {
		this.coneOfSilenceSize = coneOfSilenceSize;
		this.rangeGateResolution = rangeGateResolution;
		this.numAzimuthalGates = numAzimuthalGates;
	}
}
