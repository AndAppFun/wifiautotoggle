package com.andappfun.android.wifiautotoggle;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import java.text.DateFormat;

import com.andappfun.android.wifiautotoggle.R;

import android.view.View;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class LocationListViewBinder implements ViewBinder {

	/**
	 * Version
	 */
	static final public String version = "$Id$";

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	LocationListViewBinder(Context context) {
		dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
		timeFormat = android.text.format.DateFormat.getTimeFormat(context);
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		boolean bRet = false; /* data not bound */
		int viewId = view.getId();
		switch (viewId) {
		case R.id.listLocationCreated:
			TextView dateTextView = (TextView) view;
			Date date = new Date(cursor.getLong(columnIndex));
			dateTextView.setText(dateFormat.format(date) + " "
					+ timeFormat.format(date));
			bRet = true; /* data has been bound */
			break;
		}
		return bRet;
	}
}
