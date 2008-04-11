package com.fourspaces.featherdb.views;

public class ViewException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7985089404325957097L;

	public ViewException() {
	}

	public ViewException(String message) {
		super(message);
	}

	public ViewException(Throwable cause) {
		super(cause);
	}

	public ViewException(String message, Throwable cause) {
		super(message, cause);
	}

}
