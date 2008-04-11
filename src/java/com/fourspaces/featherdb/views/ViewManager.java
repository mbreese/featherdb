package com.fourspaces.featherdb.views;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.JSONDocument;
import com.fourspaces.featherdb.utils.Logger;

/**
 * 
 * View urls are in the form of: /db/_view/viewname
 * 
 * View results are generated for each document when it is updated.
 * The main engine is notified about the update and the engine notifies the ViewManager.
 * 
 * The view manager then processes the document through all the views for the enclosing database.
 * If the view returns a result, that result is appended to the cached results file for that view.
 * 
 * Oh yeah, there is a cached results file for all views.
 * 
 * There is also an index for all views that contains two things:
 * 1) a sorted list of keys
 * 2) the position in the cached results file where the cached data is located.
 * 
 * This allows for appending of data to the cached results file, while only having to re-sort the index.
 * This is a much lighter weight operation.  The newly sorted index is only written if there are no pending
 * requests... basically, requests for results will place a Lock on the View.  The cached results can be optimized
 * for sequential access, but this isn't required.  
 *
 * Views can be handled by anything that implements the View interface.  This includes javascript
 * CouchDB style views.  If the "type" of view is 'text/javascript', then the javascript handler is used.
 * Otherwise, the 'type' is assumed to be a fully-qualified class name.
 * 
 */
abstract public class ViewManager {
	protected final Logger log = Logger.get(getClass());
	public final static String DEFAULT_FUNCTION_NAME = "default";
	
	abstract public void init(FeatherDB featherDB) throws ViewException;
	abstract public void shutdown();
//	abstract public void addView(JSONDocument viewDoc) throws ViewException;
	abstract public void addView(String db, String view, String function, View instance) throws ViewException;
	abstract public boolean doesViewExist(String db, String view, String function);
	abstract public void recalculateDocument(Document doc);
	abstract public JSONObject getViewResults(String db, String view, String function);
	abstract public void initDatabaseViews(String db) throws ViewException;
	abstract public void removeDatabaseViews(String db);
	
	protected Map<String,ViewFactory> factories = new HashMap<String,ViewFactory> ();
	{ 
		factories.put("java", new JavaViewFactory()); 
		factories.put("text/javascript", new JavaScriptViewFactory()); 
	}
	
	public void addView(JSONDocument doc) throws ViewException {
		String viewType = (String) doc.get("type");
		for (String key:factories.keySet()) {
			if (key.equals(viewType)) {
				Map<String,View> views = factories.get(key).buildViews(doc);
				for (String viewFunction:views.keySet()) {
					addView(doc.getDatabase(),doc.getId(),viewFunction,views.get(viewFunction));
				}
				return;
			}
		}
		throw new ViewException("Don't know how to handle view type: "+viewType+" =>"+doc.toString(2));
	}
}
