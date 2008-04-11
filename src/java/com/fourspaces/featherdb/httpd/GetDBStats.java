package com.fourspaces.featherdb.httpd;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.fourspaces.featherdb.auth.Credentials;

public class GetDBStats extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException{
		Map<String,Object> m = featherDB.getBackend().getDatabaseStats(db);
		sendJSONString(response, new JSONObject(m));
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null && !db.startsWith("_") && id==null && request.getMethod().equals("GET") && credentials.isAuthorizedRead(db) && featherDB.getBackend().doesDatabaseExist(db));
	}

}
