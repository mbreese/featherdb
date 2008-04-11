package com.fourspaces.featherdb.document;

public class DocumentCreationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1833989511252667605L;

	public DocumentCreationException() {
	}

	public DocumentCreationException(String message) {
		super(message);
	}

	public DocumentCreationException(Throwable cause) {
		super(cause);
	}

	public DocumentCreationException(String message, Throwable cause) {
		super(message, cause);
	}

}
