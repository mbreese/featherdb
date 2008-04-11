package com.fourspaces.featherdb.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
import com.fourspaces.featherdb.document.JSONDocument;
import com.fourspaces.featherdb.utils.BCrypt;
import com.fourspaces.featherdb.utils.Lock;
import com.fourspaces.featherdb.utils.Logger;

/**
 * <p>
 * This authentication backend does two things for checking a username/password. First, it loads an administrator
 * (sa) username and password from the properties file.   Second, it checks to see if a system document 
 * "_sys/users/username" exists.
 * <p>
 * The user document must contain a "password" entry, and _can_ contain a boolean "is_sa" flag.
 * 
 * @author mbreese
 *
 */
public class BasicAuthentication implements Authentication {
	protected Logger log = Logger.get(Lock.class);
	
	protected List<Credentials> credentials = Collections.synchronizedList(new ArrayList<Credentials>());

	protected static Random random = new java.util.Random();
	protected Thread monitor = null;
	
	protected String saUsername;
	protected String saPasswordHash;
	protected FeatherDB featherDB;
	protected int timeout;

	private boolean allowAnonymous;
	
	public BasicAuthentication () { 
	}
	
	public void init(FeatherDB featherDB) {
		this.featherDB = featherDB;
		if (!featherDB.getBackend().doesDatabaseExist(FeatherDB.USERS_DB)) {
			try {
				featherDB.getBackend().addDatabase(FeatherDB.USERS_DB);
			} catch (BackendException e) {
				e.printStackTrace();
				log.error(e);
				throw new RuntimeException(e);
			}
		}
		String u = featherDB.getProperty("sa.username");
		String p = featherDB.getProperty("sa.password");
		this.saUsername=u;
		this.saPasswordHash=BCrypt.hashpw(p, BCrypt.gensalt());
		this.allowAnonymous = featherDB.getProperty("auth.anonymous","false").toLowerCase().equals("true");
		this.timeout=Integer.parseInt(featherDB.getProperty("auth.timeout.seconds","300"));
		
		monitor = new Thread() {
			boolean stop = false;
			@Override
			public void run() {
				log.debug("Starting authentication cache monitoring thread");
				while (!stop) {
					List<Credentials> expired = new ArrayList<Credentials>();
					for (Credentials cred:credentials) {
						if (cred.isExpired()) {
							expired.add(cred);
						}
					}
					for (Credentials cred: expired) {
						log.debug("Invalidating credentials {} (timeout)", cred.getUsername());
						invalidate(cred);
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						stop = true;
					}
				}
				log.debug("Stopping authentication cache monitoring thread");
			}

			@Override
			public void interrupt() {
				this.stop = true;
				super.interrupt();
			}
		};
		monitor.start();
	}
	
	public void shutdown() {
		monitor.interrupt();
	}
	
	public Credentials addCredentials(Credentials cred) {
		log.debug("Adding credentials {} to cache", cred.getUsername());
		credentials.add(cred);
		return cred;
	}
	
	public Credentials getCredentialsFromToken(String token) {
		for (Credentials cred:credentials) {
			if (cred.getToken().equals(token)) {
				if (!cred.isExpired()) {
					cred.resetTimeout();
					return cred;
				} else {
					invalidate(cred);
				}
			}
		}
		return null;
	}

	public void invalidate(Credentials cred) {
		if (credentials.contains(cred)) {
			credentials.remove(cred);
		}
	}

	public Credentials authenticate(String username, String password) {
		if (password==null) {
			password="";
		}
		log.debug("Attempting authentication: {}", username);
		
		if (allowAnonymous) {
			return addCredentials(new SACredentials(username,generateToken(),timeout));
		} else if (username.equals(saUsername) && BCrypt.checkpw(password,saPasswordHash)) {
			return addCredentials(new SACredentials(username,generateToken(),timeout));
		} else {
			JSONDocument userdoc = (JSONDocument) featherDB.getBackend().getDocument(FeatherDB.USERS_DB,username);
			if (userdoc!=null) {
				String hashedPassword = (String) userdoc.get("password");
				if (hashedPassword == null) {
					hashedPassword = BCrypt.hashpw("", BCrypt.gensalt());
				}
				if (BCrypt.checkpw(password,hashedPassword)) {
					return new DocumentCredentials(userdoc,generateToken(),timeout);
				} else {
					log.debug("Invalid password for user {}",username);
				}
			} else {
				log.debug("No document for user {} found",username);
			}
		}
		return null;
	}

	public synchronized String generateToken() {
		String token = null;
		while (token==null || getCredentialsFromToken(token)!=null) {
			token = Long.toHexString(random.nextLong())+Long.toHexString(random.nextLong());
		}
		return token;
	}

	public void addUser(Credentials cred, String username, String password, boolean sa) throws NotAuthorizedException {
		if (cred.isSA()) {
			try {
				JSONDocument userdoc = (JSONDocument) Document.newDocument(featherDB.getBackend(),FeatherDB.USERS_DB,username,cred.getUsername());
				userdoc.put("username", username);
				userdoc.put("is_sa", sa);
				userdoc.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
				featherDB.getBackend().saveDocument(userdoc);
			} catch (BackendException e) {
				log.error("Backend exception adding user: {}",e,username);
			} catch (DocumentCreationException e) {
				log.error("Backend exception adding user: {}",e,username);
			}
		} else {
			throw new NotAuthorizedException("Only sa users can add new users");
		}
	}

	public void removeUser(Credentials cred, String username) throws NotAuthorizedException {
		if (cred.isSA()) {
			try {
				featherDB.getBackend().deleteDocument(FeatherDB.USERS_DB,username);
			} catch (BackendException e) {
				log.error("Backend exception removing user: {}",e,username);
			}
		} else {
			throw new NotAuthorizedException("Only sa users can remove users");
		}
	}

	public List<Credentials> getCredentials() {
		return credentials;
	}

}