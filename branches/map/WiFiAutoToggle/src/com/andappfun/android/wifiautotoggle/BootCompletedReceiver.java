package com.andappfun.android.wifiautotoggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
	
	/**
	 * Version
	 */
	static final public String version = "$Id: BootCompletedReceiver.java 12 2011-10-15 23:48:21Z andappfun $";

	@Override
	public void onReceive(Context context, Intent intent) {
		/* start service to add proximity alerts to all locations */
		Intent startServiceIntent = new Intent(context,
				ProximityAlertAddService.class);
		context.startService(startServiceIntent);
	}
}
