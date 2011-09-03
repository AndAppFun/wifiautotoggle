package com.andappfun.android.wifiautotoggle;

import java.util.List;

import com.andappfun.android.wifiautotoggle.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiAutoToggleActivity extends ListActivity implements
		LocationListener {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private WiFiLog log;

	private IntentFilter wifiStateFilter;

	/* About dialog id */
	private static final int DIALOG_ABOUT = 1;

	private LocationManager locationManager;

	private WifiManager wifiManager;

	private CheckedTextView autowifiCheckedTextView;
	private TextView autowifiLocationName;
	private TextView autowifiLatitude;
	private TextView autowifiLongitude;
	private TextView autowifiAccuracy;

	private Cursor mCursor;

	private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction()
					.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
						WifiManager.WIFI_STATE_UNKNOWN)) {
				case WifiManager.WIFI_STATE_DISABLED:
					autowifiCheckedTextView.setChecked(false);
					autowifiCheckedTextView.setEnabled(true);
					break;
				case WifiManager.WIFI_STATE_ENABLED:
					autowifiCheckedTextView.setChecked(true);
					autowifiCheckedTextView.setEnabled(true);
					break;
				case WifiManager.WIFI_STATE_DISABLING: /* no break */
				case WifiManager.WIFI_STATE_ENABLING:
					/* disable the button when changing state */
					autowifiCheckedTextView.setEnabled(false);
					break;
				default:
					break;
				}
			}
		}
	};

	/**
	 * Runnable that adds proximity alerts for all known locations
	 * 
	 */
	private class ProximityAlertAdder implements Runnable {
		@Override
		public void run() {
			/* add proximity alerts to all locations */
			Cursor c = managedQuery(Definitions.Location.CONTENT_URI, null,
					null, null, null);
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
					PendingIntent startServiceIntent = PendingIntent
							.getService(getApplicationContext(), 0,
									startIntent, 0);
					locationManager.addProximityAlert(latitude, longitude,
							Definitions.WIFIRADIUS, -1, startServiceIntent);

					if (log.isInfoEnabled()) {
						log.info("WiFiAutoToggleActivity.onCreate(): proximity alert added latitude: "
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

	/**
	 * Asynchronous task that updates location name
	 * 
	 */
	private class LocationNameUpdater extends
			AsyncTask<Location, Integer, String> {

		@Override
		protected String doInBackground(Location... locations) {

			String locationName = new String("");

			/* try to find out the location address */
			Geocoder geocoder = new Geocoder(getApplicationContext());
			try {
				List<Address> list = geocoder.getFromLocation(
						locations[0].getLatitude(),
						locations[0].getLongitude(), 1);

				/* get the first element */
				Address address = list.get(0);
				locationName = address.getAddressLine(0);
			} catch (Exception e) {
				/* address can't be obtained, it won't be displayed */
			}
			return locationName;
		}

		@Override
		protected void onPostExecute(String result) {
			autowifiLocationName.setText(result);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		log = new WiFiLog(this);

		setContentView(R.layout.autowifi);

		registerForContextMenu(getListView());

		mCursor = this.getContentResolver().query(
				Definitions.Location.CONTENT_URI, null, null, null, null);
		startManagingCursor(mCursor);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.list_item, mCursor, new String[] {
						Definitions.Location.NAME,
						Definitions.Location.CREATED_DATE }, new int[] {
						R.id.listLocationName, R.id.listLocationCreated });
		adapter.setViewBinder(new LocationListViewBinder(this));
		setListAdapter(adapter);

		/* get Wi-Fi manager */
		wifiManager = (WifiManager) getSystemService(WiFiOnOffService.WIFI_SERVICE);

		/* register receiver for Wi-Fi state updates */
		wifiStateFilter = new IntentFilter(
				WifiManager.WIFI_STATE_CHANGED_ACTION);
		registerReceiver(wifiStateReceiver, wifiStateFilter);

		/* get location manager */
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		/* get screen elements */
		autowifiCheckedTextView = (CheckedTextView) findViewById(R.id.autowifiCheckedTextView);
		autowifiCheckedTextView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (log.isInfoEnabled()) {
					log.info("WiFiAutoToggleActivity.onCreate().OnTouchListener.onTouch(): set WiFi enabled to : "
							+ !wifiManager.isWifiEnabled());
				}
				wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
				return true;
			}
		});

		autowifiLocationName = (TextView) findViewById(R.id.autowifiLocationName);
		autowifiLatitude = (TextView) findViewById(R.id.autowifiLatitude);
		autowifiLongitude = (TextView) findViewById(R.id.autowifiLongitude);
		autowifiAccuracy = (TextView) findViewById(R.id.autowifiAccuracy);

		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				viewDetails(id);
			}
		});

		cancelNotification();

		/* add proximity alerts to all locations */
		Thread t = new Thread(new ProximityAlertAdder());
		t.start();
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(wifiStateReceiver, wifiStateFilter);

		updateWiFi();

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

		/* get last known location */
		Location location = locationManager.getLastKnownLocation(provider);
		updateLocation(location);

		/* register for location updates */
		locationManager.requestLocationUpdates(provider, 0, 0, this);
	}

	private void updateLocation(Location location) {
		if (location != null) {
			autowifiLatitude.setText(Double.toString(location.getLatitude()));
			autowifiLongitude.setText(Double.toString(location.getLongitude()));
			autowifiAccuracy.setText(Float.toString(location.getAccuracy())
					+ " m");
			new LocationNameUpdater().execute(location);
		} else {
			autowifiLocationName.setText("");
			autowifiLatitude.setText("");
			autowifiLongitude.setText("");
			autowifiAccuracy.setText("");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
		unregisterReceiver(wifiStateReceiver);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		cancelNotification();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_item_about:
			showDialog(DIALOG_ABOUT);
			return true;

		case R.id.menu_item_prefs:
			Intent launchPreferences = new Intent();
			launchPreferences.setClass(this, Preferences.class);
			startActivity(launchPreferences);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_context_menu, menu);
		menu.setHeaderTitle(R.string.listContextMenuTitle);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		/* retrieve id */
		final long id = info.id;

		switch (item.getItemId()) {
		case R.id.listContextMenuView:
			viewDetails(id);
			return true;
		case R.id.listContextMenuEdit:
			editDetails(id);
			return true;
		case R.id.listContextMenuDelete:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.wifiDeleteLocationQuestion);
			builder.setCancelable(false);

			builder.setPositiveButton(R.string.wifiDeleteYes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							/* remove location */
							Uri uri = ContentUris.withAppendedId(
									Definitions.Location.CONTENT_URI, id);
							getContentResolver().delete(uri, null, null);

							/* remove proximity alert */
							Intent intent = new Intent(getApplicationContext(),
									WiFiOnOffService.class);
							PendingIntent startServiceIntent = PendingIntent
									.getService(getApplicationContext(), 0,
											intent, 0);
							locationManager
									.removeProximityAlert(startServiceIntent);
							if (log.isInfoEnabled()) {
								log.info("WiFiAutoToggleActivity.onContextItemSelected() delete, remove proximity alert for location: "
										+ id);
							}

							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.wifiDeleteDeleted,
									Toast.LENGTH_SHORT);
							toast.show();
						}
					});

			builder.setNegativeButton(R.string.wifiDeleteNo, null);
			builder.show();
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case DIALOG_ABOUT:
			dialog = new Dialog(this);
			dialog.setTitle(R.string.dialogAboutTitle);
			dialog.setContentView(R.layout.dialog_about);
			TextView versionTV = (TextView) dialog
					.findViewById(R.id.dialogAboutVersion);
			String versionString = getString(R.string.dialogAboutVersion);

			PackageInfo pi = null;
			try {
				pi = getPackageManager().getPackageInfo(getPackageName(),
						PackageManager.GET_META_DATA);
				versionTV.setText(versionString + " " + pi.versionName + " ("
						+ pi.versionCode + ")");
			} catch (NameNotFoundException e) {
				if (log.isErrorEnabled()) {
					log.error("WiFiAutoToggleActivity.onCreateDialog DIALOG_ABOUT error: "
							+ e.toString());
				}
			}
			break;
		default:
			break;
		}
		return dialog;
	}

	private void updateWiFi() {
		autowifiCheckedTextView.setChecked(wifiManager.isWifiEnabled());
	}

	/**
	 * cancel notification.
	 */
	private void cancelNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(WiFiOnOffService.NOTIFICATION_ID);
	}

	/**
	 * View location details.
	 */
	private void viewDetails(long id) {
		Intent intent = new Intent(WiFiAutoToggleActivity.this,
				ListItemDetailsView.class);
		intent.putExtra(Definitions.Location.TABLE_NAME + "."
				+ Definitions.Location._ID, id);
		startActivity(intent);
	}

	/**
	 * Edit location details.
	 */
	private void editDetails(long id) {
		Intent intent = new Intent(WiFiAutoToggleActivity.this,
				ListItemDetailsEdit.class);
		intent.putExtra(Definitions.Location.TABLE_NAME + "."
				+ Definitions.Location._ID, id);
		startActivity(intent);
	}

	// LocationListener part

	@Override
	public void onLocationChanged(Location location) {
		updateLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}