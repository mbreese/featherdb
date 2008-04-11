package com.fourspaces.featherdb.backend;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;

/**
 * Caching backend that operates on a Least Recently Used basis.  It will store X number of Documents
 * by revision.  Only one revision per document is stored.  This class must be backed by a persistent
 * Backend that this class can use to save / retrieve uncached entries.  When necessary, this class will
 * access the backing store using the RootCredentials, otherwise access is based upon the calling
 * credentials.
 * <p>
 * The use of a ConcurrentLinkedQueue to store the LRU data and a ConcurrentHashMap to store the cached Documents
 * makes this class thread-safe.
 * <p>
 * Cached items are stored as weak references, allowing them to be garbage collected if needed.  This will let
 * the JVM adjust the size of the cache if more memory is needed.
 * <p>
 * Configuration settings in coffeedb.properties:<br>
 * backend.cache.class - the fully qualified class name of the backing class<br>
 * backend.cache.size - the number of documents to cache (default: 5000)<br>
 * sa.username - the username for ROOT access (req'd if not default)<br> 
 * sa.password - the password for ROOT access (req'd if not default)<br>
 * @author mbreese
 *
 */
public class LRUCachingBackend implements Backend {
	public static final String BACKEND_CACHE_SIZE = "backend.cache.size";
	public static final String BACKING_CLASS = "backend.cache.class";
	
	protected Backend backend;
	
//	final protected Backend cachingBackend = new InMemoryBackend();
	
	protected Set<String> databaseNames = null;
	
	protected int cacheMax = 5000;
	
	protected Queue<String> lru = new ConcurrentLinkedQueue<String> (); 
	protected Map<String,WeakReference<Document>> cache = new ConcurrentHashMap<String,WeakReference<Document>>();

	public LRUCachingBackend() {
	}
	
	public void init(FeatherDB featherDB) {
		backend.init(featherDB);
		String backendClassName=featherDB.getProperty(BACKING_CLASS);
		if (backendClassName == null) {
			throw new RuntimeException("Missing backend.cache.class value");
		}
		Class backendClass;
		try {
			backendClass = getClass().getClassLoader().loadClass(backendClassName);
			this.backend = (Backend) backendClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
		if (featherDB.getProperty(BACKEND_CACHE_SIZE)!=null) {
			try {
				this.cacheMax=Integer.parseInt(featherDB.getProperty(BACKEND_CACHE_SIZE));;
			} catch (NumberFormatException e) {
				throw new RuntimeException("Error in backend.cache.size setting",e);
			}
		}

		this.databaseNames = backend.getDatabaseNames();
	}
	public void shutdown() {
		backend.shutdown();
	}
	
	protected void trim() {
		while (lru.size()>cacheMax) {
			String key = lru.remove();
			cache.remove(key);
		}
	}
	protected void remove(String db, String id) {
		lru.remove(key(db,id));
		cache.remove(key(db,id));
	}
	protected void add(Document doc) {
		lru.add(key(doc.getDatabase(),doc.getId()));
		cache.put(key(doc.getDatabase(),doc.getId()),new WeakReference<Document>(doc));
		trim();
	}

	protected Document get(String db, String id, String revision) {
		if (lru.contains(key(db,id))) {
			WeakReference<Document> ref=cache.get(key(db,id));
			if (ref!=null && ref.get()!=null) {
				Document doc = ref.get();
				if (revision == null) {
					lru.remove(key(db,id));
					lru.add(key(db,id));
					return doc;
				} else if (doc.getRevision().equals(revision)) {
					lru.remove(key(db,id));
					lru.add(key(db,id));
					return doc;
				} else {
					// do nothing... if we don't have the proper revision, we need to retrieve it below
					// this is just a cache miss.
				}
			}
		}
		
		Document doc = backend.getDocument(db, id,revision);
		add(doc);
		return doc;		
	}
	
	protected String key(String db, String id) {
		return db+"/"+id;
	}
	
	public void addDatabase(String name) throws BackendException{
		backend.addDatabase(name);
		databaseNames = backend.getDatabaseNames();	
	}
	public void deleteDatabase(String name) throws BackendException {
		backend.deleteDatabase(name);
		List<String> keysToRemove = new ArrayList<String>();
		for (String key: lru)
		{
			if (key.startsWith(name+"/")) {
				keysToRemove.add(key);
			}
		}
		for (String key: keysToRemove) {
			lru.remove(key);
			cache.remove(key);
		}
 		databaseNames = backend.getDatabaseNames();	
	}
	public void deleteDocument(String db, String id) throws BackendException {
		try {
			backend.deleteDocument(db, id);
			remove(db,id);
		} catch (BackendException e) {
			throw e;
		}
	}
	public boolean doesDocumentExist(String db, String id) {
		if (lru.contains(key(db,id))) {
			return true;
		} 
		return backend.doesDocumentExist(db, id);
	}
	public boolean doesDocumentRevisionExist(String db, String id, String revision) {
		if (lru.contains(key(db,id))) {
			WeakReference<Document> ref =cache.get(key(db,id));
			if (ref!=null && ref.get()!=null) {
				if (ref.get().getRevision().equals(revision)) {
					return true;
				}
			}
		} 
		return backend.doesDocumentRevisionExist(db, id,revision);
	}
	public Iterable<Document> allDocuments(final String db){
		return getDocuments(db,null);
	}
	public Iterable<Document> getDocuments(final String db, final String[] ids) {
		return backend.allDocuments(db); // no way to cache them all :)
	}
	public Set<String> getDatabaseNames(){
		return databaseNames;
	}
	public Document getDocument(String db, String id){
		return getDocument(db,id,null);
	}
	public Document getDocument(String db, String id, String rev){
		return get(db,id,rev);
	}
	public Document saveDocument(Document doc) throws BackendException{
		Document saved = backend.saveDocument(doc);
		add(saved);
		return saved;
	}
	public JSONArray getDocumentRevisions(String db, String id){
		return backend.getDocumentRevisions(db,id); // no way to cache them all :)
	}
	public boolean doesDatabaseExist(String db) {
		return backend.doesDatabaseExist(db);
	}

	public Map<String, Object> getDatabaseStats(String name) {
		return backend.getDatabaseStats(name);
	}
	public void touchRevision(String database, String id, String rev) {
		backend.touchRevision(database, id, rev);
	}

}
