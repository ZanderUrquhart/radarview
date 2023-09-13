package com.ameliaWx.radarView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ameliaWx.radarView.nwpModel.PtypeAlgorithm;
import com.ameliaWx.radarView.nwpModel.RapInterpModel;
import com.ameliaWx.radarView.nwpModel.RapModel;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class RadarData {
	// information to find post-processing coefficients is in
	// NetcdfFile.findVariable("...").toString();
	// range folded value is also in there

	// try a step-function weighting for kdp smoothing instead of a gaussian one

	// reflectivity
	private static PostProc reflPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return (0.5f * (data) - 33);
		}
	};

	// velocity
	private static PostProc vlcyPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return 0.5f * data - 64.5f;
		}
	};

	// spectrum width
	private static PostProc spwdPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return 0.5f * data - 64.5f;
		}
	};

	// differential reflectivity
	private static PostProc drflPostProc = new PostProc() {
		@Override
		public float process(float data) {
//			return 0.05 * data - 8.0;
			return 0.04714f * data - 8.0f;
		}
	};

	// correlation coefficient
	private static PostProc crcfPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return (0.0033333334f * data + 0.20166667f);
		}
	};

	// differential phase
	private static PostProc dphsPostProc = new PostProc() {
		@Override
		public float process(float data) {
			return 0.35259685f * data - 0.7051937f;
		}
	};

	private double radarLat;
	private double radarLon;

	private float[][][] reflectivity;
	private byte[][] precipitationType;
	private float[][][] radialVelocity;
	private float[][][] dealiasedRadialVelocity;
	private float[][][] spectrumWidth;
	private float[][][] diffReflectivity;
	private float[][][] correlationCoefficient;
	private float[][][] differentialPhase;
	private float[][][] specificDifferentialPhase;

	private float[][][] stormRelativeRadialVelocity;

	private float stormMotionDirection;
	private float stormMotionSpeed;
	
	private boolean dataHasChanged = true;

	private DateTime scanTime;

	// get some elevation data in here and filter out the duplicate range-folded
	// reflectivity tilts
	// make a velocity scale for testing the display of other fields
	// maybe do the same for correlation coefficient

	public RadarData() {
	}

	@SuppressWarnings("deprecation")
	public RadarData(String fPath, String timestamp, double radarLat, double radarLon) throws IOException {
		this(NetcdfFile.open(fPath), timestamp, -1, radarLat, radarLon);
	}

	public RadarData(NetcdfFile ncfile, String timestamp, double radarLat, double radarLon) {
		this(ncfile, timestamp, -1, radarLat, radarLon);
	}

	public RadarData(NetcdfFile ncfile, String timestamp, int maxAmtTilts, double radarLat, double radarLon) {
		try {
			this.radarLat = radarLat;
			this.radarLon = radarLon;
			
			String year = timestamp.substring(0, 4);
			String month = timestamp.substring(4, 6);
			String day = timestamp.substring(6, 8);
			String hour = timestamp.substring(9, 11);
			String minute = timestamp.substring(11, 13);
			String second = timestamp.substring(13, 15);

			scanTime = new DateTime(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day),
					Integer.valueOf(hour), Integer.valueOf(minute), Integer.valueOf(second), DateTimeZone.UTC);

//			List<Variable> vars = ncfile.getVariables();
//			for(Variable v : vars) {
//				System.out.println(v.getNameAndDimensions());
//			}

			Variable diffRefl = ncfile.findVariable("DifferentialReflectivity_HI");
			
			RadarSiteType siteType;
			
			if (diffRefl == null) {
				siteType = RadarSiteType.TDWR;
			} else {
				siteType = RadarSiteType.WSR_88D;
			}

			switch(siteType) {
			case WSR_88D:
				Variable baseRefl = ncfile.findVariable("Reflectivity_HI");
				Variable baseReflAzi = ncfile.findVariable("azimuthR_HI");

				Variable baseVlcy = ncfile.findVariable("RadialVelocity_HI");
				Variable baseVlcyAzi = ncfile.findVariable("azimuthV_HI");

				Variable specWdth = ncfile.findVariable("SpectrumWidth_HI");
				Variable specWdthAzi = baseVlcyAzi;

				Variable diffReflAzi = ncfile.findVariable("azimuthD_HI");

				Variable corrCoef = ncfile.findVariable("CorrelationCoefficient_HI");
				Variable corrCoefAzi = ncfile.findVariable("azimuthC_HI");

				Variable diffPhse = ncfile.findVariable("DifferentialPhase_HI");
				Variable diffPhseAzi = ncfile.findVariable("azimuthP_HI");

				System.out.println("baseRefl: " + baseRefl);
				System.out.println("baseVlcy: " + baseVlcy);
				System.out.println("specWdth: " + specWdth);
				System.out.println("diffRefl: " + diffRefl);
				System.out.println("corrCoef: " + corrCoef);
				System.out.println("diffPhse: " + diffPhse);

				System.out.println("baseReflAzi: " + baseReflAzi);
				System.out.println("baseVlcyAzi: " + baseVlcyAzi);
				System.out.println("diffReflAzi: " + diffReflAzi);
				System.out.println("corrCoefAzi: " + corrCoefAzi);
				System.out.println("diffPhseAzi: " + diffPhseAzi);

				reflectivity = readNexradData(baseRefl, baseReflAzi, reflPostProc, -33.0f, -32.5f, maxAmtTilts);
				radialVelocity = readNexradData(baseVlcy, baseVlcyAzi, vlcyPostProc, -64.5f, -64.0f, maxAmtTilts);
				spectrumWidth = readNexradData(specWdth, specWdthAzi, spwdPostProc, -64.5f, -64.0f, maxAmtTilts);
				diffReflectivity = readNexradData(diffRefl, diffReflAzi, drflPostProc, -8.0f, -2048f, maxAmtTilts);
				correlationCoefficient = readNexradData(corrCoef, corrCoefAzi, crcfPostProc, 0.20166667f, 0.205f,
						maxAmtTilts);
				differentialPhase = readNexradData(diffPhse, diffPhseAzi, dphsPostProc, -0.7051936984062195f, -2048f,
						maxAmtTilts);
				
//				System.out.println("cc null: " + correlationCoefficient[0][360][correlationCoefficient[0][360].length - 1]);
//				System.exit(0);

				computeKdp(differentialPhase);

				dealiasedRadialVelocity = dealiasVelocity(radialVelocity);

				computeStormRelativeRadialVelocityZeroIsodopMethod(dealiasedRadialVelocity);

//				precipitationType = new int[reflectivity[0].length][findFurthestValueAboveZero(reflectivity[0])];
//				if(RadarView.radarData[0] != null) {
//					precipitationType = RadarView.radarData[0].getPtype().clone();
//				} else {
					precipitationType = new byte[reflectivity[0].length][reflectivity[0][0].length];
//				}
				break;
			case TDWR:
				System.out.println("Starting TDWR");
				baseRefl = ncfile.findVariable("Reflectivity");
				baseReflAzi = ncfile.findVariable("azimuthR");

				baseVlcy = ncfile.findVariable("RadialVelocity");
				baseVlcyAzi = ncfile.findVariable("azimuthV");

				specWdth = ncfile.findVariable("SpectrumWidth");
				specWdthAzi = ncfile.findVariable("azimuthV");

				System.out.println("baseRefl: " + baseRefl);
				System.out.println("baseVlcy: " + baseVlcy);
				System.out.println("specWdth: " + specWdth);
				System.out.println("diffRefl: " + diffRefl);

				reflectivity = readNexradData(baseRefl, baseReflAzi, reflPostProc, -1024, -32.5f, maxAmtTilts);
				radialVelocity = readNexradData(baseVlcy, baseVlcyAzi, vlcyPostProc, -64.5f, -64.0f, maxAmtTilts);
				spectrumWidth = readNexradData(specWdth, specWdthAzi, spwdPostProc, -64.5f, -64.0f, maxAmtTilts);

				dealiasedRadialVelocity = dealiasVelocity(radialVelocity);

				computeStormRelativeRadialVelocityZeroIsodopMethod(dealiasedRadialVelocity);

//				precipitationType = new int[reflectivity[0].length][findFurthestValueAboveZero(reflectivity[0])];
//				if(RadarView.radarData[0] != null) {
//					precipitationType = RadarView.radarData[0].getPtype().clone();
//				} else {
					precipitationType = new byte[reflectivity[0].length][reflectivity[0][0].length];
//				}
				break;
			}

			computePtypes();

			new File(ncfile.getLocation()).delete();
			new File(ncfile.getLocation() + ".uncompress").delete();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: RadarData object could not be created!");

			new File(ncfile.getLocation()).delete();
			new File(ncfile.getLocation() + ".uncompress").delete();
		}
	}

