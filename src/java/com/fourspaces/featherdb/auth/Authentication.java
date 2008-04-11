package com.fourspaces.featherdb.auth;

import java.util.List;

import com.fourspaces.featherdb.FeatherDB;


/**
 * Authentication backend interface
 * 
 * Should allow for authenticating a username/password, generating credentials, and cache credentials for 
 * retrieval by the token (cookie or http-param).
 * 
 * @author mbreese
 *
 */
public interface Authentication {
	
	/**
	 *  called upon engine init
	 */
	public void init(FeatherDB featherDB);
	/**
	 * called upon engine shutdown
	 *
	 */
	public void shutdown();
	
	/**
	 * Add a user to this backend
	 * @param cred - credentials of the user asking for the new user to be created (needs to be sa)
	 * @param username
	 * @param password
	 * @param sa
	 */
	public void addUser(Credentials cred, String username, String password, boolean sa) throws NotAuthorizedException;
	/**
	 * Remove a user from this backend
	 * @param cred - credentials of the user requesting the removal (needs to be sa)
	 * @param username
	 */
	public void removeUser(Credentials cred, String username) throws NotAuthorizedException;
	
	/**
	 * Authenticate a user by username/password
	 * @param username
	 * @param password
	 * @return null if not valid
	 */
	public Credentials authenticate(String username, String password);
	/**
	 * Based upon a token, retrieve the cached credentials.  This allows the username/password authentication
	 * to occur only once and let the engine figure out who the user is based upon the token (passed in via header, cookie, or param)
	 * 
	 * This is the same as servlet sessions, but done at a lower level to allow the engine to use alternative
	 * mechanisms if needed.
	 *   
	 * @param token
	 * @return
	 */
	public Credentials getCredentialsFromToken(String token);
	/**
	 * Invalidates this set of credentials from the cache (equivalent to invalidating an http session or logging out)
	 * @param credentials
	 */
	public void invalidate(Credentials credentials);
	public List<Credentials> getCredentials();
	
}