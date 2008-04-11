package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.BackendException;

public class DeleteDocument extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException, BackendException{
		featherDB.getBackend().deleteDocument(db,id);
		sendOK(response, db+"/"+ id +" removed");
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null && id!=null && request.getMethod().equals("DELETE") && featherDB.getBackend().doesDatabaseExist(db) && credentials.isAuthorizedWrite(db));
	}

}