//	private static double[][][] readNexradData(Variable rawData, Variable azimuths, PostProc proc) throws IOException {
//		return readNexradData(rawData, azimuths, proc, -1024, -2048);
//	}

	@SuppressWarnings("deprecation")
	private static float[][][] readNexradData(Variable rawData, Variable azimuths, PostProc proc, float ndValue,
			float rfValue, int maxAmtTilts) throws IOException {
		if(rawData == null) return new float[1][720][1832];
		
		int[] shape = rawData.getShape();
		Array _data = null;
		Array _azi = null;

		System.out.println("maxamttilt check: " + shape[0] + "\t" + maxAmtTilts);

		if (maxAmtTilts != -1)
			shape[0] = Integer.min(shape[0], maxAmtTilts);

		_data = rawData.read();
		_azi = azimuths.read();

		boolean isDiffRefl = ("DifferentialReflectivity_HI".equals(rawData.getName()));

		if (isDiffRefl) {
			System.out.println(Arrays.toString(shape));
		}
		System.out.println("shape: " + Arrays.toString(shape));

		double[][] azi = new double[shape[0]][shape[1]];
		for (int h = 0; h < _azi.getSize(); h++) {
			int i = h / (shape[1]);
			int j = h % shape[1];

			if (i >= shape[0])
				break;

			azi[i][j] = _azi.getDouble(h);
		}

		float[][][] data = new float[shape[0]][shape[1]][shape[2]];
		for (int h = 0; h < _data.getSize(); h++) {
			int i = h / (shape[1] * shape[2]);
			int j = h / (shape[2]) % shape[1];
			int k = h % shape[2];

			if (i >= shape[0])
				break;

			if (isDiffRefl) {
				if (k % 2 == 1) {
					k /= 2;
				} else {
					k /= 2;
					k += shape[2] / 2;
				}
			}

			float record = proc.process(_data.getFloat(h));

//			int[] coords = { h, i, j, k };

			if (isDiffRefl) {
				
//				if(i == 0 && h % shape[2] < 6) {
//					System.out.printf("%25s", Arrays.toString(coords) + " " + record);
//				}
//				
//				if(i == 0 && k == 6) {
//					System.out.println();
//				}
			}

			if (record == ndValue) {
				data[i][(int) Math.floor((shape[1]/360) * azi[i][j])][k] = -1024;
			} else if (record == rfValue) {
				data[i][(int) Math.floor((shape[1]/360) * azi[i][j])][k] = -2048;
			} else {
//				if(isDiffRefl && record < -5) {
//					record += 12;
//				}
				data[i][(int) Math.floor((shape[1]/360) * azi[i][j])][k] = record;
			}
		}

//		System.out.println(rawData);
//		System.out.println(rawData.getName());
//		System.out.println(Arrays.toString(shape));
//		System.out.println();

		return data;
	}

	public float[][][][] getData() {
		return new float[][][][] { getReflectivity(), getRadialVelocity(), getSpectrumWidth(), getDiffReflectivity(),
				getCorrelationCoefficient(), getDifferentialPhase(), getSpecificDifferentialPhase(),
				getStormRelativeRadialVelocity(), getDealiasedRadialVelocity() };
	}

	public float[][][] getReflectivity() {
		if (reflectivity == null)
			return new float[4][720][1];

		return reflectivity;
	}

	public float[][][] getRadialVelocity() {
		if (radialVelocity == null)
			return new float[4][720][1];

		return radialVelocity;
	}

	public float[][][] getDealiasedRadialVelocity() {
		if (dealiasedRadialVelocity == null)
			return new float[4][720][1];

		return dealiasedRadialVelocity;
	}

	public float[][][] getStormRelativeRadialVelocity() {
		return stormRelativeRadialVelocity;
	}

	public float[][][] getSpectrumWidth() {
		if (spectrumWidth == null)
			return new float[4][720][1];

		return spectrumWidth;
	}

	public float[][][] getDiffReflectivity() {
		if (diffReflectivity == null)
			return new float[4][720][1];

		return diffReflectivity;
	}

	public float[][][] getCorrelationCoefficient() {
		if (correlationCoefficient == null)
			return new float[4][720][1];

		return correlationCoefficient;
	}

	public float[][][] getDifferentialPhase() {
		if (differentialPhase == null)
			return new float[4][720][1];

		return differentialPhase;
	}

	public float[][][] getSpecificDifferentialPhase() {
		if (specificDifferentialPhase == null)
			return new float[4][720][1];

		return specificDifferentialPhase;
	}

	public DateTime getScanTime() {
		return scanTime;
	}

	public double getStormMotionDirection() {
		return stormMotionDirection;
	}

	public double getStormMotionSpeed() {
		return stormMotionSpeed;
	}
	
	public String inspect(double latitude, double longitude) {
		double latDiff = latitude - radarLat;
		double lonDiff = longitude - radarLon;
		
		double lonStretchFactor = Math.cos(Math.toRadians(radarLat));
		double latDiffKm = latDiff * 111.32;
		double lonDiffKm = lonDiff * 111.32 * lonStretchFactor;
		
		double disKm = Math.hypot(latDiffKm, lonDiffKm);
		double aziDeg = (Math.toDegrees(Math.atan2(lonDiffKm, latDiffKm)) + 360) % 360;
		
		int range = (int) ((disKm - 1.875)/0.25);
		int azi = (int) (2 * aziDeg);
		
		if(range < 0 || range >= getData()[RadarView.chosenField.dataLocation][0][azi].length) {
			return "-";
		}
		
		double data = getData()[RadarView.chosenField.dataLocation][0][azi][range];
		
		String dataStr = "";
		
		if(data == -1024) {
			dataStr = "-";
		} else if (data == -2048.0) {
			dataStr = "RANGE FOLDED";
		} else {
			switch(RadarView.chosenField.dataLocation) {
			case 0:
				if(RadarView.chosenField.colorLocation == 7) {
					byte mask = getPtype()[azi][range];
					
					String pType = "";
					
					switch(mask) {
					case 0:
						pType = "RAIN";
						break;
					case 1:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "FREEZING RAIN";
						} else if(RadarView.numPtypes == 12) {
							pType = "FREEZING RAIN (SURFACE)";
						}
						break;
					case 2:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "ICE PELLETS";
						} else if(RadarView.numPtypes == 12) {
							pType = "ICE PELLETS";
						}
						break;
					case 3:
						if(RadarView.numPtypes == 3) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 4) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 12) {
							pType = "VERY DRY SNOW";
						}
						break;
					case 4:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "FREEZING RAIN";
						} else if(RadarView.numPtypes == 12) {
							pType = "FRZR-SNOW MIX";
						}
						break;
					case 5:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 12) {
							pType = "RAIN-SNOW MIX";
						}
						break;
					case 6:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "FREEZING RAIN";
						} else if(RadarView.numPtypes == 12) {
							pType = "FREEZING RAIN (ELEVATED)";
						}
						break;
					case 7:
						if(RadarView.numPtypes == 3) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 4) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 12) {
							pType = "DRY SNOW";
						}
						break;
					case 8:
						if(RadarView.numPtypes == 3) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 4) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 12) {
							pType = "WET SNOW";
						}
						break;
					case 9:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "ICE PELLETS";
						} else if(RadarView.numPtypes == 12) {
							pType = "RAIN-ICEP MIX";
						}
						break;
					case 10:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "FREEZING RAIN";
						} else if(RadarView.numPtypes == 12) {
							pType = "FRZR-ICEP MIX";
						}
						break;
					case 11:
						if(RadarView.numPtypes == 3) {
							pType = "WINTRY MIX";
						} else if(RadarView.numPtypes == 4) {
							pType = "SNOW";
						} else if(RadarView.numPtypes == 12) {
							pType = "ICEP-SNOW MIX";
						}
						break;
					}
					
					if(data >= 0) {
						dataStr = String.format("%3.1f", data) + " dBZ " + pType;
						break;
					} else {
						dataStr = "-";
						break;
					}
				} else {
					dataStr = String.format("%3.1f", data) + " dBZ";
					break;
				}
			case 1:
				String dir = (data > 0) ? "OUTBOUND" : "INBOUND";
				dataStr = String.format("%3.1f", 2.2369 * Math.abs(data)) + " mph " + dir;
				break;
			case 2:
				dataStr = String.format("%3.1f", Math.abs(2.2369 * data)) + " mph";
				break;
			case 3:
				dataStr = String.format("%3.1f", data) + " dBZ";
				break;
			case 4:
				dataStr = String.format("%6.4f", data) + "";
				break;
			case 5:
				dataStr = String.format("%3.1f", data) + " °";
				break;
			case 6:
				dataStr = String.format("%3.1f", data) + " °/km";
				break;
			case 7:
				dir = (data > 0) ? "OUTBOUND" : "INBOUND";
				dataStr = String.format("%3.1f", 2.2369 * Math.abs(data)) + " mph " + dir;
				break;
			case 8:
				dir = (data > 0) ? "OUTBOUND" : "INBOUND";
				dataStr = String.format("%3.1f", 2.2369 * Math.abs(data)) + " mph " + dir;
				break;
			default:
				dataStr = data + "";
			}
		}
		
		return dataStr;
