package com.fourspaces.featherdb.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

@ContentTypes({Document.DEFAULT_CONTENT_TYPE,"text/javascript"})
public class JSONDocument extends Document {
	public JSONDocument() {}

	public JSONDocument(JSONObject source) throws DocumentCreationException {
		setRevisionData(source);
	}
	
	
	@Override
	public void setRevisionData(InputStream dataInput) throws DocumentCreationException {
		try {
			metaData = JSONObject.read(dataInput);
		} catch (IOException e) {
			throw new DocumentCreationException(e);
		}
	}

	public void setRevisionData(JSONObject source) throws DocumentCreationException {
		for (String key:source.keySet()) {
			if (key.startsWith("_") && !key.equals(REV) && !key.equals(REV_DATE) && !key.equals(REV_USER)) {
				commonData.put(key, source.get(key));
			} else {
				metaData.put(key, source.get(key));
			}
		}
	}

	@Override
	public void writeRevisionData(OutputStream dataOutput) throws IOException {
		// does nothing since all JSON data is stored in the MetaData stream
	}

	@Override
	public void sendDocument(OutputStream dataOutput,Map<String,String[]> params) throws IOException {
		boolean pretty=false;
		if (params.containsKey("pretty")) {
			String[] values = params.get("pretty");
			for (String value:values) {
				if (value.equals("true")) {
					pretty=true;
				}
			}
		}
		for (String key: params.keySet()) {
			log.debug("{} => {}",key,params.get(key));
			int i=0;
			for (String value:params.get(key)) {
				log.debug("[{}] => ", i++,value);
			}
		}
		log.info("pretty? = {}", pretty);
		Writer writer = new OutputStreamWriter(dataOutput);
		if (pretty) {
			writer.write(toString(2));
		} else {
			writer.write(toString());
		}
		writer.close();
	}

	public void put(String key,Object value) {
		if (key.startsWith("_")) {
			commonDirty=true;
			commonData.put(key, value);
		} else {
			dataDirty=true;
			metaData.put(key, value);
		}
	}

	public Object get(String key) {
		if (key.startsWith("_")) {
			return commonData.get(key);
		} 
		return metaData.get(key);
	}
	
	public Set<String> keys() {
		return metaData.keySet();
	}

	@Override
	public boolean writesRevisionData() {
		return false;
	}
}
