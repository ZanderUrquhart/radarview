package com.ameliaWx.radarView.srtm;

import com.ameliaWx.srtmWrapper.SrtmModel;

// wrapper for the SRTM wrapper, weird i know
public class SrtmModel2 {
	private short[][][][] data;
	
	private static long LOAD_TIMEOUT = 15000; // milliseconds
	
	LoadManagerThread loadManager;
	LoadTimeoutThread loadTimeout;
	
	public static void main(String[] args) {
		SrtmModel2 srtm = new SrtmModel2();
		
		System.out.println(srtm.getElevation(35.5, -97.5));
		System.out.println(srtm.getElevation(33.01, -96.5));
	}
	
	private int resDivider = 1;
	
	private SrtmModel srtm;
	
	public SrtmModel2() {
		this(1);
	}
	
	public SrtmModel2(int resDivider) {
		data = new short[28][9][][];
		this.resDivider = resDivider;
		
		srtm = new SrtmModel(System.getProperty("user.home") + "/Documents/RadarView/data/temp/");
		 
		loadManager = new LoadManagerThread();
		loadTimeout = new LoadTimeoutThread();
		
		loadManager.start();
		loadTimeout.start();
	}
	
	private class LoadManagerThread extends Thread {
		public LoadManagerThread() {
			super(new Runnable() {

				@Override
				public void run() {
//					System.out.println("start load manager thread");
					
					LoadWorkerThread worker0 = new LoadWorkerThread(0);
					LoadWorkerThread worker1 = new LoadWorkerThread(1);
					LoadWorkerThread worker2 = new LoadWorkerThread(2);
					LoadWorkerThread worker3 = new LoadWorkerThread(3);
					
					worker0.start();
					worker1.start();
					worker2.start();
					worker3.start();
					
					try {
						worker0.join();
						worker1.join();
						worker2.join();
						worker3.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
//					System.out.println("finish load manager thread");
				}
			});
		}
	}
	
	// should work in quartets
	private class LoadWorkerThread extends Thread {
		public LoadWorkerThread(int id) {
			super(new Runnable() {
				
				@Override
				public void run() {
//					System.out.println("start load worker thread " + id);
					
					for(int i = id; i < data.length; i += 4) {
						for(int j = 0; j < data[0].length; j++) {
							data[i][j] = srtm.loadElevData(i + 1, j + 1, resDivider);
						}
					}
				}
			});
		}
	}
	
	private class LoadTimeoutThread extends Thread {
		public LoadTimeoutThread() {
			super(new Runnable() {

				@SuppressWarnings("deprecation")
				@Override
				public void run() {
//					System.out.println("start load timeout thread");
					
					try {
						Thread.sleep(LOAD_TIMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					System.out.println(loadManager.isAlive());
					if(loadManager.isAlive()) {
//						System.out.println("doing timeout process");
						
						loadManager.stop();

						data = new short[28][9][2000][2000];
						
						for(int i = 0; i < 28; i++) {
							for(int j = 0; j < 9; j++) {
								for(int k = 0; k < 2000; k++) {
									for(int l = 0; l < 2000; l++) {
										data[i][j][k][l] = -1024;
									}
								}
							}
						}
					} else {
//						System.out.println("process done, no timeout required");
					}
				}
			});
		}
	}
	
	public double getElevation(double latitude, double longitude) {
		int x = (int) Math.round((longitude + 180) * 400);
		int y = (int) Math.round((60 - latitude) * 400);
		
		if(x < 0) return -1024.0;
		if(x >= 28 * 5 * 400) return -1024.0;
		if(y < 0) return -1024.0;
		if(y >= 9 * 5 * 400) return -1024.0;

		// nearest neighbor interp, work on bilinear
		return getElevation(x, y);
	}
	
	private double getElevation(int x, int y) {
		int i = x / 2000;
		int j = y / 2000;
		int k = x % 2000;
		int l = y % 2000;
		
		return data[i][j][k/resDivider][l/resDivider];
	}
}
