package com.ameliaWx.radarView.nwpModel;

import java.util.HashMap;

import org.joda.time.DateTime;

import com.ameliaWx.weatherUtils.PrecipitationType;
import com.ameliaWx.weatherUtils.PtypeAlgorithms;
import com.ameliaWx.weatherUtils.WeatherUtils;

import ucar.nc2.NetcdfFile;

public class RapInterpModel {
	RapModel model1;
	RapModel model2;
	DateTime model1time;
	DateTime model2time;

	public RapInterpModel(RapModel model1, RapModel model2, DateTime model1time, DateTime model2time) {
		this.model1 = model1;
		this.model2 = model2;
		this.model1time = model1time;
		this.model2time = model2time;
	}

	public RapInterpModel(NetcdfFile modelFile1, NetcdfFile modelFile2, DateTime model1time, DateTime model2time) {
		this.model1 = new RapModel(modelFile1);
		this.model2 = new RapModel(modelFile2);
		this.model1time = model1time;
		this.model2time = model2time;
	}
	
	private float getTimeInterpWeight(DateTime queryTime) {
		long queryMillis = (queryTime.getMillis() - model1time.getMillis());
		long intvMillis = (model2time.getMillis() - model1time.getMillis());
		
//		System.out.println(model1time);
//		System.out.println(model2time);
//		System.out.println();
//		System.out.println(queryTime);
//		System.out.println();
//		System.out.println(queryMillis);
//		System.out.println(intvMillis);
		
		if(queryMillis < 0) queryMillis = 0;
		if(queryMillis > intvMillis) queryMillis = intvMillis;
		
		return (float) (queryMillis) / intvMillis;
	}

	public double getData(DateTime time, double latitude, double longitude, NwpField f) {
		return getData(time, latitude, longitude, f, InterpolationMode.BILINEAR, false);
	}

	public double getData(DateTime time, double latitude, double longitude, NwpField f, InterpolationMode m,
			boolean srtmAdjusted) {
		double timeScaled = getSubhTime(time);

		if (timeScaled < 0) {
			return -1024.0;
		} else if (timeScaled == 0) {
			return model1.getData(0, latitude, longitude, f, m, srtmAdjusted);
		} else if (timeScaled < 1) {
			double w1 = (timeScaled) % 1;
			double w0 = 1 - w1;

			double d0 = model1.getData(0, latitude, longitude, f, m, srtmAdjusted);
			double d1 = model2.getData(0, latitude, longitude, f, m, srtmAdjusted);

			return w0 * d0 + w1 * d1;
		} else if (timeScaled == 1) {
			return model2.getData(0, latitude, longitude, f, m, srtmAdjusted);
		} else {
			return -1024.0;
		}
	}

	public int getPrecipitationType(DateTime time, double latitude, double longitude) {
		return getPrecipitationType(time, latitude, longitude, PtypeAlgorithm.BOURGOUIN_REVISED_EXTENDED, true, -1024.0);
	}

	public int getPrecipitationType(DateTime time, double latitude, double longitude, double srtmElev) {
		return getPrecipitationType(time, latitude, longitude, PtypeAlgorithm.BOURGOUIN_REVISED_EXTENDED, true, srtmElev);
	}

	private static final double SCALE_HEIGHT = 7290;
	// rewrite to use getDataForPtypesAsArray
	public int getPrecipitationType(DateTime time, double latitude, double longitude, PtypeAlgorithm algo,
			boolean dynamicInitLayer, double srtmElev) {
		return getPrecipitationType(time, latitude, longitude, algo, dynamicInitLayer, srtmElev, false);
	}
	
