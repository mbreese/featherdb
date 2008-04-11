package com.fourspaces.featherdb.auth;




public class SACredentials extends Credentials {
	public SACredentials(String username,String token, int timeout) {
		super(username,token,true,timeout);
	}

	@Override
	public boolean isAuthorizedRead(String db) {
		return true;
	}

	@Override
	public boolean isAuthorizedWrite(String db) {
		return true;
	}
}
