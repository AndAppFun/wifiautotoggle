package com.andappfun.android.wifiautotoggle;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

public class LocationChangedService extends IntentService {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	public LocationChangedService() {
		super("LocationChangedService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		/* setup log */
		WiFiLog log = new WiFiLog(this);

		Bundle bundle = intent.getExtras();
		Location location = (Location) bundle
				.get(LocationManager.KEY_LOCATION_CHANGED);

		if (location != null) {
			/* check accuracy */
			if (location.hasAccuracy()
					&& location.getAccuracy() <= Definitions.WIFIRADIUS) {

				/* check Wi-Fi connection */
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (networkInfo.isConnected()) {

					/* remove location updates request */
					PendingIntent pendingIntent = PendingIntent.getService(
							this, 0, intent, 0);

					LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					locationManager.removeUpdates(pendingIntent);
					if (log.isInfoEnabled()) {
						log.info("LocationChangedService.onHandleIntent(): WiFi connected - remove location updates");
					}

					/* add location */
					
					DbAdapter dbAdapter = DbAdapterFactory.getInstance().getDbAdapter(this);
					
					WiFiLocation wifiLocationToAdd = new WiFiLocation(location.getLatitude(),
							location.getLongitude(), location.getAccuracy());
					
					WiFiLocation wifiLocation = dbAdapter.addLocation(wifiLocationToAdd);
					/* new location has been added */
					if (wifiLocation != null) {
						Intent startIntent = new Intent(this,
								WiFiOnOffService.class);
						startIntent.setData(ContentUris.withAppendedId(Definitions.Location.CONTENT_URI, wifiLocation.getId()));
						PendingIntent startServiceIntent = PendingIntent
								.getService(this, 0, startIntent, 0);
						locationManager.addProximityAlert(
								location.getLatitude(),
								location.getLongitude(),
								Definitions.WIFIRADIUS, -1, startServiceIntent);
						if (log.isInfoEnabled()) {
							log.info("LocationChangedServive.onHandleIntent(): proximity alert added latitude: "
									+ wifiLocation.getLatitude()
									+ " longitude: "
									+ wifiLocation.getLongitude()
									+ " intent: "
									+ startIntent);
						}
					}
				}
			}
		}
	}
}
