package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;

public class InvalidateAuth extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException{
		featherDB.getAuthentication().invalidate(credentials);
		Cookie[] cookies = request.getCookies();
		if (cookies !=null) {
			for (Cookie cookie: cookies) {
				if (cookie.getName().equals(FeatherDBHandler.COOKIE_ID)) {
					cookie.setMaxAge(0); // TODO: check this
				}
			}
		}
		sendOK(response, "Authentication invalidated");
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db.equals("_invalidate"));
	}

}
