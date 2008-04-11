package com.fourspaces.featherdb.document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

@ContentTypes({
	"text/plain",
	"text/html",
	"text/xml",
	"text/css",
	"text/javascript"
})
public class TextDocument extends Document {
	protected String content=null;
	
	@Override
	public void setRevisionData(InputStream dataInput) throws DocumentCreationException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(dataInput));
		StringBuilder sb = new StringBuilder();
		try {
			char[] buffer = new char[4*1024];
			int read=-1;
			while ((read=reader.read(buffer,0,buffer.length))>-1) {
				sb.append(buffer,0,read);
			}
		} catch (IOException e) {
			throw new DocumentCreationException(e);
		}
		content = sb.toString();
	}

	@Override
	public void writeRevisionData(OutputStream dataOutput) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataOutput));
		writer.write(content);
	}

	@Override
	public void sendDocument(OutputStream dataOutput,Map<String,String[]> params) throws IOException {
		writeRevisionData(dataOutput);
	}

	@Override
	public boolean writesRevisionData() {
		return true;
	}
}
