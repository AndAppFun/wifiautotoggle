package com.andappfun.android.wifiautotoggle;

import com.andappfun.android.wifiautotoggle.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class WiFiOnOffService extends IntentService {

	/**
	 * Version
	 */
	static final public String version = "$Id: WiFiOnOffService.java 13 2011-09-03 01:02:00Z andappfun $";

	/**
	 * notification id
	 */
	public static final int NOTIFICATION_ID = 1;

	private WiFiLog log;

	/* constant for uri matching, see static block for adding uri */
	private static final int LOCATION_ID = 2;

	private static final UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(Definitions.AUTHORITY,
				Definitions.Location.TABLE_NAME + "/#", LOCATION_ID);
	}

	public WiFiOnOffService() {
		super("WiFiOnOffService");

		log = new WiFiLog(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (log.isDebugEnabled()) {
			log.debug("WiFiOnOffService.onHandleIntent(): " + intent);
		}

		/* get intent uri */
		Uri uri = intent.getData();

		if (uri != null) {
			/* check if the intent has the right data */
			if (uriMatcher.match(uri) == LOCATION_ID) {
				/* check if the toggle is enabled */
				SharedPreferences p = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				if (p.getBoolean("enabledKey", true)) {
					toggleWifi(intent);
				}
			}
		}
	}

	/**
	 * Toggle Wi-Fi
	 * 
	 */
	private void toggleWifi(Intent intent) {
		/* get wi-fi manager */
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/* current Wi-Fi state */
		boolean bWiFiEnabled = wifiManager.isWifiEnabled();

		if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING,
				false)) {
			/* turn Wi-Fi on when entering */
			if (wifiManager.setWifiEnabled(true)) {
				notifyEnabled(intent, bWiFiEnabled);
			}
		} else {
			/* turn Wi-Fi off when leaving */
			if (wifiManager.setWifiEnabled(false)) {
				notifyDisabled(intent, bWiFiEnabled);
			}
		}
	}

	/**
	 * Get location name based on the location id from the intent
	 * 
	 * @param intent
	 *            intent with location id as an extra
	 * @return String containing location name, or null if it can't be found
	 */
	private String getLocationName(Intent intent) {
		String name = null;

		Uri uri = intent.getData();
		if (uri != null) {
			ContentResolver cr = getContentResolver();
			Cursor c = cr.query(uri, null, null, null, null);
			if (c.getCount() != 1) {
				if (log.isErrorEnabled()) {
					log.error("WiFiOnOffService.getLocationName(): "
							+ c.getCount() + " rows returned for " + uri);
				}
				throw new RuntimeException(c.getCount() + " rows returned for "
						+ uri);
			}
			if (c.moveToFirst()) {
				name = c.getString(c.getColumnIndex(Definitions.Location.NAME));
			}
			c.close();
		}
		return name;
	}

	/**
	 * Notify that Wi-Fi has been or already is enabled
	 * 
	 * @param bWiFiAlreadyEnabled
	 *            was Wi-Fi enabled before
	 */
	private void notifyEnabled(Intent intent, boolean bWiFiAlreadyEnabled) {
		/* create notification text including location name */
		StringBuffer b = new StringBuffer();
		String name = getLocationName(intent);
		if (name != null) {
			b.append(getString(R.string.serviceEntering));
			b.append(" ");
			b.append(name);
		}

		Notification notification = new Notification(R.drawable.wifion,
				b.toString(), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this,
				WiFiAutoToggleActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		/* set title */
		String title;
		if (bWiFiAlreadyEnabled)
			title = getString(R.string.serviceWiFiAlreadyEnabled);
		else
			title = getString(R.string.serviceWiFiEnabled);

		notification.setLatestEventInfo(getApplicationContext(), title,
				b.toString(), contentIntent);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(WiFiOnOffService.NOTIFICATION_ID,
				notification);
		if (log.isInfoEnabled()) {
			log.info("WiFiOnOffService.notifyEnabled(): text: " + b.toString()
					+ " intent: " + intent);
		}
	}

	/**
	 * Notify user that Wi-Fi has been or already is disabled
	 * 
	 * @param bWiFiAlreadyEnabled
	 *            was Wi-Fi enabled before
	 */
	private void notifyDisabled(Intent intent, boolean bWiFiAlreadyEnabled) {
		/* create notification text including location name */
		StringBuffer b = new StringBuffer();
		String name = getLocationName(intent);
		if (name != null) {
			b.append(getString(R.string.serviceLeaving));
			b.append(" ");
			b.append(name);
		}

		Notification notification = new Notification(R.drawable.wifioff,
				b.toString(), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this,
				WiFiAutoToggleActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		/* set title */
		String title;
		if (bWiFiAlreadyEnabled)
			title = getString(R.string.serviceWiFiDisabled);
		else
			title = getString(R.string.serviceWiFiAlreadyDisabled);

		notification.setLatestEventInfo(getApplicationContext(), title,
				b.toString(), contentIntent);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(WiFiOnOffService.NOTIFICATION_ID,
				notification);
		if (log.isInfoEnabled()) {
			log.info("WiFiOnOffService.notifyDisabled(): text: " + b.toString()
					+ " intent: " + intent);
		}
	}
}
