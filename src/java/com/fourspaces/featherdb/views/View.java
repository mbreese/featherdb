package com.fourspaces.featherdb.views;

import java.io.Serializable;

import org.json.JSONObject;

import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.document.Document;

public interface View extends Serializable{
	public JSONObject filter(Document doc);
	public void setBackend(Backend backend);
}
