package com.andappfun.android.wifiautotoggle;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class WiFiMapActivity extends MapActivity {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private LocationManager locationManager;

	private CurrentLocationOverlay myLocationOverlay;
	
	private StoredLocationsOverlay storedLocationsOverlay;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.current_location);

		MapView mapView = (MapView) findViewById(R.id.mapview);

		/* get location manager */
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		OverlaySwitcher switcher = new OverlaySwitcher();

		storedLocationsOverlay = new StoredLocationsOverlay (switcher, this);
		switcher.addOverlay(storedLocationsOverlay);
		mapView.getOverlays().add(storedLocationsOverlay);
		
		myLocationOverlay = new CurrentLocationOverlay(switcher, this, mapView);
		switcher.addOverlay(myLocationOverlay);
		mapView.getOverlays().add(myLocationOverlay);

		/* start service to add proximity alerts to all locations */
		Intent startServiceIntent = new Intent(this,
				ProximityAlertAddService.class);
		startService(startServiceIntent);
	}

	@Override
	protected void onPause() {
		myLocationOverlay.disableMyLocation();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		cancelNotification();

		/* go to last known location */
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
			/* go to the last known location */
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				myLocationOverlay.onLocationChanged(location);
			}
		}
	}

	/**
	 * cancel notification.
	 */
	private void cancelNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(WiFiOnOffService.NOTIFICATION_ID);
	}

	@Override
	public void onNewIntent(Intent intent) {
		cancelNotification();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// no route is displayed
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.map_menu_item_about:
			showDialog(WiFiDialogFactory.DIALOG_ABOUT);
			return true;

		case R.id.map_menu_item_prefs:
			Intent launchPreferences = new Intent();
			launchPreferences.setClass(this, Preferences.class);
			startActivity(launchPreferences);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case WiFiDialogFactory.DIALOG_ABOUT:
			dialog = WiFiDialogFactory.createAboutDialog(this);
			break;
		default:
			break;
		}
		return dialog;
	}
}
