package com.andappfun.android.wifiautotoggle;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.TextView;

public class WiFiDialogFactory {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	/* About dialog id */
	public static final int DIALOG_ABOUT = 1;

	public static Dialog createAboutDialog(Context context) {
		WiFiLog log = new WiFiLog(context);

		Dialog dialog = new Dialog(context);
		dialog.setTitle(R.string.dialogAboutTitle);
		dialog.setContentView(R.layout.dialog_about);
		TextView versionTV = (TextView) dialog
				.findViewById(R.id.dialogAboutVersion);
		String versionString = context.getString(R.string.dialogAboutVersion);

		PackageInfo pi = null;
		try {
			pi = context.getPackageManager().getPackageInfo(
					context.getPackageName(), PackageManager.GET_META_DATA);
			versionTV.setText(versionString + " " + pi.versionName + " ("
					+ pi.versionCode + ")");
		} catch (NameNotFoundException e) {
			if (log.isErrorEnabled()) {
				log.error("WiFiDialogFactory.createAboutDialog(): error: "
						+ e.toString());
			}
		}
		return dialog;
	}
}
