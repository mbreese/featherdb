package com.fourspaces.featherdb.backend;

import java.util.Map;
import java.util.Set;

import org.json.JSONArray;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;

public interface Backend {
	public void init(FeatherDB featherDB);
	public void shutdown();

	public Set<String> getDatabaseNames();
	
	public void addDatabase(String name) throws BackendException;
	public void deleteDatabase(String name) throws BackendException;
	public Map<String,Object> getDatabaseStats(String name);
	
	public Iterable<Document> allDocuments(String db);
	public Iterable<Document> getDocuments(String db, String[] ids);
	
	public Document getDocument(String db,String id);
	public Document getDocument(String db,String id, String rev);
	public JSONArray getDocumentRevisions(String db,String id);

	public boolean doesDatabaseExist(String db);
	public boolean doesDocumentExist(String db, String id);
	public boolean doesDocumentRevisionExist(String db, String id, String revision);
	
	public Document saveDocument(Document doc) throws BackendException;
	public void deleteDocument(String db,String id) throws BackendException;
	public void touchRevision(String database, String id, String rev);
	
}
