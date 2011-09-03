package com.andappfun.android.wifiautotoggle;

import java.text.DateFormat;

import com.andappfun.android.wifiautotoggle.R;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ListItemDetailsEdit extends Activity {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private long id; /* location id */

	private TextView locationNameTV;
	private TextView addedDateTimeTV;
	private TextView latitudeTV;
	private TextView longitudeTV;
	private TextView accuracyTV;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_item_details_edit);

		locationNameTV = (TextView) findViewById(R.id.listItemDetailsEditLocationName);
		addedDateTimeTV = (TextView) findViewById(R.id.listItemDetailsEditAddedDateTime);
		latitudeTV = (TextView) findViewById(R.id.listItemDetailsEditLatitude);
		longitudeTV = (TextView) findViewById(R.id.listItemDetailsEditLongitude);
		accuracyTV = (TextView) findViewById(R.id.listItemDetailsEditAccuracy);

		dateFormat = android.text.format.DateFormat.getLongDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);

		final Button cancelButton = (Button) findViewById(R.id.listItemsDetailsEditCancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		final Button doneButton = (Button) findViewById(R.id.listItemsDetailsEditDone);
		doneButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(Definitions.Location.NAME, locationNameTV.getText()
						.toString());
				Uri locationUri = ContentUris.withAppendedId(
						Definitions.Location.CONTENT_URI, id);
				getContentResolver().update(locationUri, values, null, null);
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.listItemsDetailsEditDataSaved,
						Toast.LENGTH_SHORT);
				toast.show();
				finish();
			}
		});
	}

	@Override
	public void onResume() throws IllegalArgumentException {

		/* obtain the location id from the intent */
		Intent intent = getIntent();
		id = intent.getLongExtra(Definitions.Location.TABLE_NAME + "."
				+ Definitions.Location._ID, -1);

		/* validate location id */
		if (id <= 0) {
			throw new IllegalArgumentException("Incorrect location id " + id);
		}

		/* retrieve location data */
		Uri uri = ContentUris.withAppendedId(Definitions.Location.CONTENT_URI,
				id);
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		if (c.moveToFirst()) {
			int nameColumn = c.getColumnIndex(Definitions.Location.NAME);
			int createdDateColumn = c
					.getColumnIndex(Definitions.Location.CREATED_DATE);
			int latitudeColumn = c
					.getColumnIndex(Definitions.Location.LATITUDE);
			int longitudeColumn = c
					.getColumnIndex(Definitions.Location.LONGITUDE);
			int accuracyColumn = c
					.getColumnIndex(Definitions.Location.ACCURACY);
			String name = c.getString(nameColumn);
			long createdDateTime = c.getLong(createdDateColumn);
			double latitude = c.getDouble(latitudeColumn);
			double longitude = c.getDouble(longitudeColumn);
			float accuracy = c.getFloat(accuracyColumn);
			/* update data on the screen */
			locationNameTV.setText(name);
			addedDateTimeTV.setText(dateFormat.format(createdDateTime) + " "
					+ timeFormat.format(createdDateTime));
			latitudeTV.setText("" + latitude);
			longitudeTV.setText("" + longitude);
			accuracyTV.setText("" + accuracy + " m");

			c.close();
		}
		super.onResume();
	}
}
