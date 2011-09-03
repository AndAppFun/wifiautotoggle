package com.andappfun.android.wifiautotoggle;

import com.andappfun.android.wifiautotoggle.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	/**
	 * Version
	 */
	static final public String version = "$Id: Preferences.java 13 2011-09-03 01:02:00Z andappfun $";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
