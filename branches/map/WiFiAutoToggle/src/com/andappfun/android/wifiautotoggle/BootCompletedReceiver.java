package com.andappfun.android.wifiautotoggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
	
	/**
	 * Version
	 */
	static final public String version = "$Id$";

	@Override
	public void onReceive(Context context, Intent intent) {
		/* start service to add proximity alerts to all locations */
		Intent startServiceIntent = new Intent(context,
				ProximityAlertAddService.class);
		context.startService(startServiceIntent);
	}
}
