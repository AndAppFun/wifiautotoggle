package com.andappfun.android.wifiautotoggle;

import java.text.DateFormat;

import com.andappfun.android.wifiautotoggle.R;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class ListItemDetailsView extends Activity {

	/**
	 * Version
	 */
	static final public String version = "$Id: ListItemDetailsView.java 13 2011-09-03 01:02:00Z andappfun $";

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

		setContentView(R.layout.list_item_details_view);

		locationNameTV = (TextView) findViewById(R.id.listItemDetailsViewLocationName);
		addedDateTimeTV = (TextView) findViewById(R.id.listItemDetailsViewAddedDateTime);
		latitudeTV = (TextView) findViewById(R.id.listItemDetailsViewLatitude);
		longitudeTV = (TextView) findViewById(R.id.listItemDetailsViewLongitude);
		accuracyTV = (TextView) findViewById(R.id.listItemDetailsViewAccuracy);

		dateFormat = android.text.format.DateFormat.getLongDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		Long id = intent.getLongExtra(Definitions.Location.TABLE_NAME + "."
				+ Definitions.Location._ID, -1);

		/* validate location id */
		if (id <= 0) {
			throw new IllegalArgumentException("Incorrect location id " + id);
		}

		/* retrieve location data */
		Uri uri = ContentUris.withAppendedId(Definitions.Location.CONTENT_URI,
				id);
		Cursor c = this.getContentResolver().query(uri, null, null, null, null);
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
	}

}
