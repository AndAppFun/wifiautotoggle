package com.andappfun.android.wifiautotoggle;

import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.text.TextUtils;

public class WiFiLocationProvider extends ContentProvider {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private static final String DATABASE_NAME = "autowifitoggle.db";
	private static final int DATABASE_VERSION = 1;

	/* constants for uri matching, see static block for adding uri */
	private static final int LOCATION = 1;
	private static final int LOCATION_ID = 2;

	private static HashMap<String, String> locationProjectionMap;

	private static final UriMatcher uriMatcher;

	private DatabaseHelper mOpenHelper;
	
	private WiFiLog log;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(Definitions.AUTHORITY,
				Definitions.Location.TABLE_NAME, LOCATION);
		uriMatcher.addURI(Definitions.AUTHORITY,
				Definitions.Location.TABLE_NAME + "/#", LOCATION_ID);

		locationProjectionMap = new HashMap<String, String>();
		locationProjectionMap.put(Definitions.Location._ID,
				Definitions.Location._ID);
		locationProjectionMap.put(Definitions.Location.NAME,
				Definitions.Location.NAME);
		locationProjectionMap.put(Definitions.Location.LATITUDE,
				Definitions.Location.LATITUDE);
		locationProjectionMap.put(Definitions.Location.LONGITUDE,
				Definitions.Location.LONGITUDE);
		locationProjectionMap.put(Definitions.Location.ACCURACY,
				Definitions.Location.ACCURACY);
		locationProjectionMap.put(Definitions.Location.CREATED_DATE,
				Definitions.Location.CREATED_DATE);
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			/* create location table */
			String sqlLocation = "CREATE TABLE "
					+ Definitions.Location.TABLE_NAME + " ("
					+ Definitions.Location._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Definitions.Location.NAME + " TEXT, "
					+ Definitions.Location.LATITUDE + " REAL, "
					+ Definitions.Location.LONGITUDE + " REAL, "
					+ Definitions.Location.ACCURACY + " REAL, "
					+ Definitions.Location.CREATED_DATE + " INTEGER" + ");";
			db.execSQL(sqlLocation);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// nothing to do
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Validate the requested uri
		if (uriMatcher.match(uri) != LOCATION_ID) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int nRows = db.delete(Definitions.Location.TABLE_NAME,
				Definitions.Location._ID + "=?", new String[] { uri
						.getPathSegments().get(1) });
		getContext().getContentResolver().notifyChange(uri, null);
		return nRows;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	/**
	 * Insert new location
	 * 
	 * @return uri of the newly added location, or null when location exists
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) throws SQLException {
		Uri locationUri = null;

		// Validate the requested uri
		if (uriMatcher.match(uri) != LOCATION) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues cv;
		if (values != null) {
			cv = new ContentValues(values);
		} else {
			cv = new ContentValues();
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (cv.containsKey(Definitions.Location.CREATED_DATE) == false) {
			cv.put(Definitions.Location.CREATED_DATE, now);
		}

		if (cv.containsKey(Definitions.Location.NAME) == false) {
			/* try to find out the location address */
			Geocoder geocoder = new Geocoder(this.getContext());
			try {
				List<Address> list = geocoder.getFromLocation(
						cv.getAsDouble(Definitions.Location.LATITUDE),
						cv.getAsDouble(Definitions.Location.LONGITUDE), 1);

				/* get the first element */
				Address address = list.get(0);
				cv.put(Definitions.Location.NAME, address.getAddressLine(0));
			} catch (Exception e) {
				/*
				 * address can't be obtained, name "Location <number>" will be
				 * set later
				 */
			}
		}

		/* check whether such location has not been stored already */
		long rowId = -1;
		double newLatitude = cv.getAsDouble(Definitions.Location.LATITUDE);
		double newLongitude = cv.getAsDouble(Definitions.Location.LONGITUDE);
		WiFiLocation wifiLocation = new WiFiLocation(newLatitude, newLongitude,
				Definitions.WIFIRADIUS);

		SQLiteDatabase dbr = mOpenHelper.getReadableDatabase();
		String projection[] = new String[] { Definitions.Location._ID,
				Definitions.Location.LATITUDE, Definitions.Location.LONGITUDE,
				Definitions.Location.ACCURACY };
		Cursor c = dbr.query(Definitions.Location.TABLE_NAME, projection, null,
				null, null, null, Definitions.Location.DEFAULT_SORT_ORDER);
		if (c.moveToFirst()) {
			int idColumn = c.getColumnIndex(Definitions.Location._ID);
			int latitudeColumn = c
					.getColumnIndex(Definitions.Location.LATITUDE);
			int longitudeColumn = c
					.getColumnIndex(Definitions.Location.LONGITUDE);
			do {
				long id = c.getLong(idColumn);
				double latitude = c.getDouble(latitudeColumn);
				double longitude = c.getDouble(longitudeColumn);
				if (wifiLocation.isEquivalent(latitude, longitude,
						Definitions.WIFIRADIUS)) {
					rowId = id;
				}
			} while (c.moveToNext() && rowId == -1);
		}
		c.close();

		/* add a new record when equivalent location wasn't found */
		if (rowId == -1) {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			rowId = db.insert(Definitions.Location.TABLE_NAME,
					Definitions.Location.NAME, cv);
			if (rowId > 0) {
				/* if the name has not been set earlier use default one */
				if (cv.containsKey(Definitions.Location.NAME) == false) {
					cv.put(Definitions.Location.NAME, "Location " + rowId);
					db.update(Definitions.Location.TABLE_NAME, cv,
							Definitions.Location._ID + "=" + rowId, null);
				}

				locationUri = ContentUris.withAppendedId(
						Definitions.Location.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(locationUri,
						null);
			} else {
				if (log.isErrorEnabled()) {
					log.error("WiFiLocationProvider.insert(): failed to insert row uri: "
								+ uri);
				}
				throw new SQLException("Failed to insert row into " + uri);
			}
		}
		return locationUri;
	}

	@Override
	public boolean onCreate() {
		/* set up log */
		log = new WiFiLog (getContext());
		
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Definitions.Location.TABLE_NAME);

		switch (uriMatcher.match(uri)) {
		case LOCATION:
			qb.setProjectionMap(locationProjectionMap);
			break;

		case LOCATION_ID:
			qb.setProjectionMap(locationProjectionMap);
			qb.appendWhere(Definitions.Location._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = Definitions.Location.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int nRows;
		switch (uriMatcher.match(uri)) {
		case LOCATION:
			nRows = db.update(Definitions.Location.TABLE_NAME, values,
					selection, selectionArgs);
			break;

		case LOCATION_ID:
			nRows = db.update(
					Definitions.Location.TABLE_NAME,
					values,
					Definitions.Location._ID
							+ "="
							+ uri.getPathSegments().get(1)
							+ (!TextUtils.isEmpty(selection) ? " AND ("
									+ selection + ')' : ""), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return nRows;
	}

}
