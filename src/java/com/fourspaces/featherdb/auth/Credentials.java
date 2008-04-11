package com.fourspaces.featherdb.auth;

import java.util.Date;

public abstract class Credentials {

	public abstract boolean isAuthorizedRead(String db);
	public abstract boolean isAuthorizedWrite(String db);
	
	// main class
	
	final private String username;
	final private String token;
	final private Boolean isSA;
	private long timeoutTimestamp; // default is 300 seconds (300,000 millisec)
	
	private long timeoutLength = 300000;
	
	public Credentials(String username, String token, int timeoutInSeconds) {
		this.username = username;
		this.token = token;
		this.isSA = false;
		this.timeoutLength=timeoutInSeconds*1000;
		resetTimeout();
	}
	
	public Credentials(String username, String token, boolean isSA, int timeoutInSeconds) {
		this.username = username;
		this.token = token;
		this.isSA = isSA;
		this.timeoutLength=timeoutInSeconds*1000;
		resetTimeout();
	}
	public boolean isSA() {
		return isSA;
	}
	public String getToken() {
		return token;
	}
	public String getUsername() {
		return username;
	}
	public boolean isExpired() {
		return ( new Date().getTime() > timeoutTimestamp );
	}
	public void resetTimeout() {
		this.timeoutTimestamp =  new Date().getTime()+ timeoutLength;
	}
}
