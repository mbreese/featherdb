package com.fourspaces.featherdb.views;

import org.json.JSONException;
import org.json.JSONObject;

import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.utils.Logger;

public class AllDocuments implements View {
	transient Logger log = Logger.get(AllDocuments.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -7212009333888358060L;
	protected String db;
	public AllDocuments(String db) {
		log.debug("Creating AllDocuments view for db: {}", db);
		this.db=db;
	}

	public JSONObject filter(Document doc) {
		JSONObject json = new JSONObject();
		try {
			json.put("key",JSONObject.NULL);
			json.put("value",doc.getRevision());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public void setBackend(Backend backend) {
		// not needed
	}
}
