package com.ameliaWx.radarView;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

public class ColorScale {
	// write this to read in *.pal files, unmodified. use sam emmerson's colortables
	// as tests

	private static final int NUM_MASKS = 12; // number of precip types supported
	private Color[][] colors = new Color[NUM_MASKS][];
	private boolean[] masksUsed = new boolean[NUM_MASKS];
	private Color noData;
	private Color rangeFolded;

	private float ndValue = -1024;
	private float rfValue = -2048;

	private float vmax;
	private float vmin;
	private String units;

	private float interval;

	private float scale = 1;

	// for testing
	public static void main(String[] args) throws IOException {
		ColorScale aruRefl = new ColorScale(loadResourceAsFile("res/aruReflLowFilter.pal"), 0.1f, 10, "dBZ");

		ImageIO.write(aruRefl.drawColorLegend(200, 650, 67, true), "PNG", new File("aruReflLowFilter.png"));
	}

	public ColorScale(File f, float resolution, float interval, String units) {
		this.units = units;
		this.interval = interval;

		Scanner sc = null;
		try {
			sc = new Scanner(f);
		} catch (FileNotFoundException e) {
			vmax = 0.0f;
			vmin = 0.0f;

			e.printStackTrace();
		}

		ArrayList<String[]> tokensList = new ArrayList<>();

		while (sc.hasNextLine()) {
			String line = sc.nextLine();

			if (line.length() == 0)
				continue;
			if (';' == line.charAt(0))
				continue;

			if (line.toLowerCase().startsWith("color:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");

//				System.out.println(Arrays.toString(tokens));

				tokensList.add(tokens);
			}

			if (line.toLowerCase().startsWith("scale:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");

				scale = Float.valueOf(tokens[1]);
//				System.out.println("scale: " + scale);
			}

			if (line.toLowerCase().startsWith("nd:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");

				noData = new Color(Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]), Integer.valueOf(tokens[3]));
			}

