package com.fourspaces.featherdb.views;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;

public class AdHocViewRunner {
	public static JSONObject adHocView(FeatherDB featherDB,String db, String javaScript) throws ViewException {
		View view=new JavaScriptView(db,javaScript);
		return runView(featherDB,db,null,null,view);
	}
	public static JSONObject runView(FeatherDB featherDB,String db,String viewName, String functionName,View view) {
		if (view==null) {
			return null;
		}
		view.setBackend(featherDB.getBackend());
		JSONArray results = new JSONArray();
		int total = 0;
		for (Document doc: featherDB.getBackend().allDocuments(db)) {
				JSONObject result = view.filter(doc);
				if (result!=null) {
					result.put("id", doc.getId());
					results.put(result);
					total++;
				}
		}
		JSONObject out = new JSONObject();
		if (viewName!=null) {
			out.put("view", viewName+"/"+functionName);
		}
		out.put("total_rows", total);
		out.put("rows",results);
		return out;
	}

}
