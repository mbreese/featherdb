package com.fourspaces.featherdb.utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {
	public static boolean isJSONObjectDifferent(JSONObject obj1, JSONObject obj2) {
		if (obj1.keySet().size()!=obj2.keySet().size()) {
			return true;
		}
		for (String key:obj1.keySet()) {
			Object o1 = obj1.get(key);
			Object o2 = obj2.get(key);
			if (isJSONFieldDifferent(o1,o2)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isJSONFieldDifferent(Object o1, Object o2) {
		if (o1 instanceof JSONObject) {
			if (o2 instanceof JSONObject) {
				if (isJSONObjectDifferent((JSONObject)o1,(JSONObject)o2)) {
					return true; // different json fields
				}
			} else {
				return true; // o1 is json, o2 isn't.
			}
		} else if (o1 instanceof JSONArray) {
			if (o2 instanceof JSONArray) {
				if (isJSONArrayDifferent((JSONArray)o1,(JSONArray)o2)) {
					return true; // different json fields
				}
			} else {
				return true; // o1 is jsonarray, o2 isn't.
			}
		} else if (!o1.toString().equals(o2.toString())) {
			return true;
		}
		return false;
	}
	
	static public boolean isJSONArrayDifferent(JSONArray ar1, JSONArray ar2) {
		if (ar1.length()!=ar2.length()) {
			return true;
		}
		for (int i=0; i<ar1.length(); i++) {
			if (isJSONFieldDifferent(ar1.get(i),ar2.get(i))) {
				return true;
			}
		}
		return false;
	}
	

}
