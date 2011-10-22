package com.andappfun.android.wifiautotoggle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;

public class DbAdapter {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private static final String DATABASE_NAME = "autowifitoggle.db";
	private static final int DATABASE_VERSION = 2;

	private WiFiLog log;

	private Context context;

	private DatabaseHelper databaseHelper;

	private SQLiteDatabase database;

	/* optimization for getLocationsFromArea() */
	/* area */
	double latitudeFrom = 0.0;
	double longitudeFrom = 0.0;

	double latitudeTo = 0.0;
	double longitudeTo = 0.0;

	/* list of locations from this area */
	ArrayList<WiFiLocation> locationsFromAreaList = null;

	/* end of optimization for getLocationFromArea() */

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

			onUpgrade(db, 1, DATABASE_VERSION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			if (oldVersion < 2) {
				/* create latitude and longitude indexes */
				String sqlLatitudeIndex = "CREATE INDEX LATITUDE_INDEX ON "
						+ Definitions.Location.TABLE_NAME + " ("
						+ Definitions.Location.LATITUDE + ");";
				db.execSQL(sqlLatitudeIndex);
				String sqlLongitudeIndex = "CREATE INDEX LONGITUDE_INDEX ON "
						+ Definitions.Location.TABLE_NAME + " ("
						+ Definitions.Location.LONGITUDE + ");";
				db.execSQL(sqlLongitudeIndex);
			}
		}
	}

	public DbAdapter(Context context) {
		this.context = context;
		log = new WiFiLog(context);
		databaseHelper = new DatabaseHelper(context);
		database = null;
	}

	private void resetLocationsFromAreaList() {
		locationsFromAreaList = null;
	}

	private SQLiteDatabase getDatabase() {
		if (database == null)
			database = databaseHelper.getWritableDatabase();
		return database;
	}

	/**
	 * Add new location
	 * 
	 * @param location
	 *            location to be added
	 * @return newly added location or null when such location already exists
	 */
	public synchronized WiFiLocation addLocation(WiFiLocation wifiLocationToAdd) {
		WiFiLocation l = null;
		long id = -1;

		if (log.isDebugEnabled()) {
			log.debug("DbAdapter.addLocation(): latitude: "
					+ wifiLocationToAdd.getLatitude() + " longitude: "
					+ wifiLocationToAdd.getLongitude());
		}

		/* check if equivalent location exists */
		double latitudeFrom = WiFiLocation.normalizeLatitude(wifiLocationToAdd
				.getLatitude() - WiFiLocation.LATITUDERANGE);
		double longitudeFrom = WiFiLocation
				.normalizeLongitude(wifiLocationToAdd.getLongitude()
						- WiFiLocation.LONGITUDERANGE);

		double latitudeTo = WiFiLocation.normalizeLatitude(wifiLocationToAdd
				.getLatitude() + WiFiLocation.LATITUDERANGE);
		double longitudeTo = WiFiLocation.normalizeLongitude(wifiLocationToAdd
				.getLongitude() + WiFiLocation.LONGITUDERANGE);

		List<WiFiLocation> locations = getLocationsFromArea(latitudeFrom,
				longitudeFrom, latitudeTo, longitudeTo);

		Iterator<WiFiLocation> wifiLocationsIterator = locations.iterator();
		while (wifiLocationsIterator.hasNext() && id == -1) {
			WiFiLocation wifiLocation = wifiLocationsIterator.next();
			if (wifiLocationToAdd.isEquivalent(wifiLocation,
					Definitions.WIFIRADIUS)) {
				id = wifiLocation.getId();
			}
		}

		if (id == -1) {
			String name = null;
			Geocoder geocoder = new Geocoder(context);
			try {
				List<Address> list = geocoder.getFromLocation(
						wifiLocationToAdd.getLatitude(),
						wifiLocationToAdd.getLongitude(), 1);

				/* get the first element */
				Address address = list.get(0);
				name = address.getAddressLine(0);
			} catch (Exception e) {
				/*
				 * address can't be obtained, name "Location <number>" will be
				 * set later
				 */
			}
			ContentValues cv = new ContentValues();
			cv.put(Definitions.Location.LATITUDE,
					wifiLocationToAdd.getLatitude());
			cv.put(Definitions.Location.LONGITUDE,
					wifiLocationToAdd.getLongitude());
			cv.put(Definitions.Location.ACCURACY,
					wifiLocationToAdd.getAccuracy());

			/* set created date/time to current date/time */
			cv.put(Definitions.Location.CREATED_DATE,
					Long.valueOf(System.currentTimeMillis()));

			if (name != null) {
				cv.put(Definitions.Location.NAME, name);
			}
			id = getDatabase().insert(Definitions.Location.TABLE_NAME,
					Definitions.Location.NAME, cv);
			if (id > 0) {

				/*
				 * new location has been added, reset the locations from the
				 * area list
				 */
				resetLocationsFromAreaList();

				l = new WiFiLocation(id, wifiLocationToAdd.getLatitude(),
						wifiLocationToAdd.getLongitude(),
						wifiLocationToAdd.getAccuracy());

				/* set name if not set */
				if (name != null) {
					l.setName(name);
				} else {
					/* set the name to "Location <number>" */
					l.setName("Location " + id);
				}
				updateLocation(l);

				if (log.isDebugEnabled()) {
					log.debug("DbAdapter.addLocation(): location: " + id
							+ " added (" + l.getName() + ")");
				}

			} else {
				if (log.isErrorEnabled()) {
					log.error("DbAdapter.addLocation(): failed to insert row");
				}
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("DbAdapter.addLocation(): new location not added, location: "
						+ id + " found");
			}

		}
		return l;
	}

	/**
	 * Return cursor with all locations sorted by created date/time newest first
	 * 
	 * @return Cursor
	 */
	public synchronized Cursor getAllLocations() {
		return getDatabase().query(
				Definitions.Location.TABLE_NAME,
				new String[] { Definitions.Location._ID,
						Definitions.Location.NAME,
						Definitions.Location.LATITUDE,
						Definitions.Location.LONGITUDE,
						Definitions.Location.ACCURACY,
						Definitions.Location.CREATED_DATE }, null, null, null,
				null, Definitions.Location.DEFAULT_SORT_ORDER);
	}

	/**
	 * Returns true when area has changed
	 * 
	 * @param latitudeFrom
	 * @param longitudeFrom
	 * @param latitudeTo
	 * @param longitudeTo
	 * @return true when area has changed
	 */
	private boolean areaChanged(double latitudeFrom, double longitudeFrom,
			double latitudeTo, double longitudeTo) {
		boolean bChanged = false;

		if (!bChanged)
			bChanged = (this.latitudeFrom != latitudeFrom);

		if (!bChanged)
			bChanged = (this.longitudeFrom != longitudeFrom);

		if (!bChanged)
			bChanged = (this.latitudeTo != latitudeTo);

		if (!bChanged)
			bChanged = (this.longitudeTo != longitudeTo);

		return bChanged;
	}

	/**
	 * Get list of locations from an area defined by latitude and longitude span
	 * 
	 * @param latitudeFrom
	 * @param longitudeFrom
	 * @param latitudeTo
	 * @param longitudeTo
	 * @return list of locations
	 */
	public synchronized List<WiFiLocation> getLocationsFromArea(
			double latitudeFrom, double longitudeFrom, double latitudeTo,
			double longitudeTo) {

		if (areaChanged(latitudeFrom, longitudeFrom, latitudeTo, longitudeTo)) {
			this.latitudeFrom = latitudeFrom;
			this.longitudeFrom = longitudeFrom;
			this.latitudeTo = latitudeTo;
			this.longitudeTo = longitudeTo;
			resetLocationsFromAreaList();
		}

		if (locationsFromAreaList == null) {

			locationsFromAreaList = new ArrayList<WiFiLocation>();

			String sqlQuery;

			/*
			 * since latitude is capped on 90 and -90 degrees latitude from
			 * should always be smaller than latitude to
			 */
			if (longitudeFrom <= longitudeTo) {
				sqlQuery = "SELECT * FROM " + Definitions.Location.TABLE_NAME
						+ " WHERE " + Definitions.Location._ID
						+ " IN ( SELECT " + Definitions.Location._ID + " FROM "
						+ Definitions.Location.TABLE_NAME + " WHERE "
						+ Definitions.Location.LATITUDE + " >= ? AND "
						+ Definitions.Location.LATITUDE
						+ " <= ? INTERSECT SELECT " + Definitions.Location._ID
						+ " FROM " + Definitions.Location.TABLE_NAME
						+ " WHERE " + Definitions.Location.LONGITUDE
						+ " >= ? AND " + Definitions.Location.LONGITUDE
						+ " <= ? );";

			} else {
				sqlQuery = "SELECT * FROM " + Definitions.Location.TABLE_NAME
						+ " WHERE " + Definitions.Location._ID
						+ " IN ( SELECT " + Definitions.Location._ID + " FROM "
						+ Definitions.Location.TABLE_NAME + " WHERE "
						+ Definitions.Location.LATITUDE + " >= ? AND "
						+ Definitions.Location.LATITUDE
						+ " <= ? INTERSECT SELECT " + Definitions.Location._ID
						+ " FROM " + Definitions.Location.TABLE_NAME
						+ " WHERE ( " + Definitions.Location.LONGITUDE
						+ " >= ?  AND " + Definitions.Location.LONGITUDE
						+ " <= 180.0 ) OR (" + Definitions.Location.LONGITUDE
						+ " <= ? " + " AND " + Definitions.Location.LONGITUDE
						+ " >= -180.0 ));";
			}

			// if (log.isDebugEnabled()) {
			// log.debug("DbAdapter.getLocationsFromArea(): query: "
			// + sqlQuery);
			// }

			Cursor c = getDatabase().rawQuery(
					sqlQuery,
					new String[] { Double.toString(latitudeFrom),
							Double.toString(latitudeTo),
							Double.toString(longitudeFrom),
							Double.toString(longitudeTo) });

			if (c.moveToFirst()) {
				do {
					WiFiLocation location = new WiFiLocation(
							c.getLong(c
									.getColumnIndex(Definitions.Location._ID)),
							c.getDouble(c
									.getColumnIndex(Definitions.Location.LATITUDE)),
							c.getDouble(c
									.getColumnIndex(Definitions.Location.LONGITUDE)),
							c.getFloat(c
									.getColumnIndex(Definitions.Location.ACCURACY)));
					location.setName(c.getString(c
							.getColumnIndex(Definitions.Location.NAME)));
					location.setCreatedDateTime(c.getLong(c
							.getColumnIndex(Definitions.Location.CREATED_DATE)));
					locationsFromAreaList.add(location);
					// if (log.isDebugEnabled()) {
					// log.debug("DbAdapter.getLocationsFromArea(): location "
					// + location.getId() + " ("
					// + location.getLatitude() + ", "
					// + location.getLongitude()
					// + ") added to the list");
					// }
				} while (c.moveToNext());
			}
			c.close();
		}

		return locationsFromAreaList;
	}

	/**
	 * Delete location based on the given id
	 * 
	 * @param id
	 *            location id to be deleted
	 * @return number of rows that have been deleted, 1 indicates that location
	 *         has been deleted
	 */
	public synchronized int deleteLocation(long id) {
		int nRows = getDatabase().delete(Definitions.Location.TABLE_NAME,
				Definitions.Location._ID + "=?",
				new String[] { Long.toString(id) });
		if (nRows == 1) {
			resetLocationsFromAreaList();
		}
		return nRows;
	}

	/**
	 * Return WiFi location based on the given id, or null if not found
	 * 
	 * @param id
	 *            location id
	 * @return WiFiLocation Wi-Fi location for the given id
	 */
	public synchronized WiFiLocation getLocation(long id) {
		WiFiLocation location = null;

		Cursor c = getDatabase().query(
				Definitions.Location.TABLE_NAME,
				new String[] { Definitions.Location._ID,
						Definitions.Location.NAME,
						Definitions.Location.LATITUDE,
						Definitions.Location.LONGITUDE,
						Definitions.Location.ACCURACY,
						Definitions.Location.CREATED_DATE },
				Definitions.Location._ID + "=?",
				new String[] { Long.toString(id) }, null, null, null);
		if (c.moveToFirst()) {
			location = new WiFiLocation(c.getLong(c
					.getColumnIndex(Definitions.Location._ID)), c.getDouble(c
					.getColumnIndex(Definitions.Location.LATITUDE)),
					c.getDouble(c
							.getColumnIndex(Definitions.Location.LONGITUDE)),
					c.getFloat(c.getColumnIndex(Definitions.Location.ACCURACY)));
			location.setName(c.getString(c
					.getColumnIndex(Definitions.Location.NAME)));
			location.setCreatedDateTime(c.getLong(c
					.getColumnIndex(Definitions.Location.CREATED_DATE)));
		}
		c.close();

		return location;
	}

	/**
	 * Update location
	 * 
	 * @param location
	 *            Wi-Fi location to be updated
	 * @return number of rows affected. One indicates successful update.
	 */
	public synchronized int updateLocation(WiFiLocation location) {
		ContentValues values = new ContentValues();
		values.put(Definitions.Location.NAME, location.getName());

		return getDatabase().update(Definitions.Location.TABLE_NAME, values,
				Definitions.Location._ID + "=?",
				new String[] { Long.toString(location.getId()) });
	}

}
