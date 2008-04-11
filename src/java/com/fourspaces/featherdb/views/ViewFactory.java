package com.fourspaces.featherdb.views;

import java.util.Map;

import com.fourspaces.featherdb.document.JSONDocument;

public interface ViewFactory {
	public Map<String,View> buildViews(JSONDocument doc) throws ViewException;
}
