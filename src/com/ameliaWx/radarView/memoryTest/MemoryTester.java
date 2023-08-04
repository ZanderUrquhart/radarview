package com.ameliaWx.radarView.memoryTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.radarView.nwpModel.RapInterpModel;
import com.ameliaWx.radarView.nwpModel.RapModel;
import com.ameliaWx.radarView.srtm.SrtmModel2;

import ucar.nc2.NetcdfFile;

public class MemoryTester {
	public static String dataFolder = System.getProperty("user.home") + "/Documents/RadarView/data/";

	public static void main(String[] args) throws IOException {
		wellBehavedIThinkTest();
	}

	public static void wellBehavedIThinkTest() {
		
		try {
			DateTime now = DateTime.now(DateTimeZone.UTC);

			DateTime modelInitTime = now.minusHours(2);
			modelInitTime = modelInitTime.minusSeconds(modelInitTime.getSecondOfMinute());
			modelInitTime = modelInitTime.minusMinutes(modelInitTime.getMinuteOfHour());

			Runtime.getRuntime().gc();
			long maxMemory = Runtime.getRuntime().maxMemory();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("RAP 1-Pre:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");

			// g.repaint();
			String url0 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
					"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
					modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
					modelInitTime.getHourOfDay(), 1);
			System.out.println(url0);
			Runtime.getRuntime().gc();
			maxMemory = Runtime.getRuntime().maxMemory();
			usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("RAP 1-Pr3:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");
			downloadFile(url0, "rap-f01.grib2");

			Runtime.getRuntime().gc();
			maxMemory = Runtime.getRuntime().maxMemory();
			usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("RAP 1-Mid:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2Frap.20230411&file=rap.t00z.awip32f00.grib2&var_DPT=on&var_HGT=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&lev_2_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_surface=on

		if (srtm == null) {
			Runtime.getRuntime().gc();
			long maxMemory = Runtime.getRuntime().maxMemory();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Pre-SRTM:         " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");

//			System.out.println(convToGigaMega(maxMemory));
//			System.out.println(convToGigaMega(usedMemory));
//			System.out.printf("%4.1f", 100.0 * usedMemory / maxMemory);
//			System.out.println("%");

			loadingMessage = "Loading SRTM Data...";
			// g.repaint();
			srtm = new SrtmModel2();

			Runtime.getRuntime().gc();
			maxMemory = Runtime.getRuntime().maxMemory();
			usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Post-SRTM:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");
		}
	}

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

	public static SrtmModel2 srtm;

