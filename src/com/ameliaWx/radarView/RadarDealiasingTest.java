package com.ameliaWx.radarView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.utils.general.PointF;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class RadarDealiasingTest {
	private static ArrayList<ArrayList<PointF>> countyBorders;
	private static ArrayList<ArrayList<PointF>> stateBorders;
	private static ArrayList<ArrayList<PointF>> interstates;
	private static ArrayList<ArrayList<PointF>> majorRoads;

	static {
		File countyBordersKML = loadResourceAsFile("res/usCounties.kml");
		File stateBordersKML = loadResourceAsFile("res/usStates.kml");
		File interstatesKML = loadResourceAsFile("res/wms.kml");
		File majorRoadsKML = loadResourceAsFile("res/roadtrl020.kml");

		countyBorders = getPolygons(countyBordersKML);
		stateBorders = getPolygons(stateBordersKML);
		interstates = getPolygons(interstatesKML);
		majorRoads = getPolygons(majorRoadsKML);
	}

	private static double ulLat = 35.44214118030642;
	private static double ulLon = -97.65723241834044;
	private static double lrLat = 35.11361589388624;
	private static double lrLon = -97.12246832719198;
	private static double ppd = 3000; 

	private static double ulLat2 = 35.2934514352156;
	private static double ulLon2 = -97.52868379709412;
	private static double lrLat2 = 35.16141124741314;
	private static double lrLon2 = -97.29744085739682;
	private static double ppd2 = 12000;
	
	private static final Font CAPTION_FONT = new Font(Font.MONOSPACED, Font.BOLD, 96);
	
	private static final String filename = "20230227_034856";
	private static final String timestamp = "2023-02-26 09:48:56 PM CST";
	private static final String imgId = "09";

	public static void main(String[] args) throws IOException {
		ColorTable reflectivityColors = new ColorTable(RadarPanel.loadResourceAsFile("res/aruReflLowFilter.pal"), 0.1f, 10, "dBZ");
		ColorTable velocityColors = new ColorTable(RadarPanel.loadResourceAsFile("res/aruVlcy.pal"), 0.1f, 20, "mph");
		ColorTable spectrumWidthColors = new ColorTable(RadarPanel.loadResourceAsFile("res/aruSpwd.pal"), 0.1f, 20, "mph");
		@SuppressWarnings("deprecation")
		NetcdfFile ncfile = NetcdfFile.open(new File("/home/a-urq/Documents/normanTornado-20230226/data/KTLX" + filename + "_V06").getAbsolutePath());
//		NetcdfFile ncfile = NetcdfFile.open(new File("KFDR20230216_021442.nexrad").getAbsolutePath());
		@SuppressWarnings("unused")
		DateTime scanTime = new DateTime(2019, 10, 21, 5, 22, 58, DateTimeZone.UTC);

		Variable baseRefl = ncfile.findVariable("Reflectivity_HI");
		Variable baseReflAzi = ncfile.findVariable("azimuthR_HI");
		
		Variable baseVlcy = ncfile.findVariable("RadialVelocity_HI");
		Variable baseVlcyAzi = ncfile.findVariable("azimuthV_HI");

		Variable specWdth = ncfile.findVariable("SpectrumWidth_HI");

		double[][][] reflectivity = readNexradData(baseRefl, baseReflAzi, reflPostProc, -1024, -32.5, 1);
		double[][][] radialVelocity = readNexradData(baseVlcy, baseVlcyAzi, vlcyPostProc, -64.5, -64.0, 1);
		double[][][] spectrumWidth = readNexradData(specWdth, baseVlcyAzi, spwdPostProc, -64.5, -64.0, 1);

		BufferedImage basemap = drawBasemap(ulLon, ulLat, lrLon, lrLat, ppd);
		BufferedImage basemap2 = drawBasemap(ulLon2, ulLat2, lrLon2, lrLat2, ppd2);

		BufferedImage imageLo = drawPolarProjImage(reflectivity[0], 2001, 0.5, reflectivityColors);
		BufferedImage imageMd = drawPolarProjImage(reflectivity[0], 2001, 1, reflectivityColors);
		BufferedImage imageHi = drawPolarProjImage(reflectivity[0], 2001, 2, reflectivityColors);
		BufferedImage imageVh = drawPolarProjImage(reflectivity[0], 2001, 4, reflectivityColors);

		// KFWS
//		double radarLat = 32.57322872964769;
//		double radarLon = -97.30326251795913;

		// KTLX
		double radarLat = 35.33366193688193;
		double radarLon = -97.27788106021703;
		
		// KFDR
//		double radarLat = 34.36120860020982;
//		double radarLon = -98.99214455069693;
		
		// couch tower
		double locLat = 35.2003474544821;
		double locLon = -97.44490928135772;
		
		// national weather center
		double loc2Lat = 35.181510174679715;
		double loc2Lon = -97.43998969948849;
		
		// home
		double loc3Lat = 35.21649120130956;
		double loc3Lon = -97.4172543193571;

		double lonStretchFactor = Math.cos(Math.toRadians(radarLat));

		double radialLat0 = 2000.0 * 0.25 / 111.32; // distance between radar site and edge of polar image in
													// degrees
													// of latitude
		double radialLon0 = radialLat0 / lonStretchFactor; // distance between radar site and edge
															// of polar image in degrees of
															// longitude

		double radialLat1 = 1000.0 * 0.25 / 111.32;
		double radialLon1 = radialLat1 / lonStretchFactor;

		double radialLat2 = 500.0 * 0.25 / 111.32;
		double radialLon2 = radialLat2 / lonStretchFactor;

		double radialLat3 = 250.0 * 0.25 / 111.32;
		double radialLon3 = radialLat3 / lonStretchFactor;

		double northLat0 = radarLat + radialLat0;
		double southLat0 = radarLat - radialLat0;
		double eastLon0 = radarLon + radialLon0;
		double westLon0 = radarLon - radialLon0;

		double northLat1 = radarLat + radialLat1;
		double southLat1 = radarLat - radialLat1;
		double eastLon1 = radarLon + radialLon1;
		double westLon1 = radarLon - radialLon1;

		double northLat2 = radarLat + radialLat2;
		double southLat2 = radarLat - radialLat2;
		double eastLon2 = radarLon + radialLon2;
		double westLon2 = radarLon - radialLon2;

		double northLat3 = radarLat + radialLat3;
		double southLat3 = radarLat - radialLat3;
		double eastLon3 = radarLon + radialLon3;
		double westLon3 = radarLon - radialLon3;
		
		int lX = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), locLon);
		int lY = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), locLat);
		int lX_2 = (int) linScale(ulLon2, lrLon2, 0, basemap.getWidth(), locLon);
		int lY_2 = (int) linScale(ulLat2, lrLat2, 0, basemap.getHeight(), locLat);
		
		int l2X = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), loc2Lon);
		int l2Y = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), loc2Lat);
		int l2X_2 = (int) linScale(ulLon2, lrLon2, 0, basemap.getWidth(), loc2Lon);
		int l2Y_2 = (int) linScale(ulLat2, lrLat2, 0, basemap.getHeight(), loc2Lat);
		
		int l3X = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), loc3Lon);
		int l3Y = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), loc3Lat);
		int l3X_2 = (int) linScale(ulLon2, lrLon2, 0, basemap.getWidth(), loc3Lon);
		int l3Y_2 = (int) linScale(ulLat2, lrLat2, 0, basemap.getHeight(), loc3Lat);

		int ulX0 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), westLon0);
		int lrX0 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), eastLon0);
		int ulY0 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), northLat0);
		int lrY0 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), southLat0);

		int ulX1 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), westLon1);
		int lrX1 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), eastLon1);
		int ulY1 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), northLat1);
		int lrY1 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), southLat1);

		int ulX2 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), westLon2);
		int lrX2 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), eastLon2);
		int ulY2 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), northLat2);
		int lrY2 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), southLat2);

		int ulX3 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), westLon3);
		int lrX3 = (int) linScale(ulLon, lrLon, 0, basemap.getWidth(), eastLon3);
		int ulY3 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), northLat3);
		int lrY3 = (int) linScale(ulLat, lrLat, 0, basemap.getHeight(), southLat3);

		int ulX0_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), westLon0);
		int lrX0_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), eastLon0);
		int ulY0_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), northLat0);
		int lrY0_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), southLat0);

		int ulX1_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), westLon1);
		int lrX1_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), eastLon1);
		int ulY1_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), northLat1);
		int lrY1_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), southLat1);

		int ulX2_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), westLon2);
		int lrX2_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), eastLon2);
		int ulY2_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), northLat2);
		int lrY2_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), southLat2);

		int ulX3_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), westLon3);
		int lrX3_2 = (int) linScale(ulLon2, lrLon2, 0, basemap2.getWidth(), eastLon3);
		int ulY3_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), northLat3);
		int lrY3_2 = (int) linScale(ulLat2, lrLat2, 0, basemap2.getHeight(), southLat3);

		BufferedImage preDealias = new BufferedImage(basemap.getWidth(), basemap.getHeight() + 96,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = preDealias.createGraphics();

		g.drawImage(imageLo, ulX0, ulY0, lrX0, lrY0, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1, ulY1, lrX1, lrY1, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2, ulY2, lrX2, lrY2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3, ulY3, lrX3, lrY3, 0, 0, 2001, 2001, null);
		g.drawImage(basemap, 0, 0, null);

		drawLoc(g, lX, lY);
		drawLoc2(g, l2X, l2Y);

		g.setColor(Color.BLACK);
		g.fillRect(0, preDealias.getHeight() - 96, 10000, 100);
		g.setColor(Color.WHITE);
		g.setFont(CAPTION_FONT);
		g.drawString(timestamp, 0, preDealias.getHeight() - 1);
		
		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/no-home/reflectivity-" + imgId + ".png"));
		
		drawLoc3(g, l3X, l3Y);
		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/home/reflectivity-" + imgId + ".png"));

		imageLo = drawPolarProjImage(spectrumWidth[0], 2001, 0.5, spectrumWidthColors);
		imageMd = drawPolarProjImage(spectrumWidth[0], 2001, 1, spectrumWidthColors);
		imageHi = drawPolarProjImage(spectrumWidth[0], 2001, 2, spectrumWidthColors);
		imageVh = drawPolarProjImage(spectrumWidth[0], 2001, 4, spectrumWidthColors);
		
		g.drawImage(imageLo, ulX0, ulY0, lrX0, lrY0, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1, ulY1, lrX1, lrY1, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2, ulY2, lrX2, lrY2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3, ulY3, lrX3, lrY3, 0, 0, 2001, 2001, null);
		g.drawImage(basemap, 0, 0, null);

		drawLoc(g, lX, lY);
		drawLoc2(g, l2X, l2Y);

		g.setColor(Color.BLACK);
		g.fillRect(0, preDealias.getHeight() - 96, 10000, 100);
		g.setColor(Color.WHITE);
		g.setFont(CAPTION_FONT);
		g.drawString(timestamp, 0, preDealias.getHeight() - 1);

		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/no-home/spectrum-width-" + imgId + ".png"));
		
		drawLoc3(g, l3X, l3Y);
		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/home/spectrum-width-" + imgId + ".png"));

		preDealias = new BufferedImage(basemap.getWidth(), basemap.getHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		g = preDealias.createGraphics();

		imageLo = drawPolarProjImage(radialVelocity[0], 2001, 0.5, velocityColors);
		imageMd = drawPolarProjImage(radialVelocity[0], 2001, 1, velocityColors);
		imageHi = drawPolarProjImage(radialVelocity[0], 2001, 2, velocityColors);
		imageVh = drawPolarProjImage(radialVelocity[0], 2001, 4, velocityColors);

		g.drawImage(imageLo, ulX0, ulY0, lrX0, lrY0, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1, ulY1, lrX1, lrY1, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2, ulY2, lrX2, lrY2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3, ulY3, lrX3, lrY3, 0, 0, 2001, 2001, null);
		g.drawImage(basemap, 0, 0, null);

		drawLoc(g, lX, lY);
		drawLoc2(g, l2X, l2Y);
		drawLoc3(g, l3X, l3Y);

		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/z1-velocity-test.png"));

		BufferedImage preDealias2 = new BufferedImage(basemap.getWidth(), basemap.getHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		g = preDealias2.createGraphics();

		g.drawImage(imageLo, ulX0_2, ulY0_2, lrX0_2, lrY0_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1_2, ulY1_2, lrX1_2, lrY1_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2_2, ulY2_2, lrX2_2, lrY2_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3_2, ulY3_2, lrX3_2, lrY3_2, 0, 0, 2001, 2001, null);
		g.drawImage(basemap2, 0, 0, null);

		drawLoc(g, lX_2, lY_2);
		drawLoc(g, l2X_2, l2Y_2);
		drawLoc(g, l3X_2, l3Y_2);

		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/z2-velocity-test.png"));

		long startDealiasing = System.currentTimeMillis();
		double[][][] dealiasedVelocity = dealiasVelocity(radialVelocity);
		long endDealiasing = System.currentTimeMillis();

		imageLo = drawPolarProjImage(dealiasedVelocity[0], 2001, 0.5, velocityColors);
		imageMd = drawPolarProjImage(dealiasedVelocity[0], 2001, 1, velocityColors);
		imageHi = drawPolarProjImage(dealiasedVelocity[0], 2001, 2, velocityColors);
		imageVh = drawPolarProjImage(dealiasedVelocity[0], 2001, 4, velocityColors);

		BufferedImage postDealias = new BufferedImage(basemap.getWidth(), basemap.getHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		g = postDealias.createGraphics();

		g.drawImage(imageLo, ulX0, ulY0, lrX0, lrY0, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1, ulY1, lrX1, lrY1, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2, ulY2, lrX2, lrY2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3, ulY3, lrX3, lrY3, 0, 0, 2001, 2001, null);
		g.drawImage(basemap, 0, 0, null);

		drawLoc(g, lX, lY);
		drawLoc2(g, l2X, l2Y);
		drawLoc3(g, l3X, l3Y);

		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/z1-velocity-dealiased-test.png"));

		BufferedImage postDealias2 = new BufferedImage(basemap.getWidth(), basemap.getHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		g = postDealias2.createGraphics();

		g.drawImage(imageLo, ulX0_2, ulY0_2, lrX0_2, lrY0_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageMd, ulX1_2, ulY1_2, lrX1_2, lrY1_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageHi, ulX2_2, ulY2_2, lrX2_2, lrY2_2, 0, 0, 2001, 2001, null);
		g.drawImage(imageVh, ulX3_2, ulY3_2, lrX3_2, lrY3_2, 0, 0, 2001, 2001, null);
		g.drawImage(basemap2, 0, 0, null);

		drawLoc(g, lX_2, lY_2);
		drawLoc(g, l2X_2, l2Y_2);
		drawLoc(g, l3X_2, l3Y_2);

		ImageIO.write(preDealias, "PNG", new File("/home/a-urq/Documents/normanTornado-20230226/z2-velocity-dealiased-test.png"));

//		BufferedImage postDealiasRaw = new BufferedImage(dealiasedVelocity[0].length, dealiasedVelocity[0][0].length,
//				BufferedImage.TYPE_4BYTE_ABGR);
//		g = postDealiasRaw.createGraphics();
//
//		for (int i = 0; i < dealiasedVelocity[0].length; i++) {
//			for (int j = 0; j < dealiasedVelocity[0][i].length; j++) {
//				g.setColor(velocityColors.getColor(dealiasedVelocity[0][i][j]));
//				g.fillRect(i, j, 1, 1);
//			}
//		}
//
//		ImageIO.write(postDealiasRaw, "PNG", new File("velocity-after-dealiasing-raw-KFDR.png"));
		
		System.out.println("Dealiasing time: " + (endDealiasing - startDealiasing) + " ms");
	}

	private static int[][] azimuths;
	
	private static void drawLoc(Graphics2D g2d, int homeX, int homeY) {
		BasicStroke cyn = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		BasicStroke blk = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		g2d.setColor(new Color(0, 0, 0));
		g2d.setStroke(blk);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);

		g2d.setColor(new Color(128, 255, 255));
		g2d.setStroke(cyn);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);
	}
	
	private static void drawLoc2(Graphics2D g2d, int homeX, int homeY) {
		BasicStroke cyn = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		BasicStroke blk = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		g2d.setColor(new Color(0, 0, 0));
		g2d.setStroke(blk);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);

		g2d.setColor(new Color(255, 128, 255));
		g2d.setStroke(cyn);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);
	}
	
	private static void drawLoc3(Graphics2D g2d, int homeX, int homeY) {
		BasicStroke cyn = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		BasicStroke blk = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		g2d.setColor(new Color(0, 0, 0));
		g2d.setStroke(blk);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);

		g2d.setColor(new Color(255, 255, 128));
		g2d.setStroke(cyn);

		g2d.drawOval(homeX - 10, homeY - 10, 20, 20);
		g2d.drawLine(homeX, homeY - 10, homeX, homeY - 5);
		g2d.drawLine(homeX, homeY + 5, homeX, homeY + 10);
		g2d.drawLine(homeX - 10, homeY, homeX - 5, homeY);
		g2d.drawLine(homeX + 5, homeY, homeX + 10, homeY);
	}

	private static BufferedImage drawPolarProjImage(double[][] data, int size, double res, ColorTable colors) {
		if (azimuths == null || azimuths.length != size)
			computeAzimuths(size);

		BufferedImage polarProj = new BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = polarProj.createGraphics();

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int dis = (int) (Math.hypot(i - size / 2, j - size / 2) / res - 7.5);
				int az = azimuths[i][j];

				if (dis > 0 && dis < data[0].length) {
					g.setColor(colors.getColor(data[az][dis]));
					g.fillRect(i, j, 1, 1);
				} else {
					g.setColor(colors.getColor(-1024));
					g.fillRect(i, j, 1, 1);
				}
			}
		}

		g.dispose();
		return polarProj;
	}

	private static void computeAzimuths(int size) {
		azimuths = new int[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				azimuths[i][j] = (int) (2 * (540 - Math.toDegrees(Math.atan2(i - size / 2, j - size / 2)))) % 720;
			}
		}
	}

	private static double[][][] readNexradData(Variable rawData, Variable azimuths, PostProc proc, double ndValue,
			double rfValue, int maxAmtTilts) throws IOException {
		int[] shape = rawData.getShape();
		Array _data = null;
		Array _azi = null;

		System.out.println("maxamttilt check: " + shape[0] + "\t" + maxAmtTilts);

		if (maxAmtTilts != -1)
			shape[0] = Integer.min(shape[0], maxAmtTilts);

		_data = rawData.read();
		_azi = azimuths.read();

		@SuppressWarnings("deprecation")
		boolean isDiffRefl = ("DifferentialReflectivity_HI".equals(rawData.getName()));

		if (isDiffRefl) {
			System.out.println(Arrays.toString(shape));
		}

		double[][] azi = new double[shape[0]][shape[1]];
		for (int h = 0; h < _azi.getSize(); h++) {
			int i = h / (shape[1]);
			int j = h % shape[1];

			if (i >= shape[0])
				break;

			azi[i][j] = _azi.getDouble(h);
		}

		double[][][] data = new double[shape[0]][shape[1]][shape[2]];
		for (int h = 0; h < _data.getSize(); h++) {
			int i = h / (shape[1] * shape[2]);
			int j = h / (shape[2]) % shape[1];
			int k = h % shape[2];

			if (i >= shape[0])
				break;

			if (isDiffRefl) {
				if (k % 2 == 1) {
					k /= 2;
				} else {
					k /= 2;
					k += shape[2] / 2;
				}
			}

			double record = proc.process(_data.getFloat(h));

//			int[] coords = { h, i, j, k };

			if (isDiffRefl) {
//				if(i == 0 && h % shape[2] < 6) {
//					System.out.printf("%25s", Arrays.toString(coords) + " " + record);
//				}
//				
//				if(i == 0 && k == 6) {
//					System.out.println();
//				}
			}

			if (record == ndValue) {
				data[i][(int) Math.floor(2.0 * azi[i][j])][k] = -1024;
			} else if (record == rfValue) {
				data[i][(int) Math.floor(2.0 * azi[i][j])][k] = -2048;
			} else {
				data[i][(int) Math.floor(2.0 * azi[i][j])][k] = record;
			}
		}

//		System.out.println(rawData);
//		System.out.println(rawData.getName());
//		System.out.println(Arrays.toString(shape));
//		System.out.println();

		return data;
	}

	// algorithm is of my own invention
	// some points inspired by UNRAVEL (https://doi.org/10.1175/JTECH-D-19-0020.1)
	// but hopefully faster
	private static double[][][] dealiasVelocity(double[][][] data) {
		double[][][] dealiasedVelocity = new double[data.length][data[0].length][data[0][0].length];

		for (int i = 0; i < data.length; i++) {
			dealiasedVelocity[i] = dealiasVelocityAtTilt(data[i]);
		}

		return dealiasedVelocity;
	}

	// zero-isodop method similar to storm relative motion calc
	// call multiple times if possible to get rid of higher order aliasing such as
	// when the nyquist velocity is extremely low (looking at you KMPX)
	private static double[][] dealiasVelocityAtTilt(double[][] data) {
		double[][] dealiasedVelocity = new double[data.length][data[0].length];

//		double[][] smoothedData = smoothField(data, 10);
//
//		double leftZeroIsodopTrackerU = 0.0;
//		double leftZeroIsodopTrackerV = 0.0;
//
//		for (int a = 0; a < smoothedData.length; a++) {
//			double azimuth = a / 2.0;
//
//			for (int r = 0; r < smoothedData[a].length; r++) {
//				int jCCW = a - 1;
//				if (jCCW == -1)
//					jCCW = 719;
//
//				double radialVelJ = smoothedData[a][r];
//				double radialVelJCCW = smoothedData[jCCW][r];
//
//				if (radialVelJ == -1024) {
//
//				} else if (radialVelJ == -2048) {
//
//				} else {
//					if (radialVelJCCW == -1024) {
//
//					} else if (radialVelJCCW == -2048) {
//
//					} else {
//						double diffRadVelWrtAzimuth = radialVelJ - radialVelJCCW;
//
//						if (Math.abs(diffRadVelWrtAzimuth) < 5) {
//							if (radialVelJ > 0 && radialVelJCCW < 0) {
//								leftZeroIsodopTrackerU += Math.sin(Math.toRadians(azimuth));
//								leftZeroIsodopTrackerV += Math.cos(Math.toRadians(azimuth));
//							} else if (radialVelJ < 0 && radialVelJCCW > 0) {
////								leftZeroIsodopTrackerU -= Math.sin(Math.toRadians(azimuth));
////								leftZeroIsodopTrackerV -= Math.cos(Math.toRadians(azimuth));
//							}
//						}
//					}
//				}
//			}
//		}
//
//		double rightZeroIsodopTrackerU = 0.0;
//		double rightZeroIsodopTrackerV = 0.0;
//
//		for (int a = 0; a < data.length; a++) {
//			double azimuth = a / 2.0;
//
//			for (int r = 0; r < data[a].length; r++) {
//				int jCCW = a - 1;
//				if (jCCW == -1)
//					jCCW = 719;
//
//				double radialVelJ = data[a][r];
//				double radialVelJCCW = data[jCCW][r];
//
//				if (radialVelJ == -1024) {
//
//				} else if (radialVelJ == -2048) {
//
//				} else {
//					if (radialVelJCCW == -1024) {
//
//					} else if (radialVelJCCW == -2048) {
//
//					} else {
//						double diffRadVelWrtAzimuth = radialVelJ - radialVelJCCW;
//
//						if (Math.abs(diffRadVelWrtAzimuth) < 5) {
//							if (radialVelJ > 0 && radialVelJCCW < 0) {
//								
//							} else if (radialVelJ < 0 && radialVelJCCW > 0) {
//								rightZeroIsodopTrackerU += Math.sin(Math.toRadians(azimuth));
//								rightZeroIsodopTrackerV += Math.cos(Math.toRadians(azimuth));
//							}
//						}
//					}
//				}
//			}
//		}
//
//		double leftZeroIsodopTrackerMag = Math.hypot(leftZeroIsodopTrackerU, leftZeroIsodopTrackerV);
//
//		leftZeroIsodopTrackerU /= leftZeroIsodopTrackerMag;
//		leftZeroIsodopTrackerV /= leftZeroIsodopTrackerMag;
//
//		double rightZeroIsodopTrackerMag = Math.hypot(rightZeroIsodopTrackerU, rightZeroIsodopTrackerV);
//
//		rightZeroIsodopTrackerU /= rightZeroIsodopTrackerMag;
//		rightZeroIsodopTrackerV /= rightZeroIsodopTrackerMag;
//		
//		System.out.printf("[%4.2f, %4.2f]\n", leftZeroIsodopTrackerU, leftZeroIsodopTrackerV);
//		System.out.printf("[%4.2f, %4.2f]\n", rightZeroIsodopTrackerU, rightZeroIsodopTrackerV);

		double nyquistVelocity = -1.0;

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				double record = data[a][r];

				dealiasedVelocity[a][r] = record;

				if (Math.abs(record) > nyquistVelocity && record != -1024 && record != -2048) {
					nyquistVelocity = Math.abs(record);
				}
			}
		}

		nyquistVelocity += 0.5;

		@SuppressWarnings("unused")
		double[][] smoothedData = smoothField(data, 5);

		// maybe make a max too
		final double NEEDS_DEALIASING_FRACTION = 0.8;
		double[] needsDealiasingMinRange = new double[data.length];
		for (int a = 0; a < data.length; a++) {
			needsDealiasingMinRange[a] = 2000;
		}

		double[] velSum = new double[data.length];
		int[] velCount = new int[data.length];
		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				double record = data[a][r];

				if (record != -1024 && record != -2048) {
					velSum[a] += record;
					velCount[a]++;

					if (r >= 3) {
						double record3 = data[a][r - 3];
						double record2 = data[a][r - 2];
						double record1 = data[a][r - 1];
						double record0 = data[a][r];

						if (record0 != -1024 && record0 != -2048 && record1 != -1024 && record1 != -2048
								&& record2 != -1024 && record2 != -2048) {
							double addend0 = (Math.abs(record0) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;
							double addend1 = (Math.abs(record1) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;
							double addend2 = (Math.abs(record2) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;

							double sum = addend0 + addend1 + addend2;

							if (sum >= 0.5 && r < needsDealiasingMinRange[a]) {
								if (a == 0) {
									System.out.println(r);
									System.out.println(NEEDS_DEALIASING_FRACTION * nyquistVelocity);
									System.out.println(record0);
									System.out.println(record1);
									System.out.println(record2);
									System.out.println(record3);
								}

								needsDealiasingMinRange[a] = r - 2;
							}
						}
					}
				}
			}
		}

		// set storm motion direction as 90 deg clockwise from left zero-isodop
//		double stormMotionDirectionU = leftZeroIsodopTrackerV;
//		double stormMotionDirectionV = -leftZeroIsodopTrackerU;

		for (int a = 0; a < velSum.length; a++) {
			velSum[a] /= velCount[a];
		}

		double[] smoothedSums = new double[velSum.length];

		final int KERNEL_SIZE = 180;
		for (int a = KERNEL_SIZE / 2; a < 720 + KERNEL_SIZE / 2; a++) {
			double sum = 0.0;

			for (int da = -KERNEL_SIZE / 2; da <= KERNEL_SIZE / 2; da++) {
				int _a = (a + da + 720) % 720;

				sum += velSum[_a];
			}

			smoothedSums[a % 720] = sum / 180.0;
		}

		velSum = smoothedSums;

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				if (data[a][r] != -1024 && data[a][r] != -2048) {
					if (data[a][r] > 0 && velSum[a] < 0 && r >= needsDealiasingMinRange[a]) {
						dealiasedVelocity[a][r] = data[a][r] - 2 * nyquistVelocity;
					} else if (data[a][r] < 0 && velSum[a] > 0 && r >= needsDealiasingMinRange[a]) {
						dealiasedVelocity[a][r] = data[a][r] + 2 * nyquistVelocity;
					} else {
						dealiasedVelocity[a][r] = data[a][r];
					}
				}
			}
		}

		final int BOX_SIZE = 5;
		// box filter to remove straggler pixels
		for (int a = 0; a < data.length; a++) {
			for (int r = BOX_SIZE + 300; r < data[a].length - BOX_SIZE; r++) {
				int _a = (a + 720) % 720;

				double record = dealiasedVelocity[_a][r];

				if (record != -1024 && record != -2048) {
					double sum = 0;
					int count = 0;

					for (int da = -BOX_SIZE; da <= BOX_SIZE; da++) {
						for (int dr = -BOX_SIZE; dr <= BOX_SIZE; dr++) {
							int _da = (a + da + 720) % 720;

							double record0 = dealiasedVelocity[_da][r + dr];

							if (record0 != -1024 && record0 != -2048) {
								sum += record0;
								count++;
							}
							
							if(a == 0 && r == 310) {
								System.out.println(record0 + "\t" + sum + "\t" + count);
							}
						}
					}
					
					if(a == 0 && r == 310) {
						System.out.println(sum);
					}

					if (count != 0) {
						double boxAvg = sum / count;
						
						if(a == 0 && r == 310) {
							System.out.println();
							System.out.println(sum);
							System.out.println(boxAvg);
							System.out.println(record);
//							System.out.println(boxAvgRecord);
						}

						if (boxAvg - record > nyquistVelocity) {
							dealiasedVelocity[_a][r] += 2 * nyquistVelocity;
						} else if (boxAvg - record < -nyquistVelocity) {
							dealiasedVelocity[_a][r] -= 2 * nyquistVelocity;
						}
					}
				}
			}
		}

		return dealiasedVelocity;
	}

	private static final double CONE_OF_SILENCE_SIZE = 7.5; // km * 0.25
	// kernel sizeL km * 0.25

	private static double[][] smoothField(double[][] data, int kernelSize) {
		return smoothField(new double[][][] { data }, kernelSize)[0];
	}

	private static double[][][] smoothField(double[][][] data, int kernelSize) {
		// radially smoothed step
		double[][][] intm = new double[data.length][data[0].length][data[0][0].length];
		// final step
		double[][][] ret = new double[data.length][data[0].length][data[0][0].length];

		double[] rWeights = getGaussianWeights(kernelSize, (kernelSize - 1) / 4.0);
		for (int i = 0; i < ret.length; i++) {
			// radial smoothing
			for (int j = 0; j < ret[i].length; j++) {
				for (int k = 0; k < ret[i][j].length; k++) {
					if (data[i][j][k] == -1024.0) {
						intm[i][j][k] = -1024.0;
					} else if (data[i][j][k] == -2048.0) {
						intm[i][j][k] = -2048.0;
					} else {
						double vSum = 0.0;
						double wSum = 0.0;

						for (int l = -(rWeights.length - 1) / 2; l <= (rWeights.length - 1) / 2; l++) {

							if (k + l >= 0 && k + l < ret[i][j].length) {
								double value = data[i][j][k + l];
								double weight = rWeights[l + (rWeights.length - 1) / 2];

								if (value != -1024.0 && value != -2048.0) {
									vSum += value * weight;
									wSum += weight;
								}
							}
						}

						intm[i][j][k] = vSum / wSum;
					}
				}
			}

			// azimuthal smoothing
			for (int k = 0; k < ret[i][0].length; k++) {
				double azimuthalSize = ((180 * 0.25 * (kernelSize - 1) / 2)
						/ (0.25 * (k + CONE_OF_SILENCE_SIZE * Math.PI))) / 2.0;

				if (azimuthalSize < 1) {
					for (int j = 0; j < ret[i].length; j++) {
						ret[i][j][k] = intm[i][j][k];
					}

					continue;
				}

				if (azimuthalSize > 90)
					azimuthalSize = 90;

				double[] aWeights = getGaussianWeights((int) 90, azimuthalSize / 2.0);
				for (int j = 0; j < ret[i].length; j++) {
					if (intm[i][j][k] == -1024.0) {
						ret[i][j][k] = -1024.0;
					} else if (intm[i][j][k] == -2048.0) {
						ret[i][j][k] = -2048.0;
					} else {
						double vSum = 0.0;
						double wSum = 0.0;

						for (int l = -(aWeights.length - 1) / 2; l <= (aWeights.length - 1) / 2; l++) {
							int jl = (j + l + 720) % 720;

							double value = intm[i][jl][k];
							double weight = aWeights[l + (aWeights.length - 1) / 2];

							if (value != -1024.0 && value != -2048.0) {
								vSum += value * weight;
								wSum += weight;
							}
						}

						ret[i][j][k] = vSum / wSum;
					}
				}
			}
		}

		return ret;
	}

	@SuppressWarnings("unused")
	private static double[][] dealiasVelocityAtTiltOld(double[][] data) {
		double[][] dealiasedVelocity = new double[data.length][data[0].length];

		double nyquistVelocity = -1.0;

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				double record = data[a][r];

				dealiasedVelocity[a][r] = record;

				if (Math.abs(record) > nyquistVelocity && record != -1024 && record != -2048) {
					nyquistVelocity = Math.abs(record);
				}
			}
		}

		double mult = 1.85;

		// find reference radial (look for other reference radial on other side with 60
		// deg of opposite
		double[] avgVelocitySquaredInAzimuth = new double[data.length];
		double[] counts = new double[data.length];
		double avgNumValidGates = 0.0;

		boolean[] approachesNyquist = new boolean[data.length];
		final double beta = 0.7;

		for (int a = 0; a < avgVelocitySquaredInAzimuth.length; a++) {
			approachesNyquist[a] = false;

			int count = 0;
			double vSum = 0.0;
			double wSum = 0.0;

			for (int r = 0; r < data[a].length; r++) {
				if (data[a][r] != -1024 && data[a][r] != -2048) {
					double weight = 1; // (r + 7.5) / 100.0;

					if (data[a][r] >= beta * nyquistVelocity) {
						approachesNyquist[a] = true;
					}

					vSum += Math.pow(data[a][r], 1) * weight;
					wSum += weight;

					count++;
				}
			}

			avgVelocitySquaredInAzimuth[a] = vSum / wSum;
			counts[a] = count;
			avgNumValidGates = (1.0 - 1.0 / (a + 1.0)) * avgNumValidGates + 1.0 / (a + 1.0) * counts[a];
		}

		final int kernelSize = 30;
		double[] tempAvgV2 = new double[data.length];
		double[] smoothingWeights = getGaussianWeights(kernelSize, 2.5);
		for (int a = 0; a < avgVelocitySquaredInAzimuth.length; a++) {
			double vSum = 0;
			double wSum = 0;

			for (int da = -kernelSize; da <= kernelSize; da++) {
				double weight = 1;

				vSum = weight * avgVelocitySquaredInAzimuth[(a + da + 720) % 720];
				wSum = weight;
			}

			tempAvgV2[a] = vSum / wSum;
		}

		avgVelocitySquaredInAzimuth = tempAvgV2;

		double avgDirU = 0.0;
		double avgDirV = 0.0;

		for (int a = 0; a < 720; a++) {
			double angleWrtNorth = a / 2.0;

			double basisVectorU = Math.sin(Math.toRadians(angleWrtNorth));
			double basisVectorV = Math.cos(Math.toRadians(angleWrtNorth));

			double weight = 1.0 / (a + 1.0);

			avgDirU = (1 - weight) * avgDirU + weight * (basisVectorU * avgVelocitySquaredInAzimuth[a]);
			avgDirV = (1 - weight) * avgDirV + weight * (basisVectorV * avgVelocitySquaredInAzimuth[a]);
		}

		double avgDirWrtNorth = (Math.toDegrees(Math.atan2(avgDirU, avgDirV)) + 360.0) + 360.0;

		System.out.println(avgDirWrtNorth);
		System.out.println((int) (2 * ((avgDirWrtNorth) % 360.0)));
		System.out.println((int) (2 * ((avgDirWrtNorth + 90.0) % 360.0)));
		System.out.println((int) (2 * ((avgDirWrtNorth + 270.0) % 360.0)));

		int referenceRadial1Pre = (int) (2 * ((avgDirWrtNorth + 90.0) % 360.0));
		int referenceRadial2Pre = (int) (2 * ((avgDirWrtNorth + 270.0) % 360.0));

		int referenceRadial1 = -1;
		int referenceRadial2 = -1;

		for (int a = 0; a < avgVelocitySquaredInAzimuth.length; a++) {
			if (!approachesNyquist[a] && counts[a] >= avgNumValidGates * 2.0 / 3.0) {
				double minValue = 1024.0;

				for (int da = -kernelSize; da <= kernelSize; da++) {
					minValue = Double.min(minValue, avgVelocitySquaredInAzimuth[(a + da + 720) % 720]);
				}

				if (avgVelocitySquaredInAzimuth[a] == minValue) {
					for (int b = 240; b < 480; b++) {
						if (!approachesNyquist[(a + b) % 720] && counts[a] >= avgNumValidGates * 2.0 / 3.0) {
							double minValue2 = 1024.0;

							for (int da = -kernelSize; da <= kernelSize; da++) {
								minValue2 = Double.min(minValue2,
										avgVelocitySquaredInAzimuth[(a + b + da + 720) % 720]);
							}

							if (avgVelocitySquaredInAzimuth[(a + b) % 720] == minValue) {
								referenceRadial1 = a;
								referenceRadial2 = (a + b) % 720;
							}
						}
					}
				}
			}
		}

		referenceRadial1 = referenceRadial1Pre;
		referenceRadial2 = referenceRadial2Pre;

		// makes sure that refRadial1 is always lower, makes later code easier
		if (referenceRadial2 < referenceRadial1) {
			int rrs = referenceRadial2;
			referenceRadial2 = referenceRadial1;
			referenceRadial1 = rrs;
		}

		System.out.println(avgDirWrtNorth);
		System.out.println("referenceRadial1: " + referenceRadial1);
		System.out.println("referenceRadial2: " + referenceRadial2);

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				int a1 = (a + 1) % 720;

				if (data[a][r] == -1024.0 || data[a1][r] == -1024.0) {
					dealiasedVelocity[a][r] = -1024;
				} else if (data[a][r] == -2048.0 || data[a1][r] == -2048.0) {
					dealiasedVelocity[a][r] = -2048;
				} else {
					dealiasedVelocity[a][r] = 1 * (data[a1][r] - data[a][r]);
				}
			}
		}

		int[][] foldingNumsCW = new int[data.length][data[0].length];
		int[][] foldingPntsCW = new int[data.length][data[0].length]; // every time foldingNums changes, goes up by one

		for (int a = referenceRadial1 + 1; a < referenceRadial2; a++) {
			double[] radialPrev = data[(a - 1) % 720];
			double[] radialCurr = data[a % 720];

			for (int r = 0; r < radialCurr.length; r++) {
				double dataPrev = radialPrev[r];
				double dataCurr = radialCurr[r];

				if (dataPrev != -1024 && dataPrev != -2048 && dataCurr != -1024 && dataCurr != -2048) {
					double diffDataWrtAzi = (dataCurr - dataPrev);
					boolean signsDiff = !(Math.signum(dataCurr) == Math.signum(dataPrev));

					if (r == 300) {
						System.out.print(dataCurr + diffDataWrtAzi + "\t\t\t");
					}

					if (diffDataWrtAzi > beta * nyquistVelocity && signsDiff) {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r] - 1;
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r] + 1;
					} else if (diffDataWrtAzi < beta * -nyquistVelocity && signsDiff) {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r] + 1;
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r] + 1;
					} else {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r];
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r];
					}
				}
			}

			System.out.println(radialCurr[300] + "\t" + foldingNumsCW[a % 720][300] + "\t" + beta + "\t"
					+ nyquistVelocity + "\t" + beta * nyquistVelocity);
		}

		for (int a = referenceRadial2 + 1; a < referenceRadial1 + 720; a++) {
			double[] radialPrev = data[(a - 1) % 720];
			double[] radialCurr = data[a % 720];

			for (int r = 0; r < radialCurr.length; r++) {
				double dataPrev = radialPrev[r];
				double dataCurr = radialCurr[r];

				if (dataPrev != -1024 && dataPrev != -2048 && dataCurr != -1024 && dataCurr != -2048) {
					double diffDataWrtAzi = (dataCurr - dataPrev);
					boolean signsDiff = !(Math.signum(dataCurr) == Math.signum(dataPrev));

					if (diffDataWrtAzi > beta * nyquistVelocity && signsDiff) {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r] - 1;
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r] + 1;
					} else if (diffDataWrtAzi < beta * -nyquistVelocity && signsDiff) {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r] + 1;
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r] + 1;
					} else {
						foldingNumsCW[a % 720][r] = foldingNumsCW[(a - 1) % 720][r];
						foldingPntsCW[a % 720][r] = foldingPntsCW[(a - 1) % 720][r];
					}
				}
			}
		}

		int[][] foldingNumsCCW = new int[data.length][data[0].length];
		int[][] foldingPntsCCW = new int[data.length][data[0].length]; // every time foldingNums changes, goes up by one

		for (int a = referenceRadial1 + 720 - 1; a > referenceRadial2; a--) {
			double[] radialPrev = data[(a + 1) % 720];
			double[] radialCurr = data[a % 720];

			for (int r = 0; r < radialCurr.length; r++) {
				double dataPrev = radialPrev[r];
				double dataCurr = radialCurr[r];

				if (dataPrev != -1024 && dataPrev != -2048 && dataCurr != -1024 && dataCurr != -2048) {
					double diffDataWrtAzi = (dataCurr - dataPrev);
					boolean signsDiff = !(Math.signum(dataCurr) == Math.signum(dataPrev));

					if (diffDataWrtAzi > beta * nyquistVelocity && signsDiff) {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a + 1) % 720][r] - 1;
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a + 1) % 720][r] + 1;
					} else if (diffDataWrtAzi < beta * -nyquistVelocity && signsDiff) {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a - 1) % 720][r] + 1;
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a - 1) % 720][r] + 1;
					} else {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a - 1) % 720][r];
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a - 1) % 720][r];
					}
				}
			}
		}

		for (int a = referenceRadial2 - 1; a > referenceRadial1; a--) {
			double[] radialPrev = data[(a + 1) % 720];
			double[] radialCurr = data[a % 720];

			for (int r = 0; r < radialCurr.length; r++) {
				double dataPrev = radialPrev[r];
				double dataCurr = radialCurr[r];

				if (dataPrev != -1024 && dataPrev != -2048 && dataCurr != -1024 && dataCurr != -2048) {
					double diffDataWrtAzi = (dataCurr - dataPrev);
					boolean signsDiff = !(Math.signum(dataCurr) == Math.signum(dataPrev));

					if (diffDataWrtAzi > beta * nyquistVelocity && signsDiff) {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a + 1) % 720][r] - 1;
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a + 1) % 720][r] + 1;
					} else if (diffDataWrtAzi < beta * -nyquistVelocity && signsDiff) {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a - 1) % 720][r] + 1;
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a - 1) % 720][r] + 1;
					} else {
						foldingNumsCCW[a % 720][r] = foldingNumsCCW[(a - 1) % 720][r];
						foldingPntsCCW[a % 720][r] = foldingPntsCCW[(a - 1) % 720][r];
					}
				}
			}
		}

		int[][] foldingNums = new int[foldingNumsCW.length][foldingNumsCW[0].length];

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[0].length; r++) {
				if (foldingPntsCW[a][r] > foldingPntsCCW[a][r]) {
					foldingNums[a][r] = foldingNumsCW[a][r];
				} else {
					foldingNums[a][r] = foldingNumsCCW[a][r];
				}
			}
		}

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[0].length; r++) {
				if (data[a][r] != -1024 && data[a][r] != -2048) {
					dealiasedVelocity[a][r] = data[a][r] + 2 * foldingNums[a][r] * nyquistVelocity;
				}
			}
		}

