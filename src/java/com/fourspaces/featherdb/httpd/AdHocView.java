package com.fourspaces.featherdb.httpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.views.AdHocViewRunner;
import com.fourspaces.featherdb.views.ViewException;

public class AdHocView extends BaseRequestHandler {

	@SuppressWarnings("unchecked")
	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev/*, String[] fields*/) throws IOException{
		String line;
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		request.setCharacterEncoding("UTF-8");
		try {
			reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			while ((line=reader.readLine())!=null) {
				log.debug(line);
				sb.append(line);
				sb.append("\n");
			}
		} finally {
			if (reader!=null) {
				reader.close();
			}
		}
		String src = sb.toString();
		if (src!=null && src.length()>0) {
			try {
				sendJSONString(response, AdHocViewRunner.adHocView(featherDB,db,src));
			} catch (ViewException e) {
				sendError(response,"View error",e.getMessage());
				log.error("Error processing view code: {}",src,e);
			}
		} else {
			JSONObject status = new JSONObject();
			status.put("db",db);
			status.put("view","_temp_view");
			sendError(response, "View javascript source not sent",status,HttpServletResponse.SC_BAD_REQUEST);
			log.info("Bad adhoc view: {}",src);
		}
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null  && !db.startsWith("_") && id!=null && id.equals("_temp_view") && request.getMethod().equals("POST") && credentials.isAuthorizedRead(db));
	}

}
