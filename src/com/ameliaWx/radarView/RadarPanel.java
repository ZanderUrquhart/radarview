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
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

public class RadarPanel extends JComponent {
	/**
	 * maybe make radar site buttons be in a new layer drawn on top
	 */

	private static final long serialVersionUID = -5968064830481838268L;

	private static ArrayList<ArrayList<PointD>> countyBorders;
	private static ArrayList<ArrayList<PointD>> stateBorders;
	private static ArrayList<ArrayList<PointD>> interstates;
	private static ArrayList<ArrayList<PointD>> majorRoads;
	private static ArrayList<ArrayList<PointD>> estados;
	private static ArrayList<ArrayList<PointD>> canadianProvinces;
	private static ArrayList<ArrayList<PointD>> canadianProvincesSubd;
//	private static ArrayList<ArrayList<PointD>> metroAreas;
//	private static ArrayList<ArrayList<PointD>> ouCampus;
//	private static ArrayList<ArrayList<PointD>> lakeLavon;
//	private static ArrayList<ArrayList<PointD>> countryBorders;

	static {
		File countyBordersKML = loadResourceAsFile("res/usCounties.kml");
		File stateBordersKML = loadResourceAsFile("res/usStates.kml");
		File interstatesKML = loadResourceAsFile("res/wms.kml");
		File majorRoadsKML = loadResourceAsFile("res/roadtrl020.kml");
		File estadosKML = loadResourceAsFile("res/estados.kml");
		File canadianProvincesKML = loadResourceAsFile("res/canada-provinces.kml");
		File canadianProvincesSubdKML = loadResourceAsFile("res/canada-admin-subd.kml");
//		File metroAreasKML = loadResourceAsFile("res/cb_2018_us_ua10_500k.kml");
//		File ouCampusKML = loadResourceAsFile("res/ouCampus.kml");
//		File lakeLavonKML = loadResourceAsFile("res/lakeLavon.kml");
//		File countryBordersKML = loadResourceAsFile("res/countries_world.kml");

		countyBorders = getPolygons(countyBordersKML);
		stateBorders = getPolygons(stateBordersKML);
		interstates = getPolygons(interstatesKML);
		majorRoads = getPolygons(majorRoadsKML);
		estados = getPolygons(estadosKML);
		canadianProvinces = getPolygons(canadianProvincesKML, 10);
		canadianProvincesSubd = getPolygons(canadianProvincesSubdKML);
//		metroAreas = getPolygons(metroAreasKML);
//		ouCampus = getPolygons(ouCampusKML);
//		lakeLavon = getPolygons(lakeLavonKML);
//		countryBorders = getPolygons(countryBordersKML);
	}

	public Field activeField;
	public Tilt activeTilt;

	private static BufferedImage basemap; // only update when necessary

	public static BufferedImage warnings; // check performance of warnings draw
	public static BufferedImage watches; // check performance of watches draw
	public static BufferedImage spcOutlook; // check performance of spc outlook draw
	public static BufferedImage spcReports;

	public static BufferedImage buttonImg;

	public static BufferedImage[] captionImg;

	private static BufferedImage[][] imagesMd; // 0 = most recent, increasing numbers go back in time
	// stores the polar-projected data images to avoid recomputing every time
	// static because this is directly tied to RadarView.radarData
	// i = time, j = tilt
	private static BufferedImage[][] imagesLo;
	private static BufferedImage[][] imagesHi;
	private static BufferedImage[][] imagesVh;

	private double ulLon;
	private double ulLat;
	private double lrLon;
	private double lrLat;
	private double ppd;

	public static double basemapUlLon;
	public static double basemapUlLat;
	public static double basemapLrLon;
	public static double basemapLrLat;
	public static double basemapPpd;

	public static double wwaUlLon;
	public static double wwaUlLat;
	public static double wwaLrLon;
	public static double wwaLrLat;
	public static double wwaPpd;

	public String[] lowerLeftCaption;

	public static Thread basemapDrawThread;
//	public static boolean dataHasChanged = false;

	public static boolean drawWarningsAndWatches = true;

	private static final BufferedImage BLANK_IMAGE = new BufferedImage(2001, 2001, BufferedImage.TYPE_4BYTE_ABGR);

	public void drawBasemap(double ulLon, double ulLat, double lrLon, double lrLat, double ppd, float[][][] data,
			byte[][] mask, DateTime scanTime, double stormMotionDirection, double stormMotionSpeed, Field field,
			double rLon, double rLat, int timestep) {
		if (!(this.ulLon == ulLon && this.ulLat == ulLat && this.lrLon == lrLon && this.lrLat == lrLat
				&& this.ppd == ppd)) {
			basemapDrawThread = new Thread(new Runnable() {
				@Override
				public void run() {
					updateBasemap(ulLon, ulLat, lrLon, lrLat, ppd);
					RadarView.g.repaint();
				}
			});
			basemapDrawThread.start();

			this.ulLon = ulLon;
			this.ulLat = ulLat;
			this.lrLon = lrLon;
			this.lrLat = lrLat;
			this.ppd = ppd;
		}

		if (drawWarningsAndWatches) {
			drawWarnings(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.warningPolygons, RadarView.warningNames);
			drawWatches(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.watchPolygons, RadarView.watchNames);
			drawSpcOutlook(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.spcOutlookPolygons,
					RadarView.spcOutlookCategories);
			drawSpcStormReports(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.spcStormReportPoints,
					RadarView.spcStormReportTypes);
			drawSiteButtons(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.radarSites);

			wwaUlLon = ulLon;
			wwaLrLon = lrLon;
			wwaUlLat = ulLat;
			wwaLrLat = lrLat;
			wwaPpd = ppd;
		}
	}

