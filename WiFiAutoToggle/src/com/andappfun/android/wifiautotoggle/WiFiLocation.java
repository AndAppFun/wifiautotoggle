package com.andappfun.android.wifiautotoggle;

import android.location.Location;

/**
 * Utility class for Wi-Fi location
 * 
 */

public class WiFiLocation {

	/**
	 * Version
	 */
	static final public String version = "$Id$";
	
	/**
	 * Approximate latitude range for Wi-Fi radius
	 */
	public static final double LATITUDERANGE = 0.05;
	
	/**
	 * Approximate longitude range for Wi-Fi radius
	 */
	public static final double LONGITUDERANGE = 0.25;
	
	// minimal latitude
	private static final double MINLATITUDE = -90.0;
	// maximal latitude
	private static final double MAXLATITUDE = 90.0;

	// minimal longitude
	private static final double MINLONGITUDE = -180.0;
	// maximal longitude
	private static final double MAXLONGITUDE = 180.0;
	
	private long id;
	private String name;
	private double latitude;
	private double longitude;
	private float accuracy;
	private long createdDateTime;

	WiFiLocation(double latitude, double longitude, float accuracy) {
		this.id = -1;
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
		this.createdDateTime = -1;
	}
	
	WiFiLocation(long id, double latitude, double longitude, float accuracy) {
		this (latitude, longitude, accuracy);
		this.id = id;
	}

	/**
	 * Returns true when the given location is equivalent to this one, false otherwise
	 * @param wifiLocation Wi-Fi location
	 * @param accuracy accuracy
	 * @return true when the given location is equivalent to this one, false otherwise
	 */
	public boolean isEquivalent(WiFiLocation wifiLocation, float accuracy) {
		/* find the worst accuracy */
		float worstAccuracy = this.accuracy;
		if (worstAccuracy < accuracy)
			worstAccuracy = accuracy;

		float results[] = new float[3];

		Location.distanceBetween(this.latitude, this.longitude, wifiLocation.getLatitude(),
				wifiLocation.getLongitude(), results);
		return results[0] <= worstAccuracy;
	}

	/**
	 * Returns location id
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns location name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns latitude
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns longitude
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Returns accuracy
	 * @return the accuracy
	 */
	public float getAccuracy() {
		return accuracy;
	}

	/**
	 * Returns created date/time
	 * @return the createdDateTime
	 */
	public long getCreatedDateTime() {
		return createdDateTime;
	}

	/**
	 * Sets location name
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets location created date/time
	 * @param createdDateTime location created date/time
	 */
	public void setCreatedDateTime(long createdDateTime) {
		this.createdDateTime = createdDateTime;
	}
	
	/**
	 * Normalize latitude by not capping it at 90 or -90 degrees
	 * 
	 * @param latitude
	 *            latitude
	 * @return normalized latitude
	 */
	public static double normalizeLatitude(double latitude) {
		double l = latitude;

		while (l > MAXLATITUDE) {
			l = MAXLATITUDE;
		}

		while (l <= MINLATITUDE) {
			l =MINLATITUDE;
		}

		return l;
	}

	/**
	 * Normalize longitude
	 * 
	 * @param longitude
	 *            longitude
	 * @return normalized longitude
	 */
	public static double normalizeLongitude(double longitude) {
		double l = longitude;

		while (l > MAXLONGITUDE) {
			l -= (2 * MAXLONGITUDE);
		}

		while (l <= MINLONGITUDE) {
			l += (-2 * MINLONGITUDE);
		}

		return l;
	}
}
