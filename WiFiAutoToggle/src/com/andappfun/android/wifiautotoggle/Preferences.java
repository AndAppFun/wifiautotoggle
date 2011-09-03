package com.andappfun.android.wifiautotoggle;

import com.andappfun.android.wifiautotoggle.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
