package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.fourspaces.featherdb.auth.Credentials;

public class Auth extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException{
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
			.object()
				.key("ok")
				.value(true)
				.key("token")
				.value(credentials.getToken())
			.endObject();
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db.equals("_auth"));
	}

}