	@SuppressWarnings("deprecation")
	public static void downloadModelData() {
		// https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2Frap.20230411&file=rap.t00z.awip32f00.grib2&var_DPT=on&var_HGT=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&lev_2_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_surface=on

		if (srtm == null) {
			Runtime.getRuntime().gc();
			long maxMemory = Runtime.getRuntime().maxMemory();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Pre-SRTM:         " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");

//			System.out.println(convToGigaMega(maxMemory));
//			System.out.println(convToGigaMega(usedMemory));
//			System.out.printf("%4.1f", 100.0 * usedMemory / maxMemory);
//			System.out.println("%");

			loadingMessage = "Loading SRTM Data...";
			// g.repaint();
			srtm = new SrtmModel2();

			Runtime.getRuntime().gc();
			maxMemory = Runtime.getRuntime().maxMemory();
			usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Post-SRTM:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");
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

				Runtime.getRuntime().gc();
				long maxMemory = Runtime.getRuntime().maxMemory();
				long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 1-Pre:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				loadingMessage = "Downloading RAP Data (1/3)...";
				// g.repaint();
				String url0 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 1);
				System.out.println(url0);
				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 1-Pr3:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");
				downloadFile(url0, "rap-f01.grib2");

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 1-Mid:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				System.exit(200);
				loadingMessage = "Processing RAP Data (1/3)...";
				model0 = new RapModel(NetcdfFile.open(dataFolder + "rap-f01.grib2"));
				time0 = modelInitTime.plusHours(1);

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 1-Post:       " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 2-Pre:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");
				loadingMessage = "Downloading RAP Data (2/3)...";
				// g.repaint();
				String url1 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 2);
				System.out.println(url1);
				downloadFile(url1, "rap-f02.grib2");

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 2-Mid:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				loadingMessage = "Processing RAP Data (2/3)...";
				model1 = new RapModel(NetcdfFile.open(dataFolder + "rap-f02.grib2"));
				time1 = modelInitTime.plusHours(2);

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 2-Post:       " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 3-Pre:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				loadingMessage = "Downloading RAP Data (3/3)...";
				// g.repaint();
				String url2 = "https://nomads.ncep.noaa.gov/cgi-bin/filter_rap32.pl?dir=%2F" + String.format(
						"rap.%04d%02d%02d&file=rap.t%02dz.awip32f%02d.grib2&var_DPT=on&var_HGT=on&var_PRES=on&var_RH=on&var_TMP=on&var_UGRD=on&var_VGRD=on&var_VVEL=on&lev_2_m_above_ground=on&lev_10_m_above_ground=on&lev_1000_mb=on&lev_975_mb=on&lev_950_mb=on&lev_925_mb=on&lev_900_mb=on&lev_875_mb=on&lev_850_mb=on&lev_825_mb=on&lev_800_mb=on&lev_775_mb=on&lev_750_mb=on&lev_725_mb=on&lev_700_mb=on&lev_675_mb=on&lev_650_mb=on&lev_625_mb=on&lev_600_mb=on&lev_575_mb=on&lev_550_mb=on&lev_525_mb=on&lev_500_mb=on&lev_475_mb=on&lev_450_mb=on&lev_425_mb=on&lev_400_mb=on&lev_375_mb=on&lev_350_mb=on&lev_325_mb=on&lev_300_mb=on&lev_275_mb=on&lev_250_mb=on&lev_225_mb=on&lev_200_mb=on&lev_175_mb=on&lev_150_mb=on&lev_125_mb=on&lev_100_mb=on&lev_75_mb=on&lev_50_mb=on&lev_surface=on",
						modelInitTime.getYear(), modelInitTime.getMonthOfYear(), modelInitTime.getDayOfMonth(),
						modelInitTime.getHourOfDay(), 3);
				System.out.println(url2);
				downloadFile(url2, "rap-f03.grib2");

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 3-Mid:        " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				loadingMessage = "Processing RAP Data (3/3)...";
				model2 = new RapModel(NetcdfFile.open(dataFolder + "rap-f03.grib2"));
				time2 = modelInitTime.plusHours(3);

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP 3-Post:       " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

				loadingMessage = "Preparing RAP data...";
				// g.repaint();
				modelI0 = new RapInterpModel(model0, model1, time0, time1);
				modelI1 = new RapInterpModel(model1, model2, time1, time2);

				Runtime.getRuntime().gc();
				maxMemory = Runtime.getRuntime().maxMemory();
				usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("RAP Fin:          " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
						+ convToGigaMega(usedMemory).trim() + ")");

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
				// g.repaint();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				loadingMessage = "";
				// g.repaint();
			} catch (IOException e) {
				e.printStackTrace();
				modelDataBroken = true;
				loadingMessage = "RAP Data not found!";
				// g.repaint();

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				loadingMessage = "";
				// g.repaint();
			}
		}
	}

	public static void downloadFile(String url, String fileName) throws IOException {
		System.out.println("download file new");
		URL website = new URL(url);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(dataFolder + fileName);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

	public static void downloadFileLegacy(String url, String fileName) throws IOException {
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

			Runtime.getRuntime().gc();
			long maxMemory = Runtime.getRuntime().maxMemory();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Memory leak???? : " + String.format("%4.1f", 100.0 * usedMemory / maxMemory) + "%, "
					+ convToGigaMega(usedMemory).trim() + ")");
		}
		is.close();
		os.close();
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
}