	public void drawPanel(double ulLon, double ulLat, double lrLon, double lrLat, double ppd, int index,
			float[][][] data, byte[][] mask, DateTime scanTime, double stormMotionDirection, double stormMotionSpeed,
			Field field, double rLon, double rLat, int timestep) {
		if (!(this.ulLon == ulLon && this.ulLat == ulLat && this.lrLon == lrLon && this.lrLat == lrLat
				&& this.ppd == ppd)) {
			basemapDrawThread = new Thread(new Runnable() {
				@Override
				public void run() {
					updateBasemap(ulLon, ulLat, lrLon, lrLat, ppd);
					RadarView.g.repaint();
				}
			});
			basemapDrawThread.start();

			this.ulLon = ulLon;
			this.ulLat = ulLat;
			this.lrLon = lrLon;
			this.lrLat = lrLat;
			this.ppd = ppd;
		}

//		double width = (lrLon - ulLon) * ppd;
//		double height = (ulLat - lrLat) * ppd;

		if (lowerLeftCaption == null) {
			lowerLeftCaption = new String[RadarView.animLength];
		} else {
			if (lowerLeftCaption.length != RadarView.animLength) {
				lowerLeftCaption = new String[RadarView.animLength];

				for (int i = 0; i < lowerLeftCaption.length; i++) {
					lowerLeftCaption[i] = "-";
				}
			}
		}

		if (scanTime.getYear() == 1970) {
			lowerLeftCaption[timestep] = "-";
		} else {
			if (timestep >= lowerLeftCaption.length)
				timestep = 0; // definitely a patch, why is timestep sometimes 1 here
			lowerLeftCaption[timestep] = dateStringAlt(scanTime) + " | " + field.name.toUpperCase();
			if (field == Field.VLCY_SREL) {
				String stormMotion = String.format("(STORM MOTION: from %1.0f° at %3.1f MPH)",
						(stormMotionDirection + 360) % 360, stormMotionSpeed * 2.23);
				lowerLeftCaption[timestep] += " ";
				lowerLeftCaption[timestep] += stormMotion;
			}
		}

		if (drawWarningsAndWatches) {
			drawWarnings(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.warningPolygons, RadarView.warningNames);
			drawWatches(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.watchPolygons, RadarView.watchNames);
			drawSpcOutlook(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.spcOutlookPolygons,
					RadarView.spcOutlookCategories);
			drawSpcStormReports(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.spcStormReportPoints,
					RadarView.spcStormReportTypes);
			drawSiteButtons(ulLon, ulLat, lrLon, lrLat, ppd, RadarView.radarSites);

			wwaUlLon = ulLon;
			wwaLrLon = lrLon;
			wwaUlLat = ulLat;
			wwaLrLat = lrLat;
			wwaPpd = ppd;
		}

//		System.out.println("dataHasChanged - in method: " + dataHasChanged);
		drawCaptionImg(timestep);
		if (index != -1) {
//			synchronized (this) {
				if (RadarView.radarData[index].hasDataChanged()) {
					RadarView.radarData[index].markDataAsRendered();

					System.out.println("redrawing polar images");

					if (imagesVh == null || imagesHi == null || imagesMd == null || imagesLo == null) {
						imagesVh = new BufferedImage[RadarView.animLength][1];
						imagesHi = new BufferedImage[RadarView.animLength][1];
						imagesMd = new BufferedImage[RadarView.animLength][1];
						imagesLo = new BufferedImage[RadarView.animLength][1];
					} else {
						assert imagesVh.length == imagesHi.length;
						assert imagesVh.length == imagesMd.length;
						assert imagesVh.length == imagesLo.length;

						if (imagesVh.length != RadarView.animLength) {
							imagesVh = new BufferedImage[RadarView.animLength][1];
							imagesHi = new BufferedImage[RadarView.animLength][1];
							imagesMd = new BufferedImage[RadarView.animLength][1];
							imagesLo = new BufferedImage[RadarView.animLength][1];
						}
					}

					drawImageThread(data[0], mask, timestep, field, Tilt._1);
				}
//			}
		} else {
			System.out.println("redrawing polar images");

			if (imagesVh == null || imagesHi == null || imagesMd == null || imagesLo == null) {
				imagesVh = new BufferedImage[RadarView.animLength][1];
				imagesHi = new BufferedImage[RadarView.animLength][1];
				imagesMd = new BufferedImage[RadarView.animLength][1];
				imagesLo = new BufferedImage[RadarView.animLength][1];
			} else {
				assert imagesVh.length == imagesHi.length;
				assert imagesVh.length == imagesMd.length;
				assert imagesVh.length == imagesLo.length;

				if (imagesVh.length != RadarView.animLength) {
					imagesVh = new BufferedImage[RadarView.animLength][1];
					imagesHi = new BufferedImage[RadarView.animLength][1];
					imagesMd = new BufferedImage[RadarView.animLength][1];
					imagesLo = new BufferedImage[RadarView.animLength][1];
				}
			}
		}
	}

	private String dateStringAlt(DateTime d) {
		DateTime c = d.toDateTime(RadarView.timeZone);

		String daylightCode = RadarView.timeZoneCode.substring(0, RadarView.timeZoneCode.length() - 2) + "DT";

		boolean isPm = c.getHourOfDay() >= 12;
		boolean is12 = c.getHourOfDay() == 0 || c.getHourOfDay() == 12;
		return String.format("%04d", c.getYear()) + "-" + String.format("%02d", c.getMonthOfYear()) + "-"
				+ String.format("%02d", c.getDayOfMonth()) + " "
				+ String.format("%02d", c.getHourOfDay() % 12 + (is12 ? 12 : 0)) + ":"
				+ String.format("%02d", c.getMinuteOfHour()) + " " + (isPm ? "PM" : "AM") + " "
				+ (TimeZone.getTimeZone(RadarView.timeZone.getID()).inDaylightTime(d.toDate()) ? daylightCode
						: RadarView.timeZoneCode);
	}

	private static Thread t;

