package com.fourspaces.featherdb.httpd;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;

public class Shutdown extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev){
		featherDB.shutdown();
		sendOK(response, "Shutting down");
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db.equals("_shutdown") && credentials.isSA());
	}

}
