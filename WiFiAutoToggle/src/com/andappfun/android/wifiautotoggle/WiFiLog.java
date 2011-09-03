package com.andappfun.android.wifiautotoggle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Simple logging class. Provides file logging in addition to Android Log.
 */

public class WiFiLog {

	/**
	 * Version
	 */
	static final public String version = "$Id: WiFiLog.java 13 2011-09-03 01:02:00Z andappfun $";

	/**
	 * Debug level
	 */
	static final public int DEBUG = 1;

	/**
	 * Info level
	 */
	static final public int INFO = 2;

	/**
	 * Warning level
	 */
	static final public int WARN = 3;

	/**
	 * Error level
	 */
	static final public int ERROR = 4;

	/**
	 * Current level
	 */
	private static final int logLevel = INFO;

	/**
	 * Log tag
	 */
	private static final String LOG_TAG = "WiFiAutoToggle";
	
	private Context context = null;
	
	/**
	 * Create WiFiLog for a given context. Context is used to retrieve preferences determining logging.
	 * @param context Context for which WiFiLog is created.
	 */
	public WiFiLog (Context context) {
		this.context = context;
	}

	private void logToFile(String level, String message) {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean logToFile = p.getBoolean("loggingKey", false);
		if (logToFile) {
			/* check external storage */
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				File file = new File(Environment.getExternalStorageDirectory()
						+ System.getProperty("file.separator")
						+ "WiFiAutoToggle.log");
				try {
					FileWriter fileWriter = new FileWriter(file, true);
					fileWriter.write(new Date().toGMTString() + " " + level
							+ " " + message
							+ System.getProperty("line.separator"));
					fileWriter.close();
				} catch (IOException e) {
					Log.e(LOG_TAG, e.toString());
				}
			} else {
				Log.w(LOG_TAG, "External storage not ready to write");
			}
		}
	}

	/**
	 * Check whether error level is enabled for logging
	 * 
	 * @return true when error level is enabled for logging, false otherwise
	 */
	public boolean isErrorEnabled() {
		return logLevel <= ERROR;
	}

	/**
	 * Logs a message with ERROR level
	 * 
	 * @param message
	 */
	public void error(String message) {
		Log.e(LOG_TAG, message);
		logToFile("ERROR", message);
	}

	/**
	 * Check whether warn level is enabled for logging
	 * 
	 * @return true when warn level is enabled for logging, false otherwise
	 */
	public boolean isWarnEnabled() {
		return logLevel <= WARN;
	}

	/**
	 * Logs a message with WARN level
	 * 
	 * @param message
	 */
	public void warn(String message) {
		Log.w(LOG_TAG, message);
		logToFile("WARN", message);
	}

	/**
	 * Check whether info level is enabled for logging
	 * 
	 * @return true when info level is enabled for logging, false otherwise
	 */
	public boolean isInfoEnabled() {
		return logLevel <= INFO;
	}

	/**
	 * Logs a message with INFO level
	 * 
	 * @param message
	 */
	public void info(String message) {
		Log.i(LOG_TAG, message);
		logToFile("INFO", message);
	}

	/**
	 * Check whether info level is enabled for logging
	 * 
	 * @return true when info level is enabled for logging, false otherwise
	 */
	public boolean isDebugEnabled() {
		return logLevel <= DEBUG;
	}

	/**
	 * Logs a message with DEBUG level
	 * 
	 * @param message
	 */
	public void debug(String message) {
		Log.d(LOG_TAG, message);
		logToFile("DEBUG", message);
	}
}
