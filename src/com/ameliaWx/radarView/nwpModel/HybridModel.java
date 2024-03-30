package com.ameliaWx.radarView.nwpModel;

import java.util.HashMap;

import com.ameliaWx.radarView.mapProjections.LambertConformalProjection;
import com.ameliaWx.radarView.mapProjections.PolarStereographicProjection;
import com.ameliaWx.utils.general.PointF;

import ucar.nc2.NetcdfFile;

public class HybridModel implements NwpModel {
	private RapModel rap;
	private HrrrSubhourlyModel hrrr;
	private HrrrAkSubhourlyModel hrrrAk;

	private static final PointF hrrrBoundingBoxNW = new PointF(53.0, -135.0);
	private static final PointF hrrrBoundingBoxSE = new PointF(21.0, -60.0);
	private static final PointF hrrrAkBoundingBoxNW = new PointF(80.0, -180.0);
	private static final PointF hrrrAkBoundingBoxSE = new PointF(40.0, -110.0);

	private static final PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
	private static final PointF rapBoundingBoxSE = new PointF(0.7, -2.0);
	
	public HybridModel(NetcdfFile rapFile, NetcdfFile hrrrFile, NetcdfFile hrrrAkFile) {
		rap = new RapModel(rapFile);
		hrrr = new HrrrSubhourlyModel(hrrrFile);
		hrrrAk = new HrrrAkSubhourlyModel(hrrrAkFile);
	}

	@Override
	public float getData(int time, double latitude, double longitude, NwpField f) {
		return (float) getData(time, latitude, longitude, f, InterpolationMode.BILINEAR, false);
	}

	@Override
	public float getData(int time, double latitude, double longitude, NwpField f, InterpolationMode m,
			boolean srtmAdjusted) {
		if (longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()
				&& latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()) {
//			System.out.println("in RAP bounding box");
			
			if (f == NwpField.HGT_SURF || f == NwpField.PRES_SURF || f == NwpField.TMP_2M || f == NwpField.DPT_2M
					|| f == NwpField.WIND_U_10M || f == NwpField.WIND_V_10M) {
				if (longitude >= hrrrBoundingBoxNW.getY() && longitude <= hrrrBoundingBoxSE.getY()
						&& latitude >= hrrrBoundingBoxSE.getX() && latitude <= hrrrBoundingBoxNW.getX()) {
//					System.out.println("in HRRR bounding box");
					
					PointF hrrrIJ = LambertConformalProjection.hrrrProj.projectLatLonToIJ(longitude, latitude);

					if (LambertConformalProjection.hrrrProj.inDomain(hrrrIJ)) {
//						System.out.println("in HRRR domain");

//						int i = (int) hrrrIJ.getY();
//						int j = (int) hrrrIJ.getX();
//						double wI = hrrrIJ.getY() - i;
//						double wJ = hrrrIJ.getX() - j;
//						
//						double w00 = (1 - wI) * (1 - wJ);
//						double w01 = (1 - wI) * (wJ);
//						double w10 = (wI) * (1 - wJ);
//						double w11 = (wI) * (wJ);
//						
//						return hrrr.getData(time, i, j, w00, w01, w10, w11, f);
						
						return hrrr.getData(time, latitude, longitude, f, m, srtmAdjusted);
					}
				}

				if (longitude >= hrrrAkBoundingBoxNW.getY() && longitude <= hrrrAkBoundingBoxSE.getY()
						&& latitude >= hrrrAkBoundingBoxSE.getX() && latitude <= hrrrAkBoundingBoxNW.getX()) {
//					System.out.println("in HRRR-AK bounding box");
					
					PointF hrrrIJ = PolarStereographicProjection.hrrrAkProj.projectLatLonToIJ(longitude, latitude);

//					System.out.println(hrrrIJ);
//					System.out.println(PolarStereographicProjection.hrrrAkProj.inDomain(hrrrIJ));
					
					if (PolarStereographicProjection.hrrrAkProj.inDomain(hrrrIJ)) {
//						System.out.println("in HRRR-AK domain");
						
//						int i = (int) hrrrIJ.getY();
//						int j = (int) hrrrIJ.getX();
//						double wI = hrrrIJ.getY() - i;
//						double wJ = hrrrIJ.getX() - j;
//						
//						double w00 = (1 - wI) * (1 - wJ);
//						double w01 = (1 - wI) * (wJ);
//						double w10 = (wI) * (1 - wJ);
//						double w11 = (wI) * (wJ);
//						
//						return hrrrAk.getData(time, i, j, w00, w01, w10, w11, f);
						
						return (float) hrrrAk.getData(time, latitude, longitude, f, m, srtmAdjusted);
					}
				}

				return rap.getData(time, latitude, longitude, f, m, srtmAdjusted);
			} else {
				return rap.getData(time, latitude, longitude, f, m, srtmAdjusted);
			}
		} else {
			return -1024.0f;
		}
	}

