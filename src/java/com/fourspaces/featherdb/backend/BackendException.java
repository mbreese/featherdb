package com.fourspaces.featherdb.backend;

public class BackendException extends Exception {

	public BackendException() {
		super();
	}

	public BackendException(String message, Throwable cause) {
		super(message, cause);
	}

	public BackendException(String message) {
		super(message);
	}

	public BackendException(Throwable cause) {
		super(cause);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7774649915972342337L;

}
