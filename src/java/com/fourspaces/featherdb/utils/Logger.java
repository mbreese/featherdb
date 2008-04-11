package com.fourspaces.featherdb.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Logger {
	final protected static Map<String,Logger> cache = new HashMap<String,Logger>();
	private java.util.logging.Logger logger;
	private Logger(String id) {
		logger=java.util.logging.Logger.getLogger(id);
		logger.setLevel(Level.FINE);
	}
	
	public static Logger get(Class clazz) {
		return get(clazz.getCanonicalName());
	}
	
	public static Logger get(String id) {
		if (!cache.containsKey(id)) {
			cache.put(id,new Logger(id));
		}
		return cache.get(id);
	}

	protected String msg(String msg, Object...objs) {
		for (Object obj:objs) {
			if (msg.indexOf("{}")>-1) {
				String objStr;
				if (obj==null) {
					objStr = "null";
				} else {
					objStr = obj.toString();
				}
				int idx = msg.indexOf("{}");
				msg = msg.substring(0,idx) + objStr + msg.substring(idx+2);
			}
		}
		return msg;
	}
	
	public void debug(String msg, Object...objs) {
		if (logger.isLoggable(Level.INFO)) {
			msg = msg(msg,objs);
			logger.log(Level.INFO,msg);
		}
	}
	
	public void warn(String msg, Object...objs) {
		if (logger.isLoggable(Level.WARNING)) {
			msg = msg(msg,objs);
			logger.log(Level.WARNING,msg);
		}
	}
	
	public void info(String msg, Object...objs) {
		if (logger.isLoggable(Level.INFO)) {
			msg = msg(msg,objs);
			logger.log(Level.INFO,msg);
		}
	}
	
	public void error(String msg, Object...objs) {
		if (logger.isLoggable(Level.SEVERE)) {
			msg = msg(msg,objs);
			logger.log(Level.SEVERE,msg);
		}
	}

	public void error(Exception e, String msg, Object...objs) {
		if (logger.isLoggable(Level.SEVERE)) {
			msg = msg(msg,objs);
			e.printStackTrace();
			logger.log(Level.SEVERE,msg,e);
		}
	}
	
	public void error(Exception e) {
		if (logger.isLoggable(Level.SEVERE)) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getLocalizedMessage(),e);
		}
	}	
}
