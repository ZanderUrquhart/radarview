package com.ameliaWx.radarView.nwpModel.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.radarView.ColorTable;
import com.ameliaWx.radarView.RadarPanel;
import com.ameliaWx.radarView.nwpModel.HrrrAkSubhourlyModel;
import com.ameliaWx.radarView.nwpModel.HrrrSubhourlyModel;
import com.ameliaWx.radarView.nwpModel.HybridModel;
import com.ameliaWx.radarView.nwpModel.HybridModelInterp;
import com.ameliaWx.radarView.mapProjections.LambertConformalProjection;
import com.ameliaWx.radarView.nwpModel.NwpField;
import com.ameliaWx.radarView.nwpModel.PtypeAlgorithm;
import com.ameliaWx.radarView.nwpModel.RapInterpModel;
import com.ameliaWx.radarView.nwpModel.RapModel;
import com.ameliaWx.weatherUtils.WeatherUtils;
import com.ameliaWx.srtmWrapper.SrtmModel;
import com.ameliaWx.utils.general.PointF;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NwpModelTest {
	// TODO
	// render and debug the surface pressure view
	// probably need to prepare a pal file for that

	public static void main(String[] args) throws IOException {
		hybridModelTest();
	}

	public static void presSurfTest() throws IOException {
		@SuppressWarnings("deprecation")
		NetcdfFile rapFile1 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+17.grib2");
		RapModel model1 = new RapModel(rapFile1);
		DateTime time1 = new DateTime(2023, 2, 21, 17, 0, DateTimeZone.UTC);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile2 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+18.grib2");
		RapModel model2 = new RapModel(rapFile2);
		DateTime time2 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC);

		RapInterpModel model = new RapInterpModel(model1, model2, time1, time2);

		System.out.println("surface pressure: " + model.getData(time1, 35, -97, NwpField.PRES_SURF) / 100 + " mb");
	}

	public static void interpExectimeTest() throws IOException {
		@SuppressWarnings("deprecation")
		NetcdfFile rapFile1 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+17.grib2");
		RapModel model1 = new RapModel(rapFile1);
		DateTime time1 = new DateTime(2023, 2, 21, 17, 0, DateTimeZone.UTC);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile2 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+18.grib2");
		RapModel model2 = new RapModel(rapFile2);
		DateTime time2 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC);

		RapInterpModel model = new RapInterpModel(model1, model2, time1, time2);
		
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			model1.getDataForPtypes(0, 35, -97);
		}
		long endTime = System.currentTimeMillis();
		
		System.out.println("ptype data (hashmap) get time:  " + (endTime-startTime)/10000.0 + "ms/request");
		
		startTime = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			model1.getDataForPtypesAsArray(0, 35, -97);
		}
		endTime = System.currentTimeMillis();
		
		System.out.println("ptype data (array) get time:    " + (endTime-startTime)/10000.0 + "ms/request");
		
		startTime = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			model.getPrecipitationTypeHashmap(time1.plusMinutes(15), 35, -97, PtypeAlgorithm.BOURGOUIN_REVISED_EXTENDED, true, -1024.0);
		}
		endTime = System.currentTimeMillis();
		
		System.out.println("precip type (hashmap) get time: " + (endTime-startTime)/10000.0 + "ms/request");
		
		startTime = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			model.getPrecipitationType(time1.plusMinutes(15), 35, -97);
		}
		endTime = System.currentTimeMillis();
		
		System.out.println("precip type (array) get time:   " + (endTime-startTime)/10000.0 + "ms/request");
	}
	
	public static void timeInterpTest() throws IOException {
		SrtmModel srtm = new SrtmModel("/home/a-urq/Documents/RadarView/data/temp/srtmData");

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile1 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+17.grib2");
		RapModel model1 = new RapModel(rapFile1);
		DateTime time1 = new DateTime(2023, 2, 21, 17, 0, DateTimeZone.UTC);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile2 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+18.grib2");
		RapModel model2 = new RapModel(rapFile2);
		DateTime time2 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC);

		RapInterpModel model = new RapInterpModel(model1, model2, time1, time2);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFileRefl = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/ptypeTest/data/rap-20230306-15+12.grib2");

		Variable reflVar = rapFileRefl.findVariable("Composite_reflectivity_entire_atmosphere");

		float[][] reflRaw = RapModel.readVariable3Dim(reflVar)[0];

		ColorTable ptypeScale = new ColorTable(RadarPanel.loadResourceAsFile("res/aruRefl-12Ptypes.pal"), 0.1f, 10,
				"dBZ");

		PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
		PointF rapBoundingBoxSE = new PointF(0.7, -2.0);

		double[][] reflProjected = new double[70 * 50 + 1][35 * 50 + 1];
		double[][] srtmElev = new double[70 * 50 + 1][35 * 50 + 1];

		for (int i = 0; i < reflProjected.length; i++) {
			System.out.println(i);
			for (int j = 0; j < reflProjected[i].length; j++) {
//				if (dataProjected[i][j] == -1024.0) {
				double latitude = linScale(0, reflProjected[i].length, 55, 20, j);
				double longitude = linScale(0, reflProjected.length, -130, -60, i);

				if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
						&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
					srtmElev[i][j] = srtm.getElevation(latitude, longitude);

					double refl = -1024.0;

					PointF ij = LambertConformalProjection.rapProj.projectLatLonToIJ(longitude, latitude);

					if (LambertConformalProjection.rapProj.inDomain(ij)) {
						float _i = (float) ij.getX();
						float _j = (float) ij.getY();

						if (_i == (int) _i) {
							if (_j == (int) _j) {
								refl = reflRaw[(int) _j][(int) _i];
							} else {
								float[] linearWeights = RapModel.calculateLinearWeights(_i);

								double dataI0 = reflRaw[(int) _j][(int) _i];
								double dataI1 = reflRaw[(int) _j][(int) _i + 1];

								refl = linearWeights[0] * dataI0 + linearWeights[1] * dataI1;
							}
						} else {
							if (_j == (int) _j) {
								float[] linearWeights = RapModel.calculateLinearWeights(_j);

								double dataJ0 = reflRaw[(int) _j][(int) _i];
								double dataJ1 = reflRaw[(int) _j + 1][(int) _i];

								refl = linearWeights[0] * dataJ0 + linearWeights[1] * dataJ1;
							} else {
								float[][] bilinearWeights = RapModel.calculateBilinearWeights(_i, _j);

								double data00 = reflRaw[(int) _j][(int) _i];
								double data01 = reflRaw[(int) _j + 1][(int) _i];
								double data10 = reflRaw[(int) _j][(int) _i + 1];
								double data11 = reflRaw[(int) _j + 1][(int) _i + 1];

								refl = bilinearWeights[0][0] * data00 + bilinearWeights[0][1] * data01
										+ bilinearWeights[1][0] * data10 + bilinearWeights[1][1] * data11;
							}
						}
					}

					reflProjected[i][j] = refl;
				}
			}
		}

		int[][] dataProjected = new int[70 * 50 + 1][35 * 50 + 1];

		BufferedImage dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = dataProjectedImg.createGraphics();
		
		for(int t = 0; t <= 60; t++) {
			for (int i = 0; i < reflProjected.length; i++) {
				System.out.println(t + "\t" + i);
				for (int j = 0; j < reflProjected[i].length; j++) {
					double latitude = linScale(0, reflProjected[i].length, 55, 20, j);
					double longitude = linScale(0, reflProjected.length, -130, -60, i);
					
					int ptype = model.getPrecipitationType(time1.plusMinutes(t), latitude, longitude);
					
					g.setColor(ptypeScale.getColor(reflProjected[i][j], ptype));
					g.fillRect(i, j, 1, 1);
				}
			}
			
			ImageIO.write(dataProjectedImg, "PNG", new File("/home/a-urq/Documents/NwpModelTestImages/ptypeTest/images/rap-ptype-" + String.format("%02d", t) + ".png"));
		}
	}

	public static void ptypeTest() throws IOException {
		SrtmModel srtm = new SrtmModel("/home/a-urq/Documents/RadarView/data/temp/srtmData");

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile1 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/ptypeTest/data/rap-20230306-15+12.grib2");
		RapModel model1 = new RapModel(rapFile1);
		DateTime time1 = new DateTime(2023, 2, 21, 17, 0, DateTimeZone.UTC);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile2 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/ptypeTest/data/rap-20230306-15+12.grib2");
		RapModel model2 = new RapModel(rapFile2);
		DateTime time2 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC);

		RapInterpModel model = new RapInterpModel(model1, model2, time1, time2);

		Variable reflVar = rapFile1.findVariable("Composite_reflectivity_entire_atmosphere");

		float[][] reflRaw = RapModel.readVariable3Dim(reflVar)[0];

		ColorTable ptypeScale = new ColorTable(RadarPanel.loadResourceAsFile("res/aruRefl-12Ptypes.pal"), 0.1f, 10,
				"dBZ");

		int[][] dataProjected = new int[180 * 10 + 1][90 * 10 + 1];
		double[][] reflProjected = new double[180 * 10 + 1][90 * 10 + 1];

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjected[i][j] = -1024;
				reflProjected[i][j] = -1024;
			}
		}

		long wholeAlgoStartTime = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			model.getPrecipitationType(time1, 35, -97);
		}
		long wholeAlgoEndTime = System.currentTimeMillis();
		System.out.println("Whole algorithm: " + (wholeAlgoEndTime - wholeAlgoStartTime) / 10000.0 + " ms/pixel");

		long dataFetchStartTime = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			model1.getDataForPtypes(0, 35, -97);
		}
		long dataFetchEndTime = System.currentTimeMillis();
		System.out.println("Data fetch portion: " + (dataFetchEndTime - dataFetchStartTime) / 10000.0 + " ms/pixel");

