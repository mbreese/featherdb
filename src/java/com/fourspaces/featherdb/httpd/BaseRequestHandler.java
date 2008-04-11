package com.fourspaces.featherdb.httpd;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
import com.fourspaces.featherdb.utils.Logger;
import com.fourspaces.featherdb.views.ViewException;

public abstract class BaseRequestHandler {
	protected static final String JSON_MIMETYPE = "application/javascript";
	protected FeatherDB featherDB;

	abstract public boolean match(Credentials credentials, HttpServletRequest request, String db, String id);
	abstract protected void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws BackendException, IOException, DocumentCreationException, ViewException;

	protected Logger log = Logger.get(getClass());

	
	public void handle(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev){
		try {
			handleInner(credentials,request,response,db,id,rev);//,fields);
		} catch (JSONException e) {
			sendError(response, "JSON processing error");
			log.error(e,"JSON error");
		} catch (ViewException e) {
			sendError(response, "View processing error");
			log.error(e,"View error");
		} catch (BackendException e) {
			sendError(response, "Backend storage error");
			log.error(e,"Backend error");
			e.printStackTrace();
		} catch (IOException e) {
			sendError(response, "IO error");
			log.error(e,"Backend error");
			e.printStackTrace();
		} catch (DocumentCreationException e) {
			sendError(response, "IO error");
			log.error(e,"Backend error");
			e.printStackTrace();
		}
	}

	
	public void setFeatherDB(FeatherDB featherDB) {
		this.featherDB=featherDB;
	}
	
	protected void sendNotAuth(HttpServletResponse response) {
		sendError(response,"Not authorized to perform requested action", HttpServletResponse.SC_UNAUTHORIZED);
	}

	protected void sendError(HttpServletResponse response, String string) {
		sendError(response,string,HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
	protected void sendError(HttpServletResponse response, String string, int statusCode) {
		response.setStatus(statusCode);
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
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	protected void sendError(HttpServletResponse response, String string, String  status) {
		sendError(response,string,status,HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
	protected void sendError(HttpServletResponse response, String string, String  status, int statusCode) {
		response.setStatus(statusCode);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
				.object()
					.key("error")
					.value(true)
					.key("message")
					.value(string)
					.key("status")
					.value(status)
				.endObject();
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	protected void sendError(HttpServletResponse response, String string, JSONObject status) {
		sendError(response,string,status,HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
	protected void sendError(HttpServletResponse response, String string, JSONObject status, int statusCode) {
		response.setStatus(statusCode);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
				.object()
					.key("error")
					.value(true)
					.key("message")
					.value(string)
					.key("status")
					.value(status)
				.endObject();
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	protected void sendOK(HttpServletResponse response, String string){
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(JSON_MIMETYPE);
		try {
			new JSONWriter(response.getWriter())
				.object()
					.key("ok")
					.value(true)
					.key("message")
					.value(string)
				.endObject();
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}

	protected void sendJSONString(HttpServletResponse response, JSONArray ar) {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(JSON_MIMETYPE);
			response.getWriter().write(ar.toString(4));
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	protected void sendJSONString(HttpServletResponse response, JSONObject json) {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(JSON_MIMETYPE);
			response.getWriter().write(json.toString(2));
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}

	protected void sendJSONString(HttpServletResponse response, String s) {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(JSON_MIMETYPE);
			response.getWriter().write(s);
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}

	protected void sendDocument(HttpServletResponse response, Document doc, Map<String,String[]> params) {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(doc.getContentType());			
			doc.sendDocument(response.getOutputStream(),params);
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	protected void sendMetaData(HttpServletResponse response, Document doc, Map<String,String[]> params) {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Document.DEFAULT_CONTENT_TYPE);			
			doc.writeMetaData(response.getWriter(),params);
		} catch (JSONException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
}
