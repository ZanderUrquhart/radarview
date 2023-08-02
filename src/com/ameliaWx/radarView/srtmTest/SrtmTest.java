package com.ameliaWx.radarView.srtmTest;

import com.ameliaWx.srtmWrapper.SrtmModel;

public class SrtmTest {
	public static void main(String[] args) {
		SrtmModel srtm = new SrtmModel("/home/a-urq/Documents/RadarView/data/temp/");
		
		System.out.println("Norman, OK:    " + (3.28084 * srtm.getElevation(35.20551, -97.44571)) + " ft");
		System.out.println("Wylie, TX:     " + (3.28084 * srtm.getElevation(33.02, -96.5)) + " ft");
		
		System.out.println((srtm.getElevation(35.20551, -97.44571)) + " m");
		System.out.println((srtm.getElevation(33.02, -96.5)) + " m");
	}
}
