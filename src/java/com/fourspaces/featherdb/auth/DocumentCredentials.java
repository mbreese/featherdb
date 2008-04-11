package com.fourspaces.featherdb.auth;

import org.json.JSONObject;

import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.JSONDocument;

/**
 * Document based User credentials.  The user credentials should contain a JSONObject named "db_access".
 * This JSONObject will contain a list of database names with either "ro", "wo", or "rw".
 * 
 * Example document:
 * 
 * {
 *   _id = "username",
 *   _rev = "...",
 *   _db = "_users"
 *   password="hash of password",
 *   is_sa = false,
 *   db_access = {
 *          "foo" = "ro",
 *          "bar" = "rw"
 *        }
 * }
 *          
 *  
 * @author mbreese
 *
 */
public class DocumentCredentials extends Credentials{
	protected final Document doc;
	protected JSONObject dbs;
	public DocumentCredentials(JSONDocument doc, String token, int timeout) {
		super(doc.getId(),token,(Boolean)doc.get("is_sa"),timeout);
		this.doc=doc;
		dbs = (JSONObject) doc.get("db_access");
	}

	@Override
	public boolean isAuthorizedRead(String db) {
		if (isSA()) { return true; }
		if (dbs.get(db)!=null) {
			return dbs.getString(db).contains("r");
		}
		return false;
	}

	@Override
	public boolean isAuthorizedWrite(String db) {
		if (isSA()) { return true; }
		if (dbs.get(db)!=null) {
			return dbs.getString(db).contains("w");
		}
		return false;
	}

}
