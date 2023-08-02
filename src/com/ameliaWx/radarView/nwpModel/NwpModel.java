package com.ameliaWx.radarView.nwpModel;

import java.util.HashMap;

import com.ameliaWx.srtmWrapper.SrtmModel;

public interface NwpModel {
	SrtmModel srtm = new SrtmModel("/home/a-urq/Documents/RadarView/data");
	
	public float getData(int time, double latitude, double longitude, NwpField f);
	public float getData(int time, double latitude, double longitude, NwpField f, InterpolationMode m, boolean srtmAdjusted);
	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude);
	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude, InterpolationMode m, boolean srtmAdjusted);
}
