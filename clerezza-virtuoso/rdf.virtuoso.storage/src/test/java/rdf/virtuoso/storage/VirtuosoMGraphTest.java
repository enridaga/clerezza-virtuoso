package rdf.virtuoso.storage;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import virtuoso.jdbc4.VirtuosoException;

public class VirtuosoMGraphTest {
	static VirtuosoMGraph mgraph = null;
	static final String TEST_GRAPH_NAME = "VirtuosoMGraphTest";

	static Logger log = LoggerFactory.getLogger(VirtuosoMGraphTest.class);

	@BeforeClass
	public static void before() throws ClassNotFoundException, SQLException {
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		mgraph = new VirtuosoMGraph(TEST_GRAPH_NAME, TestUtils.getConnection());
		log.info("Clearing graph <{}> of size {}", TEST_GRAPH_NAME,
				mgraph.size());
		log.info("Clearing graph <{}>", TEST_GRAPH_NAME);
		mgraph.clear();
		log.info("Size is {}", mgraph.size());
		assertTrue(mgraph.size() == 0);
	}

	final UriRef enridaga = new UriRef("enridaga");
	final UriRef alexdma = new UriRef("alexdma");
	final UriRef anuzzolese = new UriRef("anuzzolese");
	final UriRef predicate = new UriRef("http://property/name");
	final PlainLiteral object = new PlainLiteralImpl("Enrico Daga");
	final UriRef knows = new UriRef(TestUtils.FOAF_NS + "knows");

	@Test
	public void testAdd() {
		log.info("Test add()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Triple triple = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return predicate;
			}

			@Override
			public Resource getObject() {
				return object;
			}
		};

		boolean success = mgraph.add(triple);
		assertTrue(success);
	}

	@Test
	public void testFilter() {
		log.info("Test filter(s,p,o)");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Iterator<Triple> it = mgraph.filter(enridaga, predicate, object);
		boolean found = false;
		while (it.hasNext()) {
			found = true;
			Triple t = it.next();
			log.info("Found matching triple: {}",t);
			assertEquals(t.getSubject(), enridaga);
			assertEquals(t.getPredicate(), predicate);
			assertEquals(t.getObject(), object);
		}
		assertTrue(found);
	}

	@Test
	public void testFilterSubject() {
		log.info("Test filter(s,null,null)");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Iterator<Triple> it = mgraph.filter(enridaga, null, null);
		boolean found = false;
		while (it.hasNext()) {
			found = true;
			Triple t = it.next();
			log.info("Found matching triple: {}",t);
			assertEquals(t.getSubject(), enridaga);
		}
		assertTrue(found);
	}

	@Test
	public void testFilterPredicate() {
		log.info("Test filter(null,p,null)");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Iterator<Triple> it = mgraph.filter(null, predicate, null);
		boolean found = false;
		while (it.hasNext()) {
			found = true;
			Triple t = it.next();
			log.info("Found matching triple: {}",t);
			assertEquals(t.getPredicate(), predicate);
		}
		assertTrue(found);
	}

	@Test
	public void testFilterObject() {
		log.info("Test filter(null,null,o)");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Iterator<Triple> it = mgraph.filter(null, null, object);
		boolean found = false;
		while (it.hasNext()) {
			found = true;
			Triple t = it.next();
			log.info("Found matching triple: {}",t);
			assertEquals(t.getObject(), object);
		}
		assertTrue(found);
	}

	@Test
	public void testSize() {
		log.info("Test size()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		// Should be 1 at this time
		log.info("How many triples are in graph <{}>? {}", TEST_GRAPH_NAME,
				mgraph.size());
		assertTrue(mgraph.size() > 0);
	}

	@Test
	public void testIncreaseSize() {
		log.info("Test add() and size()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		int beforeSize = mgraph.size();
		Triple t = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return knows;
			}

			@Override
			public Resource getObject() {
				return alexdma;
			}
		};
		assertTrue(mgraph.add(t));
		int afterSize = mgraph.size();
		assertEquals(beforeSize + 1, afterSize);
	}

	@Test
	public void testRemove() {
		log.info("Test add() and size()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		int beforeSize = mgraph.size();
		Triple t = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return knows;
			}

			@Override
			public Resource getObject() {
				return alexdma;
			}
		};
		assertTrue(mgraph.remove(t));
		int afterSize = mgraph.size();
		assertEquals(beforeSize - 1, afterSize);
	}

	@Test
	public void testGetGraph() {
		log.info("Test getGraph()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Graph g = mgraph.getGraph();
		Triple t = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return knows;
			}

			@Override
			public Resource getObject() {
				return anuzzolese;
			}
		};
		// This should not be allowed
		boolean success;
		try {
			success = g.add(t);
		} catch (UnsupportedOperationException e) {
			success = false;
		}
		assertFalse(success);
	}

	@Test
	public void testContains() {
		log.info("Test contains()");
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		Triple t = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return knows;
			}

			@Override
			public Resource getObject() {
				return anuzzolese;
			}
		};
		boolean addWorks = mgraph.add(t);
		assertTrue(addWorks);
		
		// This second triple is equivalent
		Triple t2 = new Triple() {

			@Override
			public NonLiteral getSubject() {
				return enridaga;
			}

			@Override
			public UriRef getPredicate() {
				return knows;
			}

			@Override
			public Resource getObject() {
				return anuzzolese;
			}
		};
		Iterator<Triple> it = mgraph.filter(t2.getSubject(), t2.getPredicate(), t2.getObject());
		while(it.hasNext()){
			log.info("Found matching triple: {}",it.next());
		}
		assertTrue(mgraph.contains(t2));
		// Also the related read-only graph
		assertTrue(mgraph.getGraph().contains(t2));
	}
	
	@AfterClass
	public static void clear() throws VirtuosoException, ClassNotFoundException, SQLException {
		if (TestUtils.SKIP) {
			log.warn("SKIPPED");
			return;
		}
		log.info("Removing graph <{}>",TEST_GRAPH_NAME);
		Statement st = TestUtils.getConnection().createStatement();
		st.execute("SPARQL CLEAR GRAPH <" + TEST_GRAPH_NAME + ">");
	}
}