//		return dataStr + "<a: " + azi + ", r:" + range + ">";
	}

	private static final double SIZE_RADIAL = 50;
	private static final double SIZE_AZIMUTHAL = 50;

	@SuppressWarnings("unused")
	private void computeStormRelativeRadialVelocityLocalAvg(float[][][] radialVelocity) {
		System.out.println("computing storm-relative radial velocity...");

		stormRelativeRadialVelocity = new float[radialVelocity.length][radialVelocity[0].length][radialVelocity[0][0].length];

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] == -1024) {
						stormRelativeRadialVelocity[i][j][k] = -1024;
					} else if (radialVelocity[i][j][k] == -2048) {
						stormRelativeRadialVelocity[i][j][k] = -2048;
					} else {
						float localAvgRadialVelocity = 0.0f;
						int count = 0;

						int innerRadial = (int) (k - SIZE_RADIAL / 2);
						int outerRadial = (int) (k + SIZE_RADIAL / 2);

						if (innerRadial < 0)
							innerRadial = 0;
						if (outerRadial > radialVelocity[i][j].length - 1)
							outerRadial = radialVelocity[i][j].length - 1;

						for (int l = 720 + j - (int) SIZE_AZIMUTHAL / 2; l <= 720 + j + (int) SIZE_AZIMUTHAL / 2; l++) {
							for (int m = innerRadial; m <= outerRadial; m++) {
								float velR = radialVelocity[i][l % 720][m];

								if (velR != -1024.0 && velR != -2048.0) {
									localAvgRadialVelocity += velR;
									count++;
								}
							}
						}

						localAvgRadialVelocity /= count;

						stormRelativeRadialVelocity[i][j][k] = radialVelocity[i][j][k] - localAvgRadialVelocity;
					}
				}
			}
		}

