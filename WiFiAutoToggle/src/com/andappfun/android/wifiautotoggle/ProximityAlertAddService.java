package com.andappfun.android.wifiautotoggle;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;

public class ProximityAlertAddService extends IntentService {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	public ProximityAlertAddService() {
		super("ProximityAlertAddService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		WiFiLog log = new WiFiLog(this);

		/* get location manager */
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		/* add proximity alerts to all locations */
		Cursor c = getContentResolver().query(Definitions.Location.CONTENT_URI,
				null, null, null, null);
		if (c.moveToFirst()) {
			int latitudeColumn = c
					.getColumnIndex(Definitions.Location.LATITUDE);
			int longitudeColumn = c
					.getColumnIndex(Definitions.Location.LONGITUDE);
			int idColumn = c.getColumnIndex(Definitions.Location._ID);
			do {
				double latitude = c.getDouble(latitudeColumn);
				double longitude = c.getDouble(longitudeColumn);
				int id = c.getInt(idColumn);
				Intent startIntent = new Intent(getApplicationContext(),
						WiFiOnOffService.class);
				Uri uri = ContentUris.withAppendedId(
						Definitions.Location.CONTENT_URI, id);
				startIntent.setData(uri);
				PendingIntent startServiceIntent = PendingIntent.getService(
						getApplicationContext(), 0, startIntent, 0);
				locationManager.addProximityAlert(latitude, longitude,
						Definitions.WIFIRADIUS, -1, startServiceIntent);

				if (log.isInfoEnabled()) {
					log.info("ProximityAlertAddService.onHandleIntent(): proximity alert added latitude: "
							+ latitude
							+ " longitude: "
							+ longitude
							+ " intent: " + startIntent);
				}
			} while (c.moveToNext());
		}
		c.close();
	}
}
