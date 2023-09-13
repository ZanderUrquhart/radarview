package com.ameliaWx.radarView.nwpModel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.joda.time.DateTime;

import com.ameliaWx.radarView.ColorScale;
import com.ameliaWx.radarView.PointD;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class RapModel implements NwpModel {
	private static final MapProjection proj = LambertConformalProjection.rapProj;

	public DateTime timestamp;

	// 0 - fieldId, 1 - time, 2 - I, 3 - J
	private float[][][][] modelData;

	private HashMap<NwpField, Integer> fieldIds;

	public static void main(String[] args) throws IOException {
		@SuppressWarnings("deprecation")
		NetcdfFile rapFile = NetcdfFile.open("/home/a-urq/Downloads/rap.t15z.awip32f49.grib2");
		ColorScale tmpScale = new ColorScale(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/aruTmp.pal"),
				0.1f, 10, "K");

		RapModel rap = new RapModel(rapFile);

		BufferedImage rapRaw = new BufferedImage(rap.modelData[2][3][0].length, rap.modelData[2][3].length,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = rapRaw.createGraphics();

		System.out.println(rap.modelData[rap.fieldIds.get(NwpField.TMP_1000_MB)][3][0][0]);
		System.out.println(rap.modelData[6][3][0][0]);

		for (int i = 0; i < rap.modelData[6][3].length; i++) {
			for (int j = 0; j < rap.modelData[6][3][0].length; j++) {
//				System.out.println(rap.modelData[2][0][i][j]);

				g.setColor(tmpScale.getColor(rap.modelData[6][3][i][j]));
				g.fillRect(j, i, 1, 1);
			}
		}

		ImageIO.write(rapRaw, "PNG", new File("rapRaw-1000mb.png"));
	}

	public RapModel(NetcdfFile ncfile) {
		List<Variable> vars = ncfile.getVariables();

		for (Variable v : vars) {
			System.out.println(v.getNameAndDimensions());
		}

		fieldIds = new HashMap<>();

		fieldIds.put(NwpField.HGT_SURF, 0);
		fieldIds.put(NwpField.PRES_SURF, 1);
		fieldIds.put(NwpField.TMP_SURF, 2);
		fieldIds.put(NwpField.TMP_2M, 3);
		fieldIds.put(NwpField.DPT_2M, 4);
		fieldIds.put(NwpField.WIND_U_10M, 5);
		fieldIds.put(NwpField.WIND_V_10M, 6);

		for (int z = 0; z < 37; z++) {
			fieldIds.put(NwpField.values()[8 + 5 * z], 7 + 5 * z);
			fieldIds.put(NwpField.values()[9 + 5 * z], 8 + 5 * z);
			fieldIds.put(NwpField.values()[10 + 5 * z], 9 + 5 * z);
			fieldIds.put(NwpField.values()[11 + 5 * z], 10 + 5 * z);
			fieldIds.put(NwpField.values()[12 + 5 * z], 11 + 5 * z);
		}
		
		int sizeAtTime = fieldIds.size();
		
		for(int z = 0; z < 37; z++) {
			fieldIds.put(NwpField.values()[193 + z], sizeAtTime + z);
		}

		modelData = new float[fieldIds.size()][1][][];

		Variable hgtSurfVar = ncfile.findVariable("Geopotential_height_surface");
		Variable presSurfVar = ncfile.findVariable("Pressure_surface");
		Variable tmpSurfVar = ncfile.findVariable("Temperature_surface");
		Variable tmp2mVar = ncfile.findVariable("Temperature_height_above_ground");
		Variable dpt2mVar = ncfile.findVariable("Dewpoint_temperature_height_above_ground");
		Variable windU10mVar = ncfile.findVariable("u-component_of_wind_height_above_ground");
		Variable windV10mVar = ncfile.findVariable("v-component_of_wind_height_above_ground");

		Variable tmpIsobaricVar = ncfile.findVariable("Temperature_isobaric");
		Variable rhIsobaricVar = ncfile.findVariable("Relative_humidity_isobaric");
		Variable hgtIsobaricVar = ncfile.findVariable("Geopotential_height_isobaric");
		Variable windUIsobaricVar = ncfile.findVariable("u-component_of_wind_isobaric");
		Variable windVIsobaricVar = ncfile.findVariable("v-component_of_wind_isobaric");
		Variable verticalVelocityVar = ncfile.findVariable("Vertical_velocity_pressure_isobaric");

		System.out.println(tmp2mVar);

		float[][][] hgtSurf = readVariable3Dim(hgtSurfVar);
		float[][][] presSurf = readVariable3Dim(presSurfVar);
		float[][][] tmpSurf = readVariable3Dim(tmpSurfVar);
		float[][][][] tmp2m = readVariable4Dim(tmp2mVar);
		float[][][][] dpt2m = readVariable4Dim(dpt2mVar);
		float[][][][] windU10m = readVariable4Dim(windU10mVar);
		float[][][][] windV10m = readVariable4Dim(windV10mVar);

		float[][][][] tmpIsobaric = readVariable4Dim(tmpIsobaricVar);
		float[][][][] rhIsobaric = readVariable4Dim(rhIsobaricVar);
		float[][][][] hgtIsobaric = readVariable4Dim(hgtIsobaricVar);
		float[][][][] windUIsobaric = readVariable4Dim(windUIsobaricVar);
		float[][][][] windVIsobaric = readVariable4Dim(windVIsobaricVar);
		float[][][][] vvelIsobaric = readVariable4Dim(verticalVelocityVar);

//		System.out.println(windUIsobaric);
//		System.out.println(verticalVelocityVar);
//		System.out.println(windUIsobaric[0].length);
//		System.out.println(vvelIsobaric[0].length);
		
//		System.out.println("VERTICAL VELOCITY");
//		for(int i = 0; i < vvelIsobaric[0].length; i++) {
//			System.out.println((100 + 25 * i) + "\t" + vvelIsobaric[0][i][200][200] + " Pa s^-1");
//		}

		for (int t = 0; t < 1; t++) {
			modelData[fieldIds.get(NwpField.HGT_SURF)][t] = hgtSurf[t];
			modelData[fieldIds.get(NwpField.PRES_SURF)][t] = presSurf[t];
			modelData[fieldIds.get(NwpField.TMP_SURF)][t] = tmpSurf[t];
			modelData[fieldIds.get(NwpField.TMP_2M)][t] = tmp2m[t][0];
			modelData[fieldIds.get(NwpField.DPT_2M)][t] = dpt2m[t][0];
			modelData[fieldIds.get(NwpField.WIND_U_10M)][t] = windU10m[t][0];
			modelData[fieldIds.get(NwpField.WIND_V_10M)][t] = windV10m[t][0];

			for (int z = 0; z < 37; z++) {
				modelData[fieldIds.get(NwpField.values()[8 + 5 * z])][t] = tmpIsobaric[t][38 - z];
				modelData[fieldIds.get(NwpField.values()[9 + 5 * z])][t] = rhIsobaric[t][38 - z];
				modelData[fieldIds.get(NwpField.values()[10 + 5 * z])][t] = hgtIsobaric[t][38 - z];
				modelData[fieldIds.get(NwpField.values()[11 + 5 * z])][t] = windUIsobaric[t][38 - z];
				modelData[fieldIds.get(NwpField.values()[12 + 5 * z])][t] = windVIsobaric[t][38 - z];
				modelData[fieldIds.get(NwpField.values()[193 + z])][t] = vvelIsobaric[t][36 - z];
			}
		}
	}

	public static float[][][] readVariable3Dim(Variable rawData) {
		int[] shape = rawData.getShape();
		Array _data = null;

		try {
			_data = rawData.read();
		} catch (IOException e) {
			e.printStackTrace();
			return new float[shape[0]][shape[1]][shape[2]];
		}

		float[][][] data = new float[shape[0]][shape[1]][shape[2]];
		for (int i = 0; i < _data.getSize(); i++) {
			int x = i % shape[2];
			int y = (i / shape[2]) % shape[1];
			int t = (i / (shape[2] * shape[1])) % shape[0];

			float record = _data.getFloat(i);

			data[t][shape[1] - 1 - y][x] = record;
		}

		return data;
	}

	private static float[][][][] readVariable4Dim(Variable rawData) {
		int[] shape = rawData.getShape();
		Array _data = null;

		try {
			_data = rawData.read();
		} catch (IOException e) {
			e.printStackTrace();
			return new float[shape[0]][shape[1]][shape[2]][shape[3]];
		}

		float[][][][] data = new float[shape[0]][shape[1]][shape[2]][shape[3]];
		// see if an alternate data-reading algorithm that avoids division and modulos
		// could be faster
		for (int i = 0; i < _data.getSize(); i++) {
			int x = i % shape[3];
			int y = (i / shape[3]) % shape[2];
			int z = (i / (shape[3] * shape[2])) % shape[1];
			int t = (i / (shape[3] * shape[2] * shape[1])) % shape[0];

			float record = _data.getFloat(i);

			data[t][z][shape[2] - 1 - y][x] = record;
		}

		return data;
	}

	public float getData(int time, double latitude, double longitude, NwpField f) {
		return getData(time, latitude, longitude, f, InterpolationMode.BILINEAR, true);
	}

	public float getData(int time, double latitude, double longitude, NwpField f, InterpolationMode m,
			boolean srtmAdjusted) {
		PointD ij = proj.projectLatLonToIJ(longitude, latitude);

		if (InterpolationMode.NEAREST_NEIGHBOR == m) {
			if (proj.inDomain(ij)) {
				int i = (int) Math.round(ij.getX());
				int j = (int) Math.round(ij.getY());

				return modelData[fieldIds.get(f)][time][j][i];
			} else {
				return -1024.0f;
			}
		} else if (InterpolationMode.BILINEAR == m) {
			if (proj.inDomain(ij)) {
				float i = (float) ij.getX();
				float j = (float) ij.getY();

				if (i == (int) i) {
					if (j == (int) j) {
						return modelData[fieldIds.get(f)][time][(int) j][(int) i];
					} else {
						float[] linearWeights = calculateLinearWeights(i);

						float dataI0 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						float dataI1 = modelData[fieldIds.get(f)][time][(int) j][(int) i + 1];

						return linearWeights[0] * dataI0 + linearWeights[1] * dataI1;
					}
				} else {
					if (j == (int) j) {
						float[] linearWeights = calculateLinearWeights(i);

						float dataJ0 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						float dataJ1 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i];

						return linearWeights[0] * dataJ0 + linearWeights[1] * dataJ1;
					} else {
						float[][] bilinearWeights = calculateBilinearWeights(i, j);

						float data00 = modelData[fieldIds.get(f)][time][(int) j][(int) i];
						float data01 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i];
						float data10 = modelData[fieldIds.get(f)][time][(int) j][(int) i + 1];
						float data11 = modelData[fieldIds.get(f)][time][(int) j + 1][(int) i + 1];

						return bilinearWeights[0][0] * data00 + bilinearWeights[0][1] * data01
								+ bilinearWeights[1][0] * data10 + bilinearWeights[1][1] * data11;
					}
				}
			} else {
				return -1024.0f;
			}
		}

		return -1024.0f;
	}

	private float getData(int time, float iD, float jD, int i, int j, float w00, float w01, float w10, float w11, NwpField f) {
//		if(f == NwpField.PRES_SURF) System.out.println(iD + "\t" + jD);
		if (proj.inDomain(new PointD(iD, jD))) {
			if (w00 == 1) {
				float d00 = modelData[fieldIds.get(f)][time][j][i];
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + d00);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00);
				
				return d00;
			} else if (w00 + w10 == 1) {
				float d00 = modelData[fieldIds.get(f)][time][j][i];
				float d10 = modelData[fieldIds.get(f)][time][j][i + 1];

				float ret = w00 * d00 + w10 * d10;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d01);
				
				return ret;
			} else if (w00 + w01 == 1) {
				float d00 = modelData[fieldIds.get(f)][time][j][i];
				float d01 = modelData[fieldIds.get(f)][time][j + 1][i];
				
				float ret = w00 * d00 + w01 * d01;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d10);

				return ret;
			} else {
				float d00 = modelData[fieldIds.get(f)][time][j][i];
				float d10 = modelData[fieldIds.get(f)][time][j][i + 1];
				float d01 = modelData[fieldIds.get(f)][time][j + 1][i];
				float d11 = modelData[fieldIds.get(f)][time][j + 1][i + 1];
				
				float ret = w00 * d00 + w01 * d01 + w10 * d10 + w11 * d11;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d01 + "\t" + d10 + "\t" + d11);

				return ret;
			}
		} else {
			return -1024.0f;
		}
	}

	private float getData(int time, double iD, double jD, int i, int j, float w00, float w01, float w10, float w11, int fId) {
//		if(f == NwpField.PRES_SURF) System.out.println(iD + "\t" + jD);
		if (proj.inDomain(new PointD(iD, jD))) {
			if (w00 == 1) {
				float d00 = modelData[fId][time][j][i];
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + d00);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00);
				
				return d00;
			} else if (w00 + w10 == 1) {
				float d00 = modelData[fId][time][j][i];
				float d10 = modelData[fId][time][j][i + 1];

				float ret = w00 * d00 + w10 * d10;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d01);
				
				return ret;
			} else if (w00 + w01 == 1) {
				float d00 = modelData[fId][time][j][i];
				float d01 = modelData[fId][time][j + 1][i];
				
				float ret = w00 * d00 + w01 * d01;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d10);

				return ret;
			} else {
				float d00 = modelData[fId][time][j][i];
				float d10 = modelData[fId][time][j][i + 1];
				float d01 = modelData[fId][time][j + 1][i];
				float d11 = modelData[fId][time][j + 1][i + 1];
				
				float ret = w00 * d00 + w01 * d01 + w10 * d10 + w11 * d11;
				
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + time + "\t" + i + "\t" + j + "\t" + (w00 + w01 + w10 + w11) + "\t" + ret);
//				if(f == NwpField.PRES_SURF) System.out.println(f + "\t" + d00 + "\t" + d01 + "\t" + d10 + "\t" + d11);

				return ret;
			}
		} else {
			return -1024.0f;
		}
	}

	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude) {
		return getData(time, latitude, longitude, InterpolationMode.BILINEAR, true);
	}

	public HashMap<NwpField, Float> getData(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		HashMap<NwpField, Float> data = new HashMap<>();

		PointD ij = proj.projectLatLonToIJ(longitude, latitude);
//		System.out.println("IJ\t" + ij);

		float iD = (float) ij.getX();
		float jD = (float) ij.getY();
		
		int i = (int) ij.getX();
		int j = (int) ij.getY();

		float wI = iD - i;
		float wJ = jD - j;

		float w00 = (1 - wI) * (1 - wJ);
		float w01 = (1 - wI) * (wJ);
		float w10 = (wI) * (1 - wJ);
		float w11 = (wI) * (wJ);

//		boolean inDomain = true;

//		float tmp2m = getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.TMP_2M);
//		System.out.println(tmp2m);
//		if (tmp2m == -1024.0)
//			inDomain = false;

//		System.out.println("putting");
		for (int fI = 0; fI < NwpField.values().length; fI++) {
			float record = -1024.0f;
			NwpField f = NwpField.values()[fI];
			if(fieldIds.containsKey(f)) {
				record = getData(0, iD, jD, i, j, w00, w01, w10, w11, f);
			}

			data.put(f, record);
//			System.out.println("\t" + record);
		}

//		System.out.println(data.size());

		return data;
	}

	public HashMap<NwpField, Float> getDataForPtypes(int time, double latitude, double longitude) {
		return getDataForPtypes(time, latitude, longitude, InterpolationMode.BILINEAR, true);
	}

	public HashMap<NwpField, Float> getDataForPtypes(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		HashMap<NwpField, Float> data = new HashMap<>();

		PointD ij = proj.projectLatLonToIJ(longitude, latitude);

		float iD = (float) ij.getX();
		float jD = (float) ij.getY();
		
		int i = (int) ij.getX();
		int j = (int) ij.getY();

		float wI = iD - i;
		float wJ = jD - j;

		float w00 = (1 - wI) * (1 - wJ);
		float w01 = (1 - wI) * (wJ);
		float w10 = (wI) * (1 - wJ);
		float w11 = (wI) * (wJ);

		boolean inDomain = true;

		float tmp2m = getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.TMP_2M);
