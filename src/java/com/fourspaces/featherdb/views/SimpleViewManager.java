package com.fourspaces.featherdb.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.JSONDocument;
import com.fourspaces.featherdb.utils.Logger;

/**
 * This view manager is very simple... it reruns each view upon request (basically adhoc style) 
 * It also serializes each Java view object (named /basedir/_view_name/function_name/view.obj),
 * which is how it persists whether or not a view exists...
 * 
 */
public class SimpleViewManager extends ViewManager {
	protected FeatherDB featherDB;
	protected Logger log = Logger.get(SimpleViewManager.class);
	
	protected File baseDir;
	protected Map<String,View> views = new HashMap<String,View>();
	
	protected final static String VIEW_INSTANCE_NAME = "view.obj";
	
	public SimpleViewManager(){
	}
	
	/* (non-Javadoc)
	 * @see com.fourspaces.featherdb.views.ViewManager#init()
	 */
	public void init(FeatherDB featherDB) throws ViewException {
		this.featherDB=featherDB;
		baseDir = new File(featherDB.getProperty("view.simple.path"));
		if (baseDir==null) {
			throw new RuntimeException("Could not open SimpleViewManager path (view.simple.dir)");
		}
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		//Document d = featherDB.getBackend().getDocument("_views", "list");
		for (String db : featherDB.getBackend().getDatabaseNames()) {
			log.debug("Loading views for: {}", db);
			loadViewsForDatabase(db);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.fourspaces.featherdb.views.ViewManager#shutdown()
	 */
	public void shutdown() {

	}
	
	protected File viewDbDir(String db) {
		return new File(baseDir,db);
	}
	
	protected File viewDir(String db, String viewName) {
		return new File(viewDbDir(db),viewName);
	}
	
	protected void loadViewsForDatabase(String db) throws ViewException {
		File viewDbDir = viewDbDir(db);
		if (!viewDbDir.exists()) {
			initDatabaseViews(db);
		} else {
			for(File instanceDir:viewDbDir.listFiles()) {
				if (instanceDir.isDirectory()) {
					for (File functionDir: instanceDir.listFiles()) {
						ObjectInputStream ois =null;
						try {
							ois = new ObjectInputStream(new FileInputStream(new File(functionDir,VIEW_INSTANCE_NAME)));
							log.debug("Loading view {}/{}/{} ",db,instanceDir.getName(),functionDir.getName());
							views.put(db+"/"+instanceDir.getName()+"/"+functionDir.getName(),(View) ois.readObject());
						} catch (FileNotFoundException e) {
							throw new ViewException(e);
						} catch (IOException e) {
							throw new ViewException(e);
						} catch (ClassNotFoundException e) {
							throw new ViewException(e);
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
		}
	}
	
	/* (non-Javadoc)
	 * @see com.fourspaces.featherdb.views.ViewManager#addView(com.fourspaces.featherdb.document.Document)
	 */
	public void addView(JSONDocument jsondoc) throws ViewException {
			String viewType = (String) jsondoc.get("view_type");
			if (viewType == null || viewType.equals("text/javascript")) {
				for (String key:jsondoc.keys()) {
					if (((String)jsondoc.get(key)).startsWith("function")) {
						log.debug("Adding javascript view: {}/{}/{} => {}",jsondoc.getDatabase(),jsondoc.getId(),key,jsondoc.get(key));
						addView(jsondoc.getDatabase(),jsondoc.getId(),key,new JavaScriptView(jsondoc.getDatabase(),(String) jsondoc.get(key)));
					}
				}
			} else if (viewType.startsWith("java:")){
				log.debug("Adding java view: {}/{} => {}", jsondoc.getDatabase(),jsondoc.getId(),viewType);
				try {
					Class clazz = Thread.currentThread().getContextClassLoader().loadClass(viewType.substring(5));
					addView(jsondoc.getDatabase(),jsondoc.getId(),DEFAULT_FUNCTION_NAME,(View) clazz.newInstance());
				} catch (ClassNotFoundException e) {
					throw new ViewException(e);
				} catch (ViewException e) {
					throw new ViewException(e);
				} catch (InstantiationException e) {
					throw new ViewException(e);
				} catch (IllegalAccessException e) {
					throw new ViewException(e);
				} 
			} else {
				log.warn("Don't know how to handle view type: {}\n{}", viewType,jsondoc.toString());
			}
	}
	
	protected View getView(String db, String view, String function) {
		return views.get(db+"/"+view+"/"+function);
	}
	
	public JSONObject getViewResults(String db, String viewName, String function) {
		return AdHocViewRunner.runView(featherDB,db,viewName,function,getView(db,viewName,function));
	}
	
	public void recalculateDocument(Document doc) {
		// this manager recalculates all views on the fly... so this isn't needed.
		// but we still need to add new views!
		
		if (doc.getId().startsWith("_") && doc instanceof JSONDocument) {
			try {
				addView((JSONDocument) doc);
			} catch (ViewException e) {
				log.error("Error adding new view: {}",doc.getId(),e);
			}
		}
	}

	public void initDatabaseViews(String db) throws ViewException {
		File viewDir = viewDbDir(db);
		viewDir.mkdirs();
		addView(db,"_all_docs",DEFAULT_FUNCTION_NAME,new AllDocuments(db));
	}

	public void addView(String db, String view, String function,View instance) throws ViewException {
		File viewDir = new File(viewDir(db,view),function);
		if (!viewDir.exists()) {
			viewDir.mkdirs();
		}
		ObjectOutputStream oos =null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(new File(viewDir,VIEW_INSTANCE_NAME)));
			oos.writeObject(instance);
			oos.close();
		} catch (FileNotFoundException e) {
			throw new ViewException(e);
		} catch (IOException e) {
			throw new ViewException(e);
		} finally {
			if (oos!=null) {
				try {
					oos.close();
				} catch (IOException e) {
				}
			}
		}
		views.put(db+"/"+view+"/"+function,instance);
	}
	public void removeDatabaseViews(String db) {
		recursivelyDeleteFiles(viewDbDir(db));
	}
	private void recursivelyDeleteFiles(File file) {
		if (file.isDirectory()) {
			for (File f:file.listFiles()) {
				recursivelyDeleteFiles(f);
			}
		}
		file.delete();
	}
	public boolean doesViewExist(String db, String view, String function) {
		return views.containsKey(db+"/"+view+"/"+function);
	}
	public static JSONObject adHocView(FeatherDB featherDB,String db,String viewName, String functionName, String src) throws ViewException {
		View view=new JavaScriptView(db,src);
		return adHocView(featherDB,db,viewName,functionName,view);
	}
	public static JSONObject adHocView(FeatherDB featherDB,String db,String viewName, String functionName,View view) {
		view.setBackend(featherDB.getBackend());
		JSONObject results = new JSONObject();
		int total = 0;
		for (Document doc: featherDB.getBackend().allDocuments(db)) {
				JSONObject result = view.filter(doc);
				if (result!=null) {
					results.put((String) result.get("key"),result.get("value"));
					total++;
				}
		}
		JSONObject out = new JSONObject();
		out.put("view", viewName+"/"+functionName);
		out.put("total_rows", total);
		out.put("rows",results);
		return out;
	}


}