//		System.exit(0);
	}

	@SuppressWarnings("unused")
	private void computeStormRelativeRadialVelocityDependentRadials(float[][][] radialVelocity) {
		stormRelativeRadialVelocity = new float[radialVelocity.length][radialVelocity[0].length][radialVelocity[0][0].length];

		double avgSrvU = 0.0;
		double avgSrvV = 0.0;
		double count = 0;

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				double azimuth = j / 2.0;

				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] != -1024 && radialVelocity[i][j][k] != -2048) {
						double weight = 1 / (1 + count);

						avgSrvU = (1 - weight) * avgSrvU
								+ weight * radialVelocity[i][j][k] * Math.sin(Math.toRadians(azimuth));
						avgSrvV = (1 - weight) * avgSrvV
								+ weight * radialVelocity[i][j][k] * Math.cos(Math.toRadians(azimuth));

						count += k;

//						System.out.println(count);
					}
				}
			}
		}

//		System.out.println(Arrays.toString(avgSrvR[0]));

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				double azimuth = j / 2.0;

				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] == -1024) {
						stormRelativeRadialVelocity[i][j][k] = -1024;
					} else if (radialVelocity[i][j][k] == -2048) {
						stormRelativeRadialVelocity[i][j][k] = -2048;
					} else {
						stormRelativeRadialVelocity[i][j][k] = (float) (radialVelocity[i][j][k]
								- avgSrvU * Math.sin(Math.toRadians(azimuth))
								- avgSrvV * Math.cos(Math.toRadians(azimuth)));
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void computeStormRelativeRadialVelocityIndependentRadials(float[][][] radialVelocity) {
		stormRelativeRadialVelocity = new float[radialVelocity.length][radialVelocity[0].length][radialVelocity[0][0].length];

		float[][] avgSrvR = new float[radialVelocity.length][radialVelocity[0].length];

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				double count = 0;

				for (int k = 300; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] != -1024 && radialVelocity[i][j][k] != -2048) {
						float weight = (float) (1 / (1 + count));

						avgSrvR[i][j] = (1 - weight) * avgSrvR[i][j] + weight * radialVelocity[i][j][k];

						count++;

//						System.out.println(count);
					}
				}
			}
		}

