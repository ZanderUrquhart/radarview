package com.ameliaWx.radarView;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.utils.general.PointF;

public class RadarSite {
	private String siteCode;
	private String siteCity;
	private PointF siteCoords;
	
	private boolean online;
	private boolean warned;
	
	public RadarSite(String siteCode, String siteCity, PointF siteCoords) {
		this.siteCode = siteCode;
		this.siteCity = siteCity;
		this.siteCoords = siteCoords;
		
		this.online = true;
		this.warned = false;
	}
	
	public RadarSite(String siteCode, String siteCity, double latitude, double longitude) {
		this(siteCode, siteCity, new PointF(latitude, longitude));
	}

	public String getSiteCode() {
		return siteCode;
	}

	public String getSiteCity() {
		return siteCity;
	}

	public PointF getSiteCoords() {
		return siteCoords;
	}
	
	public void drawButton(Graphics2D g, int x, int y) {
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
		g.setColor(isOnline() ? (isWarned() ? new Color(150, 150, 0) : Color.BLACK) : new Color(255, 0, 0));
		g.fillRect(x - 18, y - 8, 36, 16);
		g.setColor(Color.WHITE);
		g.drawRect(x - 18, y - 8, 36, 16);
		g.setColor(Color.WHITE);
		drawCenteredString(g, siteCode, new Rectangle(x, y, 0, 0), g.getFont());
	}
	
	/**
    * Draw a String centered in the middle of a Rectangle.
    *
    * @param g    The Graphics instance.
    * @param text The String to draw.
    * @param rect The Rectangle to center the text in.
    */
   private static void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
       // Get the FontMetrics
       FontMetrics metrics = g.getFontMetrics(font);
       // Determine the X coordinate for the text
       int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
       // Determine the Y coordinate for the text (note we add the ascent, as in java
       // 2d 0 is top of the screen)
       int y = rect.y + 4;
       // Set the font
       g.setFont(font);
       // Draw the String
       g.drawString(text, x, y);
   }
	
	public String toString() {
		return siteCode + " - " + siteCity;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isWarned() {
		return warned;
	}

	public void setWarned(boolean warned) {
		this.warned = warned;
	}
	
	public void checkOnline() {
		System.out.println("check online-" + siteCode + "...");
		try {
			RadarView.downloadFile("https://nomads.ncep.noaa.gov/pub/data/nccf/radar/nexrad_level2/" + siteCode + "/dir.list",
					siteCode + "-index.dat");

			File index = new File(RadarView.dataFolder + siteCode + "-index.dat");

			ArrayList<String> files = new ArrayList<String>();

			Scanner sc = new Scanner(index);

			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				
				Scanner line = new Scanner(nextLine);
				
				if(line.hasNextInt()) {
					int size = line.nextInt();
	
					if (Double.valueOf(size) > 1150000) {
						files.add(line.next());
					}
					
					line.close();
				}
			}

			sc.close();
			index.delete();

			Collections.reverse(files);
			
			if(files.size() == 0) {
				online = false;
				return;
			}
			
			String timestamp = files.get(0).substring(files.get(0).length() - 19,
					files.get(0).length() - 4);
			
			String year = timestamp.substring(0, 4);
			String month = timestamp.substring(4, 6);
			String day = timestamp.substring(6, 8);
			String hour = timestamp.substring(9, 11);
			String minute = timestamp.substring(11, 13);
			String second = timestamp.substring(13, 15);
			
			DateTime mostRecentScanTime = new DateTime(
					Integer.valueOf(year),
					Integer.valueOf(month),
					Integer.valueOf(day),
					Integer.valueOf(hour),
					Integer.valueOf(minute),
					Integer.valueOf(second),
					DateTimeZone.UTC);
			
			if(DateTime.now(DateTimeZone.UTC).minusMinutes(10).isAfter(mostRecentScanTime)) {
				online = false;
				return;
			} else {
				online = true;
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
