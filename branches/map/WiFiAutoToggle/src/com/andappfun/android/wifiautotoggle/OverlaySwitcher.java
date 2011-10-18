package com.andappfun.android.wifiautotoggle;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.Overlay;

public class OverlaySwitcher {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private List<Overlay> overlayList = new ArrayList<Overlay>();
	int indexActive = 0;

	/**
	 * Add overlay to overlay switcher list
	 * 
	 * @param overlay
	 */
	public void addOverlay(Overlay overlay) {
		overlayList.add(overlay);
	}

	/**
	 * Sets the next overlay as active.
	 */
	public void next() {
		if (!overlayList.isEmpty()) {
			indexActive = (indexActive + 1) % overlayList.size();
		}
	}

	/**
	 * Return true if the given overlay is an active one, false otherwise
	 * 
	 * @param overlay
	 * @return true if the given overlay is an active one, false otherwise
	 */
	public boolean isActive(Overlay overlay) {
		return overlayList.indexOf(overlay) == indexActive;
	}
}