//		System.exit(0);

		PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
		PointF rapBoundingBoxSE = new PointF(0.7, -2.0);

		BufferedImage dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = dataProjectedImg.createGraphics();

		for (int i = 0; i < dataProjected.length; i++) {
			System.out.println(i);
			for (int j = 0; j < dataProjected[i].length; j++) {
//				if (dataProjected[i][j] == -1024.0) {
				double latitude = linScale(0, dataProjected[i].length, 90, -90, j);
				double longitude = linScale(0, dataProjected.length, -180, 180, i);

				if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
						&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
					int ptype = model.getPrecipitationType(time1, latitude, longitude);
					dataProjected[i][j] = ptype;

					double refl = -1024.0;

					PointF ij = LambertConformalProjection.rapProj.projectLatLonToIJ(longitude, latitude);

					if (LambertConformalProjection.rapProj.inDomain(ij)) {
						float _i = (float) ij.getX();
						float _j = (float) ij.getY();

						if (_i == (int) _i) {
							if (_j == (int) _j) {
								refl = reflRaw[(int) _j][(int) _i];
							} else {
								float[] linearWeights = RapModel.calculateLinearWeights(_i);

								double dataI0 = reflRaw[(int) _j][(int) _i];
								double dataI1 = reflRaw[(int) _j][(int) _i + 1];

								refl = linearWeights[0] * dataI0 + linearWeights[1] * dataI1;
							}
						} else {
							if (_j == (int) _j) {
								float[] linearWeights = RapModel.calculateLinearWeights(_j);

								double dataJ0 = reflRaw[(int) _j][(int) _i];
								double dataJ1 = reflRaw[(int) _j + 1][(int) _i];

								refl = linearWeights[0] * dataJ0 + linearWeights[1] * dataJ1;
							} else {
								float[][] bilinearWeights = RapModel.calculateBilinearWeights(_i, _j);

								double data00 = reflRaw[(int) _j][(int) _i];
								double data01 = reflRaw[(int) _j + 1][(int) _i];
								double data10 = reflRaw[(int) _j][(int) _i + 1];
								double data11 = reflRaw[(int) _j + 1][(int) _i + 1];

								refl = bilinearWeights[0][0] * data00 + bilinearWeights[0][1] * data01
										+ bilinearWeights[1][0] * data10 + bilinearWeights[1][1] * data11;
							}
						}
					}

					reflProjected[i][j] = refl;

					g.setColor(ptypeScale.getColor(refl, dataProjected[i][j]));
					g.fillRect(i, j, 1, 1);
				}
//				}
			}
		}

		ImageIO.write(dataProjectedImg, "PNG", new File(
				"/home/a-urq/Documents/NwpModelTestImages/ptypeTest/images/rap-ptype-bourgouin-revised-extended.png"));

		dataProjected = new int[70 * 50 + 1][35 * 50 + 1];
		reflProjected = new double[70 * 50 + 1][35 * 50 + 1];
		double[][] srtmElev = new double[70 * 50 + 1][35 * 50 + 1];

		dataProjectedImg = new BufferedImage(dataProjected.length, dataProjected[0].length,
				BufferedImage.TYPE_3BYTE_BGR);
		g = dataProjectedImg.createGraphics();

		for (int i = 0; i < dataProjected.length; i++) {
			System.out.println(i);
			for (int j = 0; j < dataProjected[i].length; j++) {
//				if (dataProjected[i][j] == -1024.0) {
				double latitude = linScale(0, dataProjected[i].length, 55, 20, j);
				double longitude = linScale(0, dataProjected.length, -130, -60, i);

				if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
						&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
					int ptype = 0; // model.getPrecipitationType(time1, latitude, longitude);
					dataProjected[i][j] = ptype;
					srtmElev[i][j] = srtm.getElevation(latitude, longitude);

					double refl = -1024.0;

					PointF ij = LambertConformalProjection.rapProj.projectLatLonToIJ(longitude, latitude);

					if (LambertConformalProjection.rapProj.inDomain(ij)) {
						float _i = (float) ij.getX();
						float _j = (float) ij.getY();

						if (_i == (int) _i) {
							if (_j == (int) _j) {
								refl = reflRaw[(int) _j][(int) _i];
							} else {
								float[] linearWeights = RapModel.calculateLinearWeights(_i);

								double dataI0 = reflRaw[(int) _j][(int) _i];
								double dataI1 = reflRaw[(int) _j][(int) _i + 1];

								refl = linearWeights[0] * dataI0 + linearWeights[1] * dataI1;
							}
						} else {
							if (_j == (int) _j) {
								float[] linearWeights = RapModel.calculateLinearWeights(_j);

								double dataJ0 = reflRaw[(int) _j][(int) _i];
								double dataJ1 = reflRaw[(int) _j + 1][(int) _i];

								refl = linearWeights[0] * dataJ0 + linearWeights[1] * dataJ1;
							} else {
								float[][] bilinearWeights = RapModel.calculateBilinearWeights(_i, _j);

								double data00 = reflRaw[(int) _j][(int) _i];
								double data01 = reflRaw[(int) _j + 1][(int) _i];
								double data10 = reflRaw[(int) _j][(int) _i + 1];
								double data11 = reflRaw[(int) _j + 1][(int) _i + 1];

								refl = bilinearWeights[0][0] * data00 + bilinearWeights[0][1] * data01
										+ bilinearWeights[1][0] * data10 + bilinearWeights[1][1] * data11;
							}
						}
					}

					reflProjected[i][j] = refl;

					g.setColor(ptypeScale.getColor(reflProjected[i][j], dataProjected[i][j]));
					g.fillRect(i, j, 1, 1);
				}
//				}
			}
		}

