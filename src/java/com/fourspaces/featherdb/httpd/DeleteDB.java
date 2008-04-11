package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.views.ViewException;

public class DeleteDB extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException, BackendException, ViewException{
		featherDB.deleteDatabase(db);
		sendOK(response, db+" removed");
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null && id==null && request.getMethod().equals("DELETE") && featherDB.getBackend().doesDocumentExist(db, id));
	}

}
