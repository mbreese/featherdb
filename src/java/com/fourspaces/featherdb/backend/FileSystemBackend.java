package com.fourspaces.featherdb.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
import com.fourspaces.featherdb.utils.FileUtils;
import com.fourspaces.featherdb.utils.LineCallback;
import com.fourspaces.featherdb.utils.Lock;
import com.fourspaces.featherdb.utils.Logger;

/**
 * Stores documents as files / directories. All of the information is stored
 * 
 * The file structure is: / db / doc id / _revisions - list of revisions in
 * order - one line per revision, most recent is last / _common - common data
 * for all revisions (created date, current revision, etc... ) in JSON format /
 * _permissions - the permissions for this document / rev_id - a file for each
 * revision in JSON format
 * 
 * @author mbreese
 * 
 */
public class FileSystemBackend implements Backend {
	private FeatherDB featherDB;
	private File rootDir;

	final private Logger log = Logger.get(FileSystemBackend.class);


	public FileSystemBackend() {
	}

	private File dbDir(String db) {
		return new File(rootDir, db);
	}

	private File docDir(String db, String id) {
//		if (id.indexOf("/")>-1) {
//			id.replaceAll("/", "\\/");
//		}
		return new File(dbDir(db), id);
	}

	public void deleteDocument(String db, String id) throws BackendException {
		File docDir = docDir(db, id);
		if (!docDir.exists()) {
			log.warn("Deleting non-existant document {}/{}", db,id);
			return;
		}
		log.debug("Deleting document {}/{}", db, id);
		deleteRecursive(docDir);
	}

	public void init() {
	}

	public void shutdown() {
	}

	public Iterable<Document> allDocuments(final String db) {
		return getDocuments(db, null);
	}

	protected void findAllDocuments(List<String> ids, File baseDir, String baseName) {
		for (File f: baseDir.listFiles()) {
			if (f.isDirectory()) {
				if (new File(f,"_common").exists()) {
					log.debug("found _common file in {}",f.getAbsolutePath());
					if (baseName!=null) {
						log.debug("adding id: {}",baseName+"/"+f.getName());
						ids.add(baseName+"/"+f.getName());
					} else {
						log.debug("adding id: {}",f.getName());
						ids.add(f.getName());
					}
				} else {
					if (baseName!=null) {
						findAllDocuments(ids,f,baseName+"/"+f.getName());
					} else {
						findAllDocuments(ids,f,f.getName());
					}
				}
			}
		}
	}
	
	public Iterable<Document> getDocuments(final String db, final String[] ids) {
		File dbDir = dbDir(db);

		final String[] idList;

		if (ids == null) {
			List<String> existingIds = new ArrayList<String>();
			findAllDocuments(existingIds,dbDir,null);
			idList = new String[existingIds.size()];
			int i=0;
			for (String id:existingIds) {
				log.debug("found doc id => {}",id);
				idList[i++] = id;
			}
		} else {
			idList = ids;
		}
		final Iterator<Document> i = new Iterator<Document>() {
			int index = 0;

			Document nextDoc = null;

			public void findNext() {
				nextDoc = null;
				while (idList != null && index < idList.length && nextDoc == null) {
					nextDoc = getDocument(db,idList[index]);
					index++;
				}
			}

			public boolean hasNext() {
				if (index == 0 && nextDoc == null) {
					findNext();
				}
				return nextDoc != null;
			}

			public Document next() {
				Document nd = nextDoc;
				findNext();
				return nd;
			}

			public void remove() {
				findNext();
			}

		};
		return new Iterable<Document>() {
			public Iterator<Document> iterator() {
				return i;
			}
		};
	}

