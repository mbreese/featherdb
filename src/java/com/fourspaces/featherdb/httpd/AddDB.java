package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.views.ViewException;

public class AddDB extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException, BackendException, ViewException{
		featherDB.addDatabase(db);
		sendOK(response, db+" added");
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null && !db.startsWith("_") && id==null && request.getMethod().equals("PUT") && credentials.isSA());
	}

}
