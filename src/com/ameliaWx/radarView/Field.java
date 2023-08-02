package com.ameliaWx.radarView;

public enum Field {
	REFL_BASE(0, "Base Reflectivity", 0, 0), 
	REFL_PTYPE(1, "Reflectivity + Precip Types [Experimental]", 0, 7),
//	REFL_4T(2, "Base Reflectivity (3 ptypes)"), 
//	REFL_12T(3, "Base Reflectivity (3 ptypes)"), 
	VLCY_BASE(1, "Base Velocity", 8, 1), 
	VLCY_SREL(2, "Storm Relative Velocity [Experimental]", 7, 1), 
	SPEC_WDTH(3, "Spectrum Width", 2, 2), 
	DIFF_REFL(4, "Differential Reflectivity", 3, 3),
	CORR_COEF(5, "Correlation Coefficient", 4, 4),
	DIFF_PHSE(6, "Differential Phase", 5, 5),
	KDP(6, "Specific Differential Phase", 6, 6),
//	TEST1(7, "Base Reflectivity (Snow Colors)", 0, 8),
//	TEST2(8, "Base Reflectivity (Wintry Mix Colors)", 0, 9),
	VLCY_ALIAS(2, "Base Velocity (Aliased)", 1, 1);

	static int size = 11;

	int id;
	String name;
	int dataLocation;
	int colorLocation;

	Field(int id, String name, int location, int colorLocation) {
		this.id = id;
		this.name = name;
		this.dataLocation = location;
		this.colorLocation = colorLocation;
	}
}
