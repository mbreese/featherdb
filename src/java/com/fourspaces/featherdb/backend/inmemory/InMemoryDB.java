package com.fourspaces.featherdb.backend.inmemory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fourspaces.featherdb.document.Document;

public class InMemoryDB implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3305138416001352871L;
	final private String name;
	final private Map<String,InMemoryDocument> docs = new ConcurrentHashMap<String,InMemoryDocument>();
	public InMemoryDB(String name) {
		this.name=name;
	}
	
	public InMemoryDocument get(String id) {
		return docs.get(id);
	}
	
	public void save(Document doc) {
		if (docs.containsKey(doc.getId())) {
			docs.get(doc.getId()).update(doc);
		} else {
			docs.put(doc.getId(), new InMemoryDocument(doc));
		}
	}

	public void remove(String id) {
		docs.remove(id);
	}

	public String getName() {
		return name;
	}
	
	public Collection<InMemoryDocument> getAllDocuments() {
		return docs.values();
	}
	public int size() {
		return docs.size();
	}
}
