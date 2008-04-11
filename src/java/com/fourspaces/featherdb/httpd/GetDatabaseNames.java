package com.fourspaces.featherdb.httpd;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.fourspaces.featherdb.auth.Credentials;

public class GetDatabaseNames extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) {
		Set<String> dbs = featherDB.getBackend().getDatabaseNames();
		JSONArray ar = new JSONArray();
		for (String d:dbs) {
			ar.put(d);
		}
		sendJSONString(response, ar);
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db.equals("_all_dbs") && request.getMethod().equals("GET") && credentials!=null);
	}

}
