package com.andappfun.android.wifiautotoggle;

import java.text.DateFormat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiLocationDetailsActivity extends Activity {

	/**
	 * Version
	 */
	static final public String version = "$Id: ListItemDetailsEdit.java 3 2011-09-03 01:47:39Z andappfun $";

	/* constants for uri matching, see static block for adding uri */
	private static final int LOCATION_ID = 1;

	private DbAdapter dbAdapter;

	private static final UriMatcher uriMatcher;

	private long id; /* location id */

	private WiFiLog log;

	private WiFiLocation wifiLocation;

	private TextView locationNameTV;
	private TextView addedDateTimeTV;
	private TextView latitudeTV;
	private TextView longitudeTV;
	private TextView accuracyTV;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(Definitions.AUTHORITY,
				Definitions.Location.TABLE_NAME + "/#", LOCATION_ID);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log = new WiFiLog(this);

		dbAdapter = DbAdapterFactory.getInstance().getDbAdapter(this);

		setContentView(R.layout.wifi_location_details);

		locationNameTV = (TextView) findViewById(R.id.wifiLocationDetailsEditLocationName);
		addedDateTimeTV = (TextView) findViewById(R.id.wifiLocationDetailsEditAddedDateTime);
		latitudeTV = (TextView) findViewById(R.id.wifiLocationDetailsEditLatitude);
		longitudeTV = (TextView) findViewById(R.id.wifiLocationDetailsEditLongitude);
		accuracyTV = (TextView) findViewById(R.id.wifiLocationDetailsEditAccuracy);

		dateFormat = android.text.format.DateFormat.getLongDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);

		final Button saveButton = (Button) findViewById(R.id.wifiLocationDetailsEditSave);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				wifiLocation.setName(locationNameTV.getText().toString());
				if (dbAdapter.updateLocation(wifiLocation) == 1) {
					Toast toast = Toast.makeText(getApplicationContext(),
							R.string.wifiLocationDetailsEditDataSaved,
							Toast.LENGTH_SHORT);
					toast.show();
				} else {
					if (log.isErrorEnabled()) {
						log.error("WiFiLocationDetailsActivity.OnClickListener.onClick: save: location "
								+ id
								+ " name not updated to "
								+ locationNameTV.getText().toString());
					}
				}
				finish();
			}
		});
		
		final Button deleteButton = (Button) findViewById(R.id.wifiLocationDetailsEditDelete);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (dbAdapter.deleteLocation(id) == 1) {
					/* remove proximity alert */
					Intent startIntent = new Intent(
							getApplicationContext(),
							WiFiOnOffService.class);
					
					/* remove proximity alert for this location */
					Uri uri = ContentUris.withAppendedId(
							Definitions.Location.CONTENT_URI, id);
					startIntent.setData(uri);
					PendingIntent startServiceIntent = PendingIntent
							.getService(getApplicationContext(), 0,
									startIntent, 0);
					
					/* get location manager */
					LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					locationManager
							.removeProximityAlert(startServiceIntent);
					if (log.isInfoEnabled()) {
						log.info("WiFiLocationDetailsActivity.OnClickListener.onClick(): delete: remove proximity alert for location: "
								+ id);
					}

					Toast toast = Toast.makeText(getApplicationContext(),
							R.string.wifiLocationDetailsEditDataDeleted,
							Toast.LENGTH_SHORT);
					toast.show();
				} else {
					if (log.isErrorEnabled()) {
						log.error("WiFiLocationDetailsActivity.OnClickListener.onClick(): delete: location "
								+ id
								+ " not deleted");
					}
				}
				finish();
			}
		});
		
		final Button cancelButton = (Button) findViewById(R.id.wifiLocationDetailsEditCancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();

		/* obtain the location id from the intent */
		Intent intent = getIntent();
		Uri uri = intent.getData();

		// Validate the requested uri
		if (uriMatcher.match(uri) != LOCATION_ID) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		id = Long.parseLong(uri.getPathSegments().get(1));

		wifiLocation = dbAdapter.getLocation(id);

		if (wifiLocation != null) {
			/* update data on the screen */
			locationNameTV.setText(wifiLocation.getName());
			addedDateTimeTV.setText(dateFormat.format(wifiLocation
					.getCreatedDateTime())
					+ " "
					+ timeFormat.format(wifiLocation.getCreatedDateTime()));
			latitudeTV.setText(Double.toString(wifiLocation.getLatitude()));
			longitudeTV.setText(Double.toString(wifiLocation.getLongitude()));
			accuracyTV.setText(Float.toString(wifiLocation.getAccuracy())
					+ " m");
		} else {
			if (log.isErrorEnabled()) {
				log.error("WiFiLocationDetailsActivity.onResume(): location: " + id
						+ " not found, finishing activity");
			}
			finish();
		}
	}
}
