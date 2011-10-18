package com.andappfun.android.wifiautotoggle;

import java.util.List;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * The overlay to draw stored Wi-Fi locations that are in the area displayed by
 * current location overlay
 * 
 * @author andappfun
 * 
 */
public class StoredLocationsOverlay extends Overlay {

	/**
	 * Version
	 */
	static final public String version = "$Id: Definitions.java 3 2011-09-03 01:47:39Z andappfun $";

	/* radius of the whole circle drawn at the Wi-Fi location */
	static final private int WHOLECIRCLERADIUS = 10;

	/* stroke width */
	private static final float STROKEWIDTH = 5;

	private OverlaySwitcher switcher;

	private Context context;

	private WiFiLog log;

	private DbAdapter dbAdapter;

	/* Wi-Fi location marker */
	private Drawable marker;
	private int markerWidth;
	private int markerHeight;

	/* Paints for Wi-Fi locations */
	private Paint paintTransparent;
	private Paint paintOpaque;
	private Paint paintOpaqueStroke;

	public StoredLocationsOverlay(OverlaySwitcher switcher, Context context) {

		this.switcher = switcher;

		this.context = context;

		log = new WiFiLog(this.context);

		dbAdapter = DbAdapterFactory.getInstance().getDbAdapter(this.context);

		marker = context.getResources().getDrawable(R.drawable.marker);
		markerWidth = marker.getIntrinsicWidth();
		markerHeight = marker.getIntrinsicHeight();

		paintTransparent = new Paint();
		paintTransparent.setAntiAlias(true);
		paintTransparent.setARGB(60, 60, 178, 215);

		paintOpaque = new Paint(paintTransparent);
		paintOpaque.setAlpha(255);

		paintOpaqueStroke = new Paint(paintOpaque);
		paintOpaqueStroke.setStyle(Paint.Style.STROKE);
		paintOpaqueStroke.setStrokeWidth(StoredLocationsOverlay.STROKEWIDTH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.android.maps.Overlay#onTap(com.google.android.maps.GeoPoint,
	 * com.google.android.maps.MapView)
	 */
	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {

		boolean handled = false;

		/* respond only when active */
		if (switcher.isActive(this)) {

			Projection projection = mapView.getProjection();

			Point point = new Point();
			projection.toPixels(geoPoint, point);

			GeoPoint latitudePointFrom = projection.fromPixels(point.x, point.y
					+ markerHeight);
			GeoPoint longitudePointFrom = projection.fromPixels(point.x
					- markerWidth, point.y);

			GeoPoint latitudePointTo = projection.fromPixels(point.x, point.y
					- markerHeight);
			GeoPoint longitudePointTo = projection.fromPixels(point.x
					+ markerWidth, point.y);

			/* find locations near the tap point and choose the closest one */

			List<WiFiLocation> locationList = dbAdapter.getLocationsFromArea(
					latitudePointFrom.getLatitudeE6() / 1E6,
					longitudePointFrom.getLongitudeE6() / 1E6,
					latitudePointTo.getLatitudeE6() / 1E6,
					longitudePointTo.getLongitudeE6() / 1E6);

			WiFiLocation wifiLocation = null;
			float distance = -1; /* not set */
			float results[] = new float[3];
			for (WiFiLocation l : locationList) {
				Location.distanceBetween(geoPoint.getLatitudeE6() / 1E6,
						geoPoint.getLongitudeE6() / 1E6, l.getLatitude(),
						l.getLongitude(), results);

				if (distance == -1) {
					distance = results[0];
					wifiLocation = l;
				} else if (distance > results[0]) {
					distance = results[0];
					wifiLocation = l;
				}
			}

			if (wifiLocation != null) {
				if (log.isDebugEnabled()) {
					log.debug("StoredLocationsOverlay.onTap: location: "
							+ wifiLocation.getId());
				}
				handled = true;
				Intent launchWiFiLocationDetails = new Intent(context,
						WiFiLocationDetailsActivity.class);
				Uri uri = ContentUris.withAppendedId(
						Definitions.Location.CONTENT_URI, wifiLocation.getId());
				launchWiFiLocationDetails.setData(uri);
				context.startActivity(launchWiFiLocationDetails);
			} else {
				switcher.next();
			}
		}
		return handled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.maps.Overlay#draw(android.graphics.Canvas,
	 * com.google.android.maps.MapView, boolean)
	 */
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		if (!shadow) {

			int latitudeSpan = mapView.getLatitudeSpan();
			int longitudeSpan = mapView.getLongitudeSpan();
			GeoPoint center = mapView.getMapCenter();

			double latitudeFrom = WiFiLocation.normalizeLatitude((center
					.getLatitudeE6() - latitudeSpan / 2) / 1E6);
			double longitudeFrom = WiFiLocation.normalizeLongitude((center
					.getLongitudeE6() - longitudeSpan / 2) / 1E6);

			double latitudeTo = WiFiLocation.normalizeLatitude((center
					.getLatitudeE6() + latitudeSpan / 2) / 1E6);
			double longitudeTo = WiFiLocation.normalizeLongitude((center
					.getLongitudeE6() + longitudeSpan / 2) / 1E6);

			List<WiFiLocation> locationList = dbAdapter.getLocationsFromArea(
					latitudeFrom, longitudeFrom, latitudeTo, longitudeTo);

			Projection projection = mapView.getProjection();

			for (WiFiLocation location : locationList) {
				GeoPoint geoPoint = new GeoPoint(
						(int) (location.getLatitude() * 1E6),
						(int) (location.getLongitude() * 1E6));
				Point point = new Point();
				projection.toPixels(geoPoint, point);

				/* draw full circle in the center */
				canvas.drawCircle(point.x, point.y,
						StoredLocationsOverlay.WHOLECIRCLERADIUS, paintOpaque);

				/* draw accuracy circle */
				float radius = projection.metersToEquatorPixels(location
						.getAccuracy());
				canvas.drawCircle(point.x, point.y, radius, paintTransparent);

				/* draw accuracy circle outline when this overlay is active */
				if (switcher.isActive(this)) {
					canvas.drawCircle(point.x, point.y, radius,
							paintOpaqueStroke);
				}

				/* draw marker */
				marker.setBounds(point.x - markerWidth / 2, point.y
						- markerHeight
						- StoredLocationsOverlay.WHOLECIRCLERADIUS, point.x
						+ markerWidth / 2, point.y
						- StoredLocationsOverlay.WHOLECIRCLERADIUS);
				marker.draw(canvas);
			}
		}
	}
}
