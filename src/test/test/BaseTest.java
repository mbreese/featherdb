package test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.fourspaces.featherdb.FeatherDB;
import com.fourspaces.featherdb.backend.Backend;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.backend.FileSystemBackend;
import com.fourspaces.featherdb.views.ViewException;

public class BaseTest {
	static protected FeatherDB db;
	static protected Backend backend;
	
	@BeforeClass 
	static public void setup(){
		backend = new FileSystemBackend();
		db = new FeatherDB(backend);
		db.init();
		if (!backend.doesDatabaseExist("foodb")) {
			try {
				db.addDatabase("foodb");
			} catch (BackendException e) {
				e.printStackTrace();
			} catch (ViewException e) {
				e.printStackTrace();
			}
		}

	}
	@AfterClass
	static public void destroy() {
		if (db!=null) {
			db.shutdown();
//			try {
//				backend.deleteDatabase("foodb");
//			} catch (BackendException e) {
//				e.printStackTrace();
//			}
		}
	}
}