//		for (int a = 0; a < data.length; a++) {
//			for (int r = 0; r < 100; r++) {
//				dealiasedVelocity[a][r] = (avgVelocitySquaredInAzimuth[a]);
//			}
//		}

		// maybe do azimuthal passes as a separate method
		// makes it easier to do alternating azimuthal and radial if i want to

		// azimuthal clockwise pass

		// azimuthal counterclockwise pass

		int[][] foldingNumsR = new int[data.length][data[0].length];

		final double TEST_AZI = 588;
		final double TEST_RAD = 49;
		final double RAD_INTV = 20;

		// maybe a radial pass
		for (int a = 0; a < dealiasedVelocity.length; a++) {
			double dataPrev2_lastValid = 0;
			for (int r = 2; r < dealiasedVelocity[a].length; r++) {
				double dataPrev2 = dealiasedVelocity[a][r - 2];
				double dataPrev = dealiasedVelocity[a][r - 1];
				double dataCurr = dealiasedVelocity[a][r];

				if (a == TEST_AZI && r >= TEST_RAD - RAD_INTV && r <= TEST_RAD + RAD_INTV) {
					System.out.println();
					System.out.printf("[a=%03d, r=%03d]\n", a, r);
					System.out.println("dataPrev2_lv:    " + dataPrev2_lastValid);
					System.out.println("dataPrev2:       " + dataPrev2);
					System.out.println("dataPrev:        " + dataPrev);
					System.out.println("dataCurr:        " + dataCurr);
				}

				if (dataPrev2 != -1024 && dataPrev2 != -2048 && dataPrev != -1024 && dataPrev != -2048
						&& dataCurr != -1024 && dataCurr != -2048) {
					dataPrev2_lastValid = dataPrev2;

					double diffDataWrtRad = dataCurr - dataPrev;
					double diffDataWrtRad2 = dataPrev - dataPrev2;

					if (Math.abs(diffDataWrtRad) > nyquistVelocity && Math.abs(diffDataWrtRad2) < nyquistVelocity) {
						int foldingNumShift = (int) (-Math.round(diffDataWrtRad / (2 * nyquistVelocity)));

						dealiasedVelocity[a][r] += 2 * foldingNumShift * nyquistVelocity;

						// clockwise stripe removal pass
//						int da = a + 1;
//						while (true) {
//							double dataPrevR = dealiasedVelocity[(da - 1 + 720) % 720][r];
//							double dataCurrR = dealiasedVelocity[(da + 720) % 720][r];
//							double diffDataWrtAzi = dataCurrR - dataPrevR;
//
//							if (dataCurrR == -1024 && dataCurrR == -2048) {
//								break;
//							} else if (diffDataWrtAzi > beta * nyquistVelocity) {
//								dealiasedVelocity[(da + 720) % 720][r] += 2 * foldingNumShift * nyquistVelocity;
//								da++;
//							} else {
//								break;
//							}
//						}

						// counterclockwise stripe removal pass
//						da = a - 1;
//						while (true) {
//							double dataPrevR = dealiasedVelocity[(da + 1 + 720) % 720][r];
//							double dataCurrR = dealiasedVelocity[(da + 720) % 720][r];
//							double diffDataWrtAzi = dataCurrR - dataPrevR;
//
//							if (dataCurrR == -1024 && dataCurrR == -2048) {
//								break;
//							} else if (diffDataWrtAzi > beta * nyquistVelocity) {
//								dealiasedVelocity[(da + 720) % 720][r] += 2 * foldingNumShift * nyquistVelocity;
//								da++;
//							} else {
//								break;
//							}
//						}

						if (a == TEST_AZI && r >= TEST_RAD - RAD_INTV && r <= TEST_RAD + RAD_INTV) {
							System.out.println("diffDataWrtRad:  " + diffDataWrtRad);
							System.out.println("diffDataWrtRad2: " + diffDataWrtRad2);
							System.out.println("foldingNumShift: " + foldingNumShift);
							System.out.println("new dataCurr:    " + dealiasedVelocity[a][r]);
						}
					} else {

						if (a == TEST_AZI && r >= TEST_RAD - RAD_INTV && r <= TEST_RAD + RAD_INTV) {
							System.out.println("diffDataWrtRad:  " + diffDataWrtRad);
							System.out.println("diffDataWrtRad2: " + diffDataWrtRad2);
							System.out.println("foldingNumShift: 0");
							System.out.println("new dataCurr:    " + dealiasedVelocity[a][r]);
						}
					}
				} else if (dataPrev2 == -1024 || dataPrev2 != -2048) {
					if (dataPrev != -1024 && dataPrev != -2048 && dataCurr != -1024 && dataCurr != -2048) {
						double diffDataWrtRad = dataCurr - dataPrev;
						double diffDataWrtRad2 = dataPrev - dataPrev2_lastValid;

						if (Math.abs(diffDataWrtRad) > nyquistVelocity && Math.abs(diffDataWrtRad2) < nyquistVelocity) {
							int foldingNumShift = (int) (-Math.round(diffDataWrtRad / (2 * nyquistVelocity)));

							dealiasedVelocity[a][r] += 2 * foldingNumShift * nyquistVelocity;

							// clockwise stripe removal pass
//							int da = a;
//							while(true) {
//								double dataPrevR = dealiasedVelocity[(da - 1 + 720) % 720][r];
//								double dataCurrR = dealiasedVelocity[(da + 720) % 720][r];
//								double diffDataWrtAzi = dataCurrR - dataPrevR;
//
//								if (dataCurrR == -1024 && dataCurrR == -2048) {
//										break;
//								} else if(diffDataWrtAzi > beta * nyquistVelocity) {
//									dealiasedVelocity[(da + 720) % 720][r] += 2 * foldingNumShift * nyquistVelocity;
//									da++;
//								} else {
//									break;
//								}
//							}

							// counterclockwise stripe removal pass
//							da = a - 1;
//							while (true) {
//								double dataPrevR = dealiasedVelocity[(da + 1 + 720) % 720][r];
//								double dataCurrR = dealiasedVelocity[(da + 720) % 720][r];
//								double diffDataWrtAzi = dataCurrR - dataPrevR;
//
//								if (dataCurrR == -1024 && dataCurrR == -2048) {
//									break;
//								} else if (diffDataWrtAzi > beta * nyquistVelocity) {
//									dealiasedVelocity[(da + 720) % 720][r] += 2 * foldingNumShift * nyquistVelocity;
//									da++;
//								} else {
//									break;
//								}
//							}

							if (a == TEST_AZI && r >= TEST_RAD - RAD_INTV && r <= TEST_RAD + RAD_INTV) {
								System.out.println("diffDataWrtRad:  " + diffDataWrtRad);
								System.out.println("diffDataWrtRad2: " + diffDataWrtRad2);
								System.out.println("foldingNumShift: " + foldingNumShift);
								System.out.println("new dataCurr:    " + dealiasedVelocity[a][r]);
							}
						} else {

							if (a == TEST_AZI && r >= TEST_RAD - RAD_INTV && r <= TEST_RAD + RAD_INTV) {
								System.out.println("diffDataWrtRad:  " + diffDataWrtRad);
								System.out.println("diffDataWrtRad2: " + diffDataWrtRad2);
								System.out.println("foldingNumShift: 0");
								System.out.println("new dataCurr:    " + dealiasedVelocity[a][r]);
							}
						}
					}
				}
			}
		}

