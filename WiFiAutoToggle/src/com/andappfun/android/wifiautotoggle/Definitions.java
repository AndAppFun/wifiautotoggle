package com.andappfun.android.wifiautotoggle;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Definitions
 * 
 */

public final class Definitions {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	/**
	 * Authority for Wi-Fi location provider
	 */
	public static final String AUTHORITY = "com.andappfun.android.wifiautotoggle.wifilocationprovider";

	/**
	 * Wi-Fi on (state as known to this application)
	 */
	public static final String WIFI_ON = "on";

	/**
	 * Wi-Fi off (state as known to this application)
	 */
	public static final String WIFI_OFF = "off";

	/**
	 * Wi-Fi unknown (state as known to this application)
	 */
	public static final String WIFI_UNKNOWN = "unknown";

	/**
	 * Wi-Fi range
	 */
	public static final float WIFIRADIUS = (float) 300.0;

	// This class cannot be instantiated
	private Definitions() {
	}

	/**
	 * Location table
	 */
	public static final class Location implements BaseColumns {
		// This class cannot be instantiated
		private Location() {
		}

		/**
		 * The name of the location table
		 */
		public static final String TABLE_NAME = "location";

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + TABLE_NAME);

		/**
		 * The name of the location
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		/**
		 * The latitude of the location
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String LATITUDE = "latitude";

		/**
		 * The longitude of the location
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String LONGITUDE = "longitude";

		/**
		 * The accuracy of the location coordinates
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String ACCURACY = "accuracy";

		/**
		 * The timestamp for when the location was created
		 * <P>
		 * Type: INTEGER (long from System.curentTimeMillis())
		 * </P>
		 */
		public static final String CREATED_DATE = "created_date";

		/**
		 * The default location sort order descending by created date/time
		 */
		public static final String DEFAULT_SORT_ORDER = CREATED_DATE + " DESC";

	}
}
