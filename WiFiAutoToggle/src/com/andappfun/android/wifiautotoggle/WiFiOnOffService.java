package com.andappfun.android.wifiautotoggle;

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
	static final public String version = "$Id$";

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
				ContentResolver cr = getContentResolver();
				Cursor c = cr.query(uri, null, null, null, null);
				switch (c.getCount()) {

				case 1:
					/* check if the toggle is enabled */
					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(getApplicationContext());
					if (preferences.getBoolean("enabledKey", true)) {
						String name = null;
						if (c.moveToFirst()) {
							name = c.getString(c
									.getColumnIndex(Definitions.Location.NAME));
						}
						toggleWifi(intent, name);
					}
					break;

				case 0:
					/* no such location in the database, remove proximity alert */
					Intent startIntent = new Intent(getApplicationContext(),
							WiFiOnOffService.class);
					startIntent.setData(uri);
					PendingIntent startServiceIntent = PendingIntent
							.getService(getApplicationContext(), 0,
									startIntent, 0);
					LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					locationManager.removeProximityAlert(startServiceIntent);
					if (log.isInfoEnabled()) {
						log.info("WiFiOnOffService.onHandleIntent(): location "
								+ uri.getPathSegments().get(1)
								+ " not found, remove proximity alert");
					}

					break;

				default:
					/* log an error */
					if (log.isErrorEnabled()) {
						log.error("WiFiOnOffService.onHandleIntent():  "
								+ c.getCount() + " rows found for location: "
								+ uri);
					}
					break;
				}
			}
		}
	}

	/**
	 * Toggle Wi-Fi
	 * 
	 * @param name
	 *            location name
	 * 
	 */
	private void toggleWifi(Intent intent, String name) {
		/* get wi-fi manager */
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/* current Wi-Fi state */
		boolean bWiFiEnabled = wifiManager.isWifiEnabled();

		if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING,
				false)) {
			/* turn Wi-Fi on when entering */
			if (wifiManager.setWifiEnabled(true)) {
				notifyEnabled(intent, bWiFiEnabled, name);
			}
		} else {
			/* turn Wi-Fi off when leaving */
			if (wifiManager.setWifiEnabled(false)) {
				notifyDisabled(intent, bWiFiEnabled, name);
			}
		}
	}

	/**
	 * Notify that Wi-Fi has been or already is enabled
	 * 
	 * @param bWiFiAlreadyEnabled
	 *            was Wi-Fi enabled before
	 * @param name
	 *            location name
	 */
	private void notifyEnabled(Intent intent, boolean bWiFiAlreadyEnabled,
			String name) {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String notificationPreference = preferences.getString(
				"notificationIconKey", "always");

		if (notificationPreference.compareTo("always") == 0
				|| (notificationPreference.compareTo("onChange") == 0 && !bWiFiAlreadyEnabled)) {

			/* create notification text including location name */
			StringBuffer b = new StringBuffer();
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
				log.info("WiFiOnOffService.notifyEnabled(): text: "
						+ b.toString() + " intent: " + intent);
			}
		}
	}

	/**
	 * Notify user that Wi-Fi has been or already is disabled
	 * 
	 * @param bWiFiAlreadyEnabled
	 *            was Wi-Fi enabled before
	 * @param name
	 *            location name
	 */
	private void notifyDisabled(Intent intent, boolean bWiFiAlreadyEnabled,
			String name) {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String notificationPreference = preferences.getString(
				"notificationIconKey", "always");

		if (notificationPreference.compareTo("always") == 0
				|| (notificationPreference.compareTo("onChange") == 0 && bWiFiAlreadyEnabled)) {

			/* create notification text including location name */
			StringBuffer b = new StringBuffer();
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
				log.info("WiFiOnOffService.notifyDisabled(): text: "
						+ b.toString() + " intent: " + intent);
			}
		}
	}
}