//		for (int a = 0; a < data.length; a++) {
//			for (int r = 0; r < data[0].length; r++) {
//				if (data[a][r] != -1024 && data[a][r] != -2048) {
//					dealiasedVelocity[a][r] = dealiasedVelocity[a][r] + 2 * foldingNumsR[a][r] * nyquistVelocity;
//				}
//			}
//		}

		// find out how to process out the aliased azimuthal strips from
		// dealiasedVelocity

		// mark ref radials
		if (referenceRadial1 != -1) {
			for (int r = 0; r < data[0].length; r++) {
				dealiasedVelocity[referenceRadial1][r] = 1024;
			}
		}

		if (referenceRadial2 != -1) {
			for (int r = 0; r < data[0].length; r++) {
				dealiasedVelocity[referenceRadial2][r] = -1023;
			}
		}

		return dealiasedVelocity;
	}

	private static double[] getGaussianWeights(int sizeR, double stdDev) {
		double[] weights = new double[2 * sizeR + 1];

		for (int i = 0; i <= sizeR; i++) {
			weights[sizeR + i] = 1 / (stdDev * Math.sqrt(2 * Math.PI)) * Math.exp(-(i * i) / (2 * stdDev * stdDev));
			weights[sizeR - i] = weights[sizeR + i];
		}

		return weights;
	}

	private static BufferedImage drawBasemap(double westLongitude, double northLatitude, double eastLongitude,
			double southLatitude, double pixelsPerDegree) {
		double width = (eastLongitude - westLongitude) * pixelsPerDegree;
		double height = (northLatitude - southLatitude) * pixelsPerDegree;

		System.out.println(width);
		System.out.println(height);

		BufferedImage newBasemap = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = newBasemap.createGraphics();

		BasicStroke bs = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		BasicStroke ts = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		BufferedImage states = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D gg = states.createGraphics();
		gg.setColor(Color.WHITE);
		gg.setStroke(bs);
		for (int i = 0; i < stateBorders.size(); i++) {
			ArrayList<PointF> state = stateBorders.get(i);

			for (int j = 0; j < state.size(); j++) {
				int k = j + 1;
				if (k >= state.size())
					k = 0;

				PointF p1 = state.get(j);
				PointF p2 = state.get(k);

				// prevents the weird "wobble" from panning around
				PointF _p1 = new PointF(Math.round(p1.getX() * pixelsPerDegree) / pixelsPerDegree,
						Math.round(p1.getY() * pixelsPerDegree) / pixelsPerDegree);
				PointF _p2 = new PointF(Math.round(p2.getX() * pixelsPerDegree) / pixelsPerDegree,
						Math.round(p2.getY() * pixelsPerDegree) / pixelsPerDegree);
				p1 = _p1;
				p2 = _p2;

				boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude
						&& p1.getY() >= southLatitude && p1.getY() <= northLatitude);
				boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude
						&& p2.getY() >= southLatitude && p2.getY() <= northLatitude);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(westLongitude, eastLongitude, 0,
							(eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
					int x2 = (int) linScale(westLongitude, eastLongitude, 0,
							(eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
					int y1 = (int) linScale(northLatitude, southLatitude, 0,
							(northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
					int y2 = (int) linScale(northLatitude, southLatitude, 0,
							(northLatitude - southLatitude) * pixelsPerDegree, p2.getY());

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}
		gg.dispose();

		BufferedImage counties = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		if (pixelsPerDegree > 100) {
			gg = counties.createGraphics();
			gg.setColor(new Color(255, 255, 255, 127));
			gg.setStroke(bs);
			for (int i = 0; i < countyBorders.size(); i++) {
				ArrayList<PointF> state = countyBorders.get(i);

				for (int j = 0; j < state.size(); j++) {
					int k = j + 1;
					if (k >= state.size())
						k = 0;

					PointF p1 = state.get(j);
					PointF p2 = state.get(k);

					boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude
							&& p1.getY() >= southLatitude && p1.getY() <= northLatitude);
					boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude
							&& p2.getY() >= southLatitude && p2.getY() <= northLatitude);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(westLongitude, eastLongitude, 0,
								(eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
						int x2 = (int) linScale(westLongitude, eastLongitude, 0,
								(eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
						int y1 = (int) linScale(northLatitude, southLatitude, 0,
								(northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
						int y2 = (int) linScale(northLatitude, southLatitude, 0,
								(northLatitude - southLatitude) * pixelsPerDegree, p2.getY());

						gg.drawLine(x1, y1, x2, y2);
					}
				}
			}
			gg.dispose();
		}

		BufferedImage metros = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		gg = metros.createGraphics();
		gg.setColor(new Color(0, 255, 255));
		gg.setStroke(bs);
//		for(int i = 0; i < metroAreas.size(); i++) {
//			ArrayList<PointD> state = metroAreas.get(i);
//			
//			for(int j = 0; j < state.size(); j++) {
//				int k = j + 1;
//				if(k >= state.size()) k = 0;
//				
//				PointD p1 = state.get(j);
//				PointD p2 = state.get(k);
//				
//				boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude && p1.getY() >= southLatitude && p1.getY() <= northLatitude);
//				boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude && p2.getY() >= southLatitude && p2.getY() <= northLatitude);
//				
//				if(renderP1 || renderP2) {
//					int x1 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
//					int x2 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
//					int y1 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
//					int y2 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * pixelsPerDegree, p2.getY());
//					
//					gg.drawLine(x1, y1, x2, y2);
//				}
//			}
//		}
		gg.dispose();

		BufferedImage highways = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		BufferedImage highwaysBg = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		gg = highways.createGraphics();
		Graphics2D gh = highwaysBg.createGraphics();

		gg.setColor(new Color(180, 0, 0));
		gg.setStroke(bs);
		gh.setColor(new Color(0, 0, 0));
		gh.setStroke(ts);

		for (int i = 0; i < interstates.size(); i++) {
			ArrayList<PointF> state = interstates.get(i);

			for (int j = 0; j < state.size() - 1; j++) {
				int k = j + 1;

				PointF p1 = state.get(j);
				PointF p2 = state.get(k);

				boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude
						&& p1.getY() >= southLatitude && p1.getY() <= northLatitude);
				boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude
						&& p2.getY() >= southLatitude && p2.getY() <= northLatitude);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(westLongitude, eastLongitude, 0,
							(eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
					int x2 = (int) linScale(westLongitude, eastLongitude, 0,
							(eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
					int y1 = (int) linScale(northLatitude, southLatitude, 0,
							(northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
					int y2 = (int) linScale(northLatitude, southLatitude, 0,
							(northLatitude - southLatitude) * pixelsPerDegree, p2.getY());

					gg.drawLine(x1, y1, x2, y2);
					gh.drawLine(x1, y1, x2, y2);
				}
			}
		}
		gg.dispose();
		gh.dispose();

		BufferedImage highwaysComposite = highwaysBg;
		gg = highwaysComposite.createGraphics();

		gg.drawImage(highways, 0, 0, null);

		highways = highwaysComposite;

		BufferedImage roads = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		BufferedImage roadsBg = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
		if (pixelsPerDegree > 150) {
			gg = roads.createGraphics();
			gh = roadsBg.createGraphics();
			gh.setColor(new Color(0, 0, 0));
			gh.setStroke(ts);
			gg.setColor(new Color(127, 127, 255));
			gg.setStroke(bs);
			for (int i = 0; i < majorRoads.size(); i++) {
				ArrayList<PointF> state = majorRoads.get(i);

				for (int j = 0; j < state.size() - 1; j++) {
					int k = j + 1;

					PointF p1 = state.get(j);
					PointF p2 = state.get(k);

					boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude
							&& p1.getY() >= southLatitude && p1.getY() <= northLatitude);
					boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude
							&& p2.getY() >= southLatitude && p2.getY() <= northLatitude);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(westLongitude, eastLongitude, 0,
								(eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
						int x2 = (int) linScale(westLongitude, eastLongitude, 0,
								(eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
						int y1 = (int) linScale(northLatitude, southLatitude, 0,
								(northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
						int y2 = (int) linScale(northLatitude, southLatitude, 0,
								(northLatitude - southLatitude) * pixelsPerDegree, p2.getY());

						gg.drawLine(x1, y1, x2, y2);
						gh.drawLine(x1, y1, x2, y2);
					}
				}
			}
			gh.dispose();
			gg.dispose();

			BufferedImage roadsComposite = roadsBg;
			gg = roadsComposite.createGraphics();

			gg.drawImage(roads, 0, 0, null);

			roads = roadsComposite;
		}

//		BufferedImage sites = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
//		gg = sites.createGraphics();
//		for (int i = 0; i < RadarView.radarSites.size(); i++) {
//			RadarSite rs = RadarView.radarSites.get(i);
//
//			double lat = rs.getSiteCoords().getX();
//			double lon = rs.getSiteCoords().getY();
//
//			if (lat >= southLatitude && lat <= northLatitude && lon >= westLongitude && lon <= eastLongitude) {
//				int x = (int) linScale(westLongitude, eastLongitude, 0,
//						(eastLongitude - westLongitude) * pixelsPerDegree, lon);
//				int y = (int) linScale(northLatitude, southLatitude, 0,
//						(northLatitude - southLatitude) * pixelsPerDegree, lat);
//
//				rs.drawButton(gg, x, y);
//			}
//		}
//		gg.dispose();

		/*
		 * Create a rescale filter op that makes the image 50% opaque.
		 */
		float[] scales = { 1f, 1f, 1f, 0.5f };
		float[] offsets = new float[4];
		RescaleOp rop = new RescaleOp(scales, offsets, null);
		float[] scales2 = { 1f, 1f, 1f, 0.3f };
		float[] offsets2 = new float[4];
		RescaleOp rop2 = new RescaleOp(scales2, offsets2, null);

//		g.drawImage(metros, rop2, 0, 0);
		g.drawImage(roads, rop2, 0, 0);
		g.drawImage(highways, 0, 0, null);
		g.drawImage(counties, rop, 0, 0);
		g.drawImage(states, 0, 0, null);
//		g.drawImage(sites, 0, 0, null);

		g.dispose();

		return newBasemap;
	}

	private static File loadResourceAsFile(String urlStr) {
		System.out.println(urlStr);
		URL url = RadarView.class.getResource(urlStr);
		InputStream is = RadarView.class.getResourceAsStream(urlStr);
		System.out.println(url);
		System.out.println(is);
		URL tilesObj = url;

		// System.out.println("Temp-file created.");

		RadarView.tempFilesToDelete.add(RadarView.dataFolder + "temp/" + urlStr + "");
		File file = new File(RadarView.dataFolder + "temp/" + urlStr + "");

		if (tilesObj == null) {
			System.out.println("Loading failed to start.");
			return null;
		}

		// System.out.println("Loading successfully started.");

		try {
			FileUtils.copyURLToFile(tilesObj, file);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		return file;
	}

	private static double linScale(double preMin, double preMax, double postMin, double postMax, double value) {
		double slope = (postMax - postMin) / (preMax - preMin);

		return slope * (value - preMin) + postMin;
	}

	private static ArrayList<ArrayList<PointF>> getPolygons(File kml) {

		Pattern p = Pattern.compile("<coordinates>.*?</coordinates>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		ArrayList<String> coordList = new ArrayList<>();

		while (m.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			coordList.add(m.group().substring(13, m.group().length() - 14));
		}

		ArrayList<ArrayList<PointF>> polygons = new ArrayList<>();

		for (String coords : coordList) {
			Scanner sc = new Scanner(coords);
			sc.useDelimiter(" ");

			ArrayList<PointF> polygon = new ArrayList<>();

			while (sc.hasNext()) {
				String s = sc.next();
				// System.out.println(s);

				String[] pp = s.split(",");

				if (pp.length >= 2 && pp[0].length() > 0 && pp[1].length() > 0) {
				} else
					continue;

				polygon.add(new PointF(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
			}

			sc.close();
			polygons.add(polygon);
		}

		return polygons;
	}

	private static String usingBufferedReader(File filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				contentBuilder.append(sCurrentLine).append(" ");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

	// reflectivity
	private static PostProc reflPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return (0.5f * (data) - 33f);
		}
	};

	// velocity
	private static PostProc vlcyPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return 0.5f * data - 64.5f;
		}
	};

	// spectrum width
	private static PostProc spwdPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return 0.5f * data - 64.5f;
		}
	};
}
