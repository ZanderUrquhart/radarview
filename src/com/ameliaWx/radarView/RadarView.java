package com.ameliaWx.radarView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.radarView.nwpModel.LambertConformalProjection;
import com.ameliaWx.radarView.nwpModel.NwpField;
import com.ameliaWx.radarView.nwpModel.RapInterpModel;
import com.ameliaWx.radarView.nwpModel.RapModel;
import com.ameliaWx.radarView.srtm.SrtmModel2;
import com.ameliaWx.soundingViewer.Sounding;
import com.ameliaWx.soundingViewer.SoundingFrame;
import com.ameliaWx.weatherUtils.WeatherUtils;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class RadarView extends JFrame {
	/**
	 * IMPORTANT: only show color legend in single panel mode
	 * 
	 * refactor polygon lists with ArrayList<ArrayList<PointD>> into
	 * ArrayList<Polygon>
	 * 
	 * BEFORE RELEASE OF BETA VERSION 4 MAKE IT SO YOU CAN EDIT THE HOME RADAR, TIME ZONE, AND HOME MARKER
	 * 
	 * TRY TO FIGURE OUT WHY IT BREAKS WHILE ATTEMPTING TO READ CONFIG.TXT ON WINDOWS
	 * 
	 * show user-marked locations if map is zoomed in enough, maybe >850 ppd
	 * 
	 * rewrite the ptype algorithm, try to get it fast, still do outside of main
	 * thread anyway
	 * 
	 * still need to implement bourgouin 2000, urquhart experimental 2023
	 * 
	 * work on spanish language support, lay groundwork for future languages
	 * 
	 * modify urquhart experimental 2023 (maybe bourgouin extended) to look for
	 * initial precip layer and precip type
	 */

	private static final long serialVersionUID = -4570229750715867902L;

	public static String dataFolder = System.getProperty("user.home") + "/Documents/RadarView/data/";
	public static ArrayList<String> tempFilesToDelete = new ArrayList<>();

	public static SrtmModel2 srtm;

	public static String chosenRadar = "KTLX";
	public static ArrayList<RadarSite> radarSites = new ArrayList<>();
	public static ArrayList<String> radarCodes = new ArrayList<>();

	public static ArrayList<City> cities = new ArrayList<>();

	public static double homeLat;
	public static double homeLon;

	public static DateTimeZone timeZone = DateTimeZone.forID("America/Chicago");
	public static String timeZoneCode = "CST";

	public static String[] fieldChoices;
	public static Field chosenField = Field.REFL_BASE;

	public static String[] tiltChoices;
	public static Tilt chosenTilt = Tilt._1;

	public static ColorScale[] colors;

	public static RadarData[] radarData;
	public static String[] radarDataFileNames;

	public static ColorScale reflectivityColors;
	public static ColorScale reflectivityColorsLowFilter;
	public static ColorScale refl03PTypesColors;
	public static ColorScale refl04PTypesColors;
	public static ColorScale refl12PTypesColors;
	public static ColorScale velocityColors;
	public static ColorScale specWdthColors;
	public static ColorScale diffReflColors;
	public static ColorScale corrCoefColors;
	public static ColorScale diffPhseColors;
	public static ColorScale kdpColors;

	public static ColorScale testColors1;
	public static ColorScale testColors2;

	public static double centralLat = 33;
	public static double centralLon = -96.5;
	public static double pixelsPerDegree = 200.0;

	public static double radarLat = 32.57322872964769; // falls back to the KFWS site in case of catastrophic errors
	public static double radarLon = -97.30326251795913;

	public static int chosenTimestep = 0;
	public static boolean animationMode = false;
	public static int animLength = 1;
	public static int animLengthWhenPlaying = 6;
	public static String mostRecentFilename = "";

	public static RadarView instance;

	public static boolean lowFilterOn = true;
	public static boolean inspectToolActive = false;

	public static boolean viewCities = true;
	
	public static boolean isWindows = false;

	public static void main(String[] args) {
//		try {
//			RadarPanel.drawWorldMap();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		String osName = System.getProperty("os.name");
		
		if(osName.contains("Windows")) {
			isWindows = true;
		}
		
		System.out.println(osName);
		System.out.println(isWindows);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Shutdown Hook");

				for (String file : tempFilesToDelete) {
					System.out.println(file.toString());
					new File(file).delete();
				}

				new File(dataFolder + "temp/day1otlk_cat.kml").delete();
				new File(dataFolder + "temp/warnings.kml").delete();
				new File(dataFolder + "temp/wwa.kmz").delete();
				new File(dataFolder + "temp/wwa/legend.png").delete();
				new File(dataFolder + "temp/wwa/noaa_logo.jpg").delete();
				new File(dataFolder + "temp/wwa/wwa.kml").delete();
				new File(dataFolder + "temp/spcStormReports.kmz").delete();
			}
		}));

		instance = new RadarView();
	}

	public static RVGraphics g = new RVGraphics();

	public static boolean runBigPolarTest = false;

	private static RefreshTimerThread refreshThread;

	public static JFrame loadWindow = new JFrame("Initializing RadarView...");

	public RadarView() {
		long initStartTime = System.currentTimeMillis();

		loadWindow.setSize(400, 0);
		loadWindow.setLocationRelativeTo(null);
		loadWindow.setVisible(true);

		fieldChoices = new String[Field.values().length];
		for (int i = 0; i < fieldChoices.length; i++) {
			fieldChoices[i] = Field.values()[i].name;
		}

		loadWindow.setTitle("Initializing RadarView: Loading radar sites...");
		loadRadarSites();

		loadWindow.setTitle("Initializing RadarView: Loading cities...");
		loadCities();

		if (new File(dataFolder + "config.txt").exists()) {
			loadWindow.setTitle("Initializing RadarView: Loading config.txt...");
			System.out.println("Reading config.txt...");
			readConfigTxt();
		} else {
			loadWindow.setTitle("Initializing RadarView: Asking for config info...");
			createConfigTxt();
		}

		RadarSite activeRadarSite = radarSites.get(radarCodes.indexOf(chosenRadar));

		radarLat = (centralLat = activeRadarSite.getSiteCoords().getX());
		radarLon = (centralLon = activeRadarSite.getSiteCoords().getY());

		loadWindow.setTitle("Initializing RadarView: Loading KMLs...");

		try {
			getSevereWarnings();
			getSevereWatches();
			getSpcDay1Outlook();
			getSpcStormReports();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		panel1 = new RadarPanel();
		panel2 = new RadarPanel();
		panel3 = new RadarPanel();
		panel4 = new RadarPanel();

		loadWindow.setTitle("Initializing RadarView: Loading Color Scales...");

		reflectivityColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruRefl.pal"), 0.1f, 10, "dBZ");
		reflectivityColorsLowFilter = new ColorScale(RadarPanel.loadResourceAsFile("res/aruReflLowFilter.pal"), 0.1f,
				10, "dBZ");
		refl03PTypesColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruRefl-3Ptypes.pal"), 0.1f, 10, "dBZ");
		refl04PTypesColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruRefl-4Ptypes.pal"), 0.1f, 10, "dBZ");
		refl12PTypesColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruRefl-12Ptypes.pal"), 0.1f, 10, "dBZ");
		velocityColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruVlcy.pal"), 0.1f, 20, "mph");
		specWdthColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruSpwd.pal"), 0.1f, 10, "mph");
		diffReflColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruDrfl.pal"), 0.01f, 1, "dBZ");
		corrCoefColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruCrcf.pal"), 0.001f, 0.1f, "");
		diffPhseColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruDphs.pal"), 0.1f, 30, "°");
		kdpColors = new ColorScale(RadarPanel.loadResourceAsFile("res/aruKdp.pal"), 0.01f, 1, "°/km");

		testColors1 = new ColorScale(RadarPanel.loadResourceAsFile("res/aruReflSnow.pal"), 0.1f, 10, "dBZ");
		testColors2 = new ColorScale(RadarPanel.loadResourceAsFile("res/aruReflMix.pal"), 0.1f, 10, "dBZ");

		colors = new ColorScale[] { reflectivityColorsLowFilter, velocityColors, specWdthColors, diffReflColors,
				corrCoefColors, diffPhseColors, kdpColors, refl04PTypesColors, testColors1, testColors2 };

		loadWindow.setTitle("Initializing RadarView: Running Polar-projected Image Test...");

//		BufferedImage polarProjT = drawPolarProjImage(radarData[0].getReflectivity()[0], 4001, 2, reflectivityColors);
//		BufferedImage polarProjTHR = drawPolarProjImage(radarData[0].getReflectivity()[0], 4001, 4, reflectivityColors);
//		try {
//			ImageIO.write(polarProjT, "PNG", new File("polarProj-KFWS-live.png"));
//			ImageIO.write(polarProjTHR, "PNG", new File("polarProj-KFWS-live-hires.png"));
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}

		if (runBigPolarTest) {
			try {
				@SuppressWarnings("deprecation")
				NetcdfFile ncfile = NetcdfFile.open("/home/a-urq/eclipse-workspace/RadarViewTakeThree/KEWX-tornado");

				List<Variable> vars = ncfile.getVariables();

				for (Variable v : vars) {
					System.out.println(v.getNameAndDimensions());
				}

				Variable baseRefl = ncfile.findVariable("RadialVelocity_HI");
				Variable baseReflAzi = ncfile.findVariable("azimuthV_HI");
				int[] shape = baseRefl.getShape();
				Array _data = null;
				Array _azi = null;

				System.out.println(baseRefl);

				_data = baseRefl.read();
				_azi = baseReflAzi.read();

				double[][] azi = new double[shape[0]][shape[1]];
				for (int h = 0; h < _azi.getSize(); h++) {
					int i = h / (shape[1]);
					int j = h % shape[1];

					azi[i][j] = _azi.getDouble(h);
				}

				double[][][] __data = new double[shape[0]][shape[1]][shape[2]];
				for (int h = 0; h < _data.getSize(); h++) {
					int i = h / (shape[1] * shape[2]);
					int j = h / (shape[2]) % shape[1];
					int k = h % shape[2];

					__data[i][(int) Math.floor(2.0 * azi[i][j])][k] = vlcyPostProc(_data.getDouble(h));

					if (__data[i][j][k] == -64.5)
						__data[i][j][k] = -1024.0;
					if (__data[i][j][k] == -64.0)
						__data[i][j][k] = -2048.0;
				}

				double[][][] data = __data;

				System.out.println(data[0][360][500]);

				for (int i = 0; i < data.length; i++) {
					BufferedImage baseReflImg = new BufferedImage(shape[1], shape[2], BufferedImage.TYPE_3BYTE_BGR);
					Graphics2D g = baseReflImg.createGraphics();

					for (int j = 0; j < data[i].length; j++) {
						for (int k = 0; k < data[i][j].length; k++) {

							g.setColor(velocityColors.getColor(data[i][j][k]));
							g.fillRect(j, k, 1, 1);
						}
					}

					ImageIO.write(baseReflImg, "PNG", new File("diff-refl-rv_tilt-" + i + ".png"));
				}

				for (int t = 0; t < shape[0]; t++) {
					BufferedImage polarProj = drawPolarProjImage(data[t], new int[data[0].length][data[0][0].length],
							4001, 2, velocityColors);

					ImageIO.write(polarProj, "PNG", new File("polarProjKEWX-vel_tornado-tilt-" + t + ".png"));
				}

				for (int t = 0; t < shape[0]; t++) {
					BufferedImage polarProj = drawPolarProjImage(data[t], new int[data[0].length][data[0][0].length],
							4001, 4, velocityColors);

					ImageIO.write(polarProj, "PNG", new File("polarProjKEWX-vel-hires_tornado-tilt-" + t + ".png"));
				}

				for (int t = 0; t < shape[0]; t++) {
					BufferedImage polarProj = drawPolarProjImage(data[t], new int[data[0].length][data[0][0].length],
							4001, 1, velocityColors);

					ImageIO.write(polarProj, "PNG", new File("polarProjKEWX-vel-lores_tornado-tilt-" + t + ".png"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.exit(0);
		}

		loadWindow.setTitle("Initializing RadarView: Loading NEXRAD data get test...");
		downloadNexradData(chosenRadar, animLength);
		try {
			downloadThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		loadWindow.setTitle("Initializing RadarView: Creating App Window...");

		this.setSize(1600, 900);
		this.setTitle("RadarView");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		g.addMouseListener(new RVMouseListener());
		g.addMouseMotionListener(new RVMouseMotionListener());
		g.addMouseWheelListener(new RVMouseWheelListener());

		this.add(g);

		this.addKeyListener(new RVKeyListener());

		double width = 1600;
		double height = 900;

		if (radarData[0] != null) {
			RadarView.radarData[0].markDataAsChanged();
		}

		System.out.println("radarData.length: " + radarData.length);
		if (radarData[0] != null) {
			for (int i = 0; i < radarData.length; i++) {
				loadWindow.setTitle(
						"Initializing RadarView: Rendering Radar Images(" + (i + 1) + "/" + radarData.length + ")...");
				System.out.println(
						"Initializing RadarView: Rendering Radar Images(" + (i + 1) + "/" + radarData.length + ")...");

				System.out.println("drawing panel " + (i + 1));

				RadarView.radarData[0].markDataAsChanged();

				panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
						centralLat + ((height) / 2) / pixelsPerDegree,
						centralLon + ((width - 200) / 2) / pixelsPerDegree,
						centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, i,
						radarData[i].getData()[chosenField.dataLocation], radarData[i].getPtype(),
						radarData[i].getScanTime(), radarData[i].getStormMotionDirection(),
						radarData[i].getStormMotionSpeed(), chosenField, radarLon, radarLat, i);
			}
		}

		g.repaint();

		final boolean testMultiFrame = false;

		if (testMultiFrame) {
			loadWindow.setTitle("Initializing RadarView: Testing Multi-Frame...");

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			for (int i = 0; i < radarData.length; i++) {
				loadWindow.setTitle(
						"Initializing RadarView: Testing Multi-Frame (" + (i + 1) + "/" + (radarData.length) + ")...");

				try {
					ImageIO.write(panel1.getMdResImage(i, chosenField, chosenTilt), "PNG",
							new File("test-multiframe-" + String.format("%02d", i) + ".png"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

//		chosenRadar = "KENX";
//		
//		try {
//			radarData[0] = new RadarData("/home/a-urq/eclipse-workspace/RadarViewTakeThree/KENX-severe-thundersnow.nexrad", "20191230 202112");
//			mostRecentFilename = "KENX-severe-thundersnow.nexrad";
//
//			RadarPanel.dataHasChanged = true;
//
//			int i = 0;
//			panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
//					centralLat + ((height) / 2) / pixelsPerDegree,
//					centralLon + ((width - 200) / 2) / pixelsPerDegree,
//					centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree,
//					radarData[i].getData()[chosenField.dataLocation], radarData[i].getScanTime(),
//					radarData[i].getStormMotionDirection(), radarData[i].getStormMotionSpeed(), chosenField,
//					radarLon, radarLat, i);
//			
//			System.out.println("assigned historical data");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		loadWindow.dispose();
		this.setVisible(true);

		refreshThread = new RefreshTimerThread();
		refreshThread.start();

		new CheckOnlineThread().start();

		long initFinishTime = System.currentTimeMillis();

		System.out.println("Initialization Time: " + (initFinishTime - initStartTime) / 1000.0 + " sec");
	}

	public static boolean refreshThreadSuspended;

	private static BufferedImage icon;
	private static boolean iconSet = false;

	private static class RefreshTimerThread extends Thread {
		private static final int REFRESH_INTERVAL = 30000; // milliseconds

		private static RefreshWorkerThread workerThread;

		private static boolean firstLoop = true;

		public RefreshTimerThread() {
			super(new Runnable() {
				@Override
				public void run() {
					while (true) {
						if (firstLoop)
							downloadModelData();

						try {
							Thread.sleep(REFRESH_INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						workerThread = new RefreshWorkerThread();
						workerThread.start();
					}
				}
			});
		}

		private static class RefreshWorkerThread extends Thread {
			public void run() {
				if (!iconSet) {
					try {
						icon = ImageIO.read(RadarPanel.loadResourceAsFile("res/radarview-supercell.ico"));
						instance.setIconImage(icon);
						iconSet = true;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

				if (!refreshThreadSuspended) {
					downloadNexradData(chosenRadar, animLength);
					downloadModelData();

					try {
						getSevereWarnings();
						getSevereWatches();
						getSpcDay1Outlook();
						getSpcStormReports();
					} catch (IOException e) {
						e.printStackTrace();
					}

					RadarView.radarData[0].markDataAsChanged();

					panel1.drawPanel(centralLon - ((g.getWidth() - 200) / 2) / pixelsPerDegree,
							centralLat + ((g.getHeight()) / 2) / pixelsPerDegree,
							centralLon + ((g.getWidth() - 200) / 2) / pixelsPerDegree,
							centralLat - ((g.getHeight()) / 2) / pixelsPerDegree, pixelsPerDegree, 0,
							radarData[0].getData()[chosenField.dataLocation], radarData[0].getPtype(),
							radarData[0].getScanTime(), radarData[0].getStormMotionDirection(),
							radarData[0].getStormMotionSpeed(), chosenField, radarLon, radarLat, 0);

					redrawPanels();

					g.repaint();
				}
			}
		}
	}

	private static Thread drawPanelThread;

	public static void redrawPanels() {
		drawPanelThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int width = g.getWidth();
				int height = g.getHeight();

				if (radarData == null) {
					for (int i = 0; i < radarData.length; i++) {
						panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
								centralLat + ((height) / 2) / pixelsPerDegree,
								centralLon + ((width - 200) / 2) / pixelsPerDegree,
								centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, -1,
								new RadarData().getData()[chosenField.dataLocation], new RadarData().getPtype(),
								new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC), -1, -1, chosenField, radarLon,
								radarLat, 0);
					}
				} else {
					for (int i = 0; i < radarData.length; i++) {
						if (radarData[i] == null) {
							panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
									centralLat + ((height) / 2) / pixelsPerDegree,
									centralLon + ((width - 200) / 2) / pixelsPerDegree,
									centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, -1,
									new RadarData().getData()[chosenField.dataLocation], new RadarData().getPtype(),
									new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC), -1, -1, chosenField, radarLon,
									radarLat, 0);
						} else {
//							RadarView.radarData[i].markDataAsChanged();
							panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
									centralLat + ((height) / 2) / pixelsPerDegree,
									centralLon + ((width - 200) / 2) / pixelsPerDegree,
									centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, i,
									radarData[i].getData()[chosenField.dataLocation], radarData[i].getPtype(),
									radarData[i].getScanTime(), radarData[i].getStormMotionDirection(),
									radarData[i].getStormMotionSpeed(), chosenField, radarLon, radarLat, i);
						}

						g.repaint();
					}
				}
			}
		});

		drawPanelThread.start();
	}

	private static class CheckOnlineThread extends Thread {
		private static final int REFRESH_INTERVAL = 120000; // milliseconds

		public CheckOnlineThread() {
			super(new Runnable() {
				@Override
				public void run() {
					while (true) {
						checkSitesOnline();

						g.repaint();

						try {
							Thread.sleep(REFRESH_INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		public static void checkSitesOnline() {
			try {
				DateTime now = DateTime.now(DateTimeZone.UTC);
				downloadFile("https://radar2pub.ncep.noaa.gov/", "nexradOnlineStatus.html");

				File f = new File(dataFolder + "nexradOnlineStatus.html");
				Scanner sc = new Scanner(f);
				String contents = "";

				while (sc.hasNextLine()) {
					contents += sc.nextLine() + "\n";
				}
				sc.close();
				f.delete();

				Pattern tableCellPattern = Pattern.compile("<td .*?<\\/td>");
				Matcher tableCellMatcher = tableCellPattern.matcher(contents);

				ArrayList<String> tableCellHtml = new ArrayList<>();

				while (tableCellMatcher.find()) {
					String tableCell = tableCellMatcher.group();
					tableCellHtml.add(tableCell);
				}

				HashMap<String, Integer> nexradOnlineStatus = new HashMap<>();
				for (String tableCell : tableCellHtml) {
					Pattern nexradSitePattern = Pattern.compile("TITLE=\".*?\"");
					Matcher nexradSiteMatcher = nexradSitePattern.matcher(tableCell);

					Pattern nexradStatusPattern = Pattern.compile("<br>.*<\\/span>");
					Matcher nexradStatusMatcher = nexradStatusPattern.matcher(tableCell);

					Pattern nexradBgColorPattern = Pattern.compile("BGCOLOR=\".*?\"");
					Matcher nexradBgColorMatcher = nexradBgColorPattern.matcher(tableCell);

					String nexradSite = "-";
					if (nexradSiteMatcher.find()) {
						nexradSite = nexradSiteMatcher.group();
						nexradSite = nexradSite.substring(7, nexradSite.length() - 1);
					}

					String nexradStatus = "-";
					if (nexradStatusMatcher.find()) {
						nexradStatus = nexradStatusMatcher.group();
						nexradStatus = nexradStatus.substring(4, nexradStatus.length() - 7);
					}

					String nexradBgColor = "-";
					if (nexradBgColorMatcher.find()) {
						nexradBgColor = nexradBgColorMatcher.group();
						nexradBgColor = nexradBgColor.substring(9, nexradBgColor.length() - 1);
					}

					if ("#FF0000".equals(nexradBgColor)) {
						nexradOnlineStatus.put(nexradSite, 2);
					} else {
						int hr = Integer.valueOf(nexradStatus.substring(0, 2));
						int mn = Integer.valueOf(nexradStatus.substring(3, 5));
						int se = Integer.valueOf(nexradStatus.substring(6, 8));

						DateTime scanTime = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), hr,
								mn, se, DateTimeZone.UTC);
						long scanTimeMillis = scanTime.getMillis();
						long nowMillis = now.getMillis();

						long millisDiff = nowMillis - scanTimeMillis;

//						System.out.println(nexradSite + "\t" + millisDiff/1000.0/60.0);

						if (millisDiff < 0) {
							nexradOnlineStatus.put(nexradSite, 2);
						} else if (millisDiff > 1000 * 60 * 30) {
							nexradOnlineStatus.put(nexradSite, 2);
						} else if (millisDiff > 1000 * 60 * 10) {
							nexradOnlineStatus.put(nexradSite, 1);
						} else {
							nexradOnlineStatus.put(nexradSite, 0);
						}
					}
				}

//				System.out.println(Arrays.toString(nexradOnlineStatus.keySet().toArray()));

				for (RadarSite r : radarSites) {
//					System.out.println(r.getSiteCode());
					int onlineStatus = nexradOnlineStatus.get(r.getSiteCode());
					switch (onlineStatus) {
					case 0:
						r.setOnline(true);
						r.setWarned(false);
//						System.out.println(r.getSiteCode() + " is ONLINE");
						break;
					case 1:
						r.setOnline(true);
						r.setWarned(true);
//						System.out.println(r.getSiteCode() + " is WARNED");
						break;
					case 2:
						r.setOnline(false);
						r.setWarned(false);
//						System.out.println(r.getSiteCode() + " is OFFLINE");
						break;
					default:
						r.setOnline(true);
						r.setWarned(false);
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static AnimateThread animateThread = null;
	private static boolean playingAnimation = false;

	private static class AnimateThread extends Thread {
		private static int frameInterval = 125; // milliseconds

		public AnimateThread() {
			super(new Runnable() {
				@Override
				public void run() {
					while (true) {
						if (playingAnimation) {
							if (RadarView.chosenTimestep > 0) {
								RadarView.chosenTimestep--;
							} else {
								try {
									Thread.sleep(2 * frameInterval);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

								RadarView.chosenTimestep = RadarView.radarData.length - 1;

								while (true) {
									if (radarData[chosenTimestep] != null) {
										break;
									}

									chosenTimestep--;
								}
							}

							g.repaint();
						}

						try {
							Thread.sleep(frameInterval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		public void setFrameInterval(int intv) {
			frameInterval = intv;
		}
	}

	private static int[][] azimuths;

	private static BufferedImage drawPolarProjImage(double[][] data, int[][] mask, int size, double res,
			ColorScale colors) {
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

	private static RadarPanel panel1; // only implement single panel view at first
	private static RadarPanel panel2;
	private static RadarPanel panel3;
	private static RadarPanel panel4;

	// when writing frontend listeners, make it so basemap updating is ALWAYS
	// handled off of the main thread
	// make it so if the map area updates in the middle of a basemap render, replace
	// the current render thread with a new thread
	private static final int WIDTH_PADDING = 200;

	static class RVGraphics extends JComponent {
		private static final long serialVersionUID = -3102979302944927428L;

		public static BufferedImage mapWithOverlays = null;
		public static int mwoX1 = -1;
		public static int mwoX2 = -1;
		public static int mwoY1 = -1;
		public static int mwoY2 = -1;

		public static Thread mwoDrawThread;

		public static final Font CITY_FONT = new Font(Font.MONOSPACED, Font.BOLD, 18);
		public static final Font TOWN_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

		public void paintComponent(Graphics g) {
			System.out.println("Start paint method");
			System.out.println("ppd: " + pixelsPerDegree);

			Graphics2D g2d = (Graphics2D) g;

			int width = this.getWidth();
			int height = this.getHeight();

//			long paintStartTime = System.currentTimeMillis();

			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, width, height);

//			long drawPanelStartTime = System.currentTimeMillis();
//			System.out.println(radarData[0]);
//			System.out.println(radarData.length);

			if (radarData == null) {
				for (int i = 0; i < radarData.length; i++) {
					panel1.drawBasemap(centralLon - ((width - 200) / 2) / pixelsPerDegree,
							centralLat + ((height) / 2) / pixelsPerDegree,
							centralLon + ((width - 200) / 2) / pixelsPerDegree,
							centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree,
							new RadarData().getData()[chosenField.dataLocation], new RadarData().getPtype(),
							new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC), -1, -1, chosenField, radarLon,
							radarLat, 0);
				}
			} else {
				for (int i = 0; i < 1; i++) {
					if (radarData[i] == null) {
						panel1.drawBasemap(centralLon - ((width - 200) / 2) / pixelsPerDegree,
								centralLat + ((height) / 2) / pixelsPerDegree,
								centralLon + ((width - 200) / 2) / pixelsPerDegree,
								centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree,
								new RadarData().getData()[chosenField.dataLocation], new RadarData().getPtype(),
								new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC), -1, -1, chosenField, radarLon,
								radarLat, 0);
					} else {
						panel1.drawBasemap(centralLon - ((width - 200) / 2) / pixelsPerDegree,
								centralLat + ((height) / 2) / pixelsPerDegree,
								centralLon + ((width - 200) / 2) / pixelsPerDegree,
								centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree,
								radarData[i].getData()[chosenField.dataLocation], radarData[i].getPtype(),
								radarData[i].getScanTime(), radarData[i].getStormMotionDirection(),
								radarData[i].getStormMotionSpeed(), chosenField, radarLon, radarLat, i);
					}
				}
			}

//			long drawPanelEndTime = System.currentTimeMillis();
//			System.out.println("drawPanel exec time: " + (drawPanelEndTime - drawPanelStartTime) + " ms");

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

			double ulLon = centralLon - ((width - 200) / 2) / pixelsPerDegree;
			double ulLat = centralLat + ((height) / 2) / pixelsPerDegree;
			double lrLon = centralLon + ((width - 200) / 2) / pixelsPerDegree;
			double lrLat = centralLat - ((height) / 2) / pixelsPerDegree;

//			System.out.println(radialLat1);
//			System.out.println(radialLon1);

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

//			System.out.println("eastLon1: " + eastLon1);
//			System.out.println("westLon1: " + westLon1);
//			System.out.println("avg?east-westLon1: " + (eastLon1 + westLon1) / 2);
//			System.out.println("radarLon: " + radarLon);
//
//			System.out.println(ulLon);
//			System.out.println(lrLon);
//			System.out.println(westLon1);

			int ulX0 = (int) linScale(ulLon, lrLon, 0, width - 200, westLon0);
			int lrX0 = (int) linScale(ulLon, lrLon, 0, width - 200, eastLon0);
			int ulY0 = (int) linScale(ulLat, lrLat, 0, height, northLat0);
			int lrY0 = (int) linScale(ulLat, lrLat, 0, height, southLat0);

			int ulX1 = (int) linScale(ulLon, lrLon, 0, width - 200, westLon1);
			int lrX1 = (int) linScale(ulLon, lrLon, 0, width - 200, eastLon1);
			int ulY1 = (int) linScale(ulLat, lrLat, 0, height, northLat1);
			int lrY1 = (int) linScale(ulLat, lrLat, 0, height, southLat1);

			int ulX2 = (int) linScale(ulLon, lrLon, 0, width - 200, westLon2);
			int lrX2 = (int) linScale(ulLon, lrLon, 0, width - 200, eastLon2);
			int ulY2 = (int) linScale(ulLat, lrLat, 0, height, northLat2);
			int lrY2 = (int) linScale(ulLat, lrLat, 0, height, southLat2);

			int ulX3 = (int) linScale(ulLon, lrLon, 0, width - 200, westLon3);
			int lrX3 = (int) linScale(ulLon, lrLon, 0, width - 200, eastLon3);
			int ulY3 = (int) linScale(ulLat, lrLat, 0, height, northLat3);
			int lrY3 = (int) linScale(ulLat, lrLat, 0, height, southLat3);

			int bUlX = (int) linScale(ulLon, lrLon, 0, width - 200, RadarPanel.basemapUlLon);
			int bLrX = (int) linScale(ulLon, lrLon, 0, width - 200, RadarPanel.basemapLrLon);
			int bUlY = (int) linScale(ulLat, lrLat, 0, height, RadarPanel.basemapUlLat);
			int bLrY = (int) linScale(ulLat, lrLat, 0, height, RadarPanel.basemapLrLat);

			int wUlX = (int) linScale(ulLon, lrLon, 0, width - 200, RadarPanel.wwaUlLon);
			int wLrX = (int) linScale(ulLon, lrLon, 0, width - 200, RadarPanel.wwaLrLon);
			int wUlY = (int) linScale(ulLat, lrLat, 0, height, RadarPanel.wwaUlLat);
			int wLrY = (int) linScale(ulLat, lrLat, 0, height, RadarPanel.wwaLrLat);

//			System.out.println("ulX1: " + ulX1);
//			System.out.println("lrX1: " + lrX1);
//			System.out.println("ulY1: " + ulY1);
//			System.out.println("lrY1: " + lrY1);
//			System.out.println();
//			System.out.println("cx1: " + (ulX1 + lrX1) / 2);
//			System.out.println("cy1: " + (ulY1 + lrY1) / 2);
//			System.out.println("rX1: " + rX1);
//			System.out.println("rY1: " + rY1);

//			long drawImageStartTime = System.currentTimeMillis();
			g2d.drawImage(panel1.getLoResImage(chosenTimestep, chosenField, Tilt._1), ulX0, ulY0, lrX0, lrY0, 0, 0,
					2001, 2001, null);
			if (lrY1 - ulY1 > 1000) {
				g2d.drawImage(panel1.getMdResImage(chosenTimestep, chosenField, Tilt._1), ulX1, ulY1, lrX1, lrY1, 0, 0,
						2001, 2001, null);
			}
			if (lrY2 - ulY2 > 1000) {
				g2d.drawImage(panel1.getHiResImage(chosenTimestep, chosenField, Tilt._1), ulX2, ulY2, lrX2, lrY2, 0, 0,
						2001, 2001, null);
			}
			if (lrY3 - ulY3 > 1000) {
				g2d.drawImage(panel1.getVhResImage(chosenTimestep, chosenField, Tilt._1), ulX3, ulY3, lrX3, lrY3, 0, 0,
						2001, 2001, null);
			}

			BufferedImage basemap = panel1.getBasemap();
			if (basemap != null) {
				g2d.drawImage(basemap, bUlX, bUlY, bLrX, bLrY, 0, 0, basemap.getWidth(), basemap.getHeight(), null);
			}

//			if(panel1.getImage(0, Field.REFL_BASE, Tilt._1) != null) {
//				try {
//					ImageIO.write(panel1.getImage(0, Field.REFL_BASE, Tilt._1), "PNG", new File("polarProj-" + chosenRadar + "-live.png"));
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}

			g2d.drawImage(RadarPanel.warnings, wUlX, wUlY, wLrX, wLrY, 0, 0, RadarPanel.warnings.getWidth(),
					RadarPanel.warnings.getHeight(), null);
			g2d.drawImage(RadarPanel.watches, wUlX, wUlY, wLrX, wLrY, 0, 0, RadarPanel.watches.getWidth(),
					RadarPanel.watches.getHeight(), null);
			g2d.drawImage(RadarPanel.spcOutlook, wUlX, wUlY, wLrX, wLrY, 0, 0, RadarPanel.spcOutlook.getWidth(),
					RadarPanel.spcOutlook.getHeight(), null);
			g2d.drawImage(RadarPanel.spcReports, wUlX, wUlY, wLrX, wLrY, 0, 0, RadarPanel.spcReports.getWidth(),
					RadarPanel.spcReports.getHeight(), null);
			g2d.drawImage(RadarPanel.buttonImg, wUlX, wUlY, wLrX, wLrY, 0, 0, RadarPanel.buttonImg.getWidth(),
					RadarPanel.buttonImg.getHeight(), null);

			// draw cities
			if (viewCities) {
				for (City c : cities) {
					String name = c.getName();
					double lat = c.getLatitude();
					double lon = c.getLongitude();
					int pop = c.getPopulation();
					double prm = c.getProminence();

					if (pixelsPerDegree < 25) {
						continue;
					}
					if (pixelsPerDegree < 100 && prm < 5) {
						continue;
					}
					if (pixelsPerDegree < 200 && prm < 1) {
						continue;
					}
					if (pixelsPerDegree < 400 && prm < 0.47) {
						continue;
					}
					if (pixelsPerDegree < 600 && prm < 0.25) {
						continue;
					}
					if (pixelsPerDegree < 800 && prm < 0.1) {
						continue;
					}
					if (pixelsPerDegree < 1000 && prm < 0.05) {
						continue;
					}
					if (pixelsPerDegree < 1200 && prm < 0.025) {
						continue;
					}
					if (pixelsPerDegree < 1400 && prm < 0.01) {
						continue;
					}
					if (pixelsPerDegree < 2000 && prm < 0.0001) {
						continue;
					}

					g.setFont((pop > 100000) ? CITY_FONT : TOWN_FONT);

					if (lon > ulLon && lon < lrLon && lat < ulLat && lat > lrLat) {
						int cityX = (int) linScale(ulLon, lrLon, 0, width - 200, lon);
						int cityY = (int) linScale(ulLat, lrLat, 0, height, lat);

						g.setColor(Color.BLACK);
//						drawCenteredString(name, g2d, cityX - 1, cityY - 1);
						drawCenteredString(name, g2d, cityX + 0, cityY - 1);
//						drawCenteredString(name, g2d, cityX + 1, cityY - 1);
						drawCenteredString(name, g2d, cityX - 1, cityY + 0);
						drawCenteredString(name, g2d, cityX + 1, cityY + 0);
//						drawCenteredString(name, g2d, cityX - 1, cityY + 1);
						drawCenteredString(name, g2d, cityX + 0, cityY + 1);
//						drawCenteredString(name, g2d, cityX + 1, cityY + 1);
						g.setColor(Color.WHITE);
						drawCenteredString(name, g2d, cityX, cityY);
					}
				}
			}

			// draw home location
			if (homeLat != -1024.0 && homeLon != -1024.0) {
				int homeX = (int) linScale(ulLon, lrLon, 0, width - 200, homeLon);
				int homeY = (int) linScale(ulLat, lrLat, 0, height, homeLat);

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

			if (inspectToolActive) {
				g2d.setFont(RadarPanel.CAPTION_FONT);
				FontMetrics fm = g.getFontMetrics();
				squareCrosshair(g2d, mx, my, 10, new Color(128, 255, 255));

				double mLon = linScale(0, width - 200, ulLon, lrLon, mx);
				double mLat = linScale(0, height, ulLat, lrLat, my);

				String data = RadarView.radarData[chosenTimestep].inspect(mLat, mLon);
				if (my - 25 - (fm.getAscent() + fm.getDescent()) < 5) {
					if (mx + 15 + fm.stringWidth(data) + 10 > width - 205) {
						g2d.setStroke(AS);
						g2d.setColor(Color.BLACK);
						g2d.fillRect(mx - 25 - fm.stringWidth(data), my + 15, fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);

						g2d.setColor(Color.WHITE);
						g2d.drawRect(mx - 25 - fm.stringWidth(data), my + 15, fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);
						g2d.drawString(data, mx - 20 - fm.stringWidth(data),
								my + 20 + (fm.getAscent() + fm.getDescent()));
					} else {
						g2d.setStroke(AS);
						g2d.setColor(Color.BLACK);
						g2d.fillRect(mx + 15, my + 15, fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);

						g2d.setColor(Color.WHITE);
						g2d.drawRect(mx + 15, my + 15, fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);
						g2d.drawString(data, mx + 20, my + 20 + (fm.getAscent() + fm.getDescent()));
					}
				} else {
					if (mx + 15 + fm.stringWidth(data) + 10 > width - 205) {
						g2d.setStroke(AS);
						g2d.setColor(Color.BLACK);
						g2d.fillRect(mx - 25 - fm.stringWidth(data), my - 25 - (fm.getAscent() + fm.getDescent()),
								fm.stringWidth(data) + 10, fm.getAscent() + fm.getDescent() + 10);

						g2d.setColor(Color.WHITE);
						g2d.drawRect(mx - 25 - fm.stringWidth(data), my - 25 - (fm.getAscent() + fm.getDescent()),
								fm.stringWidth(data) + 10, fm.getAscent() + fm.getDescent() + 10);
						g2d.drawString(data, mx - 20 - fm.stringWidth(data), my - 20);
					} else {
						g2d.setStroke(AS);
						g2d.setColor(Color.BLACK);
						g2d.fillRect(mx + 15, my - 25 - (fm.getAscent() + fm.getDescent()), fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);

						g2d.setColor(Color.WHITE);
						g2d.drawRect(mx + 15, my - 25 - (fm.getAscent() + fm.getDescent()), fm.stringWidth(data) + 10,
								fm.getAscent() + fm.getDescent() + 10);
						g2d.drawString(data, mx + 20, my - 20);
					}
				}
			}

			drawScrollBar(g2d);

			g2d.drawImage(colors[chosenField.colorLocation].drawColorLegend(200, height, 67, true), width - 200, 0,
					null);
			g2d.drawImage(panel1.getCaptionImg(chosenTimestep), 0, height - 31, null);

			if (loadingMessage.length() > 0) {
				g.setColor(Color.BLACK);
				g.setFont(RadarPanel.CAPTION_FONT);
				FontMetrics fm = g.getFontMetrics();
				g.fillRect(width - 208 - fm.stringWidth(loadingMessage), height - 13,
						fm.stringWidth(loadingMessage) + 8, 13);

				g.setColor(Color.WHITE);
				drawRightAlignedString(RadarView.loadingMessage.toUpperCase(), g2d, width - 203, height - 1);
			}

//			try {
//				ImageIO.write(RadarPanel.watches, "PNG", new File("watches.png"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

//			long drawImageEndTime = System.currentTimeMillis();
//			System.out.println("drawImage exec time: " + (drawImageEndTime - drawImageStartTime) + " ms");

			// use for memory consumption measurement
//			Runtime.getRuntime().gc();

			long maxMemory = Runtime.getRuntime().maxMemory();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

//			System.out.println(convToGigaMega(maxMemory));
//			System.out.println(convToGigaMega(usedMemory));
//			System.out.printf("%4.1f", 100.0 * usedMemory / maxMemory);
//			System.out.println("%");

			instance.setTitle("RadarView Beta Version 4-dev (" + String.format("%4.1f", 100.0 * usedMemory / maxMemory)
					+ "%, " + convToGigaMega(usedMemory).trim() + ")");
//			long paintEndTime = System.currentTimeMillis();
//			System.out.println("paint exec time: " + (paintEndTime - paintStartTime) + " ms");
		}

		private void drawScrollBar(Graphics2D g) {
			int width = RadarView.g.getWidth() - 200;
			int height = RadarView.g.getHeight();

			if (animLength > 1 && width > 2 * WIDTH_PADDING) {
				g.setColor(new Color(0, 0, 0, 100));

				g.fillRect(WIDTH_PADDING - 12, height - 70, width - 2 * WIDTH_PADDING + 24, 40);

				g.setColor(Color.WHITE);

				g.fillRect(WIDTH_PADDING - 2, height - 52, width - 2 * WIDTH_PADDING + 4, 4);

				for (int i = 0; i < animLength; i++) {
					int x = (int) linScale(0, animLength - 1, width - WIDTH_PADDING, WIDTH_PADDING, i);

					if (i == chosenTimestep) {
						g.setColor(Color.BLUE);

						g.fillRect(x - 4, height - 64, 8, 28);
					} else {
						if (i < radarData.length) {
							if (radarData[i] == null) {
								g.setColor(Color.GRAY);
							} else {
								g.setColor(Color.WHITE);
							}
						} else {
							g.setColor(Color.GRAY);
						}

						g.fillRect(x - 2, height - 62, 4, 24);
					}
				}
			}
		}

		private double linScale(double preMin, double preMax, double postMin, double postMax, double value) {
			double slope = (postMax - postMin) / (preMax - preMin);

			return slope * (value - preMin) + postMin;
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

		private static final BasicStroke AS = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		private static final BasicStroke BS = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		private static final BasicStroke TS = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		private static void squareCrosshair(Graphics2D g, int x, int y, int size, Color color) {
			g.setStroke(TS);
			g.setColor(Color.BLACK);
			g.drawLine(x - size, y - size, x + size, y - size);
			g.drawLine(x + size, y - size, x + size, y + size);
			g.drawLine(x + size, y + size, x - size, y + size);
			g.drawLine(x - size, y + size, x - size, y - size);
			g.drawLine(x, y + size / 2, x, y + 3 * size / 2);
			g.drawLine(x, y - size / 2, x, y - 3 * size / 2);
			g.drawLine(x + size / 2, y, x + 3 * size / 2, y);
			g.drawLine(x - size / 2, y, x - 3 * size / 2, y);

			g.setStroke(BS);
			g.setColor(color);
			g.drawLine(x - size, y - size, x + size, y - size);
			g.drawLine(x + size, y - size, x + size, y + size);
			g.drawLine(x + size, y + size, x - size, y + size);
			g.drawLine(x - size, y + size, x - size, y - size);
			g.drawLine(x, y + size / 2, x, y + 3 * size / 2);
			g.drawLine(x, y - size / 2, x, y - 3 * size / 2);
			g.drawLine(x + size / 2, y, x + 3 * size / 2, y);
			g.drawLine(x - size / 2, y, x - 3 * size / 2, y);
		}

		public static void drawCenteredString(String s, Graphics2D g, int x, int y) {
			FontMetrics fm = g.getFontMetrics();
			int ht = fm.getAscent() + fm.getDescent();
			int width = fm.stringWidth(s);
			g.drawString(s, x - width / 2, y + (fm.getAscent() - ht / 2));
		}

		public static void drawRightAlignedString(String s, Graphics2D g, int x, int y) {
			FontMetrics fm = g.getFontMetrics();
			int width = fm.stringWidth(s);
			g.drawString(s, x - width, y);
		}
	}

	public static int numPtypes = 4;

	private class RVKeyListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int key = e.getKeyCode();

			System.out.println(key);

			switch (key) {
			case KeyEvent.VK_A:
				boolean changeAnimLength = (animLength == animLengthWhenPlaying);

				// keeps it off main thread so program still responds
				new Thread(new Runnable() {
					@Override
					public void run() {
						animLengthWhenPlaying = Integer.valueOf(JOptionPane
								.showInputDialog("How many animation frames would you like to use? (default is 6)"));
						if (changeAnimLength) {
							animLength = animLengthWhenPlaying;

							downloadNexradData(chosenRadar, animLength);
							g.repaint();

//							JOptionPane.showMessageDialog(null, "The data download is finished.");
						}
					}
				}).start();

//				if (changeAnimLength)
//					JOptionPane.showMessageDialog(null,
//							"The data download may take a while. You will be notified when it is finished.");
				break;
			case KeyEvent.VK_C:
				viewCities = !viewCities;
				g.repaint();
				break;
			case KeyEvent.VK_F:
				if (e.isControlDown()) {
					int newFramerate = Integer.valueOf(JOptionPane
							.showInputDialog("How many frames per second should play while the radar is animating?"));
					int intervalMs = (int) (1000.0 / (newFramerate));

					animateThread.setFrameInterval(intervalMs);
				} else {
					// keeps it off main thread so program still responds
					new Thread(new Runnable() {
						@Override
						public void run() {
							chooseField();
							g.repaint();
						}
					}).start();
				}
				break;
			case KeyEvent.VK_H:
				openHelpPage();
				break;
			case KeyEvent.VK_I:
				inspectToolActive = !inspectToolActive;

				if (inspectToolActive) {
					g.setCursor(blankCursor);
				} else {
					g.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

				RadarView.g.repaint();
				break;
			case KeyEvent.VK_L:
				lowFilterOn = !lowFilterOn;
				if (lowFilterOn) {
					colors[0] = reflectivityColorsLowFilter;
				} else {
					colors[0] = reflectivityColors;
				}

				for (int i = 0; i < RadarView.radarData.length; i++) {
					RadarView.radarData[i].markDataAsChanged();
				}
				redrawPanels();

				g.repaint();

				break;
			case KeyEvent.VK_R:
				viewSpcStormReports = !viewSpcStormReports;
				g.repaint();
				break;
			case KeyEvent.VK_S:
				viewSpcOutlook = !viewSpcOutlook;
				g.repaint();
				break;
			case KeyEvent.VK_V:
				chooseWatchWarningMode();
				break;
			case KeyEvent.VK_LEFT:
				chosenTimestep++;
				if (chosenTimestep > animLength - 1) {
					chosenTimestep = 0;
				}

				if (radarData[chosenTimestep] == null) {
					chosenTimestep = 0;
				}

				g.repaint();

				break;
			case KeyEvent.VK_RIGHT:
				chosenTimestep--;
				if (chosenTimestep < 0) {
					chosenTimestep = animLength - 1;
				}

				while (true) {
					if (radarData[chosenTimestep] != null) {
						break;
					}

					chosenTimestep--;
				}

				g.repaint();

				break;
			case KeyEvent.VK_SPACE:
				if (e.isControlDown()) {
					if (animateThread == null) {
						animateThread = new AnimateThread();
						animateThread.start();
					}

					playingAnimation = !playingAnimation;
				} else {
					animationMode = !animationMode;

					if (animationMode) {
						// keeps it off main thread so program still responds
						new Thread(new Runnable() {
							@Override
							public void run() {
								animLength = animLengthWhenPlaying;
								downloadNexradData(chosenRadar, animLength);
								g.repaint();
//								JOptionPane.showMessageDialog(null, "The data download is finished.");
							}
						}).start();

						try {
							Thread.sleep(5);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						g.repaint();

//						JOptionPane.showMessageDialog(null,
//								"The data download may take a while. You will be notified when it is finished.");
					} else {
						panel1.copyToArraysOfNewLength(1);
						animLength = 1;
						playingAnimation = false;

						g.repaint();
					}
				}
				break;
			case KeyEvent.VK_2:
				if (e.isControlDown()) {
					RadarView.colors[7] = RadarView.refl12PTypesColors;

					for (int i = 0; i < RadarView.radarData.length; i++) {
						RadarView.radarData[i].markDataAsChanged();
					}
					RadarView.redrawPanels();
					numPtypes = 12;
				}
				break;
			case KeyEvent.VK_3:
				if (e.isControlDown()) {
					RadarView.colors[7] = RadarView.refl03PTypesColors;

					for (int i = 0; i < RadarView.radarData.length; i++) {
						RadarView.radarData[i].markDataAsChanged();
					}
					RadarView.redrawPanels();
					numPtypes = 3;
				}
				break;
			case KeyEvent.VK_4:
				if (e.isControlDown()) {
					RadarView.colors[7] = RadarView.refl04PTypesColors;

					for (int i = 0; i < RadarView.radarData.length; i++) {
						RadarView.radarData[i].markDataAsChanged();
					}
					RadarView.redrawPanels();
					numPtypes = 4;
				}
				break;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	}

	private static int mPressedX = -1;
	private static int mPressedY = -1;

	private static int mx = -1;
	private static int my = -1;

	private static BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
	public static Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0),
			"blank cursor");

	private class RVMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			double westLon = centralLon - ((g.getWidth() - 200) / 2) / pixelsPerDegree;
			double eastLon = centralLon + ((g.getWidth() - 200) / 2) / pixelsPerDegree;
			double northLat = centralLat + ((g.getHeight()) / 2) / pixelsPerDegree;
			double southLat = centralLat - ((g.getHeight()) / 2) / pixelsPerDegree;

			double lon = linScale(0, g.getWidth() - 200, westLon, eastLon, x);
			double lat = linScale(0, g.getHeight(), northLat, southLat, y);

			if (SwingUtilities.isLeftMouseButton(e)) {
				double rLonTolerance = 18 / pixelsPerDegree;
				double rLatTolerance = 8 / pixelsPerDegree;

				System.out.printf("%5.2f\t%5.2f\n", lat, lon);

				for (int i = 0; i < radarSites.size(); i++) {
					RadarSite r = radarSites.get(i);
					PointD rP = r.getSiteCoords();

					double rLon = rP.getY();
					double rLat = rP.getX();
//				System.out.printf("%5.2f\t%5.2f\t%5.2f\t%5.2f\t%5.2f\t%5.2f\n", rLat, rLon, westLon, eastLon, northLat, southLat);
//				System.out.println(r.getSiteCode() + "\t" + (rLon > westLon) + "\t" + (rLon < eastLon) + "\t" + (rLat > southLat) + "\t" + (rLat < northLat));

					if (rLon > westLon && rLon < eastLon && rLat > southLat && rLat < northLat) {
						if (Math.abs(lat - rLat) < rLatTolerance && Math.abs(lon - rLon) < rLonTolerance) {
							animLength = 1;
							prevAnimLength = 1;
							
							panel1.blankOutImages();
							System.out.println("blanking out images");

							List<String> nexradFiles = null;
							try {
								nexradFiles = listNexradFiles(r.getSiteCode());
							} catch (IOException e1) {
								e1.printStackTrace();
								return;
							}

							if (nexradFiles.size() == 0) {
								JOptionPane.showMessageDialog(null,
										"This radar site is offline and no data currently exists for it.");
								return;
							}

							String mostRecentTimestamp = nexradFiles.get(0).substring(nexradFiles.get(0).length() - 19,
									nexradFiles.get(0).length() - 4);

							String year = mostRecentTimestamp.substring(0, 4);
							String month = mostRecentTimestamp.substring(4, 6);
							String day = mostRecentTimestamp.substring(6, 8);
							String hour = mostRecentTimestamp.substring(9, 11);
							String minute = mostRecentTimestamp.substring(11, 13);
							String second = mostRecentTimestamp.substring(13, 15);

							DateTime scanTime = new DateTime(Integer.valueOf(year), Integer.valueOf(month),
									Integer.valueOf(day), Integer.valueOf(hour), Integer.valueOf(minute),
									Integer.valueOf(second), DateTimeZone.UTC);

							if (scanTime.plusMinutes(12).isBeforeNow()) {
								new Thread(new Runnable() {
									@Override
									public void run() {
										JOptionPane.showMessageDialog(null,
												"This radar site is offline. Data exists for it, but be advised that it is old.");
									}
								}).start();
							}

							chosenRadar = r.getSiteCode();

							radarLat = (centralLat = rLat);
							radarLon = (centralLon = rLon);

							chosenTimestep = 0;

							panel1.copyToArraysOfNewLength(1);

							if (!chosenRadar.equals(prevChosenRadar)) {
								RadarPanel.initImages();
								g.repaint();
							}

							Thread t = new Thread(new Runnable() {
								@Override
								public void run() {
									downloadNexradData(chosenRadar, animLength);

									try {
										downloadThread.join();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

									int width = g.getWidth();
									int height = g.getHeight();

									for (int t = 0; t < radarData.length; t++) {
										RadarView.radarData[t].markDataAsChanged();

										panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
												centralLat + ((height) / 2) / pixelsPerDegree,
												centralLon + ((width - 200) / 2) / pixelsPerDegree,
												centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, t,
												radarData[t].getData()[chosenField.dataLocation],
												radarData[t].getPtype(), radarData[t].getScanTime(),
												radarData[t].getStormMotionDirection(),
												radarData[t].getStormMotionSpeed(), chosenField, radarLon, radarLat, t);
									}

									g.repaint();

									refreshThread = new RefreshTimerThread();
									refreshThread.start();
								}
							});

							t.start();

							break;
						}
					}
				}
			} else if (SwingUtilities.isRightMouseButton(e)) {
				System.out.printf("display right click RAP sounding at [%7.4f, %7.4f]\n", lat, lon);

				DateTime scanTime = radarData[chosenTimestep].getScanTime();

				HashMap<NwpField, Float> sounding0 = null; // non-interpolated sounding before scan time
				HashMap<NwpField, Float> soundingM = null; // interpolated sounding at scan time
				HashMap<NwpField, Float> sounding1 = null; // non-interpolated sounding after scan time

				DateTime soundingTime0 = null;
				DateTime soundingTimeM = null;
				DateTime soundingTime1 = null;

				if (scanTime.isAfter(time1)) {
					sounding0 = modelI1.getDataForSounding(time1, lat, lon);
					soundingM = modelI1.getDataForSounding(scanTime, lat, lon);
					sounding1 = modelI1.getDataForSounding(time2, lat, lon);

					soundingTime0 = time1;
					soundingTimeM = scanTime;
					soundingTime1 = time2;
				} else {
					if (scanTime.isBefore(time0)) {
						int confirm = JOptionPane.showConfirmDialog(null,
								"Requesting sounding from before earliest downloaded RAP data. \nProceed and view earliest downloaded data?",
								"Requested data not downloaded", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);

						if (confirm == 0) {
							sounding0 = modelI0.getDataForSounding(time0, lat, lon);
							soundingM = modelI0.getDataForSounding(time0, lat, lon);
							sounding1 = modelI0.getDataForSounding(time1, lat, lon);

							soundingTime0 = time0;
							soundingTimeM = time0;
							soundingTime1 = time1;
						} else {
							return;
						}
					} else {
						sounding0 = modelI0.getDataForSounding(time0, lat, lon);
						soundingM = modelI0.getDataForSounding(scanTime, lat, lon);
						sounding1 = modelI0.getDataForSounding(time1, lat, lon);

						soundingTime0 = time0;
						soundingTimeM = scanTime;
						soundingTime1 = time1;
					}
				}
				
				Sounding sounding0Obj = hashMapToNwpSounding(sounding0);
				Sounding soundingMObj = hashMapToNwpSounding(soundingM);
				Sounding sounding1Obj = hashMapToNwpSounding(sounding1);

				// figure out wind offset angle
				PointD dR1 = LambertConformalProjection.rapProj.projectLatLonToIJ(lon, lat);
				PointD dR2 = LambertConformalProjection.rapProj.projectLatLonToIJ(lon + 0.01, lat);
				PointD dR = PointD.subtract(dR2, dR1);
				double windOffsetAngle = Math.atan2(-dR.getY(), dR.getX());

				new SoundingFrame("RAP", sounding0Obj, soundingTime0, soundingMObj, soundingTimeM, sounding1Obj, soundingTime1, lat,
						lon, windOffsetAngle, new RadarMapInset());
			}
		}

		private Sounding hashMapToNwpSounding(HashMap<NwpField, Float> hashMap) {
			float[] pressureLevels = new float[39];
			float[] temperature = new float[39];
			float[] dewpoint = new float[38];
			float[] height = new float[39];
			float[] uWind = new float[38];
			float[] vWind = new float[38];
			float[] wWind = new float[37];

			NwpField[] fields = NwpField.values();

//			System.out.println("VERTICAL VELOCITY");
			for (int i = 0; i < 37; i++) {
				pressureLevels[i] = 10000 + 2500 * i;

				temperature[i] = hashMap.get(fields[188 - 5 * i]);

				double relativeHumidity = hashMap.get(fields[189 - 5 * i]) / 100.0;
				dewpoint[i] = (float) WeatherUtils.dewpoint(temperature[i], relativeHumidity);

				height[i] = hashMap.get(fields[190 - 5 * i]);

				uWind[i] = hashMap.get(fields[191 - 5 * i]);

				vWind[i] = hashMap.get(fields[192 - 5 * i]);
				
				wWind[i] = hashMap.get(fields[192 + 37 - i]);
//				System.out.println((192 + 37 - i) + "\t" + wWind[i]);
			}

			pressureLevels[38] = hashMap.get(NwpField.PRES_SURF);
			pressureLevels[37] = (float) WeatherUtils.pressureAtHeight(pressureLevels[38], 2);

			temperature[38] = hashMap.get(NwpField.TMP_SURF);
			temperature[37] = hashMap.get(NwpField.TMP_2M);
			dewpoint[37] = hashMap.get(NwpField.DPT_2M);

			height[38] = hashMap.get(NwpField.HGT_SURF);
			height[37] = height[38] + 2;

			uWind[37] = hashMap.get(NwpField.WIND_U_10M);
			vWind[37] = hashMap.get(NwpField.WIND_V_10M);

			for (int i = 0; i < 37; i++) {
				if (pressureLevels[i] > pressureLevels[37]) {
					pressureLevels[i] = pressureLevels[37];
					temperature[i] = temperature[37];
					dewpoint[i] = dewpoint[37];
					height[i] = height[37];
					uWind[i] = uWind[37];
					vWind[i] = vWind[37];
					wWind[i] = 0.0f;
				}
			}

			return new Sounding(pressureLevels, temperature, dewpoint, height, uWind, vWind, wWind);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			RadarPanel.drawWarningsAndWatches = false;

			if (mPressedX == -1 && mPressedY == -1) {
				mPressedX = e.getX();
				mPressedY = e.getY();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			RadarPanel.drawWarningsAndWatches = true;

			if (inspectToolActive) {
				g.setCursor(blankCursor);
			} else {
				g.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			if (mPressedX != -1 && mPressedY != -1) {
				mPressedX = -1;
				mPressedY = -1;
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}
	}

	private static class RVMouseMotionListener implements MouseMotionListener {
		private double prevX = -1024.0;
		private double prevY = -1024.0;

		private static final int SCROLLBAR_PADDING_X = 24;
		private static final int SCROLLBAR_HEIGHT = 30;

		@Override
		public void mouseDragged(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				int width = RadarView.g.getWidth() - 200;
				int height = RadarView.g.getHeight();

				int scrollbarX1 = WIDTH_PADDING - SCROLLBAR_PADDING_X;
				int scrollbarY1 = height - 50 - SCROLLBAR_HEIGHT;
				int scrollbarX2 = scrollbarX1 + width - 2 * WIDTH_PADDING + 2 * SCROLLBAR_PADDING_X;
				int scrollbarY2 = scrollbarY1 + 2 * SCROLLBAR_HEIGHT;

				int anchorL = WIDTH_PADDING;
				int anchorR = width - WIDTH_PADDING;

				if (mPressedX >= scrollbarX1 && mPressedX < scrollbarX2 && mPressedY >= scrollbarY1
						&& mPressedY < scrollbarY2) {
					int t = (int) Math.round(linScale(anchorL, anchorR, animLength - 1, 0, e.getX()));

					if (t < 0)
						t = 0;
					if (t > animLength - 1)
						t = animLength - 1;

					while (true) {
						if (radarData[t] != null) {
							break;
						}

						t--;
					}

					chosenTimestep = t;

					g.repaint();
				} else {
					g.setCursor(new Cursor(Cursor.MOVE_CURSOR));

					if (prevX != -1024.0 && prevY != -1024.0) {
						double currX = e.getX();
						double currY = e.getY();

						double deltaX = currX - prevX;
						double deltaY = currY - prevY;

						double deltaLon = deltaX / pixelsPerDegree;
						double deltaLat = deltaY / pixelsPerDegree;

						centralLon -= deltaLon;
						centralLat += deltaLat;

						g.repaint();
					}

					prevX = e.getX();
					prevY = e.getY();

					mx = e.getX();
					my = e.getY();

					if (inspectToolActive) {
						g.repaint();
					}
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			RadarPanel.drawWarningsAndWatches = true;

			prevX = e.getX();
			prevY = e.getY();

			mx = e.getX();
			my = e.getY();

			if (inspectToolActive) {
				g.repaint();
			}
		}
	}

	private static class RVMouseWheelListener implements MouseWheelListener {
		private static final double ZOOM_CONST = 0.9;

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int rotations = e.getWheelRotation();

			double distFromCenterX = e.getX() - ((g.getWidth() - 200) / 2);
			double distFromCenterY = e.getY() - (g.getHeight() / 2);

			if (rotations < 0) {
				centralLon -= Math.signum(rotations) * (1 - ZOOM_CONST) * distFromCenterX / pixelsPerDegree;
				centralLat += Math.signum(rotations) * (1 - ZOOM_CONST) * distFromCenterY / pixelsPerDegree;
			}

			pixelsPerDegree = pixelsPerDegree * Math.pow(ZOOM_CONST, rotations);

			if (rotations > 0) {
				centralLon -= Math.signum(rotations) * (1 - ZOOM_CONST) * distFromCenterX / pixelsPerDegree;
				centralLat += Math.signum(rotations) * (1 - ZOOM_CONST) * distFromCenterY / pixelsPerDegree;
			}

			g.repaint();
		}
	}

	private static final String[] timeZones = { "Hawaii Time Zone", "Aleutian Time Zone", "Alaska Time Zone",
			"Pacific Time Zone", "Mountain Time Zone", "Mountain Time Zone (Arizona)", "Central Time Zone",
			"Eastern Time Zone", "Atlantic Time Zone", "Chamorro Time Zone" };

	private static void createConfigTxt() {
		String[] radarNames = new String[radarSites.size()];

		for (int i = 0; i < radarNames.length; i++) {
			radarNames[i] = radarSites.get(i).toString();
		}

		String defaultRadar = (String) JOptionPane.showInputDialog(null,
				"What radar would you like to set as the default?\nThis can be changed at any time with Ctrl+R (not yet lmao).",
				"Choose Default Radar", JOptionPane.QUESTION_MESSAGE, null, radarNames, 0);

		chosenRadar = defaultRadar.substring(0, 4);

		String timeZone = (String) JOptionPane.showInputDialog(null,
				"What time zone would you like to use?\nThis can be changed at any time with Ctrl+T (not yet lmao).",
				"Choose Time Zone", JOptionPane.QUESTION_MESSAGE, null, timeZones, 0);

		int timeZoneIndex = -1;
		for (int i = 0; i < timeZones.length; i++) {
			if (timeZone.equals(timeZones[i])) {
				timeZoneIndex = i;
				break;
			}
		}

		int enterHomeLocation = JOptionPane.showConfirmDialog(null,
				"Would you like to specify a home location to show on the map?", "Specify Home Location?",
				JOptionPane.YES_NO_OPTION);

		double hLat = -1024.0;
		double hLon = -1024.0;

		if (enterHomeLocation == 0) {
			String hCoordStr = JOptionPane.showInputDialog(
					"Enter the latitude and longitude, in that order.\n\nRight-click over your home in Google Maps, left-click the coordinates that pop up, and then paste them into this dialog box.");

			try {
				String[] hCoord = hCoordStr.split(", ");

				hLat = Double.valueOf(hCoord[0]);
				hLon = Double.valueOf(hCoord[1]);
			} catch (NumberFormatException e) {
				hLat = -1024.0;
				hLon = -1024.0;
			}
		}

		homeLat = hLat;
		homeLon = hLon;

		try {
			PrintWriter pw = new PrintWriter(dataFolder + "config.txt");

			pw.println(defaultRadar.substring(0, 4));
			pw.println(hLat);
			pw.println(hLon);
			pw.println(timeZones[timeZoneIndex]);

			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void readConfigTxt() {
		try {
			Scanner sc = new Scanner(new File(dataFolder + "config.txt"));
			sc.useDelimiter("\n|\r\n");

			chosenRadar = sc.next();
			homeLat = sc.nextDouble();
			homeLon = sc.nextDouble();
			String timeZoneSetting = sc.next();

			System.out.println("timeZoneSetting: " + timeZoneSetting);

			if ("Hawaii Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Hawaii");
				timeZoneCode = "HST";
			} else if ("Aleutian Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Aleutian");
				timeZoneCode = "HST";
			} else if ("Alaska Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Alaska");
				timeZoneCode = "AKST";
			} else if ("Pacific Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Pacific");
				timeZoneCode = "PST";
			} else if ("Mountain Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Mountain");
				timeZoneCode = "MST";
			} else if ("Mountain Time Zone (Arizona)".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Arizona");
				timeZoneCode = "MST";
			} else if ("Central Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Central");
				timeZoneCode = "CST";
			} else if ("Eastern Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("US/Eastern");
				timeZoneCode = "EST";
			} else if ("Chamorro Time Zone".equals(timeZoneSetting)) {
				timeZone = DateTimeZone.forID("Pacific/Guam");
				timeZoneCode = "ChST";
			}

			sc.close();

			System.out.println("timeZoneCode: " + timeZoneCode);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// so far only implemented for single panel mode
	private static void chooseField() {
		String fieldChoice = (String) JOptionPane.showInputDialog(null, "What field would you like to view?",
				"Choose Field", JOptionPane.QUESTION_MESSAGE, null, fieldChoices, chosenField.id);

		for (int i = 0; i < fieldChoices.length; i++) {
			if (fieldChoice.equals(fieldChoices[i])) {
				chosenField = Field.values()[i];

				RadarView.radarData[chosenTimestep].markDataAsChanged();

				int width = g.getWidth();
				int height = g.getHeight();

				panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
						centralLat + ((height) / 2) / pixelsPerDegree,
						centralLon + ((width - 200) / 2) / pixelsPerDegree,
						centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, chosenTimestep,
						radarData[chosenTimestep].getData()[chosenField.dataLocation],
						radarData[chosenTimestep].getPtype(), radarData[chosenTimestep].getScanTime(),
						radarData[chosenTimestep].getStormMotionDirection(),
						radarData[chosenTimestep].getStormMotionSpeed(), chosenField, radarLon, radarLat,
						chosenTimestep);

				g.repaint();

				System.out.println("choose field first repaint");

				Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						// lets active image refresh first
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						for (int t = 0; t < radarData.length; t++) {
							if (t != chosenTimestep) {
								RadarView.radarData[t].markDataAsChanged();
								panel1.drawPanel(centralLon - ((width - 200) / 2) / pixelsPerDegree,
										centralLat + ((height) / 2) / pixelsPerDegree,
										centralLon + ((width - 200) / 2) / pixelsPerDegree,
										centralLat - ((height) / 2) / pixelsPerDegree, pixelsPerDegree, t,
										radarData[t].getData()[chosenField.dataLocation], radarData[t].getPtype(),
										radarData[t].getScanTime(), radarData[t].getStormMotionDirection(),
										radarData[t].getStormMotionSpeed(), chosenField, radarLon, radarLat, t);
							}
						}
					}
				});
				th.start();

				break;
			}
		}
	}

	private static final String[] wwModes = { "None", "Large-Scale Warnings Only", "Storm-Scale Warnings Only",
			"All Warnings" };

	public static boolean viewLargeScaleWarnings = false;
	public static boolean viewStormScaleWarnings = true;
	public static boolean viewSpcOutlook = false;
	public static boolean viewSpcStormReports = false;

	public static boolean viewLargeScaleWarningsPrev = false;
	public static boolean viewStormScaleWarningsPrev = true;
	public static boolean viewSpcOutlookPrev = false;
	public static boolean viewSpcStormReportsPrev = false;

	private static void chooseWatchWarningMode() {
		String viewModeChoice = (String) JOptionPane.showInputDialog(null, "Which warnings would you like to see?",
				"Choose Warning Mode", JOptionPane.QUESTION_MESSAGE, null, wwModes, 0);

		if (viewModeChoice.equals(wwModes[0])) {
			viewLargeScaleWarnings = false;
			viewStormScaleWarnings = false;
		} else if (viewModeChoice.equals(wwModes[1])) {
			viewLargeScaleWarnings = true;
			viewStormScaleWarnings = false;
		} else if (viewModeChoice.equals(wwModes[2])) {
			viewLargeScaleWarnings = false;
			viewStormScaleWarnings = true;
		} else if (viewModeChoice.equals(wwModes[3])) {
			viewLargeScaleWarnings = true;
			viewStormScaleWarnings = true;
		} else {

		}

		g.repaint();
	}

	private static String prevChosenRadar = chosenRadar;
	public static int prevAnimLength = animLength;

	private static Thread downloadThread = null;

	private static void downloadNexradData(String radarSite, int animLength) {
		downloadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					List<String> nexradFiles = listNexradFiles(radarSite);

					downloadNexradData(radarSite, animLength, nexradFiles);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		});

		downloadThread.start();
	}

	private static void downloadNexradData(String radarSite, int animLength, List<String> nexradFiles) {
		new File(dataFolder + radarSite + "-test.nexrad").delete();
		new File(dataFolder + radarSite + "-test.nexrad.uncompress").delete();
		new File(dataFolder + radarSite + "-00.nexrad").delete();
		new File(dataFolder + radarSite + "-00.nexrad.uncompress").delete();

		refreshThreadSuspended = true;

		if (radarData != null && animLength < prevAnimLength) {
			panel1.copyToArraysOfNewLength(animLength);

			prevAnimLength = animLength;
			refreshThreadSuspended = false;

			return;
		}

		try {

			assert radarData.length == radarDataFileNames.length;

			boolean testPassed = true;

			try {
				System.out.println("testing at: https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/"
						+ radarSite + "/" + nexradFiles.get(0));
				downloadFile("https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + radarSite + "/"
						+ nexradFiles.get(0), radarSite + "-test.nexrad");

				@SuppressWarnings({ "deprecation" })
				NetcdfFile testFile = NetcdfFile.open(dataFolder + radarSite + "-test.nexrad");

				testFile.findVariable("Reflectivity_HI").getShape();
				testFile.findVariable("azimuthR_HI").getShape();
				testFile.findVariable("RadialVelocity_HI").getShape();
				testFile.findVariable("azimuthV_HI").getShape();
				testFile.findVariable("SpectrumWidth_HI").getShape();
				testFile.findVariable("azimuthV_HI").getShape();
				testFile.findVariable("DifferentialReflectivity_HI").getShape();
				testFile.findVariable("azimuthD_HI").getShape();
				testFile.findVariable("CorrelationCoefficient_HI").getShape();
				testFile.findVariable("azimuthC_HI").getShape();
				testFile.findVariable("DifferentialPhase_HI").getShape();
				testFile.findVariable("azimuthP_HI").getShape();

				testFile.close();
			} catch (IOException e) {
				testPassed = false;
				System.err.println("Caught EOF!");
			} catch (NullPointerException e) {
				testPassed = false;
				System.err.println("Caught NullPtr!");
				System.out.println("test passed: " + testPassed);
			} finally {
				new File(dataFolder + radarSite + "-test.nexrad.uncompress").delete();
			}

			int testOffset = testPassed ? 0 : 1;

			System.out.println("nexrad files: " + Arrays.toString(nexradFiles.toArray()));
			System.out.println("most recent filename: " + mostRecentFilename);

			if (radarData == null || !chosenRadar.equals(prevChosenRadar)) {
				animationMode = false;

				radarData = new RadarData[animLength];
				radarDataFileNames = new String[animLength];

				for (int i = 0; i < Integer.min(animLength, nexradFiles.size()); i++) {
					if (testPassed && i == 0) {
						System.out.println("test passed");
						new File(dataFolder + radarSite + "-test.nexrad")
								.renameTo(new File(dataFolder + radarSite + "-00.nexrad"));
						System.out.println("file renamed");
					} else {
						new File(dataFolder + radarSite + "-test.nexrad").delete();
						downloadFile(
								"https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + radarSite + "/"
										+ nexradFiles.get(i + testOffset),
								radarSite + "-" + String.format("%02d", i) + ".nexrad");
					}
					String timestamp = nexradFiles.get(i).substring(nexradFiles.get(i).length() - 19,
							nexradFiles.get(i).length() - 4);

					@SuppressWarnings("deprecation")
					NetcdfFile nexradFile = NetcdfFile
							.open(dataFolder + radarSite + "-" + String.format("%02d", i) + ".nexrad");

					RadarData r = new RadarData(nexradFile, timestamp, 3, radarLat, radarLon);
					radarData[i] = r;
					System.out.println("Created RadarData[" + i + "]");
					radarDataFileNames[i] = nexradFiles.get(i);

					if (i == 0)
						mostRecentFilename = nexradFiles.get(i + testOffset);

					radarData[i].markDataAsChanged();

					if (!chosenRadar.equals(prevChosenRadar)) {
						mostRecentFilename = radarDataFileNames[0];
					}

					prevChosenRadar = chosenRadar;

					nexradFile.close();
				}
			} else {
				boolean foundInLookback = false;
				for (int lookback = 1; lookback < nexradFiles.size() - 2; lookback++) {
					if (mostRecentFilename.equals(nexradFiles.get(lookback + testOffset))) {
						System.out.println("test passed: " + testPassed);
						System.out.println("test offset: " + testOffset);

						System.out.println("lookback: " + lookback);
						for (int i = 0; i < lookback; i++) {
							System.out.println("new file name:         " + nexradFiles.get(0));
							System.out.println("most recent file name: " + mostRecentFilename);
							System.out.println("test passed: " + (testPassed));
							System.out.println("i :" + i);
//							System.out.println("file names different: "
//									+ !radarDataFileNames[i].trim().equals(nexradFiles.get(0).trim()));
							System.out.println("going to shift: "
									+ (testPassed && !radarDataFileNames[i].trim().equals(nexradFiles.get(0).trim())));
							if (i < radarDataFileNames.length) {
								if (testPassed && !radarDataFileNames[i].equals(nexradFiles.get(0))) {
									System.out.println("shifting by one");
									panel1.shiftByOne();

									mostRecentFilename = nexradFiles.get(i + testOffset);
								}
							} else {
								System.out.println("shifting by one");
								panel1.shiftByOne();

								mostRecentFilename = nexradFiles.get(i + testOffset);
							}

							if (testPassed && i == 0) {
								System.out.println("test file exists: "
										+ new File(dataFolder + radarSite + "-test.nexrad").exists());
								new File(dataFolder + radarSite + "-test.nexrad")
										.renameTo(new File(dataFolder + radarSite + "-00.nexrad"));
							} else {
								new File(dataFolder + radarSite + "-test.nexrad").delete();
								System.out.println(
										"downloading at: https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/"
												+ radarSite + "/" + nexradFiles.get(i + testOffset));
								downloadFile(
										"https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + radarSite
												+ "/" + nexradFiles.get(i + testOffset),
										radarSite + "-" + String.format("%02d", i) + ".nexrad");
							}
							String timestamp = nexradFiles.get(i + testOffset).substring(
									nexradFiles.get(i + testOffset).length() - 19,
									nexradFiles.get(i + testOffset).length() - 4);
							System.out.println("updating to timestamp: " + timestamp);

							@SuppressWarnings("deprecation")
							NetcdfFile nexradFile = NetcdfFile
									.open(dataFolder + radarSite + "-" + String.format("%02d", i) + ".nexrad");

							RadarData r = new RadarData(nexradFile, timestamp, 3, radarLat, radarLon);
							radarData[i] = r;
							System.out.println("Updated RadarData[" + i + "]");
							radarDataFileNames[i] = nexradFiles.get(i);

							radarData[i].markDataAsChanged();

//							System.out.println("dataHasChanged - outside method: " + RadarPanel.dataHasChanged);
							panel1.drawPanel(centralLon - ((g.getWidth() - 200) / 2) / pixelsPerDegree,
									centralLat + ((g.getHeight()) / 2) / pixelsPerDegree,
									centralLon + ((g.getWidth() - 200) / 2) / pixelsPerDegree,
									centralLat - ((g.getHeight()) / 2) / pixelsPerDegree, pixelsPerDegree, i,
									radarData[i].getData()[chosenField.dataLocation], radarData[i].getPtype(),
									radarData[i].getScanTime(), radarData[i].getStormMotionDirection(),
									radarData[i].getStormMotionSpeed(), chosenField, radarLon, radarLat, i);

							prevChosenRadar = chosenRadar;

							nexradFile.close();
						}
					}
				}

				if (!foundInLookback) {
					new File(dataFolder + radarSite + "-test.nexrad").delete();
					System.out.println("No new radar data detected");
				}
			}

			if (radarData != null && animLength != prevAnimLength) {
				panel1.copyToArraysOfNewLength(animLength);

				if (animLength > prevAnimLength) {
					for (int i = prevAnimLength; i < Integer.min(animLength, nexradFiles.size()); i++) {
						if (testPassed && i == 0) {
							System.out.println("test passed");
							new File(dataFolder + radarSite + "-test.nexrad")
									.renameTo(new File(dataFolder + radarSite + "-00.nexrad"));
							System.out.println("file renamed");
						} else {
							new File(dataFolder + radarSite + "-test.nexrad").delete();
							downloadFile(
									"https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + radarSite + "/"
											+ nexradFiles.get(i),
									radarSite + "-" + String.format("%02d", i) + ".nexrad");
						}
						String timestamp = nexradFiles.get(i).substring(nexradFiles.get(i).length() - 19,
								nexradFiles.get(i).length() - 4);

						@SuppressWarnings("deprecation")
						NetcdfFile nexradFile = NetcdfFile
								.open(dataFolder + radarSite + "-" + String.format("%02d", i) + ".nexrad");

						RadarData r = new RadarData(nexradFile, timestamp, 3, radarLat, radarLon);
						radarData[i] = r;
						System.out.println("Created RadarData[" + i + "]");
						radarDataFileNames[i] = nexradFiles.get(i);

						if (i == 0)
							mostRecentFilename = nexradFiles.get(i + testOffset);

						prevChosenRadar = chosenRadar;

						nexradFile.close();

						RadarView.radarData[i].markDataAsChanged();

						System.out
								.println("rendering anim frame " + i + ": " + RadarView.radarData[i].hasDataChanged());
						panel1.drawPanel(centralLon - ((g.getWidth() - 200) / 2) / pixelsPerDegree,
								centralLat + ((g.getHeight()) / 2) / pixelsPerDegree,
								centralLon + ((g.getWidth() - 200) / 2) / pixelsPerDegree,
								centralLat - ((g.getHeight()) / 2) / pixelsPerDegree, pixelsPerDegree, i,
								radarData[i].getData()[chosenField.dataLocation], radarData[i].getPtype(),
								radarData[i].getScanTime(), radarData[i].getStormMotionDirection(),
								radarData[i].getStormMotionSpeed(), chosenField, radarLon, radarLat, i);
					}

					prevAnimLength = animLength;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: Can't find NEXRAD data files!");
		}

		refreshThreadSuspended = false;
	}

	private static void getSevereWatches() throws IOException {
		downloadFile("https://www.weather.gov/source/crh/shapefiles/wwa.kmz", "temp/wwa.kmz");

		readUsingZipFile(dataFolder + "temp/wwa.kmz", dataFolder + "temp/wwa/");
		watchPolygons = getPolygonsWatches(new File(dataFolder + "temp/wwa/wwa.kml"));

		cleanUpWatches();
	}

	private static void getSevereWarnings() throws IOException {
		downloadFile("https://www.weather.gov/source/crh/shapefiles/warnings.kml", "temp/warnings.kml");
		warningPolygons = getPolygonsWarnings(new File(dataFolder + "temp/warnings.kml"));
	}

	private static void getSpcDay1Outlook() throws IOException {
		downloadFile("https://www.spc.noaa.gov/products/outlook/day1otlk_cat.kml", "temp/day1otlk_cat.kml");
		spcOutlookPolygons = getPolygonsSpcOutlook(new File(dataFolder + "temp/day1otlk_cat.kml"));
	}

	private static void getSpcStormReports() throws IOException {
		downloadFile("https://www.spc.noaa.gov/climo/reports/today.kmz", "temp/spcStormReports.kmz");

		readUsingZipFile(dataFolder + "temp/spcStormReports.kmz", dataFolder + "temp/spcStormReports/");

		DateTime today = DateTime.now(DateTimeZone.UTC);
		DateTime yesterday = DateTime.now(DateTimeZone.UTC).minusDays(1);
		DateTime ereyesterday = DateTime.now(DateTimeZone.UTC).minusDays(2);

		String todayFileId = String.format("%02d%02d%02d", today.getYearOfCentury(), today.getMonthOfYear(),
				today.getDayOfMonth());
		String yesterdayFileId = String.format("%02d%02d%02d", yesterday.getYearOfCentury(), yesterday.getMonthOfYear(),
				yesterday.getDayOfMonth());
		String ereyesterdayFileId = String.format("%02d%02d%02d", ereyesterday.getYearOfCentury(),
				ereyesterday.getMonthOfYear(), ereyesterday.getDayOfMonth());

		if (new File(dataFolder + "temp/spcStormReports/" + todayFileId + "_rpts.kml").exists()) {
			spcStormReportPoints = getPointsStormReports(
					new File(dataFolder + "temp/spcStormReports/" + todayFileId + "_rpts.kml"));

			String file = dataFolder + "temp/spcStormReports/" + todayFileId + "_rpts.kml";
			if (!tempFilesToDelete.contains(file)) {
				tempFilesToDelete.add(file);
			}
		} else if (new File(dataFolder + "temp/spcStormReports/" + yesterdayFileId + "_rpts.kml").exists()) {
			spcStormReportPoints = getPointsStormReports(
					new File(dataFolder + "temp/spcStormReports/" + yesterdayFileId + "_rpts.kml"));

			String file = dataFolder + "temp/spcStormReports/" + yesterdayFileId + "_rpts.kml";
			if (!tempFilesToDelete.contains(file)) {
				tempFilesToDelete.add(file);
			}
		} else if (new File(dataFolder + "temp/spcStormReports/" + ereyesterdayFileId + "_rpts.kml").exists()) {
			spcStormReportPoints = getPointsStormReports(
					new File(dataFolder + "temp/spcStormReports/" + ereyesterdayFileId + "_rpts.kml"));

			String file = dataFolder + "temp/spcStormReports/" + ereyesterdayFileId + "_rpts.kml";
			if (!tempFilesToDelete.contains(file)) {
				tempFilesToDelete.add(file);
			}
		}
	}

	public static ArrayList<ArrayList<PointD>> watchPolygons;
	public static ArrayList<String> watchNames = new ArrayList<>();

	private static ArrayList<ArrayList<PointD>> getPolygonsWatches(File kml) {
		watchNames = new ArrayList<>();
		Pattern p = Pattern.compile("</styleUrl><Polygon>.*?</coordinates>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		Pattern p1 = Pattern.compile("</Polygon>.*?</name>");

		Matcher m1 = p1.matcher(usingBufferedReader(kml));

		ArrayList<String> coordList = new ArrayList<>();

		while (m.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			String coords = m.group().substring(11 + 16 + 13 + 9 + 13, m.group().length() - 14);
//			System.out.println(coords);
			if ('y' != coords.charAt(0)) {
				coordList.add(coords);
			}
		}

		while (m1.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			String name = m1.group().substring(17, m1.group().length() - 7);
			if (' ' == name.charAt(0)) {
//				System.out.println(name.substring(0));
				watchNames.add(name.substring(1));
			}
		}

		System.out.println(coordList.size());

		ArrayList<ArrayList<PointD>> polygons = new ArrayList<>();

		for (String coords : coordList) {
			Scanner sc = new Scanner(coords);
			sc.useDelimiter(" ");

			ArrayList<PointD> polygon = new ArrayList<>();

			while (sc.hasNext()) {
				String s = sc.next();
				// System.out.println(s);

				String[] pp = s.split(",");

				polygon.add(new PointD(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
			}

			sc.close();
			polygons.add(polygon);
		}

		System.out.println(polygons.size());

		return polygons;
	}

	private static void cleanUpWatches() {
		ArrayList<ArrayList<PointD>> watchPolygons_ = new ArrayList<>();
		ArrayList<String> watchNames_ = new ArrayList<>();

		assert watchPolygons.size() == watchNames.size();

		for (int i = 0; i < watchPolygons.size(); i++) {
			String name = watchNames.get(i).trim();

			boolean include = false;
			boolean putInWarnings = false;

//			System.out.println(name);

			if ("Tornado Watch".equals(name))
				include = true;
			if ("Severe Thunderstorm Watch".equals(name))
				include = true;
			if ("Flood Advisory".equals(name))
				include = true;
			if ("Dense Fog Advisory".equals(name))
				include = true;
			if ("Wind Advisory".equals(name))
				include = true;
			if ("High Wind Warning".equals(name))
				include = true;
			if ("Small Craft Advisory".equals(name))
				include = true;
			if ("Gale Warning".equals(name))
				include = true;
			if ("Hazardous Seas Warning".equals(name))
				include = true;
			if ("Winter Weather Advisory".equals(name))
				include = true;
			if ("Winter Storm Watch".equals(name))
				include = true;
			if ("Winter Storm Warning".equals(name))
				include = true;
			if ("Blizzard Warning".equals(name))
				include = true;
			if ("Ice Storm Warning".equals(name))
				include = true;
			if ("Wind Chill Advisory".equals(name))
				include = true;
			if ("Wind Chill Watch".equals(name))
				include = true;
			if ("Wind Chill Warning".equals(name))
				include = true;

			if ("Snow Squall Warning".equals(name))
				putInWarnings = true;
//			if ("Flood Warning".equals(name))
//				putInWarnings = true;

//			System.out.println(include);
//			System.out.println(putInWarnings);

			if (include) {
				watchPolygons_.add(watchPolygons.get(i));
				watchNames_.add(watchNames.get(i));
			}

			if (putInWarnings) {
				warningPolygons.add(watchPolygons.get(i));
				warningNames.add(watchNames.get(i));
			}
		}

//		System.out.println(watchPolygons.size());
//		System.out.println(watchPolygons_.size());

		watchPolygons = watchPolygons_;
		watchNames = watchNames_;
//		System.out.println("new watch array assigned");

//		System.out.println(watchPolygons.size());
//		System.out.println(watchPolygons_.size());
	}

	public static ArrayList<ArrayList<PointD>> warningPolygons;
	public static ArrayList<String> warningNames = new ArrayList<>();

	private static ArrayList<ArrayList<PointD>> getPolygonsWarnings(File kml) {
		warningNames = new ArrayList<>();
		Pattern p = Pattern.compile("</altitudeMode>.*?</coordinates>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		Pattern p1 = Pattern.compile("<name>.*?</name>");

		Matcher m1 = p1.matcher(usingBufferedReader(kml));

		ArrayList<String> coordList = new ArrayList<>();

		while (m.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			String coords = m.group().substring(16 + 13, m.group().length() - 14);
			if ('y' != coords.charAt(0)) {
				coordList.add(m.group().substring(16 + 13, m.group().length() - 14));
			}
		}

		while (m1.find()) {
			// System.out.println(m.start() + " " + m.end() + " " + m.group().substring(13,
			// m.group().length() - 14));
			String name = m1.group().substring(6, m1.group().length() - 7);
			if (' ' == name.charAt(0)) {
//				System.out.println(name.substring(1));
				warningNames.add(name.substring(1));
			}
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

				polygon.add(new PointD(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
			}

			sc.close();
			polygons.add(polygon);
		}

		return polygons;
	}

	private static final String[] SPC_CATEGORIES = { "TSTM", "MRGL", "SLGT", "ENH", "MDT", "HIGH" };

	public static ArrayList<ArrayList<PointD>> spcOutlookPolygons;
	public static ArrayList<String> spcOutlookCategories = new ArrayList<>();

	private static ArrayList<ArrayList<PointD>> getPolygonsSpcOutlook(File kml) {
		spcOutlookCategories = new ArrayList<>();

		Pattern p = Pattern.compile("(<MultiGeometry>.*?</MultiGeometry>)|(<Polygon>.*?</Polygon>)");

		Matcher m = p.matcher(usingBufferedReader(kml));

		Pattern p1 = Pattern.compile("<outerBoundaryIs><LinearRing>.*?</coordinates>");

		ArrayList<String> coordList = new ArrayList<>();

		int category = 0;
		while (m.find()) {
			String element = m.group();

			Matcher m1 = p1.matcher(element);

			while (m1.find()) {
				String coords = m1.group().substring(17 + 12 + 13, m1.group().length() - 14);

				coordList.add(coords);
				spcOutlookCategories.add(SPC_CATEGORIES[category]);

//				System.out.println(SPC_CATEGORIES[category] + "\t" + coords);
			}

			category++;
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

				polygon.add(new PointD(Double.valueOf(pp[0]), Double.valueOf(pp[1])));
			}

			sc.close();
			polygons.add(polygon);
		}

		return polygons;
	}

	public static ArrayList<PointD> spcStormReportPoints;
	public static ArrayList<Integer> spcStormReportTypes = new ArrayList<>(); // 0 = tornado, 1 = hail, 2 = large hail,
																				// 3 = wind, 4 = strong wind

	private static ArrayList<PointD> getPointsStormReports(File kml) {
		spcStormReportTypes = new ArrayList<>();

		// parse by placemark

		Pattern p = Pattern.compile("<Placemark>.*?</Placemark>");

		Matcher m = p.matcher(usingBufferedReader(kml));

		ArrayList<PointD> reports = new ArrayList<>();

		while (m.find()) {
			String placemark = m.group();

			Pattern p0 = Pattern.compile("<coordinates>.*?</coordinates>");
			Matcher m0 = p0.matcher(placemark);

			Pattern p1 = Pattern.compile("<styleUrl>.*?</styleUrl>");
			Matcher m1 = p1.matcher(placemark);

			m0.find();
			m1.find();

			String coordsSuperstring = m0.group();
			String nameSuperstring = m1.group();

			String coords = coordsSuperstring.substring(13, coordsSuperstring.length() - 14);
			String name = nameSuperstring.substring(10, nameSuperstring.length() - 11);

			String[] latLonAlt = coords.split(",");

			PointD coord = new PointD(Double.valueOf(latLonAlt[1]), Double.valueOf(latLonAlt[0]));

			if ("#wind".equals(name) || "#wind_n".equals(name) || "#wind_h".equals(name)) {
				reports.add(coord);
				spcStormReportTypes.add(3);
			} else if ("#strong_wind".equals(name) || "#strongwind_n".equals(name) || "#strongwind_h".equals(name)) {
				reports.add(coord);
				spcStormReportTypes.add(4);
			} else if ("#hail".equals(name) || "#hail_n".equals(name) || "#hail_h".equals(name)) {
				reports.add(coord);
				spcStormReportTypes.add(1);
			} else if ("#large_hail".equals(name) || "#largehail_n".equals(name) || "#largehail_h".equals(name)) {
				reports.add(coord);
				spcStormReportTypes.add(2);
			} else if ("#tornado".equals(name) || "#tornado_n".equals(name) || "#tornado_h".equals(name)) {
				reports.add(coord);
				spcStormReportTypes.add(0);
			}
		}

		System.out.println("storm report stats");
		System.out.println(reports.size());
		System.out.println(spcStormReportTypes.size());

		return reports;
	}

	private static List<String> listNexradFiles(String radarSite) throws IOException { // breaks if dir.list does not
																						// update, work on exp
		downloadFile("https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + radarSite + "/dir.list",
				radarSite + "-index.dat");

		File index = new File(dataFolder + radarSite + "-index.dat");

		ArrayList<String> files = new ArrayList<String>();

		Scanner sc = new Scanner(index);

		while (sc.hasNextLine()) {
			Scanner line = new Scanner(sc.nextLine());

			int size = line.nextInt();

			if (Double.valueOf(size) > 0) {
//				System.out.println(size);
				files.add(line.next());
			}
		}

		sc.close();
		index.delete();

		Collections.reverse(files);

		return files;
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

	/*
	 * Example of reading Zip archive using ZipFile class
	 */

	private static void readUsingZipFile(String fileName, String outputDir) throws IOException {
		new File(outputDir).mkdirs();
		final ZipFile file = new ZipFile(fileName);
		// System.out.println("Iterating over zip file : " + fileName);

		try {
			final Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				extractEntry(entry, file.getInputStream(entry), outputDir);
			}
			// System.out.printf("Zip file %s extracted successfully in %s",
			// fileName, outputDir);
		} finally {
			file.close();
		}
	}

	/*
	 * Utility method to read data from InputStream
	 */

	private static void extractEntry(final ZipEntry entry, InputStream is, String outputDir) throws IOException {
		String exractedFile = outputDir + entry.getName();
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(exractedFile);
			final byte[] buf = new byte[8192];
			int length;

			while ((length = is.read(buf, 0, buf.length)) >= 0) {
				fos.write(buf, 0, length);
			}

		} catch (IOException ioex) {
			fos.close();
		}

	}

	private Thread downloadModelDataThread;
	public static boolean modelDataDownloaded = false;
	public static boolean modelDataBroken = false;
	private static DateTime timeLastDownloaded = new DateTime(1970, 1, 1, 0, 0, DateTimeZone.UTC);

	public static RapModel model0;
	public static DateTime time0;
	public static RapModel model1;
	public static DateTime time1;
	public static RapModel model2;
	public static DateTime time2;

	public static RapInterpModel modelI0;
	public static RapInterpModel modelI1;

	public static String loadingMessage;

	@SuppressWarnings("deprecation")
	public static void downloadModelData() {
		// https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2Frap.20230411&file=rap.t00z.awip32f00.grib2&var_DPT=on&var_HGT=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&lev_2_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_surface=on

		if (srtm == null) {
			loadingMessage = "Loading SRTM Data...";
			g.repaint();
			srtm = new SrtmModel2();
		}

		DateTime now = DateTime.now(DateTimeZone.UTC);

//		System.out.println("RadarView.downloadModelData()");
//		System.out.println(now);
//		System.out.println(timeLastDownloaded);

//		loadWindow.setTitle("Initializing RadarView: Loading SRTM...");

		if (now.getHourOfDay() != timeLastDownloaded.getHourOfDay()
				|| now.getDayOfYear() != timeLastDownloaded.getDayOfYear()) {
			try {
				modelDataDownloaded = false;
				modelDataBroken = false;

				DateTime modelInitTime = now.minusHours(2);
				modelInitTime = modelInitTime.minusSeconds(modelInitTime.getSecondOfMinute());
				modelInitTime = modelInitTime.minusMinutes(modelInitTime.getMinuteOfHour());

				loadingMessage = "Loading RAP Data (1/3)...";
				g.repaint();
				String url0 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 1);
				System.out.println(url0);
				downloadFile(url0, "rap-f01.grib2");
				model0 = new RapModel(NetcdfFile.open(dataFolder + "rap-f01.grib2"));
				time0 = modelInitTime.plusHours(1);

				loadingMessage = "Loading RAP Data (2/3)...";
				g.repaint();
				String url1 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 2);
				System.out.println(url1);
				downloadFile(url1, "rap-f02.grib2");
				model1 = new RapModel(NetcdfFile.open(dataFolder + "rap-f02.grib2"));
				time1 = modelInitTime.plusHours(2);

				loadingMessage = "Loading RAP Data (3/3)...";
				g.repaint();
				String url2 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 3);
				System.out.println(url2);
				downloadFile(url2, "rap-f03.grib2");
				model2 = new RapModel(NetcdfFile.open(dataFolder + "rap-f03.grib2"));
				time2 = modelInitTime.plusHours(3);

				loadingMessage = "Preparing RAP data...";
				g.repaint();
				modelI0 = new RapInterpModel(model0, model1, time0, time1);
				modelI1 = new RapInterpModel(model1, model2, time1, time2);

//				System.out.println(modelI0.getPrecipitationType(time1, 47.505129578564706, -111.30034270721687));
//				System.exit(0);

				new File(dataFolder + "rap-f01.grib2").delete();
				new File(dataFolder + "rap-f01.grib2.gbx9").delete();
				new File(dataFolder + "rap-f01.grib2.ncx4").delete();
				new File(dataFolder + "rap-f02.grib2").delete();
				new File(dataFolder + "rap-f02.grib2.gbx9").delete();
				new File(dataFolder + "rap-f02.grib2.ncx4").delete();
				new File(dataFolder + "rap-f03.grib2").delete();
				new File(dataFolder + "rap-f03.grib2.gbx9").delete();
				new File(dataFolder + "rap-f03.grib2.ncx4").delete();

				modelDataDownloaded = true;
				timeLastDownloaded = now;

				loadingMessage = "RAP data loaded!";
				g.repaint();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				loadingMessage = "";
				g.repaint();
			} catch (IOException e) {
				e.printStackTrace();
				modelDataBroken = true;
				loadingMessage = "RAP Data not found!";
				g.repaint();

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				loadingMessage = "";
				g.repaint();
			}
		}
	}

	public static void downloadFile(String url, String fileName) throws IOException {
//		System.out.println("Downloading from: " + url);
		URL dataURL = new URL(url);

		File dataDir = new File(dataFolder);
//		System.out.println("Creating Directory: " + dataFolder);
		dataDir.mkdirs();
		InputStream is = dataURL.openStream();

//		System.out.println("Output File: " + dataFolder + fileName);
		OutputStream os = new FileOutputStream(dataFolder + fileName);
		byte[] buffer = new byte[16 * 1024];
		int transferredBytes = is.read(buffer);
		while (transferredBytes > -1) {
			os.write(buffer, 0, transferredBytes);
			// System.out.println("Transferred "+transferredBytes+" for "+fileName);
			transferredBytes = is.read(buffer);
		}
		is.close();
		os.close();
	}

	public double vlcyPostProc(double data) {
		return 0.5 * data - 64.5;
	}

	private static double linScale(double preMin, double preMax, double postMin, double postMax, double value) {
		double slope = (postMax - postMin) / (preMax - preMin);

		return slope * (value - preMin) + postMin;
	}

	private static void loadRadarSites() {
		try {
			Scanner sc = new Scanner(RadarPanel.loadResourceAsFile("res/radarSites.csv"));

			while (sc.hasNextLine()) {
				String line = sc.nextLine();

				if (line.length() > 0 && line.charAt(0) != ';') {
					Scanner lineSc = new Scanner(line);
					lineSc.useDelimiter(";");

					while (lineSc.hasNext()) {
						String code = lineSc.next().trim();
						String city = lineSc.next().trim();

						String lat = "-999";
						String lon = "-999";
						if (lineSc.hasNext()) {
							lat = lineSc.next().trim();
							if (lat.length() == 0)
								lat = "-999";

							if (lineSc.hasNext()) {
								lon = lineSc.next().trim();
							}
						}

						RadarSite r = new RadarSite(code, city, new PointD(Double.valueOf(lat), Double.valueOf(lon)));

						radarSites.add(r);
						radarCodes.add(code);
					}

					lineSc.close();
				}
			}

			Collections.reverse(radarSites);
			Collections.reverse(radarCodes);

			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("ERROR: Radar Sites files cannot be found!");
		}
	}

	private static void loadCities() {
		try {
			Scanner sc = new Scanner(RadarPanel.loadResourceAsFile("res/usCities.csv"));

			while (sc.hasNextLine()) {
				String line = sc.nextLine();

				String[] tokens = line.split(",");

//				System.out.println(Arrays.toString(tokens));

				City city = new City(tokens[0], Double.valueOf(tokens[1]), Double.valueOf(tokens[2]),
						Integer.valueOf(tokens[3]));
				
				double closestLargerCityDis = 10000;
				int closestLargerCityPop = 0;
				if(cities == null) {
					city.setProminence(100);
				} else {
					for(City c : cities) {
						double cityDis = Math.hypot(city.getLatitude() - c.getLatitude(), city.getLongitude() - c.getLongitude());
						
						if(cityDis < closestLargerCityDis) {
							closestLargerCityDis = cityDis;
							closestLargerCityPop = c.getPopulation();
						}
					}
					
					double prominence = (double) city.getPopulation()*Math.log10(city.getPopulation())/closestLargerCityPop * closestLargerCityDis;
					
					// special handling for weird okc and norman prominence ratings
					if(city.getLatitude() > 35 && city.getLatitude() < 36 && city.getLongitude() > -98 && city.getLongitude() < -97) {
						if("Norman".equals(city.getName()) || "Oklahoma City".equals(city.getName())) {
							prominence *= 2;
						}
					}
					
					city.setProminence(prominence);
				}

				cities.add(city);
			}

			Collections.reverse(cities);
			
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("ERROR: City file cannot be found!");
		}
	}

	private void openHelpPage() {
		RadarPanel.loadResourceAsFile("res/helpPage.html");
		RadarPanel.loadResourceAsFile("res/about.html");
		RadarPanel.loadResourceAsFile("res/radar.jpeg");

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI("file:///" + dataFolder + "temp/res/helpPage.html"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