	public static void drawWorldMap() throws IOException {
		double westLongitude = -180.0;
		double eastLongitude = 40.0;
		double northLatitude = 60.0;
		double southLatitude = 15.0;

		double ppd = 200;

		BufferedImage worldMap = new BufferedImage(140 * (int) ppd + 1, (int) ppd * 45 + 1,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = worldMap.createGraphics();
		g.setColor(new Color(0, 0, 0));
		BasicStroke cyn = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g.setStroke(cyn);
		for (int i = 0; i < interstates.size(); i++) {
			ArrayList<PointD> state = interstates.get(i);

			for (int j = 0; j < state.size() - 1; j++) {
				int k = j + 1;
				if (k >= state.size())
					k = 0;

				PointD p1 = state.get(j);
				PointD p2 = state.get(k);

				// prevents the weird "wobble" from panning around
				PointD _p1 = new PointD(Math.round(p1.getX() * ppd) / ppd, Math.round(p1.getY() * ppd) / ppd);
				PointD _p2 = new PointD(Math.round(p2.getX() * ppd) / ppd, Math.round(p2.getY() * ppd) / ppd);
				p1 = _p1;
				p2 = _p2;

				boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude
						&& p1.getY() >= southLatitude && p1.getY() <= northLatitude);
				boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude
						&& p2.getY() >= southLatitude && p2.getY() <= northLatitude);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * ppd,
							p1.getX());
					int x2 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * ppd,
							p2.getX());
					int y1 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * ppd,
							p1.getY());
					int y2 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * ppd,
							p2.getY());

					g.drawLine(x1, y1, x2, y2);
				}
			}
		}
		g.dispose();

		ImageIO.write(worldMap, "PNG", new File("northAmericaBasemap-interstates-borders.png"));
	}

	@SuppressWarnings("deprecation")
	private void updateBasemap(double westLongitude, double northLatitude, double eastLongitude, double southLatitude,
			double pixelsPerDegree) {
		if (t != null) {
			if (t.isAlive()) {
				t.stop();
			}
		}

		t = new Thread(new Runnable() {
			@Override
			public void run() {
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
					ArrayList<PointD> state = stateBorders.get(i);

					for (int j = 0; j < state.size(); j++) {
						int k = j + 1;
						if (k >= state.size())
							k = 0;

						PointD p1 = state.get(j);
						PointD p2 = state.get(k);

						// prevents the weird "wobble" from panning around
						PointD _p1 = new PointD(Math.round(p1.getX() * pixelsPerDegree) / pixelsPerDegree,
								Math.round(p1.getY() * pixelsPerDegree) / pixelsPerDegree);
						PointD _p2 = new PointD(Math.round(p2.getX() * pixelsPerDegree) / pixelsPerDegree,
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
				for (int i = 0; i < canadianProvinces.size(); i++) {
					ArrayList<PointD> state = canadianProvinces.get(i);

					for (int j = 0; j < state.size(); j++) {
						int k = j + 1;
						if (k >= state.size())
							k = 0;

						PointD p1 = state.get(j);
						PointD p2 = state.get(k);

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
				for (int i = 0; i < estados.size(); i++) {
					ArrayList<PointD> state = estados.get(i);

					for (int j = 0; j < state.size(); j++) {
						int k = j + 1;
						if (k >= state.size())
							k = 0;

						PointD p1 = state.get(j);
						PointD p2 = state.get(k);

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
					gg.setColor(new Color(255, 255, 255, 255));
					gg.setStroke(bs);
					for (int i = 0; i < countyBorders.size(); i++) {
						ArrayList<PointD> state = countyBorders.get(i);

						for (int j = 0; j < state.size(); j++) {
							int k = j + 1;
							if (k >= state.size())
								k = 0;

							PointD p1 = state.get(j);
							PointD p2 = state.get(k);

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
					for (int i = 0; i < canadianProvincesSubd.size(); i++) {
						ArrayList<PointD> state = canadianProvincesSubd.get(i);

						for (int j = 0; j < state.size(); j++) {
							int k = j + 1;
							if (k >= state.size())
								k = 0;

							PointD p1 = state.get(j);
							PointD p2 = state.get(k);

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
//				for(int i = 0; i < metroAreas.size(); i++) {
//					ArrayList<PointD> state = metroAreas.get(i);
//					
//					for(int j = 0; j < state.size(); j++) {
//						int k = j + 1;
//						if(k >= state.size()) k = 0;
//						
//						PointD p1 = state.get(j);
//						PointD p2 = state.get(k);
//						
//						boolean renderP1 = (p1.getX() >= westLongitude && p1.getX() <= eastLongitude && p1.getY() >= southLatitude && p1.getY() <= northLatitude);
//						boolean renderP2 = (p2.getX() >= westLongitude && p2.getX() <= eastLongitude && p2.getY() >= southLatitude && p2.getY() <= northLatitude);
//						
//						if(renderP1 || renderP2) {
//							int x1 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * pixelsPerDegree, p1.getX());
//							int x2 = (int) linScale(westLongitude, eastLongitude, 0, (eastLongitude - westLongitude) * pixelsPerDegree, p2.getX());
//							int y1 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * pixelsPerDegree, p1.getY());
//							int y2 = (int) linScale(northLatitude, southLatitude, 0, (northLatitude - southLatitude) * pixelsPerDegree, p2.getY());
//							
//							gg.drawLine(x1, y1, x2, y2);
//						}
//					}
//				}
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
					ArrayList<PointD> state = interstates.get(i);

					for (int j = 0; j < state.size() - 1; j++) {
						int k = j + 1;

						PointD p1 = state.get(j);
						PointD p2 = state.get(k);

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
						ArrayList<PointD> state = majorRoads.get(i);

						for (int j = 0; j < state.size() - 1; j++) {
							int k = j + 1;

							PointD p1 = state.get(j);
							PointD p2 = state.get(k);

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

//				BufferedImage sites = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
//				gg = sites.createGraphics();
//				for (int i = 0; i < RadarView.radarSites.size(); i++) {
//					RadarSite rs = RadarView.radarSites.get(i);
//
//					double lat = rs.getSiteCoords().getX();
//					double lon = rs.getSiteCoords().getY();
//
//					if (lat >= southLatitude && lat <= northLatitude && lon >= westLongitude && lon <= eastLongitude) {
//						int x = (int) linScale(westLongitude, eastLongitude, 0,
//								(eastLongitude - westLongitude) * pixelsPerDegree, lon);
//						int y = (int) linScale(northLatitude, southLatitude, 0,
//								(northLatitude - southLatitude) * pixelsPerDegree, lat);
//
//						rs.drawButton(gg, x, y);
//					}
//				}
//				gg.dispose();

				/*
				 * Create a rescale filter op that makes the image 50% opaque.
				 */
				float[] scales = { 1f, 1f, 1f, 0.4f };
				float[] offsets = new float[4];
				RescaleOp rop = new RescaleOp(scales, offsets, null);
				float[] scales2 = { 1f, 1f, 1f, 0.3f };
				float[] offsets2 = new float[4];
				RescaleOp rop2 = new RescaleOp(scales2, offsets2, null);

//				g.drawImage(metros, rop2, 0, 0);
				g.drawImage(roads, rop2, 0, 0);
				g.drawImage(highways, 0, 0, null);
				g.drawImage(counties, rop, 0, 0);
				g.drawImage(states, 0, 0, null);
//				g.drawImage(sites, 0, 0, null);

				g.dispose();

				basemap = newBasemap;

				basemapUlLon = ulLon;
				basemapLrLon = lrLon;
				basemapUlLat = ulLat;
				basemapLrLat = lrLat;
				basemapPpd = ppd;

				RadarView.g.repaint();
			}
		});
		t.start();
	}

	private void drawImageThread(float[][] data, byte[][] mask, int timestep, Field field, Tilt tilt) {
//		System.out.println("drawing radar images (timestamp=" + timestep + ")...");
		Thread t1 = drawImageThread(data, mask, timestep, field, tilt, 0);
		Thread t2 = drawImageThread(data, mask, timestep, field, tilt, 1);
		Thread t3 = drawImageThread(data, mask, timestep, field, tilt, 2);
		Thread t4 = drawImageThread(data, mask, timestep, field, tilt, 3);

		t1.start();
		t2.start();
		t3.start();
		t4.start();

		try {
			t1.join();
			t2.join();
			t3.join();
			t4.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return;
	}

	private Thread drawImageThread(float[][] data, byte[][] mask, int timestep, Field field, Tilt tilt, int zoomLevel) {
//		System.out.println("drawing radar images (zoomLevel=" + zoomLevel + ") (timestamp=" + timestep + ")...");
//		System.out.printf("%8d%8d%8d%8d", imagesLo.length, imagesMd.length, imagesHi.length, imagesVh.length);
//		System.out.println(imagesLo);
//		System.out.println(imagesLo.length);
//		System.out.println(imagesLo[timestep].length);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				double coneOfSilence = (data.length == 720) ? 7.5 : -1;
				double disMult = (data.length == 720) ? 1 : 0.2967444/0.25;
				
				switch (zoomLevel) {
				case 0:
					if (imagesLo[timestep] == null)
						imagesLo[timestep] = new BufferedImage[1];

					imagesLo[timestep][0] = drawPolarProjImage(data, mask, 2001, 0.5*disMult, coneOfSilence,
							RadarView.colors[field.colorLocation]);
					break;
				case 1:
					if (imagesMd[timestep] == null)
						imagesMd[timestep] = new BufferedImage[1];

					imagesMd[timestep][0] = drawPolarProjImage(data, mask, 2001, 1*disMult, coneOfSilence,
							RadarView.colors[field.colorLocation]);
					break;
				case 2:
					if (imagesHi[timestep] == null)
						imagesHi[timestep] = new BufferedImage[1];

					imagesHi[timestep][0] = drawPolarProjImage(data, mask, 2001, 2*disMult, coneOfSilence,
							RadarView.colors[field.colorLocation]);
					break;
				case 3:
					if (imagesVh[timestep] == null)
						imagesVh[timestep] = new BufferedImage[1];

					imagesVh[timestep][0] = drawPolarProjImage(data, mask, 2001, 4*disMult, coneOfSilence,
							RadarView.colors[field.colorLocation]);
					break;
				default:
					return;
				}
				RadarView.g.repaint();
			}
		});

		return t;
	}

	private void drawWarnings(double ulLon, double ulLat, double lrLon, double lrLat, double ppd,
			ArrayList<ArrayList<PointD>> warningPolygons, ArrayList<String> warningNames) {
		int imgWidth = (int) ((lrLon - ulLon) * ppd);
		int imgHeight = (int) ((ulLat - lrLat) * ppd);

		BufferedImage warningsImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = warningsImg.createGraphics();

		if (RadarView.viewStormScaleWarnings) {
			BasicStroke clr = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			BasicStroke blk = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

			g.setStroke(blk);

			for (int p = 0; p < warningPolygons.size(); p++) {
				ArrayList<PointD> polygon = warningPolygons.get(p);

				g.setColor(Color.BLACK);

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
							&& p1.getY() <= ulLat);
					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
							&& p2.getY() <= ulLat);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
						int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
						int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
						int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

						g.drawLine(x1, y1, x2, y2);
					}
				}
			}

			g.setStroke(clr);

			for (int p = 0; p < warningPolygons.size(); p++) {
				ArrayList<PointD> polygon = warningPolygons.get(p);
				String name = warningNames.get(p).substring(5, 7);
				String nameW = warningNames.get(p).trim();

				if ("TO".equals(name)) {
					g.setColor(Color.RED);
				} else if ("SV".equals(name)) {
					g.setColor(Color.YELLOW);
				} else if ("FF".equals(name)) {
					g.setColor(Color.GREEN);
				} else if ("MA".equals(name)) {
					g.setColor(new Color(255, 127, 0));
				} else if ("Snow Squall Warning".equals(nameW)) {
					g.setColor(new Color(0, 255, 255));
				} else if ("Flood Warning".equals(nameW)) {
					g.setColor(new Color(0, 170, 0));
				}

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
							&& p1.getY() <= ulLat);
					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
							&& p2.getY() <= ulLat);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
						int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
						int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
						int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

						g.drawLine(x1, y1, x2, y2);
					}
				}
			}
		}

		warnings = warningsImg;
	}

	private void drawWatches(double ulLon, double ulLat, double lrLon, double lrLat, double ppd,
			ArrayList<ArrayList<PointD>> watchPolygons, ArrayList<String> watchNames) {
		int imgWidth = (int) ((lrLon - ulLon) * ppd);
		int imgHeight = (int) ((ulLat - lrLat) * ppd);

		BufferedImage watchesImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = watchesImg.createGraphics();

		if (RadarView.viewLargeScaleWarnings) {
			BasicStroke clr = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			BasicStroke blk = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

			g.setStroke(blk);

			for (int p = 0; p < watchPolygons.size(); p++) {
				ArrayList<PointD> polygon = watchPolygons.get(p);
				String name = watchNames.get(p).trim();

//				System.out.println(name);

				g.setColor(new Color(0, 0, 0, 0));
				if ("Tornado Watch".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Severe Thunderstorm Watch".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Flood Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Wind Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Dense Fog Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("High Wind Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Small Craft Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Gale Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Hazardous Seas Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Winter Weather Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Winter Storm Watch".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Winter Storm Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Blizzard Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Ice Storm Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Wind Chill Advisory".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Wind Chill Watch".equals(name))
					g.setColor(new Color(0, 0, 0, 196));
				if ("Wind Chill Warning".equals(name))
					g.setColor(new Color(0, 0, 0, 196));

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
							&& p1.getY() <= ulLat);
					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
							&& p2.getY() <= ulLat);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
						int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
						int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
						int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

						g.drawLine(x1, y1, x2, y2);
					}
				}
			}

			g.setStroke(clr);

			for (int p = 0; p < watchPolygons.size(); p++) {
				ArrayList<PointD> polygon = watchPolygons.get(p);
				String name = watchNames.get(p).trim();

				g.setColor(new Color(0, 0, 0, 0));
				if ("Tornado Watch".equals(name))
					g.setColor(new Color(255, 64, 64, 196));
				if ("Severe Thunderstorm Watch".equals(name))
					g.setColor(new Color(255, 255, 127, 196));
				if ("Dense Fog Advisory".equals(name))
					g.setColor(new Color(170, 170, 170, 196));
				if ("Flood Advisory".equals(name))
					g.setColor(new Color(64, 255, 64, 196));
				if ("Wind Advisory".equals(name))
					g.setColor(new Color(255, 196, 127, 196));
				if ("High Wind Warning".equals(name))
					g.setColor(new Color(255, 127, 0, 196));
				if ("Small Craft Advisory".equals(name))
					g.setColor(new Color(255, 196, 127, 196));
				if ("Gale Warning".equals(name))
					g.setColor(new Color(255, 127, 0, 196));
				if ("Hazardous Seas Warning".equals(name))
					g.setColor(new Color(0, 255, 190, 196));
				if ("Winter Weather Advisory".equals(name))
					g.setColor(new Color(123, 104, 238, 196));
				if ("Winter Storm Watch".equals(name))
					g.setColor(new Color(70, 130, 180, 196));
				if ("Winter Storm Warning".equals(name))
					g.setColor(new Color(255, 105, 208, 196));
				if ("Blizzard Warning".equals(name))
					g.setColor(new Color(254, 70, 0, 196));
				if ("Ice Storm Warning".equals(name))
					g.setColor(new Color(139, 0, 139, 196));
				if ("Wind Chill Advisory".equals(name))
					g.setColor(new Color(175, 238, 238, 196));
				if ("Wind Chill Watch".equals(name))
					g.setColor(new Color(70, 130, 180, 196));
				if ("Wind Chill Warning".equals(name))
					g.setColor(new Color(176, 196, 222, 196));

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
							&& p1.getY() <= ulLat);
					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
							&& p2.getY() <= ulLat);

					if (renderP1 || renderP2) {
						int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
						int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
						int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
						int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

						g.drawLine(x1, y1, x2, y2);

//					System.out.println(x1 + "\t" + y1 + "\t" + name);
					}
				}
			}
		}

		watches = watchesImg;
	}

	private void drawSpcOutlook(double ulLon, double ulLat, double lrLon, double lrLat, double ppd,
			ArrayList<ArrayList<PointD>> outlookPolygons, ArrayList<String> outlookNames) {
		int imgWidth = (int) ((lrLon - ulLon) * ppd);
		int imgHeight = (int) ((ulLat - lrLat) * ppd);

		BufferedImage outlookImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = outlookImg.createGraphics();

		if (RadarView.viewSpcOutlook) {
			BasicStroke clr = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			BasicStroke blk = new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

			g.setStroke(blk);
			g.setColor(new Color(0, 0, 0));

			for (int p = 0; p < outlookPolygons.size(); p++) {
				ArrayList<PointD> polygon = outlookPolygons.get(p);

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

//					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
//							&& p1.getY() <= ulLat);
//					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
//							&& p2.getY() <= ulLat);

//					if (renderP1 || renderP2) {
					int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
					int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
					int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
					int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

					g.drawLine(x1, y1, x2, y2);
//					}
				}
			}

			g.setStroke(clr);

			for (int p = 0; p < outlookPolygons.size(); p++) {
				ArrayList<PointD> polygon = outlookPolygons.get(p);
				String name = outlookNames.get(p);

				if ("TSTM".equals(name))
					g.setColor(new Color(142, 232, 142));
				if ("MRGL".equals(name))
					g.setColor(new Color(27, 197, 27));
				if ("SLGT".equals(name))
					g.setColor(new Color(246, 246, 27));
				if ("ENH".equals(name))
					g.setColor(new Color(230, 124, 27));
				if ("MDT".equals(name))
					g.setColor(new Color(230, 27, 27));
				if ("HIGH".equals(name))
					g.setColor(new Color(255, 64, 255));

				for (int i = 0; i < polygon.size(); i++) {
					int j = i + 1;
					if (j == polygon.size())
						j = 0;

					PointD p1 = polygon.get(i);
					PointD p2 = polygon.get(j);

//					boolean renderP1 = (p1.getX() >= ulLon && p1.getX() <= lrLon && p1.getY() >= lrLat
//							&& p1.getY() <= ulLat);
//					boolean renderP2 = (p2.getX() >= ulLon && p2.getX() <= lrLon && p2.getY() >= lrLat
//							&& p2.getY() <= ulLat);

//					if (renderP1 || renderP2) {
					int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getX());
					int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getY());
					int x2 = (int) linScale(ulLon, lrLon, 0, imgWidth, p2.getX());
					int y2 = (int) linScale(ulLat, lrLat, 0, imgHeight, p2.getY());

					g.drawLine(x1, y1, x2, y2);
//					}
				}
			}
		}

		spcOutlook = outlookImg;
	}

	private void drawSpcStormReports(double ulLon, double ulLat, double lrLon, double lrLat, double ppd,
			ArrayList<PointD> reportPoints, ArrayList<Integer> reportNames) {
		int imgWidth = (int) ((lrLon - ulLon) * ppd);
		int imgHeight = (int) ((ulLat - lrLat) * ppd);

		BufferedImage reportsImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = reportsImg.createGraphics();

		if (RadarView.viewSpcStormReports) {
			for (int i = 0; i < reportPoints.size(); i++) {
				PointD p1 = reportPoints.get(i);

				boolean renderP1 = (p1.getX() >= lrLat && p1.getX() <= ulLat && p1.getY() >= ulLon
						&& p1.getY() <= lrLon);
				if (renderP1) {
					int x1 = (int) linScale(ulLon, lrLon, 0, imgWidth, p1.getY());
					int y1 = (int) linScale(ulLat, lrLat, 0, imgHeight, p1.getX());

					// make number zero if there's an outermost ppd at which icons are drawn
					if (ppd > 0) {
						if (reportNames.get(i) == 3) {
							drawSpcWindReport(g, x1, y1);
						} else if (reportNames.get(i) == 4) {
							drawSpcStrongWindReport(g, x1, y1);
						} else if (reportNames.get(i) == 1) {
							drawSpcHailReport(g, x1, y1);
						} else if (reportNames.get(i) == 2) {
							drawSpcLargeHailReport(g, x1, y1);
						} else if (reportNames.get(i) == 0) {
							drawSpcTornadoReport(g, x1, y1);
						}
					} else {
						if (reportNames.get(i) == 3) {
							g.setColor(new Color(0, 127, 255));
							g.fillOval(x1 - 3, y1 - 3, 7, 7);
						} else if (reportNames.get(i) == 4) {
							g.setColor(new Color(0, 0, 255));
							g.fillOval(x1 - 3, y1 - 3, 7, 7);
						} else if (reportNames.get(i) == 1) {
							g.setColor(new Color(0, 255, 0));
							g.fillOval(x1 - 3, y1 - 3, 7, 7);
						} else if (reportNames.get(i) == 2) {
							g.setColor(new Color(0, 127, 0));
							g.fillOval(x1 - 3, y1 - 3, 7, 7);
						} else if (reportNames.get(i) == 0) {
							g.setColor(new Color(255, 0, 0));
							g.fillOval(x1 - 3, y1 - 3, 7, 7);
						}
					}
				}
			}
		}

		spcReports = reportsImg;
	}

	private static final BasicStroke BS = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke TS = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private void drawSpcTornadoReport(Graphics2D g, int x, int y) {
		Color outsideMethodColor = g.getColor();

		g.setStroke(TS);
		g.setColor(new Color(0, 0, 0));

		g.drawLine(x - 6, y - 6, x + 6, y - 6);
		g.drawLine(x + 6, y - 6, x, y + 6);
		g.drawLine(x, y + 6, x - 6, y - 6);

		g.setStroke(BS);
		g.setColor(new Color(255, 0, 0));

		g.drawLine(x - 6, y - 6, x + 6, y - 6);
		g.drawLine(x + 6, y - 6, x, y + 6);
		g.drawLine(x, y + 6, x - 6, y - 6);

		g.setColor(outsideMethodColor);
	}

	private void drawSpcHailReport(Graphics2D g, int x, int y) {
		Color outsideMethodColor = g.getColor();

		g.setStroke(TS);
		g.setColor(new Color(0, 0, 0));

		g.drawOval(x - 5, y - 5, 9, 9);

		g.setStroke(BS);
		g.setColor(new Color(0, 255, 0));

		g.drawOval(x - 5, y - 5, 9, 9);

		g.setColor(outsideMethodColor);
	}

	private void drawSpcLargeHailReport(Graphics2D g, int x, int y) {
		Color outsideMethodColor = g.getColor();

		g.setStroke(TS);
		g.setColor(new Color(0, 0, 0));

		g.drawOval(x - 7, y - 7, 13, 13);
		g.drawOval(x - 3, y - 3, 5, 5);

		g.setStroke(BS);
		g.setColor(new Color(0, 127, 0));

		g.drawOval(x - 7, y - 7, 13, 13);
		g.drawOval(x - 3, y - 3, 5, 5);

		g.setColor(outsideMethodColor);
	}

	/*
	 * 
	 * if ("Wind Advisory".equals(name)) g.setColor(new Color(255, 196, 127, 196));
	 * if ("High Wind Warning".equals(name)) g.setColor(new Color(255, 127, 0,
	 * 196));
	 */
	private void drawSpcWindReport(Graphics2D g, int x, int y) {
		Color outsideMethodColor = g.getColor();

		g.setStroke(TS);
		g.setColor(new Color(0, 0, 0));

		g.drawLine(x - 5, y, x + 5, y);
		g.drawLine(x + 5, y, x + 1, y + 4);
		g.drawLine(x + 5, y, x + 1, y - 4);

		g.setStroke(BS);
		g.setColor(new Color(0, 127, 255));

		g.drawLine(x - 5, y, x + 5, y);
		g.drawLine(x + 5, y, x + 1, y + 4);
		g.drawLine(x + 5, y, x + 1, y - 4);

		g.setColor(outsideMethodColor);
	}

	private void drawSpcStrongWindReport(Graphics2D g, int x, int y) {
		Color outsideMethodColor = g.getColor();

		g.setStroke(TS);
		g.setColor(new Color(0, 0, 0));

		g.drawLine(x - 5, y - 2, x + 5, y - 2);
		g.drawLine(x - 5, y + 2, x + 5, y + 2);
		g.drawLine(x + 7, y, x + 1, y + 6);
		g.drawLine(x + 7, y, x + 1, y - 6);

		g.setStroke(BS);
		g.setColor(new Color(0, 64, 255));

		g.drawLine(x - 5, y - 2, x + 5, y - 2);
		g.drawLine(x - 5, y + 2, x + 5, y + 2);
		g.drawLine(x + 7, y, x + 1, y + 6);
		g.drawLine(x + 7, y, x + 1, y - 6);

		g.setColor(outsideMethodColor);
	}

	private void drawSiteButtons(double ulLon, double ulLat, double lrLon, double lrLat, double ppd,
			ArrayList<RadarSite> radarSites) {
		int imgWidth = (int) ((lrLon - ulLon) * ppd);
		int imgHeight = (int) ((ulLat - lrLat) * ppd);

		BufferedImage siteButtons = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D gg = siteButtons.createGraphics();

		for (int i = 0; i < RadarView.radarSites.size(); i++) {
			RadarSite rs = RadarView.radarSites.get(i);

			PointD rsP = rs.getSiteCoords();

			PointD _rsP = new PointD(Math.round(rsP.getX() * ppd) / ppd, Math.round(rsP.getY() * ppd) / ppd);
			rsP = _rsP;

			double lat = rsP.getX();
			double lon = rsP.getY();

			if (lat >= lrLat && lat <= ulLat && lon >= ulLon && lon <= lrLon) {
				int x = (int) linScale(ulLon, lrLon, 0, (lrLon - ulLon) * ppd, lon);
				int y = (int) linScale(ulLat, lrLat, 0, (ulLat - lrLat) * ppd, lat);

				rs.drawButton(gg, x, y);
			}
		}

		gg.dispose();

		buttonImg = siteButtons;
	}

	private static final String AUTHOR_MESSAGE = "MADE BY AMELIA URQUHART | PRESS 'H' FOR HELP";
	public static final Font CAPTION_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

	private void drawCaptionImg(int timestep) {
		if (captionImg == null) {
			captionImg = new BufferedImage[RadarView.animLength];
		} else {
			if (captionImg.length != RadarView.animLength) {
				captionImg = new BufferedImage[RadarView.animLength];
			}
		}

		BufferedImage dummyImg = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D dummyG = dummyImg.createGraphics();
		dummyG.setFont(CAPTION_FONT);

		int imgWidth = Integer.max(dummyG.getFontMetrics().stringWidth(lowerLeftCaption[timestep]),
				dummyG.getFontMetrics().stringWidth(AUTHOR_MESSAGE)) + 2;
		int imgHeight = 30;

		BufferedImage caption = new BufferedImage(imgWidth, imgHeight + 5, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D gg = caption.createGraphics();

		gg.setFont(CAPTION_FONT);

		gg.setColor(Color.BLACK);
		gg.fillRect(0, imgHeight - 21, gg.getFontMetrics().stringWidth(lowerLeftCaption[timestep]) + 5, 13);
		gg.fillRect(0, imgHeight - 11, gg.getFontMetrics().stringWidth(AUTHOR_MESSAGE) + 5, 13);

		gg.setColor(Color.WHITE);
		gg.drawString(lowerLeftCaption[timestep], 0, imgHeight - 10);
		gg.drawString(AUTHOR_MESSAGE, 0, imgHeight - 0);

		// Q adjustment
		gg.setColor(Color.BLACK);
		gg.fillRect(125, 27, 1, 1);
		gg.fillRect(124, 29, 1, 1);
		gg.setColor(Color.WHITE);
		gg.fillRect(123, 28, 1, 1);
		gg.fillRect(124, 28, 1, 1);
		gg.fillRect(125, 28, 1, 1);
		gg.fillRect(125, 29, 1, 1);

		gg.dispose();

//		System.out.println("drawing captionImg[" + timestep + "]...");

		captionImg[timestep] = caption;
	}

	public static void initImages() {
		imagesVh = new BufferedImage[RadarView.animLength][1];
		imagesHi = new BufferedImage[RadarView.animLength][1];
		imagesMd = new BufferedImage[RadarView.animLength][1];
		imagesLo = new BufferedImage[RadarView.animLength][1];
	}

	public static void initImages(int i) {
		imagesVh = new BufferedImage[i][1];
		imagesHi = new BufferedImage[i][1];
		imagesMd = new BufferedImage[i][1];
		imagesLo = new BufferedImage[i][1];
	}
	
	public void blankOutImages() {
		for(int i = 0; i < imagesVh.length; i++) {
			imagesVh[i][0] = BLANK_IMAGE;
			imagesHi[i][0] = BLANK_IMAGE;
			imagesMd[i][0] = BLANK_IMAGE;
			imagesLo[i][0] = BLANK_IMAGE;
			RadarView.radarData[i].markDataAsRendered();
			RadarView.g.repaint();
		}
	}
	
	public static boolean getImagesNull() {
		return imagesLo == null;
	}

	public BufferedImage getBasemap() {
		return basemap;
	}

	public BufferedImage getLoResImage(int step, Field field, Tilt tilt) {
		if (imagesLo == null) {
			return BLANK_IMAGE;
		}
		if (imagesLo[step][tilt.id] == null) {
			return BLANK_IMAGE;
		}
		return imagesLo[step][tilt.id];
	}

	public BufferedImage getMdResImage(int step, Field field, Tilt tilt) {
		if (imagesMd == null) {
			return BLANK_IMAGE;
		}
		if (imagesMd[step][tilt.id] == null) {
			return BLANK_IMAGE;
		}
		return imagesMd[step][tilt.id];
	}

	public BufferedImage getHiResImage(int step, Field field, Tilt tilt) {
		if (imagesHi == null) {
			return BLANK_IMAGE;
		}
		if (imagesHi[step][tilt.id] == null) {
			return BLANK_IMAGE;
		}
		return imagesHi[step][tilt.id];
	}

	public BufferedImage getVhResImage(int step, Field field, Tilt tilt) {
		if (imagesVh == null) {
			return BLANK_IMAGE;
		}
		if (imagesVh[step][tilt.id] == null) {
			return BLANK_IMAGE;
		}
		return imagesVh[step][tilt.id];
	}

	public BufferedImage getCaptionImg(int step) {
		if (captionImg == null) {
			return BLANK_IMAGE;
		}
		if (captionImg[step] == null) {
			return BLANK_IMAGE;
		}
		return captionImg[step];
	}

	public BufferedImage getColors(int width, int height) {
		return RadarView.colors[RadarView.chosenField.dataLocation].drawColorLegend(width, height, 67, true);
	}

	public void shiftByOne() {
		System.out.println("shifting by one method");

		assert imagesLo.length == RadarView.radarData.length;
		assert imagesLo.length == RadarView.radarDataFileNames.length;
		assert imagesLo.length == imagesMd.length;
		assert imagesLo.length == imagesHi.length;
		assert imagesLo.length == imagesVh.length;
		assert imagesLo.length == captionImg.length;

		for (int i = imagesLo.length - 2; i >= 0; i--) {
			for (int j = 0; j < imagesLo[0].length; j++) {
				imagesLo[i + 1][j] = imagesLo[i][j];
				imagesMd[i + 1][j] = imagesMd[i][j];
				imagesHi[i + 1][j] = imagesHi[i][j];
				imagesVh[i + 1][j] = imagesVh[i][j];
			}

			captionImg[i + 1] = captionImg[i];

			RadarView.radarData[i + 1] = RadarView.radarData[i];
			RadarView.radarDataFileNames[i + 1] = RadarView.radarDataFileNames[i];
		}
	}

	public void copyToArraysOfNewLength(int newLength) {
		System.out.println("copy to arrays of new length method");

		assert imagesLo.length == RadarView.radarData.length;
		assert imagesLo.length == RadarView.radarDataFileNames.length;
		assert imagesLo.length == imagesMd.length;
		assert imagesLo.length == imagesHi.length;
		assert imagesLo.length == imagesVh.length;
		assert imagesLo.length == captionImg.length;

		RadarData[] radarDataNew = new RadarData[newLength];
		String[] radarDataFileNamesNew = new String[newLength];
		BufferedImage[][] imagesLoNew = new BufferedImage[newLength][];
		BufferedImage[][] imagesMdNew = new BufferedImage[newLength][];
		BufferedImage[][] imagesHiNew = new BufferedImage[newLength][];
		BufferedImage[][] imagesVhNew = new BufferedImage[newLength][];
		BufferedImage[] captionImgNew = new BufferedImage[newLength];

		int oldLength = imagesLo.length;

		System.out.println(RadarView.prevAnimLength + "\t" + newLength);

		for (int i = 0; i < Integer.min(newLength, RadarView.prevAnimLength); i++) {
			imagesLoNew[i] = new BufferedImage[imagesLo[i].length];
			imagesMdNew[i] = new BufferedImage[imagesLo[i].length];
			imagesHiNew[i] = new BufferedImage[imagesLo[i].length];
			imagesVhNew[i] = new BufferedImage[imagesLo[i].length];

			for (int j = 0; j < imagesLo[0].length; j++) {
				imagesLoNew[i][j] = imagesLo[i][j];
				imagesMdNew[i][j] = imagesMd[i][j];
				imagesHiNew[i][j] = imagesHi[i][j];
				imagesVhNew[i][j] = imagesVh[i][j];
			}

			captionImgNew[i] = captionImg[i];

			radarDataNew[i] = RadarView.radarData[i];
			radarDataFileNamesNew[i] = RadarView.radarDataFileNames[i];
		}

		// should only run if anim length increases
		for (int i = oldLength; i < newLength; i++) {
			imagesLoNew[i] = new BufferedImage[imagesLo[0].length];
			imagesMdNew[i] = new BufferedImage[imagesLo[0].length];
			imagesHiNew[i] = new BufferedImage[imagesLo[0].length];
			imagesVhNew[i] = new BufferedImage[imagesLo[0].length];
		}

		RadarView.radarData = radarDataNew;
		RadarView.radarDataFileNames = radarDataFileNamesNew;

		captionImg = captionImgNew;

		imagesLo = imagesLoNew;
		imagesMd = imagesMdNew;
		imagesHi = imagesHiNew;
		imagesVh = imagesVhNew;
	}

	private static int[][] azimuths;

	private static BufferedImage drawPolarProjImage(float[][] data, byte[][] mask, int size, double res, double coneOfSilence,
			ColorScale colors) {
		if (azimuths == null || azimuths.length != size)
			computeAzimuths(size);
		
		double aziMult = data.length / 720.0;

		BufferedImage polarProj = new BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = polarProj.createGraphics();

//		for (int i = 0; i < 720; i++) {
//			for (int j = 0; j < 1832; j++) {
//				System.out.print(mask[i][j] + " ");
//			}
//			System.out.println();
//		}

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int dis = (int) (Math.hypot(i - size / 2, j - size / 2) / res - coneOfSilence);
				int az = azimuths[i][j];
				
//				if(data.length == 360) {
//					az += 2;
//					if(az > 360) az -= 360;
//				}

				if (dis > 0 && dis < data[0].length) {
					g.setColor(colors.getColor(data[(int) (aziMult * az)][dis],
							((dis < mask[(int) (aziMult * az)].length) ? mask[(int) (aziMult * az)][dis] : mask[(int) (aziMult * az)][mask[(int) (aziMult * az)].length - 1])));
//					if(data[az][dis] > 0) System.out.println(data[az][dis] + "\t" + mask[i][j]);
//					if(data[az][dis] > 0) System.out.println(data[az][dis] + "\t" + mask[i][j] + "\t" + colors.getColor(data[az][dis]) + "\t" + colors.getColor(data[az][dis], mask[i][j]));
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
		
		double azisPerDegree = 2;

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				azimuths[i][j] = (int) ((int) (azisPerDegree * ((270.0 * azisPerDegree) - Math.toDegrees(Math.atan2(i - size / 2, j - size / 2)))) % (360.0 * azisPerDegree));
			}
		}
	}

	public static File loadResourceAsFile(String urlStr) {
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

	private static ArrayList<ArrayList<PointD>> getPolygons(File kml) {
		if(kml == null) {
			return new ArrayList<ArrayList<PointD>>();
		}

		Pattern p = Pattern.compile("<coordinates>.*?</coordinates>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		ArrayList<String> coordList = new ArrayList<>();

		while (m.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			coordList.add(m.group().substring(13, m.group().length() - 14));
		}

		ArrayList<ArrayList<PointD>> polygons = new ArrayList<>();

		for (String coords : coordList) {
			Scanner sc = new Scanner(coords);
			sc.useDelimiter(" ");

			ArrayList<PointD> polygon = new ArrayList<>();

			while (sc.hasNext()) {
				String s = sc.next();
				// System.out.println(s);

				String[] pp = s.split(",");

				if (pp.length >= 2 && pp[0].length() > 0 && pp[1].length() > 0) {
				} else
					continue;

				polygon.add(new PointD(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
			}

			sc.close();
			polygons.add(polygon);
		}

		return polygons;
	}

	private static ArrayList<ArrayList<PointD>> getPolygons(File kml, int reduction) {
		if(kml == null) {
			return new ArrayList<ArrayList<PointD>>();
		}
		
		Pattern p = Pattern.compile("<coordinates>.*?</coordinates>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		ArrayList<String> coordList = new ArrayList<>();

		while (m.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			coordList.add(m.group().substring(13, m.group().length() - 14));
		}

		ArrayList<ArrayList<PointD>> polygons = new ArrayList<>();

		for (String coords : coordList) {
			Scanner sc = new Scanner(coords);
			sc.useDelimiter(" ");

			ArrayList<PointD> polygon = new ArrayList<>();

			int vertices = 0;
			while (sc.hasNext()) {
				String s = sc.next();
				// System.out.println(s);

				String[] pp = s.split(",");

				if (pp.length >= 2 && pp[0].length() > 0 && pp[1].length() > 0) {
				} else
					continue;

				if (vertices % reduction == 0)
					polygon.add(new PointD(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
				vertices++;
			}

			sc.close();
			polygons.add(polygon);
		}

		return polygons;
	}

	public static ArrayList<ArrayList<PointD>> getCountyBorders() {
		return countyBorders;
	}

	public static ArrayList<ArrayList<PointD>> getStateBorders() {
		return stateBorders;
	}

	public static ArrayList<ArrayList<PointD>> getInterstates() {
		return interstates;
	}

	public static ArrayList<ArrayList<PointD>> getMajorRoads() {
		return majorRoads;
	}

	public static ArrayList<ArrayList<PointD>> getEstados() {
		return estados;
	}

	public static ArrayList<ArrayList<PointD>> getCanadianProvinces() {
		return canadianProvinces;
	}

	public static ArrayList<ArrayList<PointD>> getCanadianProvincesSubd() {
		return canadianProvincesSubd;
	}
	
	public static BufferedImage getRadarImageForMapInset() {
		if (imagesMd == null) {
			return new BufferedImage(2000, 2000, BufferedImage.TYPE_4BYTE_ABGR);
		} else {
			return imagesMd[RadarView.chosenTimestep][0];
		}
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
}
