package com.ameliaWx.radarView;

public class PointD {
	private double x;

	private double y;

	public PointD(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void add(PointD p) {
		x += p.x;
		y += p.y;
	}

	public void subtract(PointD p) {
		x -= p.x;
		y -= p.y;
	}

	public PointD unitVector() {
		return new PointD(x / Math.hypot(x, y), y / Math.hypot(x, y));
	}

	public PointD projectToStereographic(double lonOffset) {
		return new PointD(
				(Math.cos(Math.toRadians(-y)) * Math.cos(Math.toRadians(lonOffset - x)))
						/ (1 - Math.asin(Math.toRadians(-y))),
				(Math.cos(Math.toRadians(-y)) * Math.sin(Math.toRadians(lonOffset - x)))
						/ (1 - Math.asin(Math.toRadians(-y))));
	}

	public void mult(double s) {
		System.out.println(x);
		System.out.println(y);
		x *= s;
		y *= s;
		System.out.println(x);
		System.out.println(y);
	}

	public static PointD add(PointD p, PointD q) {
		return new PointD(p.x + q.x, p.y + q.y);
	}

	public static PointD subtract(PointD p, PointD q) {
		return new PointD(p.x - q.x, p.y - q.y);
	}

	public static PointD unitVector(PointD p) {
		return new PointD(p.x / Math.hypot(p.x, p.y), p.y / Math.hypot(p.x, p.y));
	}

	public static PointD mult(PointD p, double s) {
		// System.out.println(p.x);
		// System.out.println(p.y);
		// System.out.println(new PointD(p.x * s, p.y * s));
		return new PointD(p.x * s, p.y * s);
	}

	@Override
	public String toString() {
		return "PointD [x=" + x + ", y=" + y + "]";
	}
}