			if (line.toLowerCase().startsWith("rf:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");

				rangeFolded = new Color(Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]),
						Integer.valueOf(tokens[3]));
			}
		}

		int[] colorKeyAmt = new int[NUM_MASKS];
		for (int i = 0; i < tokensList.size(); i++) {
			String[] tokens = tokensList.get(i);

			int mask = 0;

			if (tokens.length % 3 == 0) {
				String maskId = tokens[tokens.length - 1];

				mask = maskId(maskId);
			}

			int rgbValuesListed = (tokensList.get(i).length - 2) / 3;

			colorKeyAmt[mask] += rgbValuesListed;
		}

		float[][] colorKeys = new float[NUM_MASKS][];
		Color[][] colorValues = new Color[NUM_MASKS][];

		for (int i = 0; i < NUM_MASKS; i++) {
			colorKeys[i] = new float[colorKeyAmt[i]];
			colorValues[i] = new Color[colorKeyAmt[i]];
		}

		int[] colorKeysProcessed = new int[NUM_MASKS];
		for (int mask = 0; mask < NUM_MASKS; mask++) {
			for (int i = 0; i < tokensList.size(); i++) {
				String[] tokens = tokensList.get(i);
				int rgbValuesListed = (tokens.length - 2) / 3;

				int selectedMask = 0;

				if (tokens.length % 3 == 0) {
					String maskId = tokens[tokens.length - 1];

					selectedMask = maskId(maskId);
				}

				if (mask == selectedMask) {
					for (int j = 0; j < rgbValuesListed; j++) {
						colorKeys[mask][colorKeysProcessed[mask]] = Float.valueOf(tokens[1]);
						colorValues[mask][colorKeysProcessed[mask]] = new Color(Integer.valueOf(tokens[2 + 3 * j]),
								Integer.valueOf(tokens[3 + 3 * j]), Integer.valueOf(tokens[4 + 3 * j]));

						colorKeysProcessed[mask]++;
					}
				}
			}

			shiftKeys(colorKeys[mask]);
		}

		System.out.println(Arrays.toString(colorKeysProcessed));

		vmin = colorKeys[0][0];
		vmax = colorKeys[0][colorKeys[0].length - 1];

		for (int i = 0; i < NUM_MASKS; i++) {
			colors[i] = new Color[(int) Math.round((vmax - vmin) / resolution) + 1];

			masksUsed[i] = (colorValues[i].length != 0);

			for (float v = vmin; v <= vmax; v += resolution) {
				int index = (int) Math.round((v - vmin) / resolution);
				colors[i][index] = colorLerp(v, colorKeys[i], colorValues[i]);
			}
		}

		sc.close();
	}

	private static int maskId(String maskId) {
		if ("RAIN".equals(maskId)) {
			return 0;
		} else if ("FRZR_SRFC".equals(maskId)) {
			return 1;
		} else if ("ICEP".equals(maskId)) {
			return 2;
		} else if ("VERY_DRY_SNOW".equals(maskId)) {
			return 3;
		} else if ("FRZR_SNOW_MIX".equals(maskId)) {
			return 4;
		} else if ("RAIN_SNOW_MIX".equals(maskId)) {
			return 5;
		} else if ("FRZR_ELEV".equals(maskId)) {
			return 6;
		} else if ("DRY_SNOW".equals(maskId)) {
			return 7;
		} else if ("WET_SNOW".equals(maskId)) {
			return 8;
		} else if ("RAIN_ICEP_MIX".equals(maskId)) {
			return 9;
		} else if ("FRZR_ICEP_MIX".equals(maskId)) {
			return 10;
		} else if ("ICEP_SNOW_MIX".equals(maskId)) {
			return 11;
		}

		else if ("FRZR".equals(maskId)) {
			return 1;
		} else if ("SNOW".equals(maskId)) {
			return 3;
		}

		else if ("WINTRY_MIX".equals(maskId)) {
			return 1;
		}

		return 0;
	}

	// value should NEVER be outside keys. as such, handling for that case is not
	// implemented
	private Color colorLerp(float value, float[] keys, Color[] values) {
		if (values.length == 0)
			return Color.BLACK;

		for (int i = 0; i < keys.length - 1; i++) {
			if (value > keys[i + 1])
				continue;
			if (keys[i] == keys[i + 1])
				return values[i];

			double w1 = (keys[i + 1] - value) / (keys[i + 1] - keys[i]);
			double w2 = (value - keys[i]) / (keys[i + 1] - keys[i]);

			int r1 = values[i].getRed();
			int g1 = values[i].getGreen();
			int b1 = values[i].getBlue();
			int r2 = values[i + 1].getRed();
			int g2 = values[i + 1].getGreen();
			int b2 = values[i + 1].getBlue();

//			Color c = new Color((int) Math.pow(Math.sqrt(r1) * w1 + Math.sqrt(r2) * w2, 2), (int) Math.pow(Math.sqrt(g1) * w1 + Math.sqrt(g2) * w2, 2), (int) Math.pow(Math.sqrt(b1) * w1 + Math.sqrt(b2) * w2, 2));
			Color c = new Color((int) (r1 * w1 + r2 * w2), (int) (g1 * w1 + g2 * w2), (int) (b1 * w1 + b2 * w2));

			return c;
		}

		return values[values.length - 1];
	}

	private void shiftKeys(float[] keys) {
		for (int i = 0; i < keys.length - 2; i++) {
			if (keys[i] == keys[i + 1]) {
				keys[i + 1] = keys[i + 2];
				i++;
			}
		}
	}

	public Color getColor(double value) {
		return getColor(value, 0);
	}

	public Color getColor(double value, int mask) {
		if (vmax == vmin)
			return Color.BLACK;

		// if specific colors are not specified in this mask, point to the appropriate mask's colors
		if(!masksUsed[mask]) {
			if(masksUsed[2]) {
				//4-type logic
				switch(mask) {
				case 0:
					mask = 0;
					break;
				case 1:
					mask = 1;
					break;
				case 2:
					mask = 2;
					break;
				case 3:
					mask = 3;
					break;
				case 4:
					mask = 1;
					break;
				case 5:
					mask = 3;
					break;
				case 6:
					mask = 1;
					break;
				case 7:
					mask = 3;
					break;
				case 8:
					mask = 3;
					break;
				case 9:
					mask = 2;
					break;
				case 10:
					mask = 1;
					break;
				case 11:
					mask = 2;
					break;
				}
				
			} else if(masksUsed[1]) {
				//3-type logic
				switch(mask) {
				case 0:
					mask = 0;
					break;
				case 1:
					mask = 1;
					break;
				case 2:
					mask = 1;
					break;
				case 3:
					mask = 3;
					break;
				case 4:
					mask = 1;
					break;
				case 5:
					mask = 1;
					break;
				case 6:
					mask = 1;
					break;
				case 7:
					mask = 3;
					break;
				case 8:
					mask = 3;
					break;
				case 9:
					mask = 1;
					break;
				case 10:
					mask = 1;
					break;
				case 11:
					mask = 1;
					break;
				}
			} else {
				//1-type logic;
				mask = 0;
			}
		}

		if (value == ndValue)
			return noData;
		if (value == rfValue)
			return rangeFolded;

		double resolution = (vmax - vmin) / colors[mask].length;
		int index = (int) Math.round((value * scale - vmin) / resolution);

		if (index < 0)
			return colors[mask][0];
		if (index >= colors[mask].length)
			return colors[mask][colors[mask].length - 2];

		return colors[mask][index];
	}

	public double getNdValue() {
		return ndValue;
	}

	public void setNdValue(float ndValue) {
		this.ndValue = ndValue;
	}

	public double getRfValue() {
		return rfValue;
	}

	public void setRfValue(float rfValue) {
		this.rfValue = rfValue;
	}

	public BufferedImage drawColorLegend(int width, int height, int padding, boolean vertical) {
		int numMasksUsed = 0;

		for (int i = 0; i < NUM_MASKS; i++) {
			numMasksUsed += masksUsed[i] ? 1 : 0;
		}

		BufferedImage legend = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = legend.createGraphics();
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

		if (numMasksUsed == 1) {
			g.drawImage(drawColorLegendSegment(width, height, padding, padding, 1, 0, vertical), 0, 0, null);
		} else if (numMasksUsed <= 3) {
			g.drawImage(drawColorLegendSegment(width, height / 3, padding, 10, 1, 0, vertical), 0, 0, null);
			g.drawImage(drawColorLegendSegment(width, height / 3, padding, 10, 1, 1, vertical), 0, 1 * height / 3,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 3, padding, 10, 1, 3, vertical), 0, 2 * height / 3,
					null);

			drawCenteredString(g, "RAIN", new Rectangle(padding / 2, height / 6, 0, 0), g.getFont());

			drawCenteredString(g, "WINTRY",
					new Rectangle(padding / 2, 3 * height / 6 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 3 * height / 6 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "SNOW", new Rectangle(padding / 2, 5 * height / 6, 0, 0), g.getFont());
		} else if (numMasksUsed <= 4) {
			g.drawImage(drawColorLegendSegment(width, height / 4, padding, 10, 2, 0, vertical), 0, 0, null);
			g.drawImage(drawColorLegendSegment(width, height / 4, padding, 10, 2, 1, vertical), 0, 1 * height / 4,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 4, padding, 10, 2, 2, vertical), 0, 2 * height / 4,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 4, padding, 10, 2, 3, vertical), 0, 3 * height / 4,
					null);

			drawCenteredString(g, "RAIN", new Rectangle(padding / 2, height / 8, 0, 0), g.getFont());

			drawCenteredString(g, "FREEZING",
					new Rectangle(padding / 2, 3 * height / 8 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "RAIN",
					new Rectangle(padding / 2, 3 * height / 8 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "ICE",
					new Rectangle(padding / 2, 5 * height / 8 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "PELLETS",
					new Rectangle(padding / 2, 5 * height / 8 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "SNOW", new Rectangle(padding / 2, 7 * height / 8, 0, 0), g.getFont());
		} else if (numMasksUsed <= 12) {
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 0, vertical), 0, 0, null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 6, vertical), 0, 1 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 1, vertical), 0, 2 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 10, vertical), 0, 3 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 2, vertical), 0, 4 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 11, vertical), 0, 5 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 9, vertical), 0, 6 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 5, vertical), 0, 7 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 4, vertical), 0, 8 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 8, vertical), 0, 9 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 7, vertical), 0, 10 * height / 12,
					null);
			g.drawImage(drawColorLegendSegment(width, height / 12, padding, 10, 6, 3, vertical), 0, 11 * height / 12,
					null);

			drawCenteredString(g, "RAIN", new Rectangle(padding / 2, height / 24, 0, 0), g.getFont());

			drawCenteredString(g, "FREEZING",
					new Rectangle(padding / 2, 3 * height / 24 - g.getFontMetrics().getHeight(), 0, 0), g.getFont());
			drawCenteredString(g, "RAIN",
					new Rectangle(padding / 2, 3 * height / 24, 0, 0), g.getFont());
			drawCenteredString(g, "(ELEVATED)",
					new Rectangle(padding / 2, 3 * height / 24 + g.getFontMetrics().getHeight(), 0, 0), g.getFont());

			drawCenteredString(g, "FREEZING",
					new Rectangle(padding / 2, 5 * height / 24 - g.getFontMetrics().getHeight(), 0, 0), g.getFont());
			drawCenteredString(g, "RAIN",
					new Rectangle(padding / 2, 5 * height / 24, 0, 0), g.getFont());
			drawCenteredString(g, "(SURFACE)",
					new Rectangle(padding / 2, 5 * height / 24 + g.getFontMetrics().getHeight(), 0, 0), g.getFont());

			drawCenteredString(g, "FRZR-ICEP",
					new Rectangle(padding / 2, 7 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 7 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "ICE",
					new Rectangle(padding / 2, 9 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "PELLETS",
					new Rectangle(padding / 2, 9 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "ICEP-SNOW",
					new Rectangle(padding / 2, 11 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 11 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "RAIN-ICEP",
					new Rectangle(padding / 2, 13 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 13 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "RAIN-SNOW",
					new Rectangle(padding / 2, 15 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 15 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());

			drawCenteredString(g, "FRZR-SNOW",
					new Rectangle(padding / 2, 17 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "MIX",
					new Rectangle(padding / 2, 17 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			
			drawCenteredString(g, "WET SNOW", new Rectangle(padding / 2, 19 * height / 24, 0, 0), g.getFont());
			
			drawCenteredString(g, "DRY SNOW", new Rectangle(padding / 2, 21 * height / 24, 0, 0), g.getFont());

			drawCenteredString(g, "VERY DRY",
					new Rectangle(padding / 2, 23 * height / 24 - g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
			drawCenteredString(g, "SNOW",
					new Rectangle(padding / 2, 23 * height / 24 + g.getFontMetrics().getHeight() / 2, 0, 0), g.getFont());
		}

		return legend;
	}

	public BufferedImage drawColorLegendSegment(int width, int height, int padding, int hPadding, int intvMult,
			int mask, boolean vertical) {
		BufferedImage legend = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = legend.createGraphics();

		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.WHITE);
		g.drawLine(0, 0, 0, height);

		if (vertical) {
			for (int i = 0; i < height - 2 * hPadding; i++) {
				double val = vmin + (vmax - vmin) / (height - 2 * hPadding) * i;

				g.setColor(getColor(val / scale, mask));
				g.fillRect(padding, height - hPadding - i, width - 2 * padding, 1);
			}

			int numMarks = (int) Math.round((vmax - vmin) / (interval * intvMult));
			g.setColor(Color.WHITE);
			for (int i = 0; i <= numMarks; i++) {
				int y = (int) ((height - hPadding) - (height - 2.0 * hPadding) / numMarks * i);
				double val = vmin + (vmax - vmin) / numMarks * i;

				if ("C".equals(units))
					val = convertKtoC(val);
				if ("F".equals(units))
					val = convertKtoF(val);

				g.fillRect(padding, y, width - 2 * padding, 1);
				drawCenteredString(g, String.format("%6.1f " + units, val),
						new Rectangle(width - padding + 28, y, 0, 0), g.getFont());
			}
		} else {
			for (int i = 0; i < width - 2 * padding; i++) {
				double val = vmin + (vmax - vmin) / (width - 2 * padding) * i;

				g.setColor(getColor(val / scale, mask));
				g.fillRect(i + padding, padding, 1, height - 2 * padding);
			}

			int numMarks = (int) (Math.round(vmax - vmin) / interval);
			g.setColor(Color.WHITE);
			for (int i = 0; i <= numMarks; i++) {
				int y = (int) ((width - padding) - (width - 2.0 * padding) / numMarks * i);
				double val = vmin + (vmax - vmin) / numMarks * i;

				if ("C".equals(units))
					val = convertKtoC(val);
				if ("F".equals(units))
					val = convertKtoF(val);

				g.fillRect(padding, y, height - 2 * padding, 1);

				String u = (i == 0) ? " " + units : "";

				drawCenteredString(g, String.format("%6.1f" + u, val), new Rectangle(y, padding - 25, 0, 0),
						g.getFont());
			}
		}

		g.dispose();
		return legend;
	}

	/**
	 * Draw a String centered in the middle of a Rectangle.
	 *
	 * @param g    The Graphics instance.
	 * @param text The String to draw.
	 * @param rect The Rectangle to center the text in.
	 */
	public static void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
		// Get the FontMetrics
		FontMetrics metrics = g.getFontMetrics(font);
		// Determine the X coordinate for the text
		int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
		// Determine the Y coordinate for the text (note we add the ascent, as in java
		// 2d 0 is top of the screen)
		int y = rect.y + (rect.height + metrics.getHeight()) / 3;
		// Set the font
		g.setFont(font);
		// Draw the String
		g.drawString(text, x, y);
	}

	private static double convertKtoC(double k) {
		return k - 273.15;
	}

	private static double convertCtoF(double c) {
		return 1.8 * c + 32.0;
	}

	private static double convertKtoF(double k) {
		return convertCtoF(convertKtoC(k));
	}

	private static File loadResourceAsFile(String urlStr) {
//		System.out.println(urlStr);
		URL url = RadarView.class.getResource(urlStr);
//		System.out.println(url);
//		System.out.println(is);
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
}
