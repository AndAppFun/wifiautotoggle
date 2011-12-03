package com.andappfun.android.wifiautotoggle;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WiFiStateStore {

	/**
	 * Version
	 */
	static final public String version = "$Id$";
	
	static final private String STATE_KEY = "wifiStateKey"; 

	static final private String STATE_DATE_KEY = "wifiStateDateKey";
	
	private WiFiLog log;

	private SharedPreferences preferences;

	/**
	 * Create WiFiStateStore for a given context. Context is used to store and retrieve WiFi state as know to the application 
	 * @param context Context for which WiFiStateStore is created
	 */
	public WiFiStateStore(Context context) {
		log = new WiFiLog(context);

		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Store Wi-Fi enabled state
	 */
	public void storeEnabled() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(WiFiStateStore.STATE_KEY, Definitions.WIFI_ON);
		editor.putLong(WiFiStateStore.STATE_DATE_KEY, new Date().getTime());
		editor.commit();
		if (log.isDebugEnabled()) {
			log.debug("WiFiStateStore.storeEnabled()");
		}
	}

	/**
	 * Store Wi-Fi disabled state
	 */
	public void storeDisabled() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(WiFiStateStore.STATE_KEY, Definitions.WIFI_OFF);
		editor.putLong(WiFiStateStore.STATE_DATE_KEY, new Date().getTime());
		editor.commit();
		if (log.isDebugEnabled()) {
			log.debug("WiFiStateStore.storeDisabled()");
		}
	}
	
	/**
	 * Retrieves stored Wi-Fi state
	 * @return Wi-Fi state
	 */
	public String getState() {
		return preferences.getString(WiFiStateStore.STATE_KEY, Definitions.WIFI_UNKNOWN);
	}
	
	/**
	 * Returns true when the time stamp of the last Wi-Fi state store is relatively new comparing to current date/time
	 * @return true when the time stamp of the last Wi-Fi state store is relatively new comparing to current date/time, false otherwise
	 */
	public boolean isRelativelyNew() {
		boolean bRelativelyNew = false;
		
		long stateDateLong = preferences.getLong(WiFiStateStore.STATE_DATE_KEY, -1);
		if (stateDateLong > 0) {
			long currentDateLong = new Date().getTime();
			
			/* state is relatively new when it has been stored no earlier than 10 seconds ago */
			bRelativelyNew = ((currentDateLong - stateDateLong) < 10000);
			if (log.isDebugEnabled()) {
				log.debug("WiFiStateStore.isRelativelyNew(): state date: " + new Date(stateDateLong).toGMTString() + " bRelativelyNew: " + bRelativelyNew);
			}
		}
		return bRelativelyNew;
	}
}