//		System.out.println(Arrays.toString(avgSrvR[0]));

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] == -1024) {
						stormRelativeRadialVelocity[i][j][k] = -1024;
					} else if (radialVelocity[i][j][k] == -2048) {
						stormRelativeRadialVelocity[i][j][k] = -2048;
					} else {
						stormRelativeRadialVelocity[i][j][k] = radialVelocity[i][j][k] - avgSrvR[i][j];
					}
				}
			}
		}
	}

	private void computeStormRelativeRadialVelocityZeroIsodopMethod(float[][][] radialVelocity) {
		stormRelativeRadialVelocity = new float[radialVelocity.length][radialVelocity[0].length][radialVelocity[0][0].length];

		double leftZeroIsodopTrackerU = 0.0;
		double leftZeroIsodopTrackerV = 0.0;

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				double azimuth = j / 2.0;

				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					int jCCW = j - 1;
					if (jCCW == -1)
						jCCW = radialVelocity[0].length - 1;

					double radialVelJ = radialVelocity[i][j][k];
					double radialVelJCCW = radialVelocity[i][jCCW][k];

					if (radialVelJ == -1024) {

					} else if (radialVelJ == -2048) {

					} else {
						if (radialVelJCCW == -1024) {

						} else if (radialVelJCCW == -2048) {

						} else {
							double diffRadVelWrtAzimuth = radialVelJ - radialVelJCCW;

							if (Math.abs(diffRadVelWrtAzimuth) < 10) {
								if (radialVelJ > 0 && radialVelJCCW < 0) {
									leftZeroIsodopTrackerU += Math.sin(Math.toRadians(azimuth));
									leftZeroIsodopTrackerV += Math.cos(Math.toRadians(azimuth));
								} else if (radialVelJ < 0 && radialVelJCCW > 0) {
									leftZeroIsodopTrackerU -= Math.sin(Math.toRadians(azimuth));
									leftZeroIsodopTrackerV -= Math.cos(Math.toRadians(azimuth));
								}
							}
						}
					}
				}
			}
		}

		double leftZeroIsodopTrackerMag = Math.hypot(leftZeroIsodopTrackerU, leftZeroIsodopTrackerV);

		leftZeroIsodopTrackerU /= leftZeroIsodopTrackerMag;
		leftZeroIsodopTrackerV /= leftZeroIsodopTrackerMag;

		// set storm motion direction as 90 deg clockwise from left zero-isodop
		double stormMotionDirectionU = leftZeroIsodopTrackerV;
		double stormMotionDirectionV = -leftZeroIsodopTrackerU;

		double stormMotionVectorU = 0.0;
		double stormMotionVectorV = 0.0;

		double count = 0;

		System.out.printf("storm motion direction: %5.2f  E %5.2f  N m/s\n", stormMotionDirectionU,
				stormMotionDirectionV);

		for (int i = 0; i < radialVelocity.length; i++) {
			for (int j = 0; j < radialVelocity[i].length; j++) {
				double azimuth = j / 2.0;

				double azimuthU = Math.sin(Math.toRadians(azimuth));
				double azimuthV = Math.cos(Math.toRadians(azimuth));

				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					double azimuthDotStormMotion = azimuthU * stormMotionDirectionU + azimuthV * stormMotionDirectionV;

					if (Math.abs(azimuthDotStormMotion) > 0.05) {
						if (radialVelocity[i][j][k] != -1024 && radialVelocity[i][j][k] != -2048) {
							double trueVel = radialVelocity[i][j][k] / (azimuthDotStormMotion);

							double velU = trueVel * stormMotionDirectionU;
							double velV = trueVel * stormMotionDirectionV;

							double newWeight = k + 7.5;

							double weight = newWeight / (count + newWeight);

							stormMotionVectorU = (1 - weight) * stormMotionVectorU + weight * velU;
							stormMotionVectorV = (1 - weight) * stormMotionVectorV + weight * velV;

//							if(count < 500) System.out.printf("%d\t%7.5f\t%5.1f\t%5.1f\t%5.1f\n", count, weight, trueVel, stormMotionVectorU, stormMotionVectorV);

							count += newWeight;
						}
					}
				}
			}
		}

		System.out.printf("storm motion: %5.1f  E %5.1f  N m/s\n", stormMotionVectorU, stormMotionVectorV);
		for (int i = 0; i < radialVelocity.length; i++) {

			for (int j = 0; j < radialVelocity[i].length; j++) {
				double azimuth = j / 2.0;

				double azimuthU = Math.sin(Math.toRadians(azimuth));
				double azimuthV = Math.cos(Math.toRadians(azimuth));

				for (int k = 0; k < radialVelocity[i][j].length; k++) {
					if (radialVelocity[i][j][k] == -1024) {
						stormRelativeRadialVelocity[i][j][k] = -1024;
					} else if (radialVelocity[i][j][k] == -2048) {
						stormRelativeRadialVelocity[i][j][k] = -2048;
					} else {
						stormRelativeRadialVelocity[i][j][k] = (float) (radialVelocity[i][j][k]
								- (azimuthU * stormMotionVectorU + azimuthV * stormMotionVectorV));
					}
				}
			}
		}

		stormMotionDirection = (float) Math.toDegrees(Math.atan2(-stormMotionVectorU, -stormMotionVectorV));
		stormMotionSpeed = (float) Math.hypot(stormMotionVectorU, stormMotionVectorV);
	}

	private void computeKdp(float[][][] dphsRaw) {
		dphsRaw = filterOutWithCC(dphsRaw, getCorrelationCoefficient(), 0.6f, 1.01f);

		dphsRaw = despeckleDphs(dphsRaw);

		float[][][] dphs = smoothField(dphsRaw, 20);

		float[][][] kdp = new float[dphs.length][dphs[0].length][dphs[0][0].length - 1];

		for (int i = 0; i < kdp.length; i++) {
			for (int j = 0; j < kdp[i].length; j++) {
				for (int k = 0; k < kdp[i][j].length; k++) {
					if (dphs[i][j][k] == -1024.0 || dphs[i][j][k + 1] == -1024.0) {
						kdp[i][j][k] = -1024.0f;
					} else if (dphs[i][j][k] == -2048.0) {
						kdp[i][j][k] = -2048.0f;
					} else {
						kdp[i][j][k] = 4 * (dphs[i][j][k + 1] - dphs[i][j][k]);
					}
				}
			}
		}

		specificDifferentialPhase = kdp;
	}

	private float[][][] filterOutWithCC(float[][][] data, float[][][] cc, float ccThreshold,
			float highCcThreshold) {
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				for (int k = 0; k < data[i][j].length; k++) {
					if (cc[i][j][k] < ccThreshold) {
						data[i][j][k] = -1024.0f;
					}

					if (cc[i][j][k] > highCcThreshold) {
						data[i][j][k] = -1024.0f;
					}
				}
			}
		}

		return data;
	}

	private float[][][] despeckleDphs(float[][][] dphs) {
		return despeckleDphs(dphs, 60);
	}

	private float[][][] despeckleDphs(float[][][] dphs, float discontDiff) {
		float[][][] newDphs = dphs.clone();

		for (int t = 0; t < dphs.length; t++) {
			for (int a = 0; a < dphs[t].length; a++) {
				for (int r = 1; r < dphs[t][a].length - 1; r++) {
					double dphsC = dphs[t][a][r];

					double dphsRP1 = dphs[t][a][r + 1];
					double dphsRN1 = dphs[t][a][r - 1];
					double dphsAP1 = dphs[t][(a + 720 + 1) % 720][r];
					double dphsAN1 = dphs[t][(a + 720 - 1) % 720][r];

					double dphsRP1valid = (dphsRP1 != -1024.0 && dphsRP1 != 2048.0) ? 0.25 : 0;
					double dphsRN1valid = (dphsRN1 != -1024.0 && dphsRN1 != 2048.0) ? 0.25 : 0;
					double dphsAP1valid = (dphsAP1 != -1024.0 && dphsAP1 != 2048.0) ? 0.25 : 0;
					double dphsAN1valid = (dphsAN1 != -1024.0 && dphsAN1 != 2048.0) ? 0.25 : 0;

					double dphsValidSum = dphsRP1valid + dphsRN1valid + dphsAP1valid + dphsAN1valid;

					if (dphsValidSum >= 0.5) {
						double dphsRP1discont = (dphsRP1 == -1024.0 || dphsRP1 == 2048.0
								|| Math.abs(dphsRP1 - dphsC) >= discontDiff) ? 0.25 : 0;
						double dphsRN1discont = (dphsRN1 == -1024.0 || dphsRN1 == 2048.0
								|| Math.abs(dphsRN1 - dphsC) >= discontDiff) ? 0.25 : 0;
						double dphsAP1discont = (dphsAP1 == -1024.0 || dphsAP1 == 2048.0
								|| Math.abs(dphsAP1 - dphsC) >= discontDiff) ? 0.25 : 0;
						double dphsAN1discont = (dphsAN1 == -1024.0 || dphsAN1 == 2048.0
								|| Math.abs(dphsAN1 - dphsC) >= discontDiff) ? 0.25 : 0;

						double dphsDiscontSum = dphsRP1discont + dphsRN1discont + dphsAP1discont + dphsAN1discont;

						if (dphsDiscontSum >= 0.75) {
							float vSum = 0.0f;
							float wSum = 0.0f;

							if (dphsRP1discont == 0.25 && dphsRP1valid == 0.25) {
								vSum += dphsRP1;
								wSum++;
							}

							if (dphsRN1discont == 0.25 && dphsRN1valid == 0.25) {
								vSum += dphsRP1;
								wSum++;
							}

							if (dphsAP1discont == 0.25 && dphsAP1valid == 0.25) {
								vSum += dphsRP1;
								wSum++;
							}

							if (dphsAN1discont == 0.25 && dphsAN1valid == 0.25) {
								vSum += dphsRP1;
								wSum++;
							}

							float newVal = vSum / wSum;
							newDphs[t][a][r] = newVal;
						}
					}
				}
			}
		}

		return newDphs;
	}

	private static final double CONE_OF_SILENCE_SIZE = 7.5; // km * 0.25
	// kernel sizeL km * 0.25

	public byte[][] getPtype() {
		return precipitationType;
	}

	public int findFurthestValueAboveZero(double[][] refl) {
		for (int j = refl[0].length - 1; j >= 0; j--) {
			for (int i = 0; i < refl.length; i++) {
				if (refl[i][j] > 0)
					return j;
			}
		}
		
		return 1;
	}

	public void computePtypes() {
		System.out.println("computePtypes no-param");

		computePtypesThread = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("computePtypes no-param run");
				while (!RadarView.modelDataDownloaded) {
					try {
						System.out.println("waiting for model");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (RadarView.modelDataBroken) {
						System.out.println("returning bc is broken");
						return;
					}
				}

				System.out.println("beginning ptype assignments");

//				BufferedImage ptypeTest = new BufferedImage(720, 1832, BufferedImage.TYPE_3BYTE_BGR);
//				Graphics2D g = ptypeTest.createGraphics();

				double lonStretchFactor = Math.cos(Math.toRadians(radarLat));

//				DateTime modelInitTime = DateTime.now(DateTimeZone.UTC).minusHours(2);

//				System.out.println(getScanTime());
//				System.out.println(RadarView.time1);
//				System.out.println();

				double disMult = 1;
				double coneOfSilenceSize = 7.5;
//				if(reflectivity.length == 360) {
//					disMult = 0.2967444/0.25;
//					coneOfSilenceSize = -1;
//				}

				for (int i = 0; i < precipitationType.length; i++) {
//					System.out.println(i + "\t" + reflectivity[0].length);

//					System.out.println("radarLat: " + radarLat);
//					System.out.println("radarLon: " + radarLon);
					for (int j = 0; j < precipitationType[0].length; j++) {
						if (reflectivity[0][i][j] > 0) {
							double queryLat = radarLat
									+ ((coneOfSilenceSize + j) * disMult * 0.25) / 111.32 * Math.cos(Math.toRadians(i * 0.5 + 0.25));
							double queryLon = radarLon + ((coneOfSilenceSize + j) * disMult * 0.25) / 111.32 / lonStretchFactor
									* Math.sin(Math.toRadians(i * 0.5 + 0.25));

//							if(i == 0) System.out.println("j:             " + j);
//							if(i == 0) System.out.println("lat change:    " + ((CONE_OF_SILENCE_SIZE + j)/0.25)/111.32);
//							System.out.println("queryLat:      " + queryLat);
//							System.out.println("queryLon:      " + queryLon);

							if (getScanTime().isBefore(RadarView.time1)) {
								precipitationType[i][j] = (byte) RadarView.modelI0.getPrecipitationType(getScanTime(),
										queryLat, queryLon, RadarView.srtm.getElevation(queryLat, queryLon));
							} else {
								precipitationType[i][j] = (byte) RadarView.modelI1.getPrecipitationType(getScanTime(),
										queryLat, queryLon, RadarView.srtm.getElevation(queryLat, queryLon));
							}
							
							if(i == 710 && j == 829) {
								System.out.println("ptype[710, 829]: " + precipitationType[i][j]);
								RadarView.modelI1.getPrecipitationType(getScanTime(),
									queryLat, queryLon, PtypeAlgorithm.BOURGOUIN_REVISED_EXTENDED, true, RadarView.srtm.getElevation(queryLat, queryLon), true);
							}
//							g.setColor(RadarView.refl12PTypesColors.getColor(reflectivity[0][i][j], RadarView.modelI0.getPrecipitationType(modelInitTime, 80 - j * 0.1, -130 + i * 0.1)));
//							g.fillRect(i, j, 1, 1);
						}
					}
				}

//				try {
//					ImageIO.write(ptypeTest, "PNG", new File("ptype-test-rawArray-" + RadarView.chosenRadar + ".png"));
//				} catch (IOException e) {
//					e.printStackTrace();
//				}

				markDataAsChanged();
				RadarView.redrawPanels();
			}
		});

		computePtypesThread.start();
	}

	private Thread computePtypesThread;

	public void computePtypes(RapModel model1, RapModel model2, double radarLat, double radarLon, DateTime time1,
			DateTime time2, DateTime time) {
		System.out.println("computePtypes all-param");

		computePtypesThread = new Thread(new Runnable() {
			@Override
			public void run() {
				RapInterpModel model = new RapInterpModel(model1, model2, time1, time2);
				double lonStretchFactor = Math.cos(Math.toRadians(radarLat));

				for (int i = 0; i < reflectivity[0].length; i++) {
					for (int j = 0; j < reflectivity[0][0].length; j++) {
//						if(reflectivity[0][i][j] > 0) {
						double queryLat = radarLat
								+ ((CONE_OF_SILENCE_SIZE + j) / 0.25) / 111.32 * Math.cos(Math.toRadians(i * 0.5));
						double queryLon = radarLon + ((CONE_OF_SILENCE_SIZE + j) / 0.25) / 111.32 / lonStretchFactor
								* Math.sin(Math.toRadians(i * 0.5));

						precipitationType[i][j] = (byte) model.getPrecipitationType(time, queryLat, queryLon);
//						}
					}
				}
			}
		});

		computePtypesThread.start();
	}

	@SuppressWarnings("unused")
	private float[][] smoothField(float[][] data, int kernelSize) {
		return smoothField(new float[][][] { data }, kernelSize)[0];
	}

	private float[][][] smoothField(float[][][] data, int kernelSize) {
		// radially smoothed step
		float[][][] intm = new float[data.length][data[0].length][data[0][0].length];
		// final step
		float[][][] ret = new float[data.length][data[0].length][data[0][0].length];

		float[] rWeights = getGaussianWeights(kernelSize, (kernelSize - 1) / 4.0);
		for (int i = 0; i < ret.length; i++) {
			// radial smoothing
			for (int j = 0; j < ret[i].length; j++) {
				for (int k = 0; k < ret[i][j].length; k++) {
					if (data[i][j][k] == -1024.0) {
						intm[i][j][k] = -1024.0f;
					} else if (data[i][j][k] == -2048.0) {
						intm[i][j][k] = -2048.0f;
					} else {
						float vSum = 0.0f;
						float wSum = 0.0f;

						for (int l = -(rWeights.length - 1) / 2; l <= (rWeights.length - 1) / 2; l++) {

							if (k + l >= 0 && k + l < ret[i][j].length) {
								double value = data[i][j][k + l];
								double weight = rWeights[l + (rWeights.length - 1) / 2];

								if (value != -1024.0 && value != -2048.0) {
									vSum += value * weight;
									wSum += weight;
								}
							}
						}

						intm[i][j][k] = vSum / wSum;
					}
				}
			}

			// azimuthal smoothing
			for (int k = 0; k < ret[i][0].length; k++) {
				double azimuthalSize = ((180 * 0.25 * (kernelSize - 1) / 2)
						/ (0.25 * (k + CONE_OF_SILENCE_SIZE * Math.PI))) / 2.0;

				if (azimuthalSize < 1) {
					for (int j = 0; j < ret[i].length; j++) {
						ret[i][j][k] = intm[i][j][k];
					}

					continue;
				}

				if (azimuthalSize > 90)
					azimuthalSize = 90;

				float[] aWeights = getGaussianWeights((int) 90, azimuthalSize / 2.0);
				for (int j = 0; j < ret[i].length; j++) {
					if (intm[i][j][k] == -1024.0) {
						ret[i][j][k] = -1024.0f;
					} else if (intm[i][j][k] == -2048.0) {
						ret[i][j][k] = -2048.0f;
					} else {
						float vSum = 0.0f;
						float wSum = 0.0f;

						for (int l = -(aWeights.length - 1) / 2; l <= (aWeights.length - 1) / 2; l++) {
							int jl = (j + l + 720) % 720;

							float value = intm[i][jl][k];
							float weight = aWeights[l + (aWeights.length - 1) / 2];

							if (value != -1024.0 && value != -2048.0) {
								vSum += value * weight;
								wSum += weight;
							}
						}

						ret[i][j][k] = vSum / wSum;
					}
				}
			}
		}

		return ret;
	}

	private float[] getGaussianWeights(int sizeR, double stdDev) {
		float[] weights = new float[2 * sizeR + 1];

		for (int i = 0; i <= sizeR; i++) {
			weights[sizeR + i] = (float) (1 / (stdDev * Math.sqrt(2 * Math.PI)) * Math.exp(-(i * i) / (2 * stdDev * stdDev)));
			weights[sizeR - i] = weights[sizeR + i];
		}

		return weights;
	}

	@SuppressWarnings("unused")
	private double[] getSteppedWeights(int sizeR, double stdDev) {
		stdDev *= 4;

		double[] weights = new double[2 * sizeR + 1];

		for (int i = 0; i <= sizeR; i++) {
			if (i <= stdDev) {
				weights[sizeR + i] = 1;
			} else {
				weights[sizeR + i] = 0;
			}
			weights[sizeR - i] = weights[sizeR + i];
		}

		return weights;
	}

	// algorithm is of my own invention
	// some points inspired by UNRAVEL (https://doi.org/10.1175/JTECH-D-19-0020.1)
	// but hopefully faster
	private float[][][] dealiasVelocity(float[][][] data) {
		float[][][] dealiasedVelocity = new float[data.length][data[0].length][data[0][0].length];

		for (int i = 0; i < data.length; i++) {
			dealiasedVelocity[i] = dealiasVelocityAtTilt(data[i]);
		}

		return dealiasedVelocity;
	}
	
	public void markDataAsChanged() {
		dataHasChanged = true;
	}
	
	public void markDataAsRendered() {
		dataHasChanged = false;
	}
	
	public boolean hasDataChanged() {
		return dataHasChanged;
	}

	// zero-isodop method similar to storm relative motion calc
	// call multiple times if possible to get rid of higher order aliasing such as
	// when the nyquist velocity is extremely low (looking at you KMPX)
	private static final int INNER_MID_GATE = 4000;
	private static final int MID_OUTER_GATE = 8000;

	private float[][] dealiasVelocityAtTilt(float[][] data) {
		float[][] dealiasedVelocity = new float[data.length][data[0].length];

		float nyquistVelocity = -1.0f;

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				float record = data[a][r];

				dealiasedVelocity[a][r] = record;

				if (Math.abs(record) > nyquistVelocity && record != -1024 && record != -2048) {
					nyquistVelocity = Math.abs(record);
				}
			}
		}

		nyquistVelocity += 0.5;

		// maybe make a max too
		final double NEEDS_DEALIASING_FRACTION = 0.8;
		double[][] needsDealiasingMinRange = new double[data.length][3];
		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < 3; r++) {
				needsDealiasingMinRange[a][r] = 2000;
			}
		}

		double[][] velSum = new double[data.length][3];
		int[][] velCount = new int[data.length][3];
		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				int rId = 0;
				if (r > INNER_MID_GATE)
					rId = 1;
				if (r > MID_OUTER_GATE)
					rId = 2;

				double record = data[a][r];

				if (record != -1024 && record != -2048) {
					velSum[a][rId] += record;
					velCount[a][rId]++;

					if (r >= 3) {
						double record2 = data[a][r - 2];
						double record1 = data[a][r - 1];
						double record0 = data[a][r];

						if (record0 != -1024 && record0 != -2048 && record1 != -1024 && record1 != -2048
								&& record2 != -1024 && record2 != -2048) {
							double addend0 = (Math.abs(record0) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;
							double addend1 = (Math.abs(record1) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;
							double addend2 = (Math.abs(record2) > NEEDS_DEALIASING_FRACTION * nyquistVelocity) ? 0.25
									: 0;

							double sum = addend0 + addend1 + addend2;

							if (sum >= 0.5 && r < needsDealiasingMinRange[a][rId]) {
								needsDealiasingMinRange[a][rId] = r - 2;
							}
						}
					}
				}
			}
		}

		for (int a = 0; a < velSum.length; a++) {
			for (int r = 0; r < 3; r++) {
				if (velCount[a][r] != 0) {
					velSum[a][r] /= velCount[a][r];
				} else {
					velSum[a][r] = -1024.0;
				}
			}
		}

		double[][] smoothedSums = new double[velSum.length][3];

		final int KERNEL_SIZE = 180;
		for (int a = KERNEL_SIZE / 2; a < dealiasedVelocity.length + KERNEL_SIZE / 2; a++) {
			for (int r = 0; r < 3; r++) {
				double sum = 0.0;

				for (int da = -KERNEL_SIZE / 2; da <= KERNEL_SIZE / 2; da++) {
					int _a = (a + da + dealiasedVelocity.length) % dealiasedVelocity.length;

					sum += velSum[_a][r];
				}

				smoothedSums[a % dealiasedVelocity.length][r] = sum / KERNEL_SIZE;
			}
		}

		velSum = smoothedSums;

		for (int a = 0; a < data.length; a++) {
			for (int r = 0; r < data[a].length; r++) {
				if (data[a][r] != -1024 && data[a][r] != -2048) {

					int rId = 0;
					if (r > INNER_MID_GATE)
						rId = 1;
					if (r > MID_OUTER_GATE)
						rId = 2;

					if (data[a][r] > 0 && velSum[a][rId] < 0 && r >= needsDealiasingMinRange[a][rId]) {
						dealiasedVelocity[a][r] = data[a][r] - 2 * nyquistVelocity;
					} else if (data[a][r] < 0 && velSum[a][rId] > 0 && r >= needsDealiasingMinRange[a][rId]) {
						dealiasedVelocity[a][r] = data[a][r] + 2 * nyquistVelocity;
					} else {
						dealiasedVelocity[a][r] = data[a][r];
					}
				}
			}
		}

		// box filter to remove straggler pixels
		final int BOX_SIZE = 5;
		for (int a = 0; a < data.length; a++) {
			for (int r = BOX_SIZE + 300; r < data[a].length - BOX_SIZE; r++) {
				int _a = (a + dealiasedVelocity.length) % dealiasedVelocity.length;

				double record = dealiasedVelocity[_a][r];

				if (record != -1024 && record != -2048) {
					double sum = 0;
					int count = 0;

					for (int da = -BOX_SIZE; da <= BOX_SIZE; da++) {
						for (int dr = -BOX_SIZE; dr <= BOX_SIZE; dr++) {
							int _da = (a + da + dealiasedVelocity.length) % dealiasedVelocity.length;

							double record0 = dealiasedVelocity[_da][r + dr];

							if (record0 != -1024 && record0 != -2048) {
								sum += record0;
								count++;
							}
						}
					}

					if (count != 0) {
						double boxAvg = sum / count;

						if (boxAvg - record > nyquistVelocity) {
							dealiasedVelocity[_a][r] += 2 * nyquistVelocity;
						} else if (boxAvg - record < -nyquistVelocity) {
							dealiasedVelocity[_a][r] -= 2 * nyquistVelocity;
						}
					}
				}
			}
		}

		double currVal = -1024.0;
		double prevVal = -1024.0;
		int counterSinceLastValidGate = 0;
		// second radial dealiasing
		for (int a = 0; a < data.length; a++) {
			counterSinceLastValidGate = 0;
			for (int r = 400; r < data[a].length; r++) {
				currVal = dealiasedVelocity[a][r];
				if(dealiasedVelocity[a][r - 1] != -1024 && dealiasedVelocity[a][r - 1] != -2048) {
					prevVal = dealiasedVelocity[a][r - 1];
					counterSinceLastValidGate = 0;
				} else {
					counterSinceLastValidGate++;
				}

				if (currVal != -1024 && currVal != -2048 && prevVal != -1024 && prevVal != -2048 && counterSinceLastValidGate<= 5) {
					if(currVal - prevVal > 1.5 * nyquistVelocity) {
						dealiasedVelocity[a][r] -= 2 * nyquistVelocity;
					} else if(currVal - prevVal < -1.5 * nyquistVelocity) {
						dealiasedVelocity[a][r] += 2 * nyquistVelocity;
					}
				}
			}
		}

		return dealiasedVelocity;
	}

	@SuppressWarnings("unused")
	private double[][][] dealiasVelocityOld2(double[][][] data) {
		double[][][] dealiasedVelocity = new double[data.length][data[0].length][data[0][0].length];

		double nyquistVelocity = -1.0;

		for (int t = 0; t < data.length; t++) {
			for (int a = 0; a < data[t].length; a++) {
				for (int i = 0; i < data[t][a].length; i++) {
					double record = data[t][a][i];

					if (Math.abs(record) > nyquistVelocity && record != -1024 && record != -2048) {
						nyquistVelocity = Math.abs(record);
					}
				}
			}
		}

		System.out.println("nyquistVelocity: " + nyquistVelocity);

		double mult = 1.85;

		for (int t = 0; t < data.length; t++) {
			for (int a = 0; a < data[t].length; a++) {
				double[] radialGate = data[t][a];

				int foldingNum = 0;
				double prevVal = 0;
				double prevValNoFilter = 0;

				for (int i = 0; i < radialGate.length; i++) {
					double val = radialGate[i];

					double nextVal = -1024.0;
					if (i < radialGate.length - 1) {
						nextVal = radialGate[i];
					}

					if (val != -1024.0 && val != -2048.0 && prevVal != -1024.0 && prevVal != -2048.0) {
						if (Math.signum(val) != Math.signum(prevVal)
								&& Math.abs(val - prevVal) > mult * nyquistVelocity) {
							if (nextVal != -1024.0 && nextVal != -2048.0
									&& Math.signum(prevVal) != Math.signum(nextVal)) {
								foldingNum += Math.signum(prevVal);
							}
						}

						dealiasedVelocity[t][a][i] = radialVelocity[t][a][i] + 2 * foldingNum * nyquistVelocity;

						prevVal = val;
					} else if (val == -1024.0) {
						dealiasedVelocity[t][a][i] = -1024.0;
					} else if (val == -2048.0) {
						dealiasedVelocity[t][a][i] = -2048.0;
					}

					prevValNoFilter = val;
				}
			}
		}

		return dealiasedVelocity;
	}

	@SuppressWarnings("unused")
	private double[][][] dealiasVelocityOld(double[][][] data) {
		double[][][] dealiasedVelocity = new double[data.length][data[0].length][data[0][0].length];

		double nyquistVelocity = -1.0;

		for (int t = 0; t < data.length; t++) {
			for (int a = 0; a < data[t].length; a++) {
				for (int i = 0; i < data[t][a].length; i++) {
					double record = data[t][a][i];

					if (Math.abs(record) > nyquistVelocity && record != -1024 && record != -2048) {
						nyquistVelocity = Math.abs(record);
					}
				}
			}
		}

		System.out.println("nyquistVelocity: " + nyquistVelocity);

		int testGate = 360;
		double mult = 1.25;

		for (int t = 0; t < data.length; t++) {
			for (int a = 0; a < data[t].length; a++) {
				double[] radialGate = data[t][a];

				int foldingNum = 0;
				double prevVal = 0;
				double prevValNoFilter = 0;

				for (int i = 0; i < radialGate.length; i++) {
					double val = radialGate[i];

					double nextVal = 0;
					if (i != radialGate.length - 1) {
						nextVal = radialGate[i + 1];
					}

					if (prevVal == prevValNoFilter && val != 1024 && val != -2048
							&& val - prevVal > mult * nyquistVelocity && prevValNoFilter != -1024
							&& prevValNoFilter != -2048) {
						foldingNum++;
					}

					if (prevVal == prevValNoFilter && val != 1024 && val != -2048
							&& val - prevVal < -mult * nyquistVelocity && prevValNoFilter != -1024
							&& prevValNoFilter != -2048) {
						foldingNum--;
					}

//					if((prevValNoFilter == -1024 || prevValNoFilter == -2048) && val != 1024 && val != -2048 && prevVal > 0 && val < 0 && nextVal - val > 0) {
//						foldingNum++;
//					}
//					
//					if((prevValNoFilter == -1024 || prevValNoFilter == -2048) && val != 1024 && val != -2048 && prevVal < 0 && val > 0 && nextVal - val < 0) {
//						foldingNum--;
//					}

					if (val != -1024 && val != -2048) {
						dealiasedVelocity[t][a][i] = val + 2 * foldingNum * nyquistVelocity;
					}

					if (a == testGate - 1) {
						dealiasedVelocity[t][a][i] = -2048;
					}

					if (val != -1024 && val != -2048) {
						prevVal = val;
					}
					prevValNoFilter = val;

					if (a == testGate) {
						System.out.println(i + "\t" + val + "\t" + foldingNum + "\t" + prevValNoFilter + "\t" + prevVal
								+ "\t" + nextVal + "\t" + (val - prevVal) + "\t" + mult * nyquistVelocity);
					}
				}
			}
		}

		return dealiasedVelocity;
	}
}

interface PostProc {
	float process(float data);
}
