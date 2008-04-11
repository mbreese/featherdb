package com.fourspaces.featherdb.backend.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;

public class InMemoryDocument implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8164429846100317197L;
	final private String id;
	private String currentRevision;
	private JSONObject common = new JSONObject();
	
	private Map<String,byte[]> revisions = new ConcurrentHashMap<String,byte[]>();
	private Map<String,JSONObject> metaData = new ConcurrentHashMap<String,JSONObject>();
	
	public InMemoryDocument(String id) {
		this.id=id;
	}
	public InMemoryDocument(Document doc) {
		this.id=doc.getId();
		update(doc);
	}
	public String getId() {
		return id;
	}
	
	public void update(Document doc) {
		this.common = new JSONObject();
		this.currentRevision = doc.getRevision();
		
		common.putAll(doc.getCommonData());
		
		if (doc.writesRevisionData()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				doc.writeRevisionData(baos);
			} catch (IOException e) {
				e.printStackTrace();
			}
			revisions.put(doc.getRevision(), baos.toByteArray());		
		}
		// write out all "_name" keys to common file (exclude backend/id/revision)
		metaData.put(doc.getRevision(), doc.getMetaData());
	}
	
	public Document getRevision() throws DocumentCreationException {
		return getRevision(null);
	}
	public JSONObject getCommon() {
		JSONObject newCommon = new JSONObject(common);
		newCommon.put("_current_rev", currentRevision);
		return newCommon;
	}
	public Document getRevision(String revision) throws DocumentCreationException {
		if (revision==null) {
			revision=currentRevision;
		}
		Document d = Document.loadDocument(common, metaData.get(revision));
		if (d.writesRevisionData()) {
			d.setRevisionData(new ByteArrayInputStream(revisions.get(revision)));
		}
		return d;
	}
	public String getCurrentRevision() {
		return currentRevision;
	}
	public Set<String> getRevisions() {
		return revisions.keySet();
	}
	public void touchRevision(String rev) {
		revisions.put(rev, null);
	}
}
