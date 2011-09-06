package com.andappfun.android.wifiautotoggle;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.wifi.WifiManager;

public class WiFiChangedReceiver extends BroadcastReceiver {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	@Override
	public void onReceive(Context context, Intent intent) {

		/* set up log */
		WiFiLog log = new WiFiLog(context);

		int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
				WifiManager.WIFI_STATE_DISABLED);

		switch (state) {
		case WifiManager.WIFI_STATE_ENABLED: {
			LocationManager locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

			/* define criteria */
			Criteria criteria = new Criteria();
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setSpeedRequired(false);
			criteria.setCostAllowed(false);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			criteria.setAccuracy(Criteria.ACCURACY_FINE);

			/* get best provider */
			String provider = locationManager.getBestProvider(criteria, true);

			if (provider != null) {

				Intent startIntent = new Intent(context,
						LocationChangedService.class);
				PendingIntent pendingIntent = PendingIntent.getService(context,
						0, startIntent, 0);

				/* request location updates */
				locationManager.requestLocationUpdates(provider, 0, 0,
						pendingIntent);
				if (log.isInfoEnabled()) {
					log.info("WiFiChangedReceiver.onReceive(): WiFi enabled - requesting location updates from "
							+ provider + " provider");
				}
				/*
				 * location updates request will be removed from the service
				 * once needed location has been obtained
				 */
			} else {
				if (log.isErrorEnabled()) {
					log.error("WiFiChangedReceiver.onReceive(): unable to obtain location provider, location updates not requested");
				}
			}

		}
			break;

		case WifiManager.WIFI_STATE_DISABLED: {
			LocationManager locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

			Intent startIntent = new Intent(context,
					LocationChangedService.class);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					startIntent, 0);

			/* remove location updates request */
			locationManager.removeUpdates(pendingIntent);
			if (log.isInfoEnabled()) {
				log.info("WiFiChangedReceiver.onReceive(): WiFi disabled - remove location updates");
			}
		}
			break;

		default:
			break;
		}
	}
}
