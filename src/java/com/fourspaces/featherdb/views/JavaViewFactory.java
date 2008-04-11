package com.fourspaces.featherdb.views;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.fourspaces.featherdb.document.JSONDocument;

public class JavaViewFactory implements ViewFactory {

	public Map<String, View> buildViews(JSONDocument doc) throws ViewException {
		Map<String,View> m = new HashMap<String,View>();
		if (doc.getMetaData().has("views")) {
			JSONObject views = doc.getMetaData().getJSONObject("views");
			for (String k:views.keySet()) {
				m.put(k,getInstance(views.getString(k)));
			}
		} else if (doc.getMetaData().has("view-class")) {
			m.put(ViewManager.DEFAULT_FUNCTION_NAME,getInstance(doc.getMetaData().getString("view-class")));
		}
		return m;
	}
	
	protected View getInstance(String className) throws ViewException {
		try {
			Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			return (View) clazz.newInstance();
		} catch (ClassNotFoundException e) {
			throw new ViewException(e);
		} catch (InstantiationException e) {
			throw new ViewException(e);
		} catch (IllegalAccessException e) {
			throw new ViewException(e);
		} 
	}
	
}
