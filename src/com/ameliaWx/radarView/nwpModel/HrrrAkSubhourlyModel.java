package com.ameliaWx.radarView.nwpModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;

import com.ameliaWx.radarView.mapProjections.MapProjection;
import com.ameliaWx.radarView.mapProjections.PolarStereographicProjection;
import com.ameliaWx.utils.general.PointF;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class HrrrAkSubhourlyModel {
	private static final MapProjection proj = PolarStereographicProjection.hrrrAkProj;

	public DateTime timestamp;

	// 0 - fieldId, 1 - time, 2 - I, 3 - J
	// time key
	// 0 - 15 minutes past the hour before the marked timestamp
	// 1 - 30 minutes past the hour before the marked timestamp
	// 2 - 45 minutes past the hour before the marked timestamp
	// 3 - 60 minutes past the hour before the marked timestamp
	// example
	// file: hrrr-00z+02.grib2
	// 0 - 01:15 UTC
	// 1 - 01:30 UTC
	// 2 - 01:45 UTC
	// 3 - 02:00 UTC
	private double[][][][] modelData;

	private HashMap<NwpField, Integer> fieldIds;

	public HrrrAkSubhourlyModel(NetcdfFile ncfile) {
		List<Variable> vars = ncfile.getVariables();

		for (Variable v : vars) {
			System.out.println(v.getNameAndDimensions());
		}

		fieldIds = new HashMap<>();

		fieldIds.put(NwpField.HGT_SURF, 0);
		fieldIds.put(NwpField.PRES_SURF, 1);
		fieldIds.put(NwpField.TMP_2M, 2);
		fieldIds.put(NwpField.DPT_2M, 3);
		fieldIds.put(NwpField.WIND_U_10M, 4);
		fieldIds.put(NwpField.WIND_V_10M, 5);

		modelData = new double[fieldIds.size()][4][][];

		Variable hgtSurfVar = ncfile.findVariable("Geopotential_height_surface");
		Variable presSurfVar = ncfile.findVariable("Pressure_surface");
		Variable tmp2mVar = ncfile.findVariable("Temperature_height_above_ground");
		Variable dpt2mVar = ncfile.findVariable("Dewpoint_temperature_height_above_ground");
		Variable windU10mVar = ncfile.findVariable("u-component_of_wind_height_above_ground");
		Variable windV10mVar = ncfile.findVariable("v-component_of_wind_height_above_ground");

		double[][][] hgtSurf = readVariable3Dim(hgtSurfVar);
		double[][][] presSurf = readVariable3Dim(presSurfVar);
		double[][][][] tmp2m = readVariable4Dim(tmp2mVar);
		double[][][][] dpt2m = readVariable4Dim(dpt2mVar);
		double[][][][] windU10m = readVariable4Dim(windU10mVar);
		double[][][][] windV10m = readVariable4Dim(windV10mVar);

		for (int t = 0; t < 4; t++) {
			modelData[fieldIds.get(NwpField.HGT_SURF)][t] = hgtSurf[t];
			modelData[fieldIds.get(NwpField.PRES_SURF)][t] = presSurf[t];
			modelData[fieldIds.get(NwpField.TMP_2M)][t] = tmp2m[t][0];
			modelData[fieldIds.get(NwpField.DPT_2M)][t] = dpt2m[t][0];
			modelData[fieldIds.get(NwpField.WIND_U_10M)][t] = windU10m[t][0];
			modelData[fieldIds.get(NwpField.WIND_V_10M)][t] = windV10m[t][0];
		}
	}

	private static double[][][] readVariable3Dim(Variable rawData) {
		int[] shape = rawData.getShape();
		Array _data = null;

		try {
			_data = rawData.read();
		} catch (IOException e) {
			e.printStackTrace();
			return new double[shape[0]][shape[1]][shape[2]];
		}

		double[][][] data = new double[shape[0]][shape[1]][shape[2]];
		for (int i = 0; i < _data.getSize(); i++) {
			int x = i % shape[2];
			int y = (i / shape[2]) % shape[1];
			int t = (i / (shape[2] * shape[1])) % shape[0];

			double record = _data.getDouble(i);

			data[t][shape[1] - 1 - y][x] = record;
		}

		return data;
	}

	private static double[][][][] readVariable4Dim(Variable rawData) {
		int[] shape = rawData.getShape();
		Array _data = null;

		try {
			_data = rawData.read();
		} catch (IOException e) {
			e.printStackTrace();
			return new double[shape[0]][shape[1]][shape[2]][shape[3]];
		}

		double[][][][] data = new double[shape[0]][shape[1]][shape[2]][shape[3]];
		// see if an alternate data-reading algorithm that avoids division and modulos
		// could be faster
		for (int i = 0; i < _data.getSize(); i++) {
			int x = i % shape[3];
			int y = (i / shape[3]) % shape[2];
			int z = (i / (shape[3] * shape[2])) % shape[1];
			int t = (i / (shape[3] * shape[2] * shape[1])) % shape[0];

			double record = _data.getDouble(i);

			data[t][z][shape[2] - 1 - y][x] = record;
		}

		return data;
	}

	public double getData(int time, double latitude, double longitude, NwpField f) {
		return getData(time, latitude, longitude, f, InterpolationMode.BILINEAR, true);
	}

	public double getData(int time, double latitude, double longitude, NwpField f, InterpolationMode m,
			boolean srtmAdjusted) {
		PointF ij = proj.projectLatLonToIJ(longitude, latitude);

		if (InterpolationMode.NEAREST_NEIGHBOR == m) {
			if (proj.inDomain(ij)) {
				int i = (int) Math.round(ij.getX());
				int j = (int) Math.round(ij.getY());

				return modelData[fieldIds.get(f)][time][j][i];
			} else {
				return -1024.0;
			}
		} else if (InterpolationMode.BILINEAR == m) {
			if (proj.inDomain(ij)) {
				double i = ij.getX();
				double j = ij.getY();

				if (i == (int) i) {
					if (j == (int) j) {
						return modelData[fieldIds.get(f)][time][(int) j][(int) i];
					} else {
						double[] linearWeights = calculateLinearWeights(i);

						double dataI0 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						double dataI1 = modelData[fieldIds.get(f)][time][(int) j][(int) i + 1];

						return linearWeights[0] * dataI0 + linearWeights[1] * dataI1;
					}
				} else {
					if (j == (int) j) {
						double[] linearWeights = calculateLinearWeights(i);

						double dataJ0 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						double dataJ1 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i];

						return linearWeights[0] * dataJ0 + linearWeights[1] * dataJ1;
					} else {
						double[][] bilinearWeights = calculateBilinearWeights(i, j);

						double data00 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						double data01 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i];
						double data10 = modelData[fieldIds.get(f)][time][(int) j][(int) i + 1];
						double data11 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i + 1];

						return bilinearWeights[0][0] * data00 + bilinearWeights[0][1] * data01
								+ bilinearWeights[1][0] * data10 + bilinearWeights[1][1] * data11;
					}
				}
			} else {
				return -1024.0;
			}
		}

		return -1024.0;
	}

	public double getData(int time, int i, int j, double w00, double w01, double w10, double w11, NwpField f) {
		System.out.println(w00 + "\t" + w01 + "\t" + w10 + "\t" + w11);
		if(w00 == 1) {
			double d00 = modelData[fieldIds.get(f)][time][i][j];
			return d00;
		} else if (w00 + w01 == 1) {
			double d00 = modelData[fieldIds.get(f)][time][i][j];
			double d01 = modelData[fieldIds.get(f)][time][i][j + 1];

			return w00 * d00 + w01 * d01;
		} else if (w00 + w10 == 1) {
			double d00 = modelData[fieldIds.get(f)][time][i][j];
			double d10 = modelData[fieldIds.get(f)][time][i + 1][j];
			
			return w00 * d00 + w10 * d10;
		} else {
			double d00 = modelData[fieldIds.get(f)][time][i][j];
			double d01 = modelData[fieldIds.get(f)][time][i][j + 1];
			double d10 = modelData[fieldIds.get(f)][time][i + 1][j];
			double d11 = modelData[fieldIds.get(f)][time][i + 1][j + 1];
			
			return w00 * d00 + w01 * d01 + w10 * d10 * w11 * d11;
		}
	}

	public HashMap<NwpField, Double> getData(int time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, true);
	}

	public HashMap<NwpField, Double> getData(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		HashMap<NwpField, Double> data = new HashMap<>();
		
		PointF ij = proj.projectLatLonToIJ(longitude, latitude);
		
		int i = (int) ij.getX();
		int j = (int) ij.getY();
		
		double wI = ij.getX() - i;
		double wJ = ij.getY() - j;
		
		double w00 = (1 - wI) * (1 - wJ);
		double w01 = (1 - wI) * (wJ);
		double w10 = (wI) * (1 - wJ);
		double w11 = (wI) * (wJ);
		
		for(NwpField f : fieldIds.keySet()) {
			double record = getData(time, i, j, w00, w01, w10, w11, f);
			
			data.put(f, record);
		}
		
		return null;
	}

	// assumes a regular 1-dimensional grid with a spacing of 1
	private static double[] calculateLinearWeights(double i) {
		double weightI1 = i % 1.0;
		double weightI0 = 1 - weightI1;

		double[] linearWeights = { weightI0, weightI1 };

		return linearWeights;
	}

	// assumes a regular i-j grid with a spacing of [1, 1]
	private static double[][] calculateBilinearWeights(double i, double j) {
		double weightI1 = i % 1.0;
		double weightI0 = 1 - weightI1;
		double weightJ1 = j % 1.0;
		double weightJ0 = 1 - weightJ1;

		double[][] bilinWeights = { { weightI0 * weightJ0, weightI0 * weightJ1 },
				{ weightI1 * weightJ0, weightI1 * weightJ1 } };

		return bilinWeights;
	}
}
