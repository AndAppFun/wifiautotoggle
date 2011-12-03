package com.andappfun.android.wifiautotoggle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;

/**
 * {@link MyLocationOverlay} extension that keeps current location on the screen
 * and zooms according to current location accuracy.
 * 
 * @author andappfun
 * @see com.google.android.maps.MyLocationOverlay
 * 
 */
public class CurrentLocationOverlay extends MyLocationOverlay {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	// Earth radius length in meters
	private static final float radius = 6378137;

	/* radius of the whole circle drawn at the current location */
	private static final int WHOLECIRCLERADIUS = 10;

	/* stroke width */
	private static final float STROKEWIDTH = 5;

	private Context context;

	private WiFiLog log;

	private OverlaySwitcher switcher;

	private MapView mapView;

	private WifiManager wifiManager;
	
	private WiFiStateStore stateStore;

	private int zoomToAdd;

	/* Paints for current locations */
	private Paint paintWiFiOnTransparent;
	private Paint paintWiFiOnOpaque;
	private Paint paintWiFiOnOpaqueStroke;

	private Paint paintWiFiOffTransparent;
	private Paint paintWiFiOffOpaque;
	private Paint paintWiFiOffOpaqueStroke;

	public CurrentLocationOverlay(OverlaySwitcher switcher, Context context,
			MapView mapView) {

		super(context, mapView);

		this.context = context;

		log = new WiFiLog(context);

		this.switcher = switcher;

		this.mapView = mapView;
		
		this.stateStore = new WiFiStateStore (context);
		
		/* get Wi-Fi manager */
		wifiManager = (WifiManager) context
				.getSystemService(WiFiOnOffService.WIFI_SERVICE);

		paintWiFiOnTransparent = new Paint();
		paintWiFiOnTransparent.setAntiAlias(true);
		paintWiFiOnTransparent.setARGB(60, 85, 215, 62);

		paintWiFiOnOpaque = new Paint(paintWiFiOnTransparent);
		paintWiFiOnOpaque.setAlpha(255);

		paintWiFiOnOpaqueStroke = new Paint(paintWiFiOnOpaque);
		paintWiFiOnOpaqueStroke.setStyle(Paint.Style.STROKE);
		paintWiFiOnOpaqueStroke
				.setStrokeWidth(CurrentLocationOverlay.STROKEWIDTH);

		paintWiFiOffTransparent = new Paint();
		paintWiFiOffTransparent.setAntiAlias(true);
		paintWiFiOffTransparent.setARGB(60, 213, 59, 65);

		paintWiFiOffOpaque = new Paint(paintWiFiOffTransparent);
		paintWiFiOffOpaque.setAlpha(255);

		paintWiFiOffOpaqueStroke = new Paint(paintWiFiOffOpaque);
		paintWiFiOffOpaqueStroke.setStyle(Paint.Style.STROKE);
		paintWiFiOffOpaqueStroke
				.setStrokeWidth(CurrentLocationOverlay.STROKEWIDTH);

		// MapController.setZoom uses an integer from 1 to 21 to indicate the
		// zoom level
		// With zoom level 1 Earth equator is 256 pixels
		// The following factor increases zoom level so the zoomed area based on
		// accuracy occupies roughly half of the smallest screen dimension
		DisplayMetrics displaymetrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(displaymetrics);
		int smallestDimension = displaymetrics.heightPixels;
		if (displaymetrics.widthPixels > smallestDimension)
			smallestDimension = displaymetrics.widthPixels;
		zoomToAdd = smallestDimension / 256;
	}

