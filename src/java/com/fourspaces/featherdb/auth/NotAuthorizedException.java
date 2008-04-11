package com.fourspaces.featherdb.auth;

public class NotAuthorizedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5482262851766053942L;

	public NotAuthorizedException() {
	}

	public NotAuthorizedException(String message) {
		super(message);
	}

	public NotAuthorizedException(Throwable cause) {
		super(cause);
	}

	public NotAuthorizedException(String message, Throwable cause) {
		super(message, cause);
	}

}
