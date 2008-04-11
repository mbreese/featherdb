package com.fourspaces.featherdb.views;

import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONException;
import org.json.JSONObject;

import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.JSONDocument;
import com.fourspaces.featherdb.utils.Logger;



/**
 * 
 * Creates new JavaScript Views...  
 * 
 * This will create a new javascript engine for each view instance (not optimal, but without proper nested
 * ScriptContext's I'm not sure how else to do it).  This is <b>not thread-safe</b>.  It must be called from
 * a single ViewRunner to lower the overhead of having a JS Engine instance for each JavaScriptView instance.
 * <p>
 * The alternative is to have an engine (and initialize it) for each document... and that's even worse.
 * 
 */

@ViewType("text/javascript")

public class JavaScriptView implements View {
    /**
	 * 
	 */
	private static final long serialVersionUID = 713522274681368650L;
	transient protected ScriptEngineManager manager = new ScriptEngineManager();
	transient protected ScriptEngine engine = null;

	transient protected Backend backend;
	transient protected Logger log = Logger.get(JavaScriptView.class);

	protected String db;
	protected String src;
		
	public JavaScriptView(String db,String src) throws ViewException {		
		this.db=db;
		this.src=src;
		setupEngine();
	}
	
	protected void setupEngine() throws ViewException {
		if (engine == null) {
			engine = manager.getEngineByName("js");
			try {
				engine.eval(new InputStreamReader(getClass().getResourceAsStream("json.js")));
				engine.eval("function map(key,val) { _FeatherDB_retval = { 'key':key,'value':val }; }");
				engine.eval("function get(id,revision,db) { return eval('('+_FeatherDB_JSVIEW.get(id,revision,db)+')'); }");
		        engine.eval("function toJSON(obj) { if (obj!=null && typeof obj != 'undefined') { return obj.toJSONString(); } } ");
				engine.eval("_FeatherDB_filter="+src);
				engine.put("_FeatherDB_JSVIEW",this);
			} catch (ScriptException e) {
				log.error(e);
				throw new ViewException(e);
			}
		}
	}
	
	public void setBackend(Backend backend) {
		this.backend=backend;
	}
	
	public JSONObject get(String id, String rev, String mydb) {
		if (mydb==null) {
			mydb = this.db;
		}
		Document d = null;
		d = backend.getDocument(mydb, id,rev);
		if (d!=null) {
			if (d instanceof JSONDocument) {
				return new JSONObject(d.toString());
			}
			return new JSONObject().put("error", mydb+"/"+id+" is not a JSONDocument");
		}
		return null;
	}

	public JSONObject filter(Document doc1) {
		JSONDocument doc = (JSONDocument) doc1;
		try {
			setupEngine();
		} catch (ViewException e1) {
			log.error("Error filtering object",e1);
			return null;
		}
		try {	        
			engine.eval("_FeatherDB_retval = '' ");

			Object docJSObject = engine.eval("_FeatherDB_doc = eval('('+'"+ doc.toString()+"'+')');");

			Invocable invocable = (Invocable) engine;
			Object retval = invocable.invokeFunction("_FeatherDB_filter",docJSObject);
			String json=null;
			if (retval != null) {
				json = (String) invocable.invokeFunction("toJSON",new Object[]{retval});
			} else {
				json = (String) engine.eval("_FeatherDB_retval.toJSONString();");
			}
			if (json!=null && !json.equals("") && !json.equals("\"\"")) {
				return new JSONObject(json);
			}
			
		} catch (ScriptException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
}
