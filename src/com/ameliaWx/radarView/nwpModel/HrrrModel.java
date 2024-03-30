package com.ameliaWx.radarView.nwpModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;

import com.ameliaWx.radarView.mapProjections.LambertConformalProjection;
import com.ameliaWx.radarView.mapProjections.MapProjection;
import com.ameliaWx.utils.general.PointF;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class HrrrModel {
	// have only a TMP_SURF and a TMP_SURF_2M_DIFF field
	

	private static final MapProjection proj = LambertConformalProjection.hrrrProj;

	public DateTime timestamp;

	// 0 - fieldId, 1 - time, 2 - I, 3 - J
	private double[][][][] modelData;

	private HashMap<NwpField, Integer> fieldIds;

	public HrrrModel(NetcdfFile ncfile) {
		List<Variable> vars = ncfile.getVariables();

		for (Variable v : vars) {
			System.out.println(v.getNameAndDimensions());
		}

		fieldIds = new HashMap<>();

		fieldIds.put(NwpField.TMP_SURF, 0);
		fieldIds.put(NwpField.TMP_SURF_2M_DIFF, 1);

		modelData = new double[fieldIds.size()][4][][];

		Variable tmpSurfVar = ncfile.findVariable("Temperature_surface");
		Variable tmp2mVar = ncfile.findVariable("Temperature_height_above_ground");

		double[][][] tmpSurf = readVariable3Dim(tmpSurfVar);
		double[][][][] tmp2m = readVariable4Dim(tmp2mVar);
		
		double[][][] tmpSurf2mDiff = new double[tmpSurf.length][tmpSurf[0].length][tmpSurf[0][0].length];
		
		for(int t = 0; t < tmpSurf2mDiff.length; t++) {
			for(int j = 0; j < tmpSurf2mDiff[t].length; j++) {
				for(int i = 0; i < tmpSurf2mDiff[t][j].length; i++) {
					tmpSurf2mDiff[t][j][i] = tmpSurf[t][j][i] - tmp2m[t][0][j][i];
				}
			}
		}

		for (int t = 0; t < 1; t++) {
			modelData[fieldIds.get(NwpField.TMP_SURF)][t] = tmpSurf[t];
			modelData[fieldIds.get(NwpField.TMP_SURF_2M_DIFF)][t] = tmpSurf2mDiff[t];
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

	public HashMap<NwpField, Double> getData(int time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, true);
	}

	public HashMap<NwpField, Double> getData(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
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