	public Set<String> getDatabaseNames() {
		Set<String> dbs = new HashSet<String>();

		for (File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				dbs.add(f.getName());
			}
		}
		return dbs;
	}

	public Document getDocument(String db, String id) {
		return getDocument(db, id, null);
	}
	
	public Document getDocument(String db, String id, String rev) {
		File docDir = docDir(db, id);
		log.debug("Retrieving document {}/{}/{}", db, id, rev);

		try {
			File commonFile = new File(docDir, "_common");

			if (!commonFile.exists()) {
				log.warn("Document _common file not found: {}/{}",db,id);
				return null;
			}

			JSONObject commonJSON = JSONObject.read(new FileInputStream(commonFile));

			if (rev == null) {
				rev = commonJSON.getString("_current_revision");
			}


			File metaFile = new File(docDir, rev+".meta");
			if (!metaFile.exists()) {
				log.warn("Document .meta file not found: {}/{}/{}",db,id,rev);
				return null;
			}

			JSONObject metaJSON = JSONObject.read(new FileInputStream(metaFile));

			Document d =  Document.loadDocument(commonJSON,metaJSON);
			if (d.writesRevisionData()) {
				File revFile = new File(docDir, rev);
				if (!revFile.exists()) {
					log.warn("Document revision file not found: {}/{}/{}",db,id,rev);
					return null;
				}
				d.setRevisionData(new FileInputStream(revFile));
			}
			return d;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentCreationException e) {
			e.printStackTrace();
		}
		return null;
	}


	public void addDatabase(String name) throws BackendException {
		File dir = new File(rootDir, name);
		if (dir.exists()) {
			log.error("Database dir '{}' already exists!",name);
			throw new BackendException("The database " + name
					+ " already exists!");
		}
		log.debug("Adding database: {}",name);
		dir.mkdir();
	}

	public void deleteDatabase(String name) throws BackendException {
		File dir = dbDir(name);
		if (dir.exists() && dir.isDirectory()) {
			deleteRecursive(dir);
		}
	}

	private void deleteRecursive(File dir) {
		if (dir.isDirectory()) {
			for (File child : dir.listFiles()) {
				if (child.isDirectory()) {
					deleteRecursive(child);
				}
				child.delete();
			}
		}
		if (dir.exists()) {
			dir.delete();
		}
	}

	public boolean doesDatabaseExist(String db) {
		return dbDir(db).exists();
	}

	public boolean doesDocumentExist(String db, String id) {
		return docDir(db, id).exists();
	}

	public boolean doesDocumentRevisionExist(String db, String id,
			String revision) {
		return new File(docDir(db, id), revision).exists();
	}

	public Document saveDocument(Document doc) throws BackendException {
		JSONObject commonJSON = doc.getCommonData();
		commonJSON.put("_current_revision", doc.getRevision());

		File docDir = docDir(doc.getDatabase(), doc.getId());

		if (!docDir.exists()) {
			log.info("Creating document dir: {}",docDir(doc.getDatabase(),doc.getId()));
			docDir.mkdirs();
			try {
				File commonFile = new File(docDir, "_common");  // for common
																// data elements
				commonFile.createNewFile();
				FileUtils.writeToFile(commonFile, "{}");
				

				new File(docDir, "_revisions").createNewFile(); // for keeping
																// revisions in
																// order
			} catch (IOException e) {
				throw new BackendException(e);
			}
		}

		Lock lock = Lock.lock(docDir);
		log.info("updating revision file : {} #{}",doc.getId(),doc.getRevision());

		// write out all revision'd elements
		if (doc.isDataDirty()) {
			try {
				File revisionMetaFile = new File(docDir, doc.getRevision()+".meta");

				// add the current revision to the _revisions file (if needed)	
				if (!revisionMetaFile.exists()) {
					File revListFile = new File(docDir, "_revisions");
					FileUtils.writeToFile(revListFile, doc.getRevision() + "\n", true);
					
					// write the document's revision data if req'd
					if (doc.writesRevisionData()) {
						File revisionFile = new File(docDir, doc.getRevision());
						OutputStream out = new FileOutputStream(revisionFile);
						doc.writeRevisionData(out);
						out.close();
					}
					
					// write the meta json data
					Writer metaWriter = new FileWriter(revisionMetaFile);
					doc.getMetaData().write(metaWriter);
					metaWriter.close();
				}
				
			} catch (IOException e) {
				throw new BackendException(e);
			}
	
		}
		log.info("updating common file : {}",doc.getId());

		// write out all common elements ( if the data is dirty, there is a new current_rev, so a new common
		// needs to be written
		if (doc.isCommonDirty() || doc.isDataDirty()) {
			try {
				File commonFile = new File(docDir, "_common");
				System.err.println("Writing common: "+commonJSON.toString(2));
				Writer writer = new FileWriter(commonFile);
				commonJSON.write(writer);
				writer.close();
			} catch (IOException e) {
				throw new BackendException(e);
			}
		}
		lock.release();
		featherDB.recalculateViewForDocument(doc);
		return doc;
	}

	public JSONArray getDocumentRevisions(String db, final String id) {
		final JSONArray ar = new JSONArray();

		File docDir = docDir(db, id);
		File revListFile = new File(docDir, "_revisions");

		try {
			FileUtils.readFileByLine(revListFile,new LineCallback() {
				public void process(String line) {
					ar.put(line.trim());
				}				
			});
		} catch (IOException e) {
			// I don't like silent exceptions...
			log.error("Error loading revisions for {}/{}",db,id);
		}

		return ar;
	}

	public Map<String, Object> getDatabaseStats(String name) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("db_name", name);
		int count = 0;
		for (File f : dbDir(name).listFiles()) {
			if (f.isDirectory()) {
				count++;
			}
		}
		m.put("doc_count", count);
		return m;
	}

	public void init(FeatherDB featherDB) {
		this.featherDB=featherDB;
		String path = featherDB.getProperty("backend.fs.path");
		if (path == null) {
			throw new RuntimeException(
					"You must include a backend.fs.path element in coffeedb.properties or specify the path in the constructor");
		}
		log.info("Using database path {}", path);
		this.rootDir = new File(path);
		if (!rootDir.exists()) {
			log.debug("Creating database directory");
			rootDir.mkdirs();
		} else if (!rootDir.isDirectory()) {
			log.error("Path: {} not valid!", path);
			throw new RuntimeException("Path: " + path + " not valid!");
		}
	}


	/**
	 * This makes a place-holder file to avoid revision name duplicates.
	 */
	public void touchRevision(String db, String id, String rev) {
		try {
			docDir(db, id).mkdirs();
			new File(docDir(db, id), rev).createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