	public int getPrecipitationType(DateTime time, double latitude, double longitude, PtypeAlgorithm algo,
			boolean dynamicInitLayer, double srtmElev, boolean debug) {
//		HashMap<NwpField, Double> tmpProfile = getData(time, latitude, longitude);
		if (model1.getData(0, latitude, longitude, NwpField.TMP_2M) != -1024.0) {
			float timeNormalized = getTimeInterpWeight(time);
//			System.out.println(time);
//			System.out.println(timeNormalized);
//			System.exit(0);
			
			float timeWeight1 = 1 - timeNormalized;
			float timeWeight2 = timeNormalized;
			
			float[] tmpProfile1 = model1.getDataForPtypesAsArray(0, latitude, longitude);
			float[] tmpProfile2 = model2.getDataForPtypesAsArray(0, latitude, longitude);
			
			float[] pressureLevels = new float[22];
			float[] tmpIsobaric = new float[22];
			float[] dptIsobaric = new float[22];
			float[] hgtIsobaric = new float[22];

			for (int i = 0; i < 21; i++) {
				pressureLevels[i] = 50000 + 2500 * i;

				tmpIsobaric[i] = (timeWeight1 * tmpProfile1[5 + 3 * (20 - i)] + timeWeight2 * tmpProfile2[5 + 3 * (20 - i)]);

				double rhIsobaric = ((timeWeight1 * tmpProfile1[6 + 3 * (20 - i)] + timeWeight2 * tmpProfile2[6 + 3 * (20 - i)]))/100.0;
				dptIsobaric[i] = (float) WeatherUtils.dewpoint(tmpIsobaric[i], rhIsobaric);

				hgtIsobaric[i] = (timeWeight1 * tmpProfile1[7 + 3 * (20 - i)] + timeWeight2 * tmpProfile2[7 + 3 * (20 - i)]);
			}

			float presSurface = (timeWeight1 * tmpProfile1[1] + timeWeight2 * tmpProfile2[1]);
			float hgtSurface = (timeWeight1 * tmpProfile1[0] + timeWeight2 * tmpProfile2[0]);
			float tmpSurface = (timeWeight1 * tmpProfile1[2] + timeWeight2 * tmpProfile2[2])/2;
			
//			System.out.println(hgtSurface);
			
			pressureLevels[21] = (float) (presSurface * Math.exp(-2/SCALE_HEIGHT)); // corrects for it being the 2 m surface instead of the 0 m surface
			tmpIsobaric[21] = (timeWeight1 * tmpProfile1[3] + timeWeight2 * tmpProfile2[3]);
			dptIsobaric[21] = (timeWeight1 * tmpProfile1[4] + timeWeight2 * tmpProfile2[4]);
			hgtIsobaric[21] = (float) (hgtSurface + 2);
			
			// if used, adjusts for high-res DEM data
			if(srtmElev != -1024) {
				double hgtDiff = srtmElev - hgtSurface; // meters
				
				pressureLevels[21] *= Math.exp(-hgtDiff/SCALE_HEIGHT);
				tmpIsobaric[21] -= 0.0065 * hgtDiff;
				dptIsobaric[21] -= 0.002 * hgtDiff;
				
				if(dptIsobaric[21] > tmpIsobaric[21]) {
					dptIsobaric[21] = tmpIsobaric[21];
				}
				
				hgtIsobaric[21] += hgtDiff;
				
				tmpSurface -= 0.0065 * hgtDiff;
			}
			
//			System.out.println("RapInterpModel.java line 94 diag");
//			System.out.println(presSurface/100 + " mb");
//			System.out.println(hgtSurface/100 + " m");
//			System.out.println(tmpSurface/100 + " K");
//			System.out.print("pre\t");
			
			if(debug) System.out.println("ptype debug on");

			PrecipitationType ptype = PrecipitationType.RAIN;
			switch (algo) {
			case BOURGOUIN:
				break;
			case BOURGOUIN_REVISED:
				ptype = PtypeAlgorithms.bourgouinRevisedMethod(pressureLevels, tmpIsobaric, dptIsobaric, hgtIsobaric,
						presSurface, hgtSurface, dynamicInitLayer);
				break;
			case BOURGOUIN_REVISED_EXTENDED:
				ptype = PtypeAlgorithms.bourgouinRevisedExtendedMethod(pressureLevels, tmpIsobaric, dptIsobaric,
						hgtIsobaric, presSurface, hgtSurface, tmpSurface, dynamicInitLayer, debug);
				break;
			case URQUHART_EXPERIMENTAL:
				break;
			}
			
//			System.out.println("post\t");

			switch (ptype) {
			case RAIN:
				return 0;
			case FREEZING_RAIN_ELEVATED:
				return 6;
			case FREEZING_RAIN_SURFACE:
				return 1;
			case FRZR_ICEP_MIX:
				return 10;
			case ICE_PELLETS:
				return 2;
			case ICEP_SNOW_MIX:
				return 11;
			case RAIN_ICEP_MIX:
				return 9;
			case RAIN_SNOW_MIX:
				return 5;
			case FRZR_SNOW_MIX:
				return 4;
			case WET_SNOW:
				return 8;
			case DRY_SNOW:
				return 7;
			case VERY_DRY_SNOW:
				return 3;
				
			case FREEZING_RAIN:
				return 1;
			case SNOW:
				return 7;
			default:
				return 0;
			}
		} else {
			return 0;
		}
	}

