package com.fourspaces.featherdb.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@ContentTypes({
	"application/octet-stream",
	"image/png",
	"image/jpeg",
	"images/gif",
	"*"
})
public class BinaryDocument extends Document {
	protected byte[] contents = null;

	@Override
	public void setRevisionData(InputStream dataInput) {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			log.error(e1, "Missing MD5 digester");
		}
		
		byte[] buffer = new byte[16*1024]; // 16k buffer... should be tunable
		int read = 0;
		try {
			while ((read=dataInput.read(buffer)) > -1) {
				baos.write(buffer, 0, read);
				if (md!=null) {
					md.update(buffer,0,read);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
			}
		}
		contents = baos.toByteArray();
		metaData.put("size", contents.length);
		if (md!=null) {
			metaData.put("md5", bytesToHex(md.digest()));
		}
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b:bytes) {
			String s = Integer.toHexString(b & 0xFF);
			if (s.length()<2) {
				sb.append("0");
			}
			sb.append(s);
		}
		return sb.toString();
	}
	
	@Override
	public void writeRevisionData(OutputStream dataOutput) throws IOException {
		if (contents!=null) { // this implementation can only write once.
			ByteArrayInputStream is = new ByteArrayInputStream(contents);
			byte[] buffer = new byte[16*1024]; // 16k buffer... should be tunable
			int read = -1;
			while ((read=is.read(buffer)) > -1) {
				dataOutput.write(buffer, 0, read);
			}
			is.close();
		}
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
