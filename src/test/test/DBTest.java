package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;
import com.fourspaces.featherdb.document.JSONDocument;

public class DBTest extends BaseTest{
	@Before
	public void before() {
	}
	@Test
	public void test() {
		try {
			if (backend.doesDocumentExist("foodb", "foo")) {
				backend.deleteDocument("foodb", "foo");
			}
			String firstRev;
			String secondRev;
			
			JSONDocument d = (JSONDocument) Document.newDocument(db.getBackend(),"foodb", "foo","unittest");
			
			d.put("foo","bar");
			d=(JSONDocument) db.getBackend().saveDocument(d);
			assertNotNull(d);
			firstRev = d.getRevision();
			
			JSONDocument current = (JSONDocument) db.getBackend().getDocument("foodb", "foo");
			assertNotNull(current);
			assertEquals(current.getRevision(),d.getRevision());
			
			d = (JSONDocument) Document.newRevision(db.getBackend(), d,"unittest");
			
			d.put("foo", "baz");
			d = (JSONDocument) db.getBackend().saveDocument(d);
			secondRev = d.getRevision();
			
			JSONDocument one = (JSONDocument) db.getBackend().getDocument("foodb", "foo",firstRev);
			JSONDocument two = (JSONDocument) db.getBackend().getDocument("foodb", "foo",secondRev);
			
			assertEquals(one.get("foo"),"bar");
			assertEquals(two.get("foo"),"baz");
			assertTrue(db.getBackend().getDocumentRevisions("foodb", "foo").length()==2);
			
		} catch (BackendException e) {
			e.printStackTrace();
			assertNull(e);
		} catch (DocumentCreationException e) {
			e.printStackTrace();
			assertNull(e);
		}
	}
	@Test
	public void testCommonUpdate() {
		try {
			if (backend.doesDocumentExist("foodb", "common")) {
				backend.deleteDocument("foodb", "common");
			}
			JSONDocument d = (JSONDocument) Document.newDocument(db.getBackend(),"foodb", "common","unittest");		
			d.put("foo","bar");
			d.getCommonData().put("_commonfoo","bar");
			d=(JSONDocument) db.getBackend().saveDocument(d);
			String rev = d.getRevision();

			//d = (JSONDocument) Document.newRevision(db.getBackend(), d);
			d.getCommonData().put("_commonfoo","baz");
			d=(JSONDocument) db.getBackend().saveDocument(d);
			assertEquals(rev,d.getRevision());
			
			Document current = db.getBackend().getDocument("foodb", "common");
			assertEquals(current.getRevision(),rev);
			assertTrue(db.getBackend().getDocumentRevisions("foodb", "common").length()==1);
			
		} catch (BackendException e) {
			e.printStackTrace();
			assertNull(e);
		} catch (DocumentCreationException e) {
			e.printStackTrace();
			assertNull(e);
		}
	}
}
