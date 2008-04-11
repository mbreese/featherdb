package com.fourspaces.featherdb.backend.inmemory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
/**
 * Stores documents / databases in memory in a series of ConcurrentHashMaps.  The use of ConcurrentHashMaps
 * makes this backend provider thread-safe.
 * 
 * @author mbreese
 *
 */
public class InMemoryBackend implements Backend {
	final private Map<String,InMemoryDB> dbs = new ConcurrentHashMap<String,InMemoryDB>();
	private String filename="feather.db";
	private FeatherDB featherDB;
	
	
	public InMemoryBackend() {
	}
	
	public InMemoryBackend(String filename) {
		this.filename =  filename;
	}
	
	public void deleteDocument(String db, String id){
		dbs.get(db).remove(id);
	}

	@SuppressWarnings("unchecked")
	public void init(FeatherDB featherDB) {
		this.featherDB=featherDB;
		String configFilename = featherDB.getProperty("backend.inmemory.filename");
		if (configFilename != null) {
			filename = configFilename;
		}
		if (filename !=null) {
			File f = new File(filename);
			if (f!=null && f.exists()) {
				System.out.println("loading inmemory hashmap from file: "+filename);
				ObjectInputStream ois=null;
				try {
					FileInputStream fis = new FileInputStream(f);
					ois = new ObjectInputStream(fis);
					Map<String, InMemoryDB> map = (Map<String, InMemoryDB>) ois.readObject();
					dbs.putAll(map);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} finally {
					if (ois!=null) {
						try {
							ois.close();
						} catch (IOException e) {
						}
					}
				}
			}
		}
	}

	public void shutdown() {
		if (filename !=null) {
			File f = new File(filename);
			if (f==null) {
				try {
					f.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println("saving featherDB to file: "+filename);
			ObjectOutputStream oos=null;
			try {
				FileOutputStream fos = new FileOutputStream(f);
				oos = new ObjectOutputStream(fos);
				oos.writeObject(dbs);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} finally {
				if (oos!=null) {
					try {
						oos.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	public Iterable<Document> allDocuments(final  String db) {
		return getDocuments(db,null);
	}
	public Iterable<Document> getDocuments(final  String db, final String[] ids) {
		if (!dbs.containsKey(db)) {
			return null;
		}

		final Collection<InMemoryDocument> list;
		if (ids== null) { 
			list = dbs.get(db).getAllDocuments(); 
		} else {
			list = new ArrayList<InMemoryDocument>();
			for (String id: ids) {
				list.add(dbs.get(db).get(id));
			}
		}
		
		final Iterator<Document> i = new Iterator<Document> () {
			int index = 0;
			Document nextDoc = null;
			Iterator<InMemoryDocument> iterator = list.iterator();
			
			private void findNext() {
				nextDoc=null;
				while (iterator.hasNext() && nextDoc == null) {
					InMemoryDocument imdoc = iterator.next();
					index++;
					nextDoc = getDocument(db,imdoc.getId());
				}
			}
			
			public boolean hasNext() {
				if (index==0 && nextDoc==null) {
					findNext();
				}
				return nextDoc!=null;
			}

			public Document next() {
				Document obj=nextDoc;
				findNext();
				return obj;
				
			}

			public void remove() {
				findNext();
			}
		};
		
		return new Iterable<Document>() {
			public Iterator<Document> iterator() {
				return i;
			}};
	}

	public Set<String> getDatabaseNames(Credentials credentials) {
		return dbs.keySet();
	}

	public Document getDocument(String db, String id){
		if (!dbs.containsKey(db)) {
			return null;
		}
		return getDocument(db,id,null);
	}

	public Document getDocument(String db, String id, String rev) {
		if (!dbs.containsKey(db)) {
			return null;
		}
		if (dbs.get(db).get(id) == null) {
			return null;
		}		

		try {
			return dbs.get(db).get(id).getRevision(rev);
		} catch (DocumentCreationException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void addDatabase(String name) throws BackendException{
		if (!dbs.containsKey(name)) {
			dbs.put(name, new InMemoryDB(name));
		} else {
			throw new BackendException("The database '"+name+"' already exists");
		}
	}

	public void deleteDatabase(String name) throws BackendException{
		if (!dbs.containsKey(name)) {
			throw new BackendException("The database '"+name+"' was not found");
		}
		dbs.remove(name);
	}
	
	public boolean doesDatabaseExist(String db) {
		return dbs.containsKey(db);
	}
	
	public boolean doesDocumentExist(String db, String id) {
		if (dbs.get(db)==null) {
			return false;
		}
		return dbs.get(db).get(id)!=null;
	}

	public boolean doesDocumentRevisionExist(String db, String id, String revision) {
		if (dbs.get(db)==null) {
			return false;
		}
		if (dbs.get(db).get(id)==null) {
			return false;
		}
		try {
			return dbs.get(db).get(id).getRevision(revision)!=null;
		} catch (DocumentCreationException e) {
		}
		return false;
	}

	public Document saveDocument(Document doc) {
		dbs.get(doc.getDatabase()).save(doc);
		featherDB.recalculateViewForDocument(doc);
		return doc;
	}

	public JSONArray getDocumentRevisions(String db, String id) {
		if (!dbs.containsKey(db)) {
			return null;
		}
		JSONArray ar = new JSONArray();
		InMemoryDocument imdoc = dbs.get(db).get(id);

		for (String rev: imdoc.getRevisions()) {
			JSONObject o = new JSONObject();
			try {
				o.put("_id", imdoc.getId());
				o.put("_rev", rev);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			ar.put(o);
		}
		
		return ar;
	}

	public Set<String> getDatabaseNames() {
		return dbs.keySet();
	}
	public Map<String, Object> getDatabaseStats(String name) {
		Map<String, Object> m = new HashMap<String,Object>();
		m.put("db_name", name);
		m.put("doc_count", dbs.get(name).size());
		return m;
	}

	public void touchRevision(String db, String id, String rev) {
		dbs.get(db).get(id).touchRevision(rev);
	}
}
