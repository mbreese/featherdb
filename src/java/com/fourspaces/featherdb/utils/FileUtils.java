package com.fourspaces.featherdb.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
	public static void writeToFile(File file, String str) throws IOException {
		writeToFile(file,str,false);
	}
	public static void writeToFile(File file, String str, boolean append) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file,append));
			writer.write(str);
		} finally {
			if (writer!=null) {
				try {
					writer.close();
				} catch (Exception e) {
				}	
			}
		}
	}
	
	public static String readFileAsString(File file) throws IOException {
		final StringBuilder buffer = new StringBuilder("");
		readFileByLine(file, new LineCallback() {
			public void process(String line) {
				buffer.append(line);
				buffer.append("\n");
			}});
		return buffer.toString();
	}
	public static void readFileByLine(File file, LineCallback callback) throws IOException {
		String line = null;
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new FileReader(file));
			while ((line=reader.readLine())!=null) {
				callback.process(line);
			}
		} finally {
			if (reader!=null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}
	public static boolean isFileGZIP(File file) {
		boolean gzip = false;
		FileInputStream fis =null;
		try {
			byte[] gzipbuf = new byte[2];
			fis = new FileInputStream(file);
			fis.read(gzipbuf);
			if (gzipbuf[0]==0x1f && gzipbuf[1]==0xffffff8b) { // gzip file header (magic is 0x8b1f)
				gzip=true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis!=null) {
				try { fis.close(); } catch (Exception e) {}
			}
		}
		return gzip;
	}
}
