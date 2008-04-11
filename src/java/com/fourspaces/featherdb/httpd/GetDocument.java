package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.document.Document;

public class GetDocument extends BaseRequestHandler {

	protected String indexFile = "index.html";
	
	@Override
	public void setFeatherDB(FeatherDB featherDB) {
		super.setFeatherDB(featherDB);
		indexFile = featherDB.getProperty("index.name");
		
	}

	@SuppressWarnings("unchecked")
	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev/*, String[] fields*/) throws IOException{
		/*
		 *  display the document, optionally revision (or _current), and optionaly
		 *  traverse the JSONObject by field names
		 */
		Document d = featherDB.getBackend().getDocument(db, id, rev);
		if (d==null && featherDB.getBackend().doesDocumentExist(db, id+"/"+indexFile)) {
			d = featherDB.getBackend().getDocument(db, id+"/"+indexFile);
		}
		if (d!=null) {
			log.debug("Got doc {} class={}",d.getId(),d.getClass());
			
			boolean showMeta = "true".equals(request.getParameter("meta"));
			boolean showRevisions = "true".equals(request.getParameter("revisions"));
			
			
			
			if (showRevisions && (showMeta || !d.writesRevisionData())) {
				JSONArray revs = featherDB.getBackend().getDocumentRevisions(db, id);
				d.setRevisions(revs);
			}

			if (showMeta && d.writesRevisionData()) { // only show meta data if the document doesn't write it by default
				log.debug("sending meta data for {}",id);
				sendMetaData(response,d,request.getParameterMap());
			} else {
				sendDocument(response,d,request.getParameterMap());
			}
		} else {
			JSONObject status = new JSONObject();
			status.put("db",db);
			status.put("id",id);
			status.put("revision",rev);
			sendError(response, "Document not found",status, HttpServletResponse.SC_NOT_FOUND);
		}
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null  && !db.startsWith("_") && id!=null && !id.startsWith("_") && request.getMethod().equals("GET") && credentials.isAuthorizedRead(db));
	}

}
