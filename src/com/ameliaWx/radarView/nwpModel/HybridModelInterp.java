package com.ameliaWx.radarView.nwpModel;

import java.util.HashMap;

import org.joda.time.DateTime;

public class HybridModelInterp {
	HybridModel model1;
	HybridModel model2;
	DateTime model1time;
	DateTime model2time;
	
	public HybridModelInterp(HybridModel model1, HybridModel model2, DateTime model1time, DateTime model2time) {
		this.model1 = model1;
		this.model2 = model2;
		this.model1time = model1time;
	}

	public double getData(DateTime time, double latitude, double longitude, NwpField f) {
		return getData(time, latitude, longitude, f, InterpolationMode.BILINEAR, false);
	}

	public double getData(DateTime time, double latitude, double longitude, NwpField f, InterpolationMode m,
			boolean srtmAdjusted) {
		double timeScaled = getSubhTime(time);
		
		if(timeScaled < -1) {
			return -1024.0;
		} else if(timeScaled == -1) {
			return model1.getData(3, latitude, longitude, f, m, srtmAdjusted);
		} else if(timeScaled < 0) {
			double w1 = (timeScaled + 10) % 1;
			double w0 = 1 - w1;
			
			double d0 = model1.getData(3, latitude, longitude, f, m, srtmAdjusted);
			double d1 = model2.getData(0, latitude, longitude, f, m, srtmAdjusted);
			
			return w0 * d0 + w1 * d1;
		} else if(timeScaled == 0) {
			return model2.getData(0, latitude, longitude, f, m, srtmAdjusted);
		} else if(timeScaled < 1) {
			double w1 = (timeScaled + 10) % 1;
			double w0 = 1 - w1;
			
			double d0 = model2.getData(0, latitude, longitude, f, m, srtmAdjusted);
			double d1 = model2.getData(1, latitude, longitude, f, m, srtmAdjusted);
			
			return w0 * d0 + w1 * d1;
		} else if(timeScaled == 1) {
			return model2.getData(1, latitude, longitude, f, m, srtmAdjusted);
		} else if(timeScaled < 2) {
			double w1 = (timeScaled + 10) % 1;
			double w0 = 1 - w1;
			
			double d0 = model2.getData(1, latitude, longitude, f, m, srtmAdjusted);
			double d1 = model2.getData(2, latitude, longitude, f, m, srtmAdjusted);
			
			return w0 * d0 + w1 * d1;
		} else if(timeScaled == 2) {
			return model2.getData(2, latitude, longitude, f, m, srtmAdjusted);
		} else if(timeScaled < 3) {
			double w1 = (timeScaled + 10) % 1;
			double w0 = 1 - w1;
			
			double d0 = model2.getData(2, latitude, longitude, f, m, srtmAdjusted);
			double d1 = model2.getData(3, latitude, longitude, f, m, srtmAdjusted);
			
			return w0 * d0 + w1 * d1;
		} else if(timeScaled == 3) {
			return model2.getData(3, latitude, longitude, f, m, srtmAdjusted);
		} else {
			return -1024.0;
		}
	}

	public HashMap<NwpField, Double> getData(DateTime time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, false);
	}

	public HashMap<NwpField, Double> getData(DateTime time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public double getSubhTime(DateTime time) {
		double minutes = time.getMinuteOfHour() + time.getSecondOfMinute()/60;
		
		return minutes/15.0 - 1.0;
	}
}
