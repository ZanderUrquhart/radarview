package com.ameliaWx.radarView.srtm;

import com.ameliaWx.radarView.RadarView;
import com.ameliaWx.srtmWrapper.SrtmModel;

// wrapper for the SRTM wrapper, weird i know
public class SrtmModel2 {
	private short[][][][] data;
	
	public static void main(String[] args) {
		SrtmModel2 srtm = new SrtmModel2();
		
		System.out.println(srtm.getElevation(35.5, -97.5));
		System.out.println(srtm.getElevation(33.01, -96.5));
	}
	
	public SrtmModel2() {
		data = new short[28][9][][];
		
		SrtmModel srtm = new SrtmModel(System.getProperty("user.home") + "/Documents/RadarView/data/temp/");
		
		for(int i = 0; i < data.length; i++) {
			for(int j = 0; j < data[0].length; j++) {
				data[i][j] = srtm.loadElevData(i + 1, j + 1);
				
				RadarView.loadWindow.setTitle("Initializing RadarView: Loading SRTM (" + String.format("%4.1f", 100*(9 * i + j)/(28.0 * 9.0)) + "%)...");
			}
		}
	}
	
	public double getElevation(double latitude, double longitude) {
		int x = (int) Math.round((longitude + 180) * 400);
		int y = (int) Math.round((60 - latitude) * 400);
		
		if(x < 0) return -1024.0;
		if(x >= 28 * 5 * 400) return -1024.0;
		if(y < 0) return -1024.0;
		if(y >= 9 * 5 * 400) return -1024.0;

		// nearest neighbor interp, work on bilinear
		return getElevation(x, y);
	}
	
	private double getElevation(int x, int y) {
		int i = x / 2000;
		int j = y / 2000;
		int k = x % 2000;
		int l = y % 2000;
		
		return data[i][j][k][l];
	}
}
