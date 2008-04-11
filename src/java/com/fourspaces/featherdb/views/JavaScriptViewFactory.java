package com.fourspaces.featherdb.views;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.fourspaces.featherdb.document.JSONDocument;

@ViewType("text/javascript")
public class JavaScriptViewFactory implements ViewFactory {

	public Map<String, View> buildViews(JSONDocument doc) throws ViewException {
		JSONObject viewDefs = doc.getMetaData().getJSONObject("view");
		Map<String,View> views = new HashMap<String,View>();
		for (String k:viewDefs.keySet()) {
			String src = viewDefs.getString(k);
			views.put(src, new JavaScriptView(doc.getDatabase(),src));
		}
		return views;
	}

}
