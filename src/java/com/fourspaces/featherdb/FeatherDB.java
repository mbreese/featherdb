package com.fourspaces.featherdb;

import java.util.Properties;

import com.fourspaces.featherdb.auth.Authentication;
import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.httpd.HTTPDServer;
import com.fourspaces.featherdb.utils.Logger;
import com.fourspaces.featherdb.views.ViewException;
import com.fourspaces.featherdb.views.ViewManager;

public class FeatherDB {
	public static final String USERS_DB = "_users";
	private boolean shutdown = false;

	protected Logger log = Logger.get(FeatherDB.class);
	
	protected final Backend backend;
	protected final HTTPDServer httpd;
	protected final Authentication auth;
	protected final ViewManager viewManager;

	protected final Properties properties;
	
	public FeatherDB () {
		this.properties = FeatherDBProperties.getProperties();
		this.backend = buildBackend();
		this.auth = buildAuthentication();
		this.httpd = buildHTTPD();
		this.viewManager = buildViewManager();
	}
	public FeatherDB (Backend backend) {
		this.properties = FeatherDBProperties.getProperties();
		this.backend=backend;
		this.auth = buildAuthentication();
		this.httpd = buildHTTPD();
		this.viewManager = buildViewManager();
	}
	public FeatherDB (Properties properties) {
		this.properties = FeatherDBProperties.getProperties();
		this.properties.putAll(properties);
		this.backend = buildBackend();
		this.auth = buildAuthentication();
		this.httpd = buildHTTPD();
		this.viewManager = buildViewManager();
	}
	public FeatherDB (Backend backend, Properties properties) {
		this.properties = FeatherDBProperties.getProperties();
		this.properties.putAll(properties);
		this.backend=backend;
		this.auth = buildAuthentication();
		this.httpd = buildHTTPD();
		this.viewManager = buildViewManager();
	}
	
	public Authentication getAuthentication() {
		return auth;
	}

	protected Backend buildBackend() {
		String backendClassStr = getProperty("backend.class");		
		try {
			return (Backend) getClass().getClassLoader().loadClass(backendClassStr).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected ViewManager buildViewManager() {
		String viewClassStr = getProperty("view.class");		
		try {
			ViewManager vm = (ViewManager) getClass().getClassLoader().loadClass(viewClassStr).newInstance();
			return vm;
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected Authentication buildAuthentication() {
		String authClassName = getProperty("auth.class");
		try {
			return (Authentication) getClass().getClassLoader().loadClass(authClassName).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}
	
	protected HTTPDServer buildHTTPD() {
		if (getProperty("server.start").equals("true")) {
			return new HTTPDServer(this);
		} else {
			return null;
		}
		

	}
		
	public void init() {
		log.info("Initializing engine");
		try {
			backend.init(this);
			if (auth!=null) {
				auth.init(this);
			}
			if (httpd!=null) {
				httpd.init();
			}
			viewManager.init(this);
		} catch (Exception e) {
			internalShutdown();
			throw new RuntimeException(e);
		}
	}

	public void shutdown() {
		shutdown=true;
	}
	
	private void internalShutdown() {
		log.info("Shutting down engine");
		backend.shutdown();
		if (auth!=null) {
			auth.shutdown();
		}
		if (httpd!=null) {
			httpd.shutdown();
		}
	}
	
	public Backend getBackend() {
		return backend;
	}
	public static void main(String[] args) {
		final FeatherDB app = new FeatherDB();

		final Thread hook = new Thread() {
			@Override
			public void run() {
				app.internalShutdown();
			}
		};
		
		Runtime.getRuntime().addShutdownHook(hook);
		
		app.run();
		
		Runtime.getRuntime().removeShutdownHook(hook);
	}

	private void run() {
		init();
		while (!shutdown) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				shutdown = true;
			}
		}
		try { Thread.sleep(1000); } catch (InterruptedException e) {}
		internalShutdown();
	}
	
	public void recalculateViewForDocument(Document doc) {
		viewManager.recalculateDocument(doc);
	}

	public void addDatabase(String db) throws BackendException, ViewException {
		backend.addDatabase(db);
		viewManager.initDatabaseViews(db);
	}

	public void deleteDatabase(String db) throws BackendException, ViewException {
		backend.deleteDatabase(db);
		viewManager.removeDatabaseViews(db);
	}

	public ViewManager getViewManager() {
		return viewManager;
	}
	public String getProperty(String key, String def) {
		return properties.getProperty(key,def);
	}
	public String getProperty(String key) {
		return properties.getProperty(key);
	}


}