//		ImageIO.write(dataProjectedImg, "PNG",
//				new File("/home/a-urq/Documents/NwpModelTestImages/ptypeTest/images/rap-ptype-bourgouin-revised-extended_zoomed-in.png"));

		dataProjectedImg = new BufferedImage(dataProjected.length, dataProjected[0].length,
				BufferedImage.TYPE_3BYTE_BGR);
		g = dataProjectedImg.createGraphics();

		for (int i = 0; i < dataProjected.length; i++) {
			System.out.println(i);
			for (int j = 0; j < dataProjected[i].length; j++) {
//				if (dataProjected[i][j] == -1024.0) {
				double latitude = linScale(0, dataProjected[i].length, 55, 20, j);
				double longitude = linScale(0, dataProjected.length, -130, -60, i);

				if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
						&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
					int ptype = model.getPrecipitationType(time1, latitude, longitude, srtmElev[i][j]);
					dataProjected[i][j] = ptype;

					g.setColor(ptypeScale.getColor(reflProjected[i][j], dataProjected[i][j]));
					g.fillRect(i, j, 1, 1);
				}
//				}
			}
		}

		ImageIO.write(dataProjectedImg, "PNG", new File(
				"/home/a-urq/Documents/NwpModelTestImages/ptypeTest/images/rap-ptype-bourgouin-revised-extended_zoomed-in_srtm_20230306.png"));

	}

	public static void rapInterpTest() throws IOException {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		DateTime modelTime12 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC).plusHours(13);
		DateTime modelTime13 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC).plusHours(14);

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile12 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+17.grib2");
		@SuppressWarnings("deprecation")
		NetcdfFile rapFile13 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+18.grib2");

		RapModel rapModel12 = new RapModel(rapFile12);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		RapModel rapModel13 = new RapModel(rapFile13);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		RapInterpModel model = new RapInterpModel(rapModel12, rapModel13, modelTime12, modelTime13);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		rapFile12.close();
		rapFile13.close();

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		ColorTable tmpScale = new ColorTable(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/aruTmp.pal"),
				0.1f, 10, "K");
		ColorTable dptScale = new ColorTable(new File("/home/a-urq/eclipse-workspace/ModelView/src/com/ameliaWx/modelView/prototypes/res/aruDpt2.pal"),
				0.1f, 10, "K");

		double[][] dataProjected = new double[360 * 10 + 1][180 * 10 + 1];

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjected[i][j] = -1024.0;
			}
		}

		double[][] dataProjectedDpt = new double[360 * 10 + 1][180 * 10 + 1];

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjectedDpt[i][j] = -1024.0;
			}
		}

		PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
		PointF rapBoundingBoxSE = new PointF(0.7, -2.0);

		BufferedImage dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = dataProjectedImg.createGraphics();

		BufferedImage dataProjectedImgDpt = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D gg = dataProjectedImgDpt.createGraphics();

		for (int t = 0; t <= 60; t++) {
			DateTime queryTime = modelTime12.plusMinutes(t);

			System.out.print(t + "\t");

//			System.out.println(model12.getSubhTime(queryTime));

			for (int i = 0; i < dataProjected.length; i++) {
				for (int j = 0; j < dataProjected[i].length; j++) {
//					if (dataProjected[i][j] == -1024.0) {
					double latitude = linScale(0, dataProjected[i].length, 90, -90, j);
					double longitude = linScale(0, dataProjected.length, -180, 180, i);

					if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
							&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
						dataProjected[i][j] = model.getData(queryTime, latitude, longitude, NwpField.TMP_2M);
						dataProjectedDpt[i][j] = model.getData(queryTime, latitude, longitude, NwpField.DPT_2M);

						g.setColor(tmpScale.getColor(dataProjected[i][j]));
						g.fillRect(i, j, 1, 1);

						gg.setColor(dptScale.getColor(dataProjectedDpt[i][j]));
						gg.fillRect(i, j, 1, 1);
					}
//					}
				}
			}

			g.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap_10.png")), 0, 0,
					null);

			ImageIO.write(dataProjectedImg, "PNG",
					new File("/home/a-urq/Documents/NwpModelTestImages/rapModelInterpTest/images/rap-temperature-2m_"
							+ String.format("%02d", t + 300) + ".png"));

			gg.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap_10.png")), 0,
					0, null);

			ImageIO.write(dataProjectedImgDpt, "PNG",
					new File("/home/a-urq/Documents/NwpModelTestImages/rapModelInterpTest/images/rap-dewpoint-2m_"
							+ String.format("%02d", t + 300) + ".png"));
		}
	}

	@SuppressWarnings("unused")
	public static void rapOnlyTest() throws IOException {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		@SuppressWarnings("deprecation")
		NetcdfFile rapFile = NetcdfFile.open("/home/a-urq/Documents/NwpModelTestImages/rap.t01z.awip32f00.grib2");

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		RapModel rap = new RapModel(rapFile);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		@SuppressWarnings("deprecation")
		NetcdfFile hrrrFile = NetcdfFile.open("/home/a-urq/Documents/NwpModelTestImages/hrrr.t00z.wrfsubhf01.grib2");
		HrrrSubhourlyModel hrrr = new HrrrSubhourlyModel(hrrrFile);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		@SuppressWarnings("deprecation")
		NetcdfFile hrrrAkFile = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hrrr.t00z.wrfsubhf01.ak.grib2");
		HrrrAkSubhourlyModel hrrrAk = new HrrrAkSubhourlyModel(hrrrAkFile);

		System.gc();
		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));
	}

	public static void hybridModelTest() throws IOException {
		@SuppressWarnings("deprecation")
		NetcdfFile rapFile = NetcdfFile.open("/home/a-urq/Documents/NwpModelTestImages/rap.t01z.awip32f00.grib2");
		@SuppressWarnings("deprecation")
		NetcdfFile hrrrFile = NetcdfFile.open("/home/a-urq/Documents/NwpModelTestImages/hrrr.t00z.wrfsubhf01.grib2");
		@SuppressWarnings("deprecation")
		NetcdfFile hrrrAkFile = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hrrr.t00z.wrfsubhf01.ak.grib2");

		ColorTable tmpScale = new ColorTable(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/aruTmp.pal"),
				0.1f, 10, "K");
		ColorTable dptScale = new ColorTable(new File("/home/a-urq/eclipse-workspace/ModelView/src/com/ameliaWx/modelView/prototypes/res/aruDpt2.pal"),
				0.1f, 10, "K");

		HybridModel model = new HybridModel(rapFile, hrrrFile, hrrrAkFile);

		double[][] dataProjected = new double[360 * 30 + 1][180 * 30 + 1];

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjected[i][j] = -1024.0;
			}
		}

		double[][] dataProjectedDpt = new double[360 * 30 + 1][180 * 30 + 1];

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjectedDpt[i][j] = -1024.0;
			}
		}

		PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
		PointF rapBoundingBoxSE = new PointF(0.7, -2.0);

		System.out.println("Oklahoma test:");
		double oklahomaTmp2m = model.getData(0, 35, -97, NwpField.TMP_2M);
		double oklahomaDpt2m = model.getData(0, 35, -97, NwpField.DPT_2M);
		System.out.println(oklahomaTmp2m + " K");
		System.out.println(oklahomaDpt2m + " K");

		System.out.println("\nAlaska test:");
		double alaskaTmp2m = model.getData(0, 60, -150, NwpField.TMP_2M);
		System.out.println(alaskaTmp2m + " K");

		BufferedImage dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = dataProjectedImg.createGraphics();

		BufferedImage dataProjectedImgDpt = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D gg = dataProjectedImgDpt.createGraphics();

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				if (dataProjected[i][j] == -1024.0) {
					double latitude = linScale(0, dataProjected[i].length, 90, -90, j);
					double longitude = linScale(0, dataProjected.length, -180, 180, i);

					if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
							&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
						dataProjected[i][j] = model.getData(0, latitude, longitude, NwpField.TMP_2M);

						dataProjectedDpt[i][j] = model.getData(0, latitude, longitude, NwpField.DPT_2M);

//						System.out.println("projecting data...\t" + i + "\t" + j + "\t" + dataProjected[i][j]);

						g.setColor(tmpScale.getColor(dataProjected[i][j]));
						g.fillRect(i, j, 1, 1);

						gg.setColor(dptScale.getColor(dataProjectedDpt[i][j]));
						gg.fillRect(i, j, 1, 1);
					}
				}
			}
		}

		g.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap.png")), 0, 0, null);

		ImageIO.write(dataProjectedImg, "PNG",
				new File("/home/a-urq/Documents/NwpModelTestImages/hybridModelTest/rap-temperature_1013-mb.png"));

		gg.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap.png")), 0, 0,
				null);

		ImageIO.write(dataProjectedImgDpt, "PNG",
				new File("/home/a-urq/Documents/NwpModelTestImages/hybridModelTest/rap-dewpoint_1013-mb.png"));

		for (int z = 0; z < 37; z++) {
			dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
					BufferedImage.TYPE_3BYTE_BGR);
			g = dataProjectedImg.createGraphics();

			System.out.println(NwpField.values()[8 + z * 5]);

			for (int i = 0; i < dataProjected.length; i++) {
				for (int j = 0; j < dataProjected[i].length; j++) {
					if (dataProjected[i][j] != -1024.0) {
						double latitude = linScale(0, dataProjected[i].length, 90, -90, j);
						double longitude = linScale(0, dataProjected.length, -180, 180, i);

						if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
								&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
							dataProjected[i][j] = model.getData(0, latitude, longitude, NwpField.values()[8 + z * 5]);
							double rh = model.getData(0, latitude, longitude, NwpField.values()[9 + z * 5]);

							dataProjectedDpt[i][j] = WeatherUtils.dewpoint(dataProjected[i][j], rh / 100.0);

							g.setColor(tmpScale.getColor(dataProjected[i][j]));
							g.fillRect(i, j, 1, 1);

							gg.setColor(dptScale.getColor(dataProjectedDpt[i][j]));
							gg.fillRect(i, j, 1, 1);
						}
					}
				}
			}

			g.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap.png")), 0, 0,
					null);

			ImageIO.write(dataProjectedImg, "PNG",
					new File("/home/a-urq/Documents/NwpModelTestImages/hybridModelTest/rap-temperature_"
							+ String.format("%04d", (1000 - 25 * z)) + "-mb.png"));

			gg.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap.png")), 0, 0,
					null);

			ImageIO.write(dataProjectedImgDpt, "PNG",
					new File("/home/a-urq/Documents/NwpModelTestImages/hybridModelTest/rap-dewpoint_"
							+ String.format("%04d", (1000 - 25 * z)) + "-mb.png"));

		}
	}

	@SuppressWarnings("deprecation")
	public static void hybridModelInterpTest() throws IOException {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		DateTime modelTime12 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC).plusHours(13);
		DateTime modelTime13 = new DateTime(2023, 2, 21, 18, 0, DateTimeZone.UTC).plusHours(14);

		NetcdfFile rapFile12 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+17.grib2");
		NetcdfFile hrrrFile12 = NetcdfFile.open(
				"/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/hrrr_subh-20230221-18+17.grib2");
		NetcdfFile hrrrAkFile12 = NetcdfFile.open(
				"/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/hrrr_ak_subh-20230221-18+17.grib2");

		NetcdfFile rapFile13 = NetcdfFile
				.open("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/rap-20230221-18+18.grib2");
		NetcdfFile hrrrFile13 = NetcdfFile.open(
				"/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/hrrr_subh-20230221-18+18.grib2");
		NetcdfFile hrrrAkFile13 = NetcdfFile.open(
				"/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/data/hrrr_ak_subh-20230221-18+18.grib2");

		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		HybridModel hybridModel12 = new HybridModel(rapFile12, hrrrFile12, hrrrAkFile12);
		HybridModel hybridModel13 = new HybridModel(rapFile13, hrrrFile13, hrrrAkFile13);

		HybridModelInterp model12 = new HybridModelInterp(hybridModel12, hybridModel13, modelTime12, modelTime13);

		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		rapFile12.close();
		hrrrFile12.close();
		hrrrAkFile12.close();

		rapFile13.close();
		hrrrFile13.close();
		hrrrAkFile13.close();

		System.gc();

		usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println(convToGigaMega(usedMemory));

		ColorTable tmpScale = new ColorTable(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/aruTmp.pal"),
				0.1f, 10, "K");

		double[][] dataProjected = new double[360 * 30 + 1][180 * 30 + 1];

		PointF rapBoundingBoxNW = new PointF(85.6, -180.0);
		PointF rapBoundingBoxSE = new PointF(0.7, -2.0);

		BufferedImage dataProjectedImg = new BufferedImage(dataProjected.length / 2, dataProjected[0].length / 2,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = dataProjectedImg.createGraphics();

		for (int i = 0; i < dataProjected.length; i++) {
			for (int j = 0; j < dataProjected[i].length; j++) {
				dataProjected[i][j] = -1024.0;
			}
		}

		for (int t = 0; t <= 60; t++) {
			DateTime queryTime = modelTime12.plusMinutes(t);

			System.out.print(t + "\t");

//			System.out.println(model12.getSubhTime(queryTime));

			for (int i = 0; i < dataProjected.length; i++) {
				for (int j = 0; j < dataProjected[i].length; j++) {
//					if (dataProjected[i][j] == -1024.0) {
					double latitude = linScale(0, dataProjected[i].length, 90, -90, j);
					double longitude = linScale(0, dataProjected.length, -180, 180, i);

					if (latitude >= rapBoundingBoxSE.getX() && latitude <= rapBoundingBoxNW.getX()
							&& longitude >= rapBoundingBoxNW.getY() && longitude <= rapBoundingBoxSE.getY()) {
						dataProjected[i][j] = model12.getData(queryTime, latitude, longitude, NwpField.TMP_2M);

//							System.out.println("projecting data...\t" + i + "\t" + j + "\t" + dataProjected[i][j]);

						if (i == 1000 && j == 1000) {
							System.out.println(
									"test " + queryTime + ": " + model12.getData(queryTime, 35, -97, NwpField.TMP_2M));
						}

						g.setColor(tmpScale.getColor(dataProjected[i][j]));
						g.fillRect(i, j, 1, 1);
					}
//					}
				}
			}

			g.drawImage(ImageIO.read(new File("/home/a-urq/eclipse-workspace/OneOffExperiments/worldMap.png")), 0, 0,
					null);

			ImageIO.write(dataProjectedImg, "PNG",
					new File("/home/a-urq/Documents/NwpModelTestImages/hybridModelInterpTest/images/rap-temperature-2m_"
							+ String.format("%02d", t + 300) + ".png"));
		}
	}

	private static String convToGigaMega(long bytes) {
		int b = (int) (bytes % 1024);
		bytes = bytes >> 10;
		int kiB = (int) (bytes % 1024);
		bytes = bytes >> 10;
		int miB = (int) (bytes % 1024);
		bytes = bytes >> 10;
		int giB = (int) (bytes % 1024);

		return String.format("%4d GiB %4d MiB %4d KiB %4d B", giB, miB, kiB, b);
	}

	private static double linScale(double preMin, double preMax, double postMin, double postMax, double value) {
		double slope = (postMax - postMin) / (preMax - preMin);

		return slope * (value - preMin) + postMin;
	}
}