	@Override
	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, false);
	}

	@Override
	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		HashMap<NwpField, Float> data = rap.getDataForPtypes(time, latitude, longitude, m, srtmAdjusted);
		
		if (longitude >= hrrrBoundingBoxNW.getY() && longitude <= hrrrBoundingBoxSE.getY()
				&& latitude >= hrrrBoundingBoxSE.getX() && latitude <= hrrrBoundingBoxNW.getX()) {
			PointF hrrrIJ = LambertConformalProjection.hrrrProj.projectLatLonToIJ(longitude, latitude);

			if (LambertConformalProjection.hrrrProj.inDomain(hrrrIJ)) {
				int i = (int) hrrrIJ.getX();
				int j = (int) hrrrIJ.getX();
				double wI = hrrrIJ.getX() - i;
				double wJ = hrrrIJ.getY() - j;
				
				double w00 = (1 - wI) * (1 - wJ);
				double w01 = (1 - wI) * (wJ);
				double w10 = (wI) * (1 - wJ);
				double w11 = (wI) * (wJ);
				
				double presSurf = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.PRES_SURF);
				double hgtSurf = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.HGT_SURF);
				double tmp2m = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.TMP_2M);
				double dpt2m = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.DPT_2M);
				double windU10m = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.WIND_U_10M);
				double windV10m = hrrr.getData(time, i, j, w00, w01, w10, w11, NwpField.WIND_V_10M);
				
				data.put(NwpField.PRES_SURF, (float) presSurf);
				data.put(NwpField.HGT_SURF, (float) hgtSurf);
				data.put(NwpField.TMP_2M, (float) tmp2m);
				data.put(NwpField.DPT_2M, (float) dpt2m);
				data.put(NwpField.WIND_U_10M, (float) windU10m);
				data.put(NwpField.WIND_V_10M, (float) windV10m);
			}
		}

		if (longitude >= hrrrAkBoundingBoxNW.getY() && longitude <= hrrrAkBoundingBoxSE.getY()
				&& latitude >= hrrrAkBoundingBoxSE.getX() && latitude <= hrrrAkBoundingBoxNW.getX()) {
			PointF hrrrAkIJ = PolarStereographicProjection.hrrrAkProj.projectLatLonToIJ(longitude, latitude);
			
			if (PolarStereographicProjection.hrrrAkProj.inDomain(hrrrAkIJ)) {
				int i = (int) hrrrAkIJ.getX();
				int j = (int) hrrrAkIJ.getX();
				double wI = hrrrAkIJ.getX() - i;
				double wJ = hrrrAkIJ.getY() - j;
				
				double w00 = (1 - wI) * (1 - wJ);
				double w01 = (1 - wI) * (wJ);
				double w10 = (wI) * (1 - wJ);
				double w11 = (wI) * (wJ);
				
				double presSurf = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.PRES_SURF);
				double hgtSurf = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.HGT_SURF);
				double tmp2m = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.TMP_2M);
				double dpt2m = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.DPT_2M);
				double windU10m = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.WIND_U_10M);
				double windV10m = hrrrAk.getData(time, i, j, w00, w01, w10, w11, NwpField.WIND_V_10M);
				
				data.put(NwpField.PRES_SURF, (float) presSurf);
				data.put(NwpField.HGT_SURF, (float) hgtSurf);
				data.put(NwpField.TMP_2M, (float) tmp2m);
				data.put(NwpField.DPT_2M, (float) dpt2m);
				data.put(NwpField.WIND_U_10M, (float) windU10m);
				data.put(NwpField.WIND_V_10M, (float) windV10m);
			}
		}
		
		return data;
	}
}
