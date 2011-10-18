package com.andappfun.android.wifiautotoggle;

import android.content.Context;

public class DbAdapterFactory {
	
	/**
	 * Version
	 */
	static final public String version = "$Id: Definitions.java 3 2011-09-03 01:47:39Z andappfun $";

    private static final DbAdapterFactory instance = new DbAdapterFactory();
    
    private Context applicationContext = null;
    
    private DbAdapter dbAdapter = null;
    
    private WiFiLog log;
    
    // Private constructor prevents instantiation from other classes
    private DbAdapterFactory() { }

    public static DbAdapterFactory getInstance() {
            return instance;
    }
    
    /**
     * Gets currently used DbAdapter or creates one based on the application context
     * @param context Context
     * @return DbAdapter
     */
    public DbAdapter getDbAdapter (Context context) {
    	
    	if (applicationContext == null) {

    		applicationContext = context.getApplicationContext();
    		
    		log = new WiFiLog (applicationContext);
    		
    		log.debug("DbAdapterFactory.getDbAdapter(): create database adapter");
    		   
    		dbAdapter = new DbAdapter (applicationContext);
    	}
    	
    	return dbAdapter;
    }
	
}
