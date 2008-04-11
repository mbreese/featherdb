package test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
import com.fourspaces.featherdb.document.JSONDocument;
import com.fourspaces.featherdb.views.ViewManager;

public class AllDocsViewTest extends BaseTest{
	@Test
	public void testAllDocs() {
		JSONObject json = db.getViewManager().getViewResults("foodb", "_all_docs", ViewManager.DEFAULT_FUNCTION_NAME);
		System.out.println(json.toString(2));
		JSONArray  ar = json.getJSONArray("rows");
		
		for (int i=0; i< ar.length();i++) {
			JSONObject obj = ar.getJSONObject(i);
			String id = obj.getString("id");
			assertTrue(backend.doesDocumentExist("foodb", id));
		}
		JSONDocument newdoc = null ;
		try {
			newdoc = (JSONDocument) Document.newDocument(backend, "foodb", null,"unittest");
			newdoc.put("foo", "bar");
			newdoc=(JSONDocument) backend.saveDocument(newdoc);
		} catch (DocumentCreationException e) {
			e.printStackTrace();
		} catch (BackendException e) {
			e.printStackTrace();
		}
		assertNotNull(newdoc);
		JSONObject json2 = db.getViewManager().getViewResults("foodb", "_all_docs", ViewManager.DEFAULT_FUNCTION_NAME);
		boolean found=false;
		ar = json2.getJSONArray("rows");
		for (int i=0; i< ar.length();i++) {
			JSONObject obj = ar.getJSONObject(i);
			String id = obj.getString("id");
			if (id.equals(newdoc.getId())) {
				found=true;
			}
		}
		assertTrue(found);
		System.out.println(json2.toString(2));
		try {
			backend.deleteDocument("foodb", newdoc.getId());
		} catch (BackendException e) {
			e.printStackTrace();
		}
	}
}
