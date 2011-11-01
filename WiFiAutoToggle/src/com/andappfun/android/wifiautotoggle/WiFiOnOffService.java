package com.andappfun.android.wifiautotoggle;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
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
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		log = new WiFiLog(getApplicationContext());

		if (log.isDebugEnabled()) {
			log.debug("WiFiOnOffService.onHandleIntent(): " + intent);
		}

		/* get intent uri */
		Uri uri = intent.getData();

		if (uri != null) {
			/* check if the intent has the right data */
			if (uriMatcher.match(uri) == LOCATION_ID) {

				long id = Long.parseLong(uri.getPathSegments().get(1));

				DbAdapter dbAdapter = DbAdapterFactory.getInstance()
						.getDbAdapter(this);

				WiFiLocation wifiLocation = dbAdapter.getLocation(id);

				if (wifiLocation != null) {
					
					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(getApplicationContext());

					/* check if the toggle is enabled */
					if (preferences.getBoolean("enabledKey", true)) {
						toggleWifi(intent, wifiLocation);
					}

				} else {
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
	private void toggleWifi(Intent intent, WiFiLocation wifiLocation) {
		/* get wi-fi manager */
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/* current Wi-Fi state */
		boolean bWiFiEnabled = wifiManager.isWifiEnabled();

		/* is user to be notified (depending on preferences) */
		boolean bNotify = false;

		if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING,
				false)) {
			/* turn Wi-Fi on when entering */
			if (bWiFiEnabled
					|| wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
				/* only notify when Wi-Fi is enabled or is being enabled */
				bNotify = true;
			} else {
				/* turn on Wi-Fi and notify is turning on was successful */
				if (wifiManager.setWifiEnabled(true)) {
					bNotify = true;
				}
			}
			if (bNotify) {
				notifyEnabled(intent, bWiFiEnabled, wifiLocation);
			}
		} else {
			/* turn Wi-Fi off when leaving */
			if (!bWiFiEnabled
					|| wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
				/* only notify when Wi-Fi is disabled or is being disabled */
				bNotify = true;
			} else {
				/* turn off Wi-Fi and notify is turning off was successful */
				if (wifiManager.setWifiEnabled(false)) {
					bNotify = true;
				}
			}
			if (bNotify) {
				notifyDisabled(intent, bWiFiEnabled, wifiLocation);
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
			WiFiLocation wifiLocation) {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String notificationPreference = preferences.getString(
				"notificationIconKey", "always");

		if (notificationPreference.compareTo("always") == 0
				|| (notificationPreference.compareTo("onChange") == 0 && !bWiFiAlreadyEnabled)) {

			/* create notification text including location name */
			StringBuffer b = new StringBuffer();
			if (wifiLocation.getName() != null) {
				b.append(getString(R.string.serviceEntering));
				b.append(" ");
				b.append(wifiLocation.getName());
			}

			Notification notification = new Notification(R.drawable.wifion,
					b.toString(), System.currentTimeMillis());
			Intent notificationIntent = new Intent(this, WiFiMapActivity.class);
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
			WiFiLocation wifiLocation) {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String notificationPreference = preferences.getString(
				"notificationIconKey", "always");

		if (notificationPreference.compareTo("always") == 0
				|| (notificationPreference.compareTo("onChange") == 0 && bWiFiAlreadyEnabled)) {

			/* create notification text including location name */
			StringBuffer b = new StringBuffer();
			if (wifiLocation.getName() != null) {
				b.append(getString(R.string.serviceLeaving));
				b.append(" ");
				b.append(wifiLocation.getName());
			}

			Notification notification = new Notification(R.drawable.wifioff,
					b.toString(), System.currentTimeMillis());
			Intent notificationIntent = new Intent(this, WiFiMapActivity.class);
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
