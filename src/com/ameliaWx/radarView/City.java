package com.ameliaWx.radarView;

public class City {
	private String name;
	private double latitude;
	private double longitude;
	private int population;
	private double prominence;

	public City(String name, double latitude, double longitude, int population) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.population = population;
	}

	public String getName() {
		return name;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getPopulation() {
		return population;
	}

	public double getProminence() {
		return prominence;
	}

	public void setProminence(double prominence) {
		this.prominence = prominence;
	}
}