//		System.out.println(tmp2m);
		if (tmp2m == -1024.0)
			inDomain = false;

		data.put(NwpField.PRES_SURF, getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.PRES_SURF));
		data.put(NwpField.HGT_SURF, getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.HGT_SURF));
		data.put(NwpField.TMP_SURF, getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.TMP_SURF));
		data.put(NwpField.TMP_2M, tmp2m); // uses already-computed value from earlier
		data.put(NwpField.DPT_2M, getData(0, iD, jD, i, j, w00, w01, w10, w11, NwpField.DPT_2M));
		
//		System.out.println(NwpField.PRES_SURF + "\t" + data.get(NwpField.PRES_SURF));
//		System.out.println(NwpField.HGT_SURF + "\t" + data.get(NwpField.HGT_SURF));
//		System.out.println(NwpField.TMP_SURF + "\t" + data.get(NwpField.TMP_SURF));
//		System.out.println(NwpField.TMP_2M + "\t" + data.get(NwpField.TMP_2M));
//		System.out.println(NwpField.DPT_2M + "\t" + data.get(NwpField.DPT_2M));
//		
//		System.out.println();

//		System.out.println("putting");
		for (int fI = 8; fI < 112; fI++) {
			float record = -1024.0f;
			NwpField f = NwpField.values()[fI];

			if (inDomain && fI % 5 != 1 && fI % 5 != 2) {
//				record = getData(time, latitude, longitude, f);
				record = getData(0, iD, jD, i, j, w00, w01, w10, w11, f);
				data.put(f, record);
//				System.out.println(f + "\t" + record);
			}
		}

