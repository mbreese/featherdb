package com.fourspaces.featherdb.httpd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.auth.SACredentials;
import com.fourspaces.featherdb.utils.Logger;

/**
 * This is very <strike>messy</strike> <b>ugly</b> and should be refactored into multiple handlers...
 * 
 * @author mbreese
 *
 */
public class FeatherDBHandler extends AbstractHandler {
	public static final String JSON_MIMETYPE = "application/javascript";
	public static final String COOKIE_ID = "FEATHERDB_ID";
	final protected FeatherDB featherDB;
	final protected boolean allowAnonymous;
	protected int timeout;

	protected List<BaseRequestHandler> baseRequestHandlers = new ArrayList<BaseRequestHandler>();
	
	protected Logger log = Logger.get(getClass());
	public FeatherDBHandler(FeatherDB featherDB) {
		this.featherDB = featherDB;
		baseRequestHandlers.add(new GetDocument());
		baseRequestHandlers.add(new AdHocView());
		baseRequestHandlers.add(new GetView());
		baseRequestHandlers.add(new Auth());
		baseRequestHandlers.add(new InvalidateAuth());
		baseRequestHandlers.add(new GetDatabaseNames());
		baseRequestHandlers.add(new GetDBStats());
		baseRequestHandlers.add(new AddDB());
		baseRequestHandlers.add(new UpdateDocument());
		baseRequestHandlers.add(new Sessions());
		baseRequestHandlers.add(new Shutdown());
		for (BaseRequestHandler handler:baseRequestHandlers) {
			handler.setFeatherDB(featherDB);
		}	
		this.allowAnonymous = featherDB.getProperty("auth.anonymous","false").toLowerCase().equals("true");
		this.timeout=Integer.parseInt(featherDB.getProperty("auth.timeout.seconds","300"));

	}

	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
	{
		Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();

		log.debug("Requested URI: {}",request.getRequestURI());
		String path = request.getRequestURI().substring(1);
		if (path.startsWith("_sys")) {
			return;
		}
		Credentials cred=getCredentials(request,response);
		

		if (response.isCommitted() || cred == null) {
			return;
		}
		
		if (path.endsWith("/")) {
			path=path.substring(0,path.length()-1);
		}
		String db = null;
		String id = null;
		String rev = null;
		
		int slashIndex = path.indexOf("/");
		if (slashIndex>-1) {
			db = path.substring(0,slashIndex);
			if (slashIndex<path.length()) {
				id = path.substring(slashIndex+1);
				rev = request.getParameter("revision");
			}
		} else {
			db=path;
		}
		
//		String[] fields = null;

//		String[] split = path.split("/");
//		
//		if (split.length>1) {
//			db = split[0];
//			id = split[1];
//		} else if (split.length>2) {
//			db = split[0];
//			id = split[1];
//			revision = split[split.length-1];
//			if (revision.equals("_current")) {
//				revision = null;
//			}
//		} else {
//			db = path;
//		}
//
//		if (split.length>3) {
//			fields = new String[split.length-3];
//			for (int i=3;i<split.length;i++) {
//				fields[i-3]=split[i];
//			}
//		}

		boolean handled = false;
		for (BaseRequestHandler handler:baseRequestHandlers) {
			if (handler.match(cred, request, db, id)) {
				handler.handle(cred, request, response, db, id, rev);
				handled = true;
				base_request.setHandled(true);
			}
		}
		if (!handled) {
			sendError(response,"Could not process request");
		}
		
		return;
	}
		
	protected void sendNoAuthError(HttpServletResponse response, String string) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
				.object()
					.key("error")
					.value(true)
					.key("message")
					.value(string)
				.endObject();
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}
	protected void sendError(HttpServletResponse response, String string) throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
				.object()
					.key("error")
					.value(true)
					.key("message")
					.value(string)
				.endObject();
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}
	protected Credentials getCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Credentials cred=null;
		if (allowAnonymous) {
			return new SACredentials("anonymous", "",timeout);
		}

		if (request.getRequestURI().equals("/_auth")) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			if (username!=null) {
				if (password == null) {
					password = "";
				}
				cred = featherDB.getAuthentication().authenticate(username, password);
				if (cred!=null) {
					if (request.getParameter("setcookie")!=null && request.getParameter("setcookie").equals("1")) {
						Cookie cookie = new Cookie(COOKIE_ID, cred.getToken());
						cookie.setMaxAge(timeout);
						response.addCookie(cookie);
					}
					return cred;
				} else {
					sendNoAuthError(response, "Bad username / password combination");
					return null;
				}
			}
		}
		
		Cookie[] cookies = request.getCookies();
		if (cookies !=null) {
			for (Cookie cookie: cookies) {
				if (cookie.getName().equals(COOKIE_ID)) {
					cred = featherDB.getAuthentication().getCredentialsFromToken(cookie.getValue());
					if (cred!=null) {
						log.debug("Got credentials from cookie token: {}", cookie.getValue());
						return cred;
					}
				}
			}
		}
		
		String param = request.getParameter("token");
		if (param!=null && !param.equals("")) {
			cred = featherDB.getAuthentication().getCredentialsFromToken(param);
			if (cred!=null) {
				return cred;
			}
		}
		
		String authHeader = request.getHeader("Authorization");
		if (authHeader!=null) {
			String[] authSplit = authHeader.split(" ");
			if (authSplit.length==2) {
				String userpass = new String(Base64.decodeBase64(authSplit[1].getBytes()));
				if (userpass!=null) {
					String[] ar = userpass.split(":");
					String u = ar[0];
					String p="";
					if (ar.length>1) {
						p = ar[1];
					}
					cred = featherDB.getAuthentication().authenticate(u,p);
					
					if (cred!=null) {
						log.debug("Authenticated as {} => {} via HTTP-AUTH",cred.getUsername(), cred.getToken());
						Cookie cookie = new Cookie(COOKIE_ID, cred.getToken());
						cookie.setMaxAge(24*60*60); // max time is one day... afterwhich, it needs to reauth.
						response.addCookie(cookie);
					}
					return cred;
				}
			}
		}
		log.warn("Error authenticating");
		response.addHeader("WWW-Authenticate"," Basic realm=\"FeatherDB\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"You need a username and password");
		return null;
	}
}