	@Override
	public synchronized void onLocationChanged(Location location) {
		super.onLocationChanged(location);

		int z = 18;
		if (location.hasAccuracy()) {
			// set zoom according to location accuracy
			z = (int) (Math.log(radius / location.getAccuracy()) / Math.log(2))
					+ zoomToAdd;
		}
		mapView.getController().setZoom(z);

		mapView.getController().animateTo(
				new GeoPoint((int) (location.getLatitude() * 1E6),
						(int) (location.getLongitude() * 1E6)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.android.maps.MyLocationOverlay#onTap(com.google.android.maps
	 * .GeoPoint, com.google.android.maps.MapView)
	 */
	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		boolean handled = false;

		/* respond only when active */
		if (switcher.isActive(this)) {

			handled = true;

			/* toggle Wi-Fi when pressed within current location */
			Location currentLocation = getLastFix();
			if (currentLocation != null) {
				float results[] = new float[3];
				Location.distanceBetween(currentLocation.getLatitude(),
						currentLocation.getLongitude(),
						geoPoint.getLatitudeE6() / 1E6,
						geoPoint.getLongitudeE6() / 1E6, results);
				if (results[0] <= currentLocation.getAccuracy()) {
					/* toggle Wi-Fi */
					boolean bWiFiEnabled = wifiManager.isWifiEnabled();

					if (log.isInfoEnabled()) {
						log.info("CurrentLocationOverlay.onTap(): set WiFi enabled to: "
								+ !bWiFiEnabled);
					}

					if (bWiFiEnabled) {
						/* turn off Wi-Fi */
						boolean bDisabled = false;
						if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
							bDisabled = true;
						} else {
							bDisabled = wifiManager.setWifiEnabled(false);
						}
						if (bDisabled) {
							stateStore.storeDisabled();
						}
					} else {
						/* turn on Wi-Fi */
						boolean bEnabled = false;
						if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
							bEnabled = true;
						} else {
							bEnabled = wifiManager.setWifiEnabled(true);
						}
						if (bEnabled) {
							stateStore.storeEnabled();
						}
					}

					/* refresh */
					mapView.invalidate();

					/* display a toast */
					int stringId;
					if (bWiFiEnabled) {
						stringId = R.string.currentLocationWiFiDisabling;
					} else {
						stringId = R.string.currentLocationWiFiEnabling;
					}
					Toast toast = Toast.makeText(context, stringId,
							Toast.LENGTH_LONG);
					toast.show();
				} else {
					/* switch overlays when pressed outside current location */
					switcher.next();
				}
			}
		}
		return handled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.android.maps.MyLocationOverlay#drawMyLocation(android.graphics
	 * .Canvas, com.google.android.maps.MapView, android.location.Location,
	 * com.google.android.maps.GeoPoint, long)
	 */
	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView,
			Location lastFix, GeoPoint myLocation, long when) {

		Projection projection = mapView.getProjection();

		Point point = new Point();
		projection.toPixels(myLocation, point);

		float radius = projection.metersToEquatorPixels(lastFix.getAccuracy());

		/* use different colors depending on the Wi-Fi state */
		if (wifiManager.isWifiEnabled()) {
			/* draw full circle in the center */
			canvas.drawCircle(point.x, point.y,
					CurrentLocationOverlay.WHOLECIRCLERADIUS, paintWiFiOnOpaque);

			/* draw accuracy circle */
			canvas.drawCircle(point.x, point.y, radius, paintWiFiOnTransparent);

			/* draw accuracy circle outline when this overlay is active */
			if (switcher.isActive(this)) {
				canvas.drawCircle(point.x, point.y, radius,
						paintWiFiOnOpaqueStroke);
			}
		} else {
			/* draw full circle in the center */
			canvas.drawCircle(point.x, point.y,
					CurrentLocationOverlay.WHOLECIRCLERADIUS,
					paintWiFiOffOpaque);

			/* draw accuracy circle */
			canvas.drawCircle(point.x, point.y, radius, paintWiFiOffTransparent);

			/* draw accuracy circle outline when this overlay is active */
			if (switcher.isActive(this)) {
				canvas.drawCircle(point.x, point.y, radius,
						paintWiFiOffOpaqueStroke);
			}
		}
	}
}
