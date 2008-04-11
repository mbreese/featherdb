package com.fourspaces.featherdb.httpd;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.utils.Logger;

public class HTTPDServer extends Server {
	
	protected Server server = null;
	
	final protected FeatherDB featherDB;
	
	public HTTPDServer(FeatherDB coffeeDB) {
		this.featherDB=coffeeDB;
	}

	public void init() throws Exception {
		int port = Integer.parseInt(featherDB.getProperty("server.port"));
		server = new Server();
		server.setSendServerVersion(false);

		Connector connector=new SelectChannelConnector();
		connector.setPort(port);
		server.setConnectors(new Connector[]{connector});
		
		ContextHandler context = new ContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		
		HandlerCollection handlers=new HandlerCollection();
		ResourceHandler resourceHandler=new ResourceHandler();
		resourceHandler.setResourceBase(".");
		handlers.setHandlers(new Handler[] {new FeatherDBHandler(featherDB), resourceHandler,new DefaultHandler()});
		context.setHandler(handlers);
		
		try {
			server.start();
		} catch (Exception e) {
			Logger.get(getClass()).error(e,"Error starting Jetty");
			throw e;
		}
	}
	
	public void shutdown() {
		try {
			Logger.get(getClass()).debug("Stopping Jetty");
			server.stop();
		} catch (Exception e) {
			Logger.get(getClass()).error(e,"Error shutting down Jetty");
		}
	}
}
