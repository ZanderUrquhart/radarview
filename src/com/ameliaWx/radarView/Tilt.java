package com.ameliaWx.radarView;

public enum Tilt {
	_1(0, "Tilt 1"), _2(1, "Tilt 2"), _3(2, "Tilt 3"), _4(3, "Tilt 4"); 
	
	static int size = 4;

	int id;
	String name;

	Tilt(int id, String name) {
		this.id = id;
		this.name = name;
	}
}
