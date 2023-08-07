package com.ameliaWx.radarView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.Arrays;

import com.ameliaWx.soundingViewer.MapInset;

public class RadarMapInset implements MapInset {
	private BufferedImage radarImage = RadarPanel.getRadarImageForMapInset();
	private double radarLat = RadarView.radarLat;
	private double radarLon = RadarView.radarLon;

	private final BasicStroke bs = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private final BasicStroke ts = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private final BasicStroke clr = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private final BasicStroke blk = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	
	/**
	 * @param lat    Latitude of center of map
	 * @param lon    Longitude of center of map
	 * @param extent Difference in latitude between top of map and center of map, in
	 *               degrees
	 * @param size   Size of image in pixels
	 */
	public BufferedImage drawMapInset(double lat, double lon, double extent, int size) {
		BufferedImage mapInset = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = mapInset.createGraphics();

		double topLat = lat + extent;
		double bottomLat = lat - extent;
		double leftLon = lon - extent;
		double rightLon = lon + extent;

		BufferedImage states = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D gg = states.createGraphics();

		gg.setColor(new Color(255, 255, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getStateBorders()) {
			for (int i = 0; i < polygon.size(); i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get((i == polygon.size() - 1) ? 0 : (i + 1));

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}
		gg.dispose();

		BufferedImage provinces = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = provinces.createGraphics();

		gg.setColor(new Color(255, 255, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getCanadianProvinces()) {
			for (int i = 0; i < polygon.size(); i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get((i == polygon.size() - 1) ? 0 : (i + 1));

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}
		gg.dispose();

		BufferedImage estados = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = estados.createGraphics();

		gg.setColor(new Color(255, 255, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getEstados()) {
			for (int i = 0; i < polygon.size(); i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get((i == polygon.size() - 1) ? 0 : (i + 1));

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}
		gg.dispose();

		BufferedImage counties = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = counties.createGraphics();

		gg.setColor(new Color(255, 255, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getCountyBorders()) {
			for (int i = 0; i < polygon.size(); i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get((i == polygon.size() - 1) ? 0 : (i + 1));

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		BufferedImage provinceSubd = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = provinceSubd.createGraphics();

		gg.setColor(new Color(255, 255, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getCanadianProvincesSubd()) {
			for (int i = 0; i < polygon.size(); i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get((i == polygon.size() - 1) ? 0 : (i + 1));

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		BufferedImage highways = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = highways.createGraphics();

		gg.setColor(new Color(0, 0, 0));
		gg.setStroke(ts);
		for (ArrayList<PointD> polygon : RadarPanel.getInterstates()) {
			for (int i = 0; i < polygon.size() - 1; i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(i + 1);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		gg.setColor(new Color(180, 0, 0));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getInterstates()) {
			for (int i = 0; i < polygon.size() - 1; i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(i + 1);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		BufferedImage roads = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = roads.createGraphics();

		gg.setColor(new Color(0, 0, 0));
		gg.setStroke(ts);
		for (ArrayList<PointD> polygon : RadarPanel.getMajorRoads()) {
			for (int i = 0; i < polygon.size() - 1; i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(i + 1);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		gg.setColor(new Color(127, 127, 255));
		gg.setStroke(bs);
		for (ArrayList<PointD> polygon : RadarPanel.getMajorRoads()) {
			for (int i = 0; i < polygon.size() - 1; i++) {
				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(i + 1);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		BufferedImage warnings = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = warnings.createGraphics();

		gg.setStroke(blk);

		for (int p = 0; p < RadarView.warningPolygons.size(); p++) {
			ArrayList<PointD> polygon = RadarView.warningPolygons.get(p);

			gg.setColor(Color.BLACK);

			for (int i = 0; i < polygon.size(); i++) {
				int j = i + 1;
				if (j == polygon.size())
					j = 0;

				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(j);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		gg.setStroke(clr);

		for (int p = 0; p < RadarView.warningPolygons.size(); p++) {
			ArrayList<PointD> polygon = RadarView.warningPolygons.get(p);
			String name = RadarView.warningNames.get(p).substring(5, 7);
			String nameW = RadarView.warningNames.get(p).trim();

			if ("TO".equals(name)) {
				gg.setColor(Color.RED);
			} else if ("SV".equals(name)) {
				gg.setColor(Color.YELLOW);
			} else if ("FF".equals(name)) {
				gg.setColor(Color.GREEN);
			} else if ("MA".equals(name)) {
				gg.setColor(new Color(255, 127, 0));
			} else if ("Snow Squall Warning".equals(nameW)) {
				gg.setColor(new Color(0, 255, 255));
			} else if ("Flood Warning".equals(nameW)) {
				gg.setColor(new Color(0, 170, 0));
			}

			for (int i = 0; i < polygon.size(); i++) {
				int j = i + 1;
				if (j == polygon.size())
					j = 0;

				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(j);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

				boolean renderP1 = (lon1 >= leftLon && lon1 <= rightLon && lat1 >= bottomLat && lat1 <= topLat);
				boolean renderP2 = (lon2 >= leftLon && lon2 <= rightLon && lat2 >= bottomLat && lat2 <= topLat);

				if (renderP1 || renderP2) {
					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
				}
			}
		}

		BufferedImage spcWatches = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
		gg = spcWatches.createGraphics();
		
		BasicStroke clr2 = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		BasicStroke blk2 = new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

		gg.setStroke(blk2);

		for (int p = 0; p < RadarView.watchParallelograms.size(); p++) {
			ArrayList<PointD> polygon = RadarView.watchParallelograms.get(p);
			String name = RadarView.spcWatchNames.get(p).trim();

			System.out.println("intrf: " + name);
			System.out.println("intrf: " + Arrays.toString(polygon.toArray()));

			gg.setColor(new Color(0, 0, 0, 0));
			if ("TOR".equals(name))
				gg.setColor(new Color(0, 0, 0, 255));
			if ("SVR".equals(name))
				gg.setColor(new Color(0, 0, 0, 255));

			for (int i = 0; i < polygon.size(); i++) {
				int j = i + 1;
				if (j == polygon.size())
					j = 0;

				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(j);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
			}
		}

		gg.setStroke(clr2);

		for (int p = 0; p < RadarView.watchParallelograms.size(); p++) {
			ArrayList<PointD> polygon = RadarView.watchParallelograms.get(p);
			String name = RadarView.spcWatchNames.get(p).trim();

			gg.setColor(new Color(0, 0, 0, 0));
			if ("TOR".equals(name))
				gg.setColor(new Color(255, 64, 64, 255));
			if ("SVR".equals(name))
				gg.setColor(new Color(255, 255, 64, 255));

			for (int i = 0; i < polygon.size(); i++) {
				int j = i + 1;
				if (j == polygon.size())
					j = 0;

				PointD p1 = polygon.get(i);
				PointD p2 = polygon.get(j);

				double lat1 = p1.getY();
				double lon1 = p1.getX();
				double lat2 = p2.getY();
				double lon2 = p2.getX();

					int x1 = (int) linScale(leftLon, rightLon, 0, size, lon1);
					int y1 = (int) linScale(topLat, bottomLat, 0, size, lat1);
					int x2 = (int) linScale(leftLon, rightLon, 0, size, lon2);
					int y2 = (int) linScale(topLat, bottomLat, 0, size, lat2);

					// System.out.println(leftLon + "\t" + rightLon + "\t" + lon1 + "\t" + x1 + "\t"
					// + y1 + "\t" + size);

					gg.drawLine(x1, y1, x2, y2);
			}
		}

		double lonStretchFactor = Math.cos(Math.toRadians(radarLat));

		double radialLat1 = 1000.0 * 0.25 / 111.32;
		double radialLon1 = radialLat1 / lonStretchFactor;

		double northLat1 = radarLat + radialLat1;
		double southLat1 = radarLat - radialLat1;
		double eastLon1 = radarLon + radialLon1;
		double westLon1 = radarLon - radialLon1;

		int ulX1 = (int) linScale(leftLon, rightLon, 0, size, westLon1);
		int lrX1 = (int) linScale(leftLon, rightLon, 0, size, eastLon1);
		int ulY1 = (int) linScale(topLat, bottomLat, 0, size, northLat1);
		int lrY1 = (int) linScale(topLat, bottomLat, 0, size, southLat1);

		g.drawImage(radarImage, ulX1, ulY1, lrX1, lrY1, 0, 0, 2001, 2001, null);

		/*
		 * Create a rescale filter op that makes the image 50% opaque.
		 */
		float[] scales = { 1f, 1f, 1f, 0.4f };
		float[] offsets = new float[4];
		RescaleOp rop = new RescaleOp(scales, offsets, null);
		float[] scales2 = { 1f, 1f, 1f, 0.3f };
		float[] offsets2 = new float[4];
		RescaleOp rop2 = new RescaleOp(scales2, offsets2, null);

		g.drawImage(roads, rop2, 0, 0);
		g.drawImage(highways, 0, 0, null);
		g.drawImage(provinceSubd, rop, 0, 0);
		g.drawImage(counties, rop, 0, 0);
		g.drawImage(estados, 0, 0, null);
		g.drawImage(provinces, 0, 0, null);
		g.drawImage(states, 0, 0, null);
		g.drawImage(spcWatches, 0, 0, null);
		g.drawImage(warnings, 0, 0, null);

		g.setColor(new Color(0, 0, 0));
		g.setStroke(ts);
		g.drawLine(size / 2 - 10, size / 2, size / 2 - 5, size / 2);
		g.drawLine(size / 2, size / 2 - 10, size / 2, size / 2 - 5);
		g.drawLine(size / 2 + 5, size / 2, size / 2 + 10, size / 2);
		g.drawLine(size / 2, size / 2 + 5, size / 2, size / 2 + 10);
		g.drawOval(size / 2 - 5, size / 2 - 5, 10, 10);
		g.drawLine(size / 2, size / 2, size / 2, size / 2);

		g.setColor(new Color(255, 0, 0));
		g.setStroke(bs);
		g.drawLine(size / 2 - 10, size / 2, size / 2 - 5, size / 2);
		g.drawLine(size / 2, size / 2 - 10, size / 2, size / 2 - 5);
		g.drawLine(size / 2 + 5, size / 2, size / 2 + 10, size / 2);
		g.drawLine(size / 2, size / 2 + 5, size / 2, size / 2 + 10);
		g.drawOval(size / 2 - 5, size / 2 - 5, 10, 10);
		g.drawLine(size / 2, size / 2, size / 2, size / 2);

		return mapInset;
	}

	private double linScale(double preMin, double preMax, double postMin, double postMax, double value) {
		double slope = (postMax - postMin) / (preMax - preMin);

		return slope * (value - preMin) + postMin;
	}
}
