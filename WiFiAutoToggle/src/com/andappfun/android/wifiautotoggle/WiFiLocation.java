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
	static final public String version = "$Id: WiFiLocation.java 13 2011-09-03 01:02:00Z andappfun $";

	public double latitude;
	public double longitude;
	public float accuracy;

	WiFiLocation(double latitude, double longitude, float accuracy) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
	}

	/**
	 * Returns true if the given latitude, longitude, accuracy is equivalent to
	 * the Wi-Fi location
	 * 
	 * @return true if the given latitude, longitude, accuracy is equivalent to
	 *         the Wi-Fi location
	 */
	public boolean isEquivalent(double latitude, double longitude,
			float accuracy) {

		/* find the worst accuracy */
		float worstAccuracy = this.accuracy;
		if (worstAccuracy < accuracy)
			worstAccuracy = accuracy;

		float results[] = new float[3];

		Location.distanceBetween(this.latitude, this.longitude, latitude,
				longitude, results);
		return results[0] <= worstAccuracy;
	}
}