//		System.out.println(data.size());

		return data;
	}


	public float[] getDataForPtypesAsArray(int time, double latitude, double longitude) {
		return getDataForPtypesAsArray(time, latitude, longitude, InterpolationMode.BILINEAR, true);
	}

	// rewrite to only fetch data being used
	public float[] getDataForPtypesAsArray(int time, double latitude, double longitude, InterpolationMode m,
			boolean srtmAdjusted) {
		float[] data = new float[68 + 6 * 3];
		
		PointD ij = proj.projectLatLonToIJ(longitude, latitude);
		
		if(proj.inDomain(ij)) {
			float iD = (float) ij.getX();
			float jD = (float) ij.getY();
			
			int i = (int) ij.getX();
			int j = (int) ij.getY();

			float wI = iD - i;
			float wJ = jD - j;

			float w00 = (1 - wI) * (1 - wJ);
			float w01 = (1 - wI) * (wJ);
			float w10 = (wI) * (1 - wJ);
			float w11 = (wI) * (wJ);
			
			data[0] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 0);
			data[1] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 1);
			data[2] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 2);
			data[3] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 3);
			data[4] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 4);
			
			for(int f = 0; f < 27; f++) {
				data[5 + 3 * f] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 7 + 5 * f);
				data[6 + 3 * f] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 8 + 5 * f);
				data[7 + 3 * f] = getData(0, iD, jD, i, j, w00, w01, w10, w11, 9 + 5 * f);
			}
			
			return data;
		} else {
			for(int f = 0; f < data.length; f++) {
				data[f] = -1024;
			}
			
			return data;
		}
	}

	// assumes a regular 1-dimensional grid with a spacing of [1]
	public static float[] calculateLinearWeights(float i) {
		float weightI1 = i % 1.0f;
		float weightI0 = 1 - weightI1;

		float[] linearWeights = { weightI0, weightI1 };

		return linearWeights;
	}

	// assumes a regular i-j grid with a spacing of [1, 1]
	public static float[][] calculateBilinearWeights(float i, float j) {
		float weightI1 = i % 1.0f;
		float weightI0 = 1 - weightI1;
		float weightJ1 = j % 1.0f;
		float weightJ0 = 1 - weightJ1;

		float[][] bilinWeights = { { weightI0 * weightJ0, weightI0 * weightJ1 },
				{ weightI1 * weightJ0, weightI1 * weightJ1 } };

		return bilinWeights;
	}
}