	public int getPrecipitationTypeHashmap(DateTime time, double latitude, double longitude, PtypeAlgorithm algo,
			boolean dynamicInitLayer, double srtmElev) {
//		HashMap<NwpField, Double> tmpProfile = getData(time, latitude, longitude);
		HashMap<NwpField, Float> tmpProfile = model1.getDataForPtypes(0, latitude, longitude);
		if (tmpProfile.get(NwpField.TMP_2M) != -1024.0) {
			double[] pressureLevels = new double[20];
			double[] tmpIsobaric = new double[20];
			double[] dptIsobaric = new double[20];
			double[] hgtIsobaric = new double[20];

			for (int i = 0; i < 19; i++) {
				pressureLevels[i] = 50000 + 2500 * i;

				tmpIsobaric[i] = tmpProfile.get(NwpField.values()[108 - 5 * i]);

				double rhIsobaric = tmpProfile.get(NwpField.values()[109 - 5 * i])/100.0;
				dptIsobaric[i] = WeatherUtils.dewpoint(tmpIsobaric[i], rhIsobaric);

				hgtIsobaric[i] = tmpProfile.get(NwpField.values()[110 - 5 * i]);
			}

			double presSurface = tmpProfile.get(NwpField.PRES_SURF);
			double hgtSurface = tmpProfile.get(NwpField.HGT_SURF);
			double tmpSurface = tmpProfile.get(NwpField.TMP_SURF);
			
			pressureLevels[19] = presSurface * Math.exp(-2/SCALE_HEIGHT); // corrects for it being the 2 m surface instead of the 0 m surface
			tmpIsobaric[19] = tmpProfile.get(NwpField.TMP_2M);
			dptIsobaric[19] = tmpProfile.get(NwpField.DPT_2M);
			hgtIsobaric[19] = hgtSurface + 2;
			
			// if used, adjusts for high-res DEM data
			if(srtmElev != -1024) {
				double hgtDiff = srtmElev - hgtSurface; // meters
				
				pressureLevels[19] *= Math.exp(-hgtDiff/SCALE_HEIGHT);
				tmpIsobaric[19] -= 0.0065 * hgtDiff;
				dptIsobaric[19] -= 0.002 * hgtDiff;
				
				if(dptIsobaric[19] > tmpIsobaric[19]) {
					dptIsobaric[19] = tmpIsobaric[19];
				}
				
				hgtIsobaric[19] += hgtDiff;
				
				tmpSurface -= 0.0065 * hgtDiff;
			}
			
//			System.out.println("RapInterpModel.java line 94 diag");
//			System.out.println(presSurface/100 + " mb");
//			System.out.println(hgtSurface/100 + " m");
//			System.out.println(tmpSurface/100 + " K");
//			System.out.print("pre\t");

			PrecipitationType ptype = PrecipitationType.RAIN;
			switch (algo) {
			case BOURGOUIN:
				break;
			case BOURGOUIN_REVISED:
				ptype = PtypeAlgorithms.bourgouinRevisedMethod(pressureLevels, tmpIsobaric, dptIsobaric, hgtIsobaric,
						presSurface, hgtSurface, dynamicInitLayer);
				break;
			case BOURGOUIN_REVISED_EXTENDED:
				ptype = PtypeAlgorithms.bourgouinRevisedExtendedMethod(pressureLevels, tmpIsobaric, dptIsobaric,
						hgtIsobaric, presSurface, hgtSurface, tmpSurface, dynamicInitLayer);
				break;
			case URQUHART_EXPERIMENTAL:
				break;
			}
			
//			System.out.println("post\t");

			switch (ptype) {
			case RAIN:
				return 0;
			case FREEZING_RAIN_ELEVATED:
				return 6;
			case FREEZING_RAIN_SURFACE:
				return 1;
			case FRZR_ICEP_MIX:
				return 10;
			case ICE_PELLETS:
				return 2;
			case ICEP_SNOW_MIX:
				return 11;
			case RAIN_ICEP_MIX:
				return 9;
			case RAIN_SNOW_MIX:
				return 5;
			case FRZR_SNOW_MIX:
				return 4;
			case WET_SNOW:
				return 8;
			case DRY_SNOW:
				return 7;
			case VERY_DRY_SNOW:
				return 3;
				
			case FREEZING_RAIN:
				return 1;
			case SNOW:
				return 7;
			default:
				return 0;
			}
		} else {
			return 0;
		}
	}

	public HashMap<NwpField, Float> getData(DateTime time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, false);
	}

	public HashMap<NwpField, Float> getData(DateTime time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		double subhTime = getSubhTime(time);
		
		// make these be arrays to save time on hashmap gets
		HashMap<NwpField, Float> data1 = model1.getDataForPtypes(0, latitude, longitude, m, srtmAdjusted);
		HashMap<NwpField, Float> data2 = model2.getDataForPtypes(0, latitude, longitude, m, srtmAdjusted);
		
		HashMap<NwpField, Float> data = new HashMap<>();
		
		for(NwpField f : data1.keySet()) {
			data.put(f, (float) ((1 - subhTime) * data1.get(f) + (subhTime) * data2.get(f)));
		}
		
		return data;
	}

	public HashMap<NwpField, Float> getDataForSounding(DateTime time, double latitude, double longitude) {
		return getDataForSounding(time, latitude, longitude, InterpolationMode.BILINEAR, false);
	}

	public HashMap<NwpField, Float> getDataForSounding(DateTime time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		double subhTime = getSubhTime(time);
		
//		System.out.println("SUBH-TIME");
//		System.out.println(subhTime);
		
		HashMap<NwpField, Float> data1 = model1.getData(0, latitude, longitude, m, srtmAdjusted);
		HashMap<NwpField, Float> data2 = model2.getData(0, latitude, longitude, m, srtmAdjusted);
		
		HashMap<NwpField, Float> data = new HashMap<>();
		
		for(NwpField f : data1.keySet()) {
			data.put(f, (float) ((1 - subhTime) * data1.get(f) + (subhTime) * data2.get(f)));
		}
		
		return data;
	}

	public double getSubhTime(DateTime queryTime) {
		long queryMillis = (queryTime.getMillis() - model1time.getMillis());
		long intvMillis = (model2time.getMillis() - model1time.getMillis());
		
//		System.out.println(queryMillis);
//		System.out.println(intvMillis);
		
		return (double) queryMillis / intvMillis;
	}
}
