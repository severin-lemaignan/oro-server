/*
 * Copyright (c) 2008-2010 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package laas.openrobots.ontology.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.connectors.SocketConnector.ClientWorker;
import laas.openrobots.ontology.exceptions.AgentNotFoundException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidModelException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.exceptions.InvalidRuleException;
import laas.openrobots.ontology.exceptions.NotComparableException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.modules.alterite.AlteriteModule;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.categorization.CategorizationModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

import org.junit.Test;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.shared.PropertyNotFoundException;

/**
 * This class holds unit tests that cover most of the {@code oro-server} features.<br/>
 * For the tests to be executed, the {@code oro_test.owl} ontology is required, and must be referenced by the {@code oro_test.conf} configuration file.<br/>
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class OpenRobotsOntologyTest {

	final String ORO_TEST_CONF = "etc/oro-server/oro_test.conf";
	Properties conf;
	
	public OpenRobotsOntologyTest() {
		String confFile = System.getProperty("ORO_TEST_CONF");
		if (confFile == null)
			confFile = ORO_TEST_CONF;
		
		conf = OroServer.getConfiguration(confFile);
	}

	/***********************************************************************
	 *                          BASIC TESTS                                *
	 ***********************************************************************/
	
	@Test
	public void helpersFunction() {
	
		System.out.println("[UNITTEST] ***** TEST: Testing helper functions *****");
		
		System.out.println("Tokenization...");
		// with a comma separator, the string below should be tokenized in "{}", "f\'\", " \}", " [r ,t,\]" ",r ] ", "a" (5 tokens)
		// with a space as separator, the string below should be tokenized in "{},f\'\,", "\},", "[r ,t,\]" ",r ]", ",a" (4 tokens)
		String strToTokenize = "{},f\\\'\\, \\}, [r ,t,\\]\" \",r ] ,a";
		assertEquals("Wrong tokenization using commas.", 5, Helpers.tokenize(strToTokenize, ',').size());
		assertEquals("Wrong tokenization using spaces.", 4, Helpers.tokenize(strToTokenize, ' ').size());
		
		System.out.println("OK.\nCleaning values...");
		
		try {
			assertTrue(Helpers.cleanValue("toto").equals("toto"));
			assertTrue(Helpers.cleanValue("toto aime tata").equals("toto aime tata"));
			assertTrue(Helpers.cleanValue("\"toto\"").equals("toto"));
			assertTrue(Helpers.cleanValue("\'toto\'").equals("toto"));
			assertTrue(Helpers.cleanValue("\'to\\nto\'").equals("to\nto"));
			assertTrue(Helpers.cleanValue("\'toto aime tata\'").equals("toto aime tata"));
			assertTrue(Helpers.cleanValue("\'toto \'aime\' tata\'").equals("toto \'aime\' tata"));
			assertTrue(Helpers.cleanValue("\"toto \'aime\' tata\"").equals("toto \'aime\' tata"));
			assertTrue(Helpers.cleanValue("\\\"toto \"aime\" tata\\\"").equals("\"toto \"aime\" tata\""));
			assertTrue(Helpers.cleanValue("\"toto \\\"aime\\\" tata\"").equals("toto \"aime\" tata"));
			assertTrue(Helpers.cleanValue("\"toto \\\'aime\\\' tata\"").equals("toto \'aime\' tata"));
			assertTrue(Helpers.cleanValue("\'toto \\\'aime\\\' tata\'").equals("toto \'aime\' tata"));
		} catch (OntologyServerException ose) {
			fail();
		}
			
		
		/**** Test stringification ****/
		System.out.println("OK.\nStringification...");
		
		assertEquals("\"toto\"", Helpers.stringify("toto"));
		assertEquals("\"toto et \"tata\"\"", Helpers.stringify("toto et \"tata\""));
		assertEquals("3.1415", Helpers.stringify(3.1415));
		assertEquals("true", Helpers.stringify(true));
		
		List<String> t1 = new ArrayList<String>();
		t1.add("toto");
		t1.add("tata");
		assertEquals("[\"toto\",\"tata\"]", Helpers.stringify(t1));
		
		Set<String> t2 = new HashSet<String>();
		assertEquals("[]", Helpers.stringify(t2));
		
		t2.add("toto");
		t2.add("titi");
		String res = Helpers.stringify(t2);
		assertTrue(res.equals("[\"toto\",\"titi\"]") || res.equals("[\"titi\",\"toto\"]"));
		
		Map<Integer, String> t3 = new HashMap<Integer, String>();
		assertEquals("{}", Helpers.stringify(t3));
		
		t3.put(1, "toto");
		t3.put(2, "tata");
		assertEquals("{1:\"toto\",2:\"tata\"}", Helpers.stringify(t3));
		
		List<String> t1bis = new ArrayList<String>();
		t1bis.add("tutu");		
		
		Set<List<String>> t4 = new HashSet<List<String>>();
		assertEquals("[]", Helpers.stringify(t4));
		
		t4.add(t1);
		t4.add(t1bis);
		res = Helpers.stringify(t4);
		assertTrue(res.equals("[[\"toto\",\"tata\"],[\"tutu\"]]") || res.equals("[[\"tutu\"],[\"toto\",\"tata\"]]"));
		
		Map<Set<String>, List<String>> t5 = new HashMap<Set<String>, List<String>>();
		assertEquals("{}", Helpers.stringify(t5));
		
		t5.put(t2, t1);
		res = Helpers.stringify(t5);
		assertTrue(res.equals("{[\"toto\",\"titi\"]:[\"toto\",\"tata\"]}") || res.equals("{[\"titi\",\"toto\"]:[\"toto\",\"tata\"]}"));
		
		/**** Test deserialization ****/
		System.out.println("OK.\nDeserialization...");
		
		try {
			
			assertTrue(Helpers.deserialize("1", String.class).equals("1"));
			assertTrue(Helpers.deserialize("1", Integer.class).equals(new Integer(1)));
			assertTrue(Helpers.deserialize("1", Double.class).equals(new Double(1.0)));
			assertTrue(Helpers.deserialize("true", Boolean.class));
			assertTrue(Helpers.deserialize("TrUe", Boolean.class));
			assertFalse(Helpers.deserialize("false", Boolean.class));
			assertFalse(Helpers.deserialize("toto", Boolean.class));
			
			try {
				Helpers.deserialize("toto", Integer.class);
				fail();
			}
			catch (IllegalArgumentException e) {}
			
			try {
				Helpers.deserialize("toto", Double.class);
				fail();
			}
			catch (IllegalArgumentException e) {}
			
			
			assertTrue(Helpers.deserialize("[]", List.class).isEmpty());
			assertTrue(Helpers.deserialize("[]", Set.class).isEmpty());
			assertTrue(Helpers.deserialize("{}", Map.class).isEmpty());
			
			try {
				assertTrue(Helpers.deserialize("{}", Set.class).isEmpty());
				fail();
			}
			catch (IllegalArgumentException e) {}
			
			try {
				assertTrue(Helpers.deserialize("[]", Map.class).isEmpty());
				fail();
			}
			catch (IllegalArgumentException e) {}
			
			assertTrue(Helpers.deserialize("[\"toto\",\"tata\"]", List.class).containsAll(t1) && t1.containsAll(Helpers.deserialize("[\"toto\",\"tata\"]", List.class)));
			
			assertTrue(Helpers.deserialize("[\"toto\",\"titi\"]", Set.class).containsAll(t2) && t2.containsAll(Helpers.deserialize("[\"toto\",\"titi\"]", Set.class)));
			
			Map<String, String> map = Helpers.deserialize("{1:\"toto\",2:\"tata\"}", Map.class);
			
			for (String i : map.keySet()) {
				assertEquals(map.get(i), t3.get(Integer.parseInt(i)));
			}
		} catch (OntologyServerException ose) {
			fail();
		}
				
		System.out.println("OK.");
	}
	
	@Test
	public void socketConnectorRequestParser() {
	
		Charset charset = Charset.forName("UTF-8");
		
		SocketConnector sc = new SocketConnector(conf, null, null);
		ClientWorker s = sc.new ClientWorker(null);
		
		List<String> res;
				
		res = s.parseBuffer(charset.encode(
				"help\n" +
				"addForAgent\nTOTO\n[\"TATA likes TOTO\"]\n#end#\n" +
				"test2\n"));
		
		assertTrue(res.get(0).equals("help"));
		
		res = s.parseBuffer(null);
		assertNotNull(res);
		assertTrue(res.get(0).equals("addForAgent"));
		
		res = s.parseBuffer(null);
		assertTrue(res == null);
		
		res = s.parseBuffer(charset.encode("#end#\nhelp\ntest3\nparam1\n#end"));
		assertNotNull(res);
		assertEquals("test2", res.get(0));
		
		res = s.parseBuffer(null);
		assertNotNull(res);
		assertEquals("help", res.get(0));
		
		
		res = s.parseBuffer(charset.encode("#\n"));
		assertNotNull(res);
		assertEquals("test3", res.get(0));
		
		
	}
	
	@Test
	public void rulesTokenizer() {
	
		System.out.println("[UNITTEST] ***** TEST: Testing rules tokenizer *****");

		String rule = "Male(?y), hasParent(?x, ?y) -> hasFather(?x, ?y)";
		
		try {
			assertEquals("Male(?y)", Helpers.tokenizeRule(rule).getLeft().get(0));
			assertEquals("hasParent(?x, ?y)", Helpers.tokenizeRule(rule).getLeft().get(1));
			assertEquals("hasFather(?x, ?y)", Helpers.tokenizeRule(rule).getRight().get(0));
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}	
			rule = "atom1   (  ?x, ?y),,atom2(abc), -> atom2(?x), atom3(?x, ?y, ?z)";

		try {
			assertEquals("atom1   (  ?x, ?y)", Helpers.tokenizeRule(rule).getLeft().get(0));
			assertEquals("atom2(abc)", Helpers.tokenizeRule(rule).getLeft().get(1));
			assertEquals("atom2(?x)", Helpers.tokenizeRule(rule).getRight().get(0));
			assertEquals("atom3(?x, ?y, ?z)", Helpers.tokenizeRule(rule).getRight().get(1));
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}
		
		try {
			rule = "atom1";
			Helpers.tokenizeRule(rule);
			fail();
		} catch (InvalidRuleException ire) {}
		
		try {
			rule = "atom1 ->";
			Helpers.tokenizeRule(rule);
			fail();
		} catch (InvalidRuleException ire) {}
		
		try {
			rule = "atom1(?x ->";
			Helpers.tokenizeRule(rule);
			fail();
		} catch (InvalidRuleException ire) {}
		
		rule = "atom1(?x) ->";

		try {
			assertEquals("atom1(?x)", Helpers.tokenizeRule(rule).getLeft().get(0));
			assertEquals(Helpers.tokenizeRule(rule).getRight().size(), 0);
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}
		
		rule = "atom1(?x) ->,,";

		try {
			assertEquals("atom1(?x)", Helpers.tokenizeRule(rule).getLeft().get(0));
			assertEquals(Helpers.tokenizeRule(rule).getRight().size(), 0);
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}

		try {
			rule = "-> atom1";
			Helpers.tokenizeRule(rule);
			fail();
		} catch (InvalidRuleException ire) {}
		
		rule = "-> atom1(?x)";

		try {
			assertEquals(Helpers.tokenizeRule(rule).getLeft().size(), 0);
			assertEquals("atom1(?x)", Helpers.tokenizeRule(rule).getRight().get(0));
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}

		rule = ",,->atom1(?x)";

		try {
			assertEquals(Helpers.tokenizeRule(rule).getLeft().size(), 0);
			assertEquals("atom1(?x)", Helpers.tokenizeRule(rule).getRight().get(0));
		} catch (InvalidRuleException e) {
			fail(e.getMessage());
		}

		try {
			rule = "atom1(?x) -> atom1(?y, ?z) -> atom1(?y, ?z)";
			Helpers.tokenizeRule(rule);
			fail();
		} catch (InvalidRuleException ire) {}

		
	}
	@Test
	public void reset() {
		
		System.out.println("[UNITTEST] ***** TEST: Server resetting procedure *****");
		
		OroServer server = new OroServer(ORO_TEST_CONF);
		
		try {
			server.serverInitialization(conf);
		} catch (OntologyServerException e1) {
			e1.printStackTrace();
			fail();
		}
		
		try {
			server.reset();
		} catch (OntologyServerException e) {
			e.printStackTrace();
			fail();
		}
		
		//TODO: wrong: some statement should be there (-> commonsense knowledge)
		assertEquals("After a reset, no statements are expected in the model.", 0, server.size());
		
		//TODO: test with several models
	}
	
	@Test
	public void save() {
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		String filename = "./test.owl";
		
		System.out.println("[UNITTEST] ***** TEST: Serializing the ontology to disk *****");
		try {
			oro.save(filename);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		File file = new File(filename);
	    
	    if (!file.exists() || !file.isFile())
	    	fail();
	    
	    //Here we get the actual size
	    if (file.length() == 0)
	    	fail("An empty ontology has been written!");
	    
	    file.delete();
	}
	
	/**
	 * Check that some facts are correctly asserted/infered in the ontology. 
	 */
	@Test
	public void check() {
		
		System.out.println("[UNITTEST] ***** TEST: Check facts in the ontology *****");
		
		long startTime = System.currentTimeMillis();
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		
		BaseModule oro = new BaseModule(onto);
		
		System.out.println("[UNITTEST] Ontology loaded in roughly "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		/****************
		 *  First check *
		 ****************/
		
		try {
			Statement s = onto.createStatement("gorilla rdf:type Monkey");
			assertTrue("The fact that gorillas are monkey IS an asserted fact.", onto.check(s));
			s = onto.createStatement("gorilla rdf:type Animal");
			assertTrue("The fact that gorillas are monkey should be inferred.", onto.check(s));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		/****************
		 *  Second check *
		 ****************/
		
		try {
			assertTrue("The fact that apples are plants should be infered!", onto.check(onto.createStatement("apple rdf:type Plant")));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		/****************
		 *  Third check *
		 ****************/
		
		try {
			assertFalse("The fact that gorillas are plants is false!", onto.check(onto.createStatement("gorilla rdf:type Plant")));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			assertFalse("what do you think!! Superman is not a coward!", onto.check(onto.createStatement("superman isA coward")));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	
	/**
	 * Performs a simple query on the ontology to check OWL loading and SPARQL query engine both work.
	 * The query should return the list of instances present in the ontology. 
	 */
	@Test
	public void query() {
		
		System.out.println("[UNITTEST] ***** TEST: Query of the test ontology *****");
	
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);

		
		/****************
		 *  First query *
		 ****************/
		long intermediateTime = System.currentTimeMillis();
		
		Set<RDFNode> result = null;
		try {
			result = onto.query("instances",
							"SELECT ?instances \n" +
							"WHERE { \n" +
							"?instances rdf:type oro:Monkey}\n");
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
	
		
		
		System.out.println("[UNITTEST] First query executed in roughly "+ (System.currentTimeMillis() - intermediateTime) + "ms.");
	

		assertEquals("Two monkeys should be returned", 2, result.size());
		
		
		/*****************
		 *  Second query *
		 *****************/
		
		long intermediateTime2 = System.currentTimeMillis();
		
		//Second test in XML mode
		/** TODO: REIMPLEMENT THAT as soon as the corresponding method is back
		String xmlResult =	oro.queryAsXML(
						"SELECT ?instances \n" +
						"WHERE { \n" +
						"?instances rdf:type oro:Animal}\n");
		assertNotNull("query didn't answered anything!",xmlResult);
		
		System.out.println("[UNITTEST] Second query executed in roughly "+ (System.currentTimeMillis() - intermediateTime2) + "ms.");
		
		int next = 0; int count = -2;
		while (next != -1){
			next = xmlResult.indexOf("instances", next + 1);
			count++;
		}
		assertEquals("Three instances of Animal should be returned.", 3, count);
		*/
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test retrieve in the ontology details about a resource. Result should be similar to Protege's [class|property|individual usage panel.

	 * @throws IllegalStatementException 
	 */
	@Test
	public void getInfos() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Informations retrieval on a resource *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
				
		Set<String> infos = null;

		//Check the behaviour of the method in case of inexistant resource
		try {
			infos = oro.getInfos("oro:i_d_ont_exist");
			fail("A NotFoundException should have been triggered!");
		} catch (NotFoundException e) {
		}
		
		long startTime = System.currentTimeMillis();
		
		try {
			infos = oro.getInfos("oro:baboon");
		}
		catch (NotFoundException nfe) {
			fail();
		}
		
		System.out.println("[UNITTEST] Information retrieval executed in roughly "+ (System.currentTimeMillis() - startTime) + "ms.");

		assertNotNull("getInfos didn't answered anything!",infos);
		assertTrue("getInfos method should tell that baboon is an instance of Animal.", infos.contains("baboon type Animal"));
		assertTrue("getInfos method should tell that baboon has a data property set to \"true\".", infos.contains("baboon isFemale true"));
	
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test is exactly the same as {@link #testGetInfos()} except we don't specify any namespaces. It should use the default one.

	 * @throws IllegalStatementException 
	 */
	@Test
	public void getInfosDefaultNs() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Informations retrieval on a resource using default namespace *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
				
		Set<String> infos = null;

		//Check the behaviour of the method in case of inexistant resource
		try {
			infos = oro.getInfos("i_d_ont_exist");
			fail("A NotFoundException should have been triggered!");
		} catch (NotFoundException e) {
		}
		
		long startTime = System.currentTimeMillis();
		
		try {
			infos = oro.getInfos("baboon");
		} catch (NotFoundException nfe)
		{
			fail();
		}
		
		System.out.println("[UNITTEST] Information retrieval executed in roughly "+ (System.currentTimeMillis() - startTime) + "ms.");

		assertNotNull("getInfosDefaultNs didn't answered anything!",infos);
		//remove this test which depends on the type of reasonner
		//assertEquals("getInfosDefaultNs method should return 10 statements about baboon.", 11, infos.size());
		assertTrue("getInfosDefaultNs method should tell that baboon is an instance of Animal.", infos.contains("baboon type Animal"));
		assertTrue("getInfosDefaultNs method should tell that baboon has a data property set to \"true\".", infos.contains("baboon isFemale true"));
			
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test add a new statement to the ontology (a new instance of the class Class1 and then query the ontology to check the individual was successfully added, with the right namespace.
	 */
	@Test
	public void addStmnt() {
		
		System.out.println("[UNITTEST] ***** TEST: Insertion of a new statement in the ontology *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		long startTime = System.currentTimeMillis();
		
		try {
			onto.add(onto.createStatement("oro:horse rdf:type oro:Animal"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		
		Set<String> stmts = new HashSet<String>();
		stmts.add("fish rdf:type Animal");
		stmts.add("sparrow rdf:type Bird");
		stmts.add("Bird rdfs:subClassOf Animal");
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements!");
			e.printStackTrace();
		}
		
		long intermediateTime = System.currentTimeMillis();
		
		//Note: there are easier ways to query to ontology for simple cases (see find() for instance). But it comes later in the unit tests.
		Set<RDFNode> result = null;
		try {
			result = onto.query("instances",
					"SELECT ?instances \n" +
					"WHERE { \n" +
					"?instances rdf:type oro:Animal}\n");
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
		assertFalse("query didn't answer anything!",result.isEmpty());

		System.out.println("[UNITTEST] One statement added in roughly "+ ((intermediateTime - startTime) / 4) + "ms, and ontology queried in roughly "+(System.currentTimeMillis()-intermediateTime)+"ms.");
		
		int count = result.size();
		
		assertEquals("Six individuals, instances of Animal, should be returned.", 6, count);
		
	
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test add a new statement with a literal as object to the ontology (a new instance of the class Class1 and then query the ontology to check the individual was successfully added, with the right namespace.
	 */
	@Test
	public void addStmntWithLiteral() {

		System.out.println("[UNITTEST] ***** TEST: Insertion of statements with literals *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		Set<String> result = null;
		
		//Add a statement
		try {
			onto.add(onto.createStatement("oro:fish oro:isFemale \"true\"^^xsd:boolean"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		//Check it was added.
		
		try {
			result = oro.query("i",
					"SELECT ?i \n" +
					"WHERE { \n" +
					"?i rdf:type owl:Thing .\n" +
					"?i oro:isFemale true}\n");
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		} catch (OntologyServerException e) {
			e.printStackTrace();
			fail();
		}
		assertFalse("query didn't answered anything!",result.isEmpty());
		
		assertTrue("The value of isFemale for baboon and fish is now true. We should get two positives.", result.contains("baboon") && result.contains("fish") && result.size() == 2);
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test tests the "safe" add that avoid leading the ontology in a 
	 * inconsistent state.
	 */
	@Test
	public void safeAddStmnt() {
		
		System.out.println("[UNITTEST] ***** TEST: Safe insertion of statements *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		
		try {
			assertTrue("A horse is innocent!", onto.add(onto.createStatement("horse rdf:type oro:Animal"), MemoryProfile.DEFAULT, true));
			assertFalse("A horse is not a plant!", onto.add(onto.createStatement("horse rdf:type Plant"), MemoryProfile.DEFAULT, true));
			assertTrue("The statement should be present", onto.check(onto.createStatement("horse rdf:type Animal")));
			assertFalse("The statement shouldn't be inserted", onto.check(onto.createStatement("horse rdf:type Plant")));
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test add statements to the ontology with different memory models and checks that everything behave as expected (for instance, short term statements must be removed after a while). 
	 * @throws InterruptedException 
	 */
	@Test
	public void addStmntInMemory() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Insertion of statements with different memory profile *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		MemoryProfile.TimeBase = 100; //a second now lasts 100ms :-) we accelerate 10 times the behaviour of the memory container.
	
		//First step -> call to the memory manager.
		onto.step();
		
		try {
			onto.add(onto.createStatement("snail rdf:type Animal"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}

		try {
			onto.add(onto.createStatement("snail eats grass"), MemoryProfile.EPISODIC, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		Set<String> stmts = new HashSet<String>();

		stmts.add("superman rdf:type Animal");
		try {
			oro.add(stmts, "SHORTTERM");
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements!");
			e.printStackTrace();
		}
		
		//call to the memory manager
		onto.step();

		int nbSeconds = 1;
		Date now = new Date();
		
		Set<Statement> rs_stmts = new HashSet<Statement>();
		Set<Statement> rs_short_term = new HashSet<Statement>();
		
	
		Property p_createdOn = onto.getModel().createProperty(Namespaces.addDefault("stmtCreatedOn"));
		Property p_memoryProfile = onto.getModel().createProperty(Namespaces.addDefault("stmtMemoryProfile"));

		RSIterator rsIter = onto.getModel().listReifiedStatements() ;
        while(rsIter.hasNext())
        {
            ReifiedStatement rs = rsIter.nextRS() ;
            
            try {

            	String lexicalDate = rs.getRequiredProperty(p_createdOn).getLiteral().getLexicalForm();
                
                long elapsedTime = (now.getTime() - Helpers.getDateFromXSD(lexicalDate).getTime());
                

                
                if (elapsedTime < nbSeconds * 1000)
                {
                    System.out.println(" * Recent stmt found. Elapsed time in ms -> " + elapsedTime);
                	rs_stmts.add(rs.getStatement());
                }
            	
            }
            catch (PropertyNotFoundException pnfe)
            {
            //the reified statement	has no createdOn property. We skip it.
            } catch (ParseException e) {
				fail("The date as created by the add() method could not be parsed!");
				e.printStackTrace();
			}

            try {

            	String lexicalMemProfile = rs.getRequiredProperty(p_memoryProfile).getString();
                
                 if (lexicalMemProfile.equals(MemoryProfile.SHORTTERM.toString())){
                	 System.out.println(" * Short term stmt found.");
                	 rs_short_term.add(rs.getStatement());
                 }
            	
            }
            catch (PropertyNotFoundException pnfe)
            {
            //the reified statement	has no stmtMemoryProfile property. We skip it.
            }
        }
        	
		assertEquals("Two recently added statements should be returned.", 2, rs_stmts.size());
		
		assertEquals("One short term statements should be returned.", 1, rs_short_term.size());
	
		try {
			onto.save("./before_cleaning.owl");
		} catch (OntologyServerException e) {
			fail();
		}
		
		
		System.out.print(" * Waiting a bit (" + (MemoryProfile.SHORTTERM.duration() + 500) + "ms)...");
		Thread.sleep(MemoryProfile.SHORTTERM.duration() + 500);
		
		//call to the memory manager. Here, older statements should be cleaned.
		onto.step();
		
		try {
			onto.save("./after_cleaning.owl");
		} catch (OntologyServerException e) {
			fail();
		}

		rsIter = onto.getModel().listReifiedStatements() ;
		int nb = rsIter.toSet().size();
		
		assertEquals("Only one reified statement should now remain.", 1, nb);
				
	
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks that statements that are addable can be removed as well.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void clear() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Remove 2 *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
	
		Set<String> stmts = new HashSet<String>();
		
		stmts.add("gorilla rdfs:label 'KingKong'");
		stmts.add("gorilla rdfs:comment \"KingKong is nice\"");
		
		try {

			int nbStmt = onto.getModel().listStatements().toList().size();
			
			oro.add(stmts);
			
			try {
				onto.save("./after_add.owl");
			} catch (OntologyServerException e) {
				fail();
			}

			try {
				oro.clear(stmts);		

				onto.save("./after_remove.owl");
			} catch (OntologyServerException e) {
				fail();
			}
			
			assertEquals("The number of statements after removal of new statements should be the same as before.", nbStmt, onto.getModel().listStatements().toList().size());
			
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * Tests ontology consistency checking.
	 */
	@Test
	public void consistency() {
		
		System.out.println("[UNITTEST] ***** TEST: Ontology consistency checking *****");
		

		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);

		assertTrue("Initial ontology should be detected as consistent!", oro.checkConsistency());
				
		try {
			Statement s = onto.createStatement("cow rdf:type Plant");
			onto.add(s, MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements in testConsistency!");
		}
				
		//This time, the consistency check should fail since we assert that a cow is both an animal and a plant which contradict the assert axiom (Animal disjointWith Plant)
		assertFalse("Ontology should be detected as inconsistent! Cows are not plants!", oro.checkConsistency());

		Set<String> stmtsToRemove = new HashSet<String>();
		
		try {			
			stmtsToRemove.add("cow rdf:type Plant");
			oro.clear(stmtsToRemove);
			assertTrue(oro.checkConsistency());
		} catch (IllegalStatementException e) {
			fail();
		} catch (OntologyServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			onto.add(onto.createStatement("cow climbsOn banana_tree"), MemoryProfile.DEFAULT, false);
			assertFalse("Ontology should be detected as inconsistent! Cows can not climb on banana trees because they are explicitely not monkeys!", oro.checkConsistency());
			
			stmtsToRemove.clear();
			stmtsToRemove.add("cow climbsOn banana_tree");
			oro.clear(stmtsToRemove);
			assertTrue("Ontology should now be back to consistency", oro.checkConsistency());
			
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements in testConsistency!");
		} catch (OntologyServerException e) {
			fail();
		}
		
		
		
		/* Test with a set of updates             */
		Set<String> updatedStmts = new HashSet<String>();
		
		
		try {
			updatedStmts.add("gorilla age 12");
			updatedStmts.add("gorilla weight 100.2");

			oro.clear(updatedStmts);
			assertTrue("The 'clear' was not successful: a functional property has now 2 values.", oro.checkConsistency());
			
			updatedStmts.clear();
			updatedStmts.add("gorilla age 21");
			updatedStmts.add("gorilla weight 99.5");
			
			oro.add(updatedStmts);
			assertTrue("'add' shouldn't cause any inconsistency, since previous statements have been cleared.", oro.checkConsistency());
		} catch (IllegalStatementException e) {
			fail();
		} catch (OntologyServerException e) {
			fail();
		}		
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks the statement update mechanism.
	 */
	@Test
	public void update() {
		
		System.out.println("[UNITTEST] ***** TEST: Update statement *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
	
		Set<String> stmts = new HashSet<String>();
		
		stmts.add("gorilla rdfs:label 'KingKong'"); //rdfs:label is not a functional property. The value shouldn't be replaced.
		
		//Other asserted statement in testsuite.oro.owl
		//gorilla isFemale false
		//gorilla weight 100.2
		//gorilla age 12
	
	
		Set<String> partial_statements = new HashSet<String>();
				
		try {
			oro.add(stmts);			
			
			try {
				
				/******************************************/
				Set<String> updatedStmts = new HashSet<String>();
				
				updatedStmts.add("gorilla age 21");
				updatedStmts.add("gorilla weight 99.5");
					
				oro.update(updatedStmts);
				assertTrue("The update was not successful: a functional property has now 2 values.", oro.checkConsistency());
				
				partial_statements.add("gorilla age ?a");
				partial_statements.add("gorilla weight ?w");
				
				Set<String> res = null;
				try {
					 res = oro.find("a", partial_statements);
				} catch (InconsistentOntologyException e) {
					fail("The update was not successful: a functional property has now 2 values.");
				}
				assertTrue(res.size() == 1);
				assertTrue(Helpers.pickRandom(res).equalsIgnoreCase("21"));
				
				res = oro.find("w", partial_statements);
				assertTrue(res.size() == 1);
				assertTrue(Helpers.pickRandom(res).equalsIgnoreCase("99.5"));
				
				partial_statements.clear();
				
				/******************************************/
				/* Test2 with a set of updates             */
				updatedStmts.clear();
				updatedStmts.add("gorilla age 99");
				updatedStmts.add("gorilla rdfs:label \"Lord of the rings\""); //non-functional predicate
				updatedStmts.add("gorilla likeIcecream true"); //non-existent predicate
				
				oro.update(updatedStmts);
				
				partial_statements.add("gorilla age ?a");
				partial_statements.add("gorilla rdfs:label ?l");
				partial_statements.add("gorilla likeIcecream ?c");

				res = oro.find("l", partial_statements);
				assertEquals(2, res.size());

				res = oro.find("a", partial_statements);
				assertEquals(1, res.size());
				assertTrue(Helpers.pickRandom(res).equalsIgnoreCase("99"));
				
				res = oro.find("c", partial_statements);
				assertTrue(res.size() == 1);
				assertTrue(Helpers.pickRandom(res).equalsIgnoreCase("true"));
				partial_statements.clear();
				
			} catch (OntologyServerException e) {
				e.printStackTrace();
				fail();
			} catch (laas.openrobots.ontology.exceptions.InconsistentOntologyException e) {
				e.printStackTrace();
				fail();
			}
			
		} catch (IllegalStatementException e) {
			e.printStackTrace();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	
	/**
	 * This test checks that concept can be retrieved by their labels. 
	 * @throws InterruptedException 
	 */
	@Test
	public void lookup() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Label lookup *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
	
		Set<String> stmts = new HashSet<String>();
		stmts.add("gorilla rdfs:label \"king kong\"");
		stmts.add("iddebile rdf:type Human");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}

		assertTrue("No label \"princess Daisy\" exists in the ontology.", onto.lookup("princess Daisy").isEmpty());
		
		assertEquals("The \"baboon\" instance should be retrieved.", "baboon", oro.lookup("BabouIn").iterator().next().get(0));
		assertEquals("The \"baboon\" type should be INSTANCE.", ResourceType.INSTANCE.toString(), oro.lookup("BabouIn").iterator().next().get(1));
		assertEquals("The \"baboon\" instance should be retieved.", "baboon", oro.lookup("Baboon monkey").iterator().next().get(0));
		assertEquals("The \"gorilla\" instance should be retrieved.", "gorilla", oro.lookup("king kong").iterator().next().get(0));
		assertEquals("The \"iddebile\" instance should be retrieved.", "iddebile", oro.lookup("iddebile").iterator().next().get(0));
		
		assertEquals("\"Monkey\" should be a CLASS.", ResourceType.CLASS.toString(), oro.lookup("Monkey").iterator().next().get(1));
		
		
		stmts.clear();
		stmts.add("KingKong rdfs:subClassOf Monkey");
		stmts.add("KingKong rdfs:label \"king kong\"");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		Set<List<String>> res = oro.lookup("king kong");
		assertEquals("The \"king kong\" label should now match two concepts (instance of gorilla, and class KingKong).", 2, res.size());
		
		
		Set<String> clearPattern = new HashSet<String>();
		clearPattern.add("gorilla ?a ?b");
		
		try {
			oro.clear(stmts);
			oro.clear(clearPattern);
		} catch (IllegalStatementException e1) {
			e1.printStackTrace();
		} catch (OntologyServerException e) {
			fail();
		}
		
		assertTrue("No label \"king kong\" should exist anymore in the ontology.", oro.lookup("king kong").isEmpty());
	
		//Test lookup with a type
		
		stmts.clear();
		stmts.add("rintintin rdf:type Dog");
		stmts.add("lassie rdf:type Cat");
		stmts.add("rintintin sees lassie");
		stmts.add("rintintin hasAge 134");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		assertTrue(onto.lookup("rintintin", ResourceType.INSTANCE).contains("rintintin"));
		assertFalse(onto.lookup("rintintin", ResourceType.CLASS).contains("rintintin"));
		assertFalse(onto.lookup("rintintin", ResourceType.OBJECT_PROPERTY).contains("rintintin"));
		assertFalse(onto.lookup("rintintin", ResourceType.DATATYPE_PROPERTY).contains("rintintin"));
		assertTrue(onto.lookup("dog", ResourceType.CLASS).contains("Dog"));
		assertFalse(onto.lookup("dog", ResourceType.INSTANCE).contains("Dog"));
		assertTrue(onto.lookup("sees", ResourceType.OBJECT_PROPERTY).contains("sees"));
		assertFalse(onto.lookup("sees", ResourceType.DATATYPE_PROPERTY).contains("sees"));
		assertFalse(onto.lookup("sees", ResourceType.DATATYPE_PROPERTY).contains("hasAge"));
		assertTrue(onto.lookup("hasage", ResourceType.DATATYPE_PROPERTY).contains("hasAge"));
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks that sub- and superclasses are correctly inferred. 
	 * @throws InterruptedException 
	 */
	@Test
	public void subSuperClasses() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Sub- and superclasses *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
	
		Set<String> stmts = new HashSet<String>();
		stmts.add("Insect rdfs:subClassOf Animal");
		stmts.add("Ladybird rdfs:subClassOf Insect");
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}

		//for (String k : oro.getInstancesOf("Monkey").keySet()){
		//	System.out.println(k);
		//}
          
		assertEquals("Four subclasses should be returned (MyAnimals, Monkey, Insect and Ladybird).", 4, oro.getSubclassesOf("Animal").size());
		assertEquals("Three direct subclasses should be returned (MyAnimals, Monkey and Insect).", 3, oro.getDirectSubclassesOf("Animal").size());
		
		assertTrue("These superclasses should be returned (Insect, Animal, owl:Thing (and possibly rdfs:Resource, depending on the reasonner).", (3 <= oro.getSuperclassesOf("Ladybird").size()) && (4 >= oro.getSuperclassesOf("Ladybird").size()));
		assertEquals("One direct superclass should be returned (Insect).", 1, oro.getDirectSuperclassesOf("Ladybird").size());
		
		assertEquals("Three instances of animal should be returned (cow, baboon and gorilla).", 3, oro.getInstancesOf("Animal").size());
		assertEquals("One direct instance of plant should be returned (banana_tree, grass, apple).", 3, oro.getDirectInstancesOf("Plant").size());
		
		assertEquals("Two classes (monkey and MyAnimals) should be returned.", 2, oro.getDirectClassesOf("baboon").size());
		
		assertEquals("Eight subclasses of A should be returned.", 8, oro.getSubclassesOf("A").size());
	
		try {
			oro.getDirectSuperclassesOf("baboon");
			fail("Baboon is not a class!");
		}
		catch (NotFoundException nfe)
		{}
		
		try {
			oro.getDirectSubclassesOf("baboon");
			fail("Baboon is not a class!");
		}
		catch (NotFoundException nfe)
		{}
	}
	
	/**
	 * This test checks the behaviour of the server when the class of a 
	 * resource is requested. 
	 * @throws InterruptedException 
	 */
	@Test
	public void classOf() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: instance of/class of... *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
         
		try {
			oro.getClassesOf("Monkey");
			fail("I shouldn't be able to return the class of a class.");
		}
		catch (NotFoundException nfe) {}
		
		//We call that to be sure that no nasty lock are being held.
		oro.checkConsistency();
		
		//is that the correct behaviour?? returning an empty set?
		assertTrue(oro.getClassesOf("owl:Class").isEmpty());
		
		try {
			oro.getInstancesOf("baboon");
			fail("I shouldn't be able to return the instances of an instance.");
		}
		catch (NotFoundException nfe) {}
	
	}
	
	/**
	 * This test try to create statements with various types of literals.
	 */
	@Test
	public void literals() {

		System.out.println("[UNITTEST] ***** TEST: Statements with literals *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		Statement tmp;
		
		//First, create statements with valid literals
		try {
			tmp = oro.createStatement("oro:fish oro:isFemale true");
			assertTrue("The datatype has not been recognized!", tmp.getBoolean());
			
			tmp = oro.createStatement("oro:fish oro:isFemale true^^xsd:boolean");
			assertTrue("The datatype has not been recognized!", tmp.getBoolean());
			
			tmp = oro.createStatement("oro:fish oro:isFemale \"true\"^^xsd:boolean");
			assertTrue("The datatype has not been recognized!", tmp.getBoolean());
			
			tmp = oro.createStatement("oro:fish oro:isFemale 'true'^^xsd:boolean");
			assertTrue("The datatype has not been recognized!", tmp.getBoolean());
			
			tmp = oro.createStatement("oro:fish oro:isFemale 1^^xsd:boolean");
			assertTrue("The datatype has not been recognized!", tmp.getBoolean());
			
			tmp = oro.createStatement("oro:fish oro:age 150");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Integer.class);
			
			tmp = oro.createStatement("oro:fish oro:age \"150\"^^xsd:int");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Integer.class);
			
			tmp = oro.createStatement("oro:fish oro:age 150^^xsd:int");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Integer.class);
			
			tmp = oro.createStatement("oro:fish oro:age \"150\"^^<http://www.w3.org/2001/XMLSchema#int>");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Integer.class);
			
			tmp = oro.createStatement("oro:fish oro:age 150^^xsd:integer");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == java.math.BigInteger.class);
			
			tmp = oro.createStatement("oro:fish oro:age 150.0");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Double.class);
						
			tmp = oro.createStatement("oro:fish oro:age \"150\"^^xsd:double");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Double.class);
			
			tmp = oro.createStatement("oro:fish oro:age 150^^xsd:double");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Double.class);
			
			tmp = oro.createStatement("oro:fish oro:age \"150\"^^xsd:float");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == Float.class);
			
			tmp = oro.createStatement("oro:fish oro:name Dudule");
			assertTrue("Dudule should'nt be recognized as a string here, but as an instance.", tmp.getObject().isResource());
			
			tmp = oro.createStatement("oro:fish oro:name 'Dudule'");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == String.class);
			
			tmp = oro.createStatement("oro:fish oro:name \"Dudule\"");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == String.class);
			
			tmp = oro.createStatement("oro:fish oro:name Dudule^^xsd:string");
			assertTrue("The datatype has not been recognized!", tmp.getLiteral().getDatatype().getJavaClass() == String.class);
			
		} catch (IllegalStatementException e) {
			fail("Error while creating statements with valid literals!");
		}
		
		//Then, create statements with invalid literals
		try {
			tmp = oro.createStatement("oro:fish oro:isFemale true^^xsd:int");
			fail("Statements with invalid literals have been created!");
		} catch (IllegalStatementException e) {
		}
		
		try {			
			tmp = oro.createStatement("oro:fish oro:age 150.0^^xsd:int");
			fail("Statements with invalid literals have been created!");
		} catch (IllegalStatementException e) {
		}

		
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
	/**
	 * This test checks that the Remove and Clear methods work as expected.
	 */
	@Test
	public void advancedRemoveAndClear() {
		
		System.out.println("[UNITTEST] ***** TEST: Remove & Clear *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		String who_is_an_animal = "SELECT ?instances \n" +
				"WHERE { \n" +
				"?instances rdf:type oro:Animal}\n";
		
		
		Set<String> stmts = new HashSet<String>();
		stmts.add("fish rdf:type Animal");
		stmts.add("sparrow rdf:type Bird");
		stmts.add("Bird rdfs:subClassOf Animal");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements!");
			e.printStackTrace();
		}
				
		Set<RDFNode> result = null;
		try {
			result = onto.query("instances", who_is_an_animal);
		} catch (InvalidQueryException e5) {
			e5.printStackTrace();
			fail();
		}
		
		assertEquals("Five individuals, instances of Animal, should be returned.", 5, result.size());
		
		//Let's remove a statement: Birds are no more animals, and thus, the sparrow shouldn't be counted as an animal.
		try {
			Set<String> stmtsToRemove = new HashSet<String>();
			stmtsToRemove.add("Bird rdfs:subClassOf Animal");
			oro.clear(stmtsToRemove);
		} catch (IllegalStatementException e) {
			fail("Error while removing a set of statements!");
			e.printStackTrace();
		} catch (OntologyServerException e) {
			fail();
		}
		
		try {
			result = onto.query("instances",who_is_an_animal);
		} catch (InvalidQueryException e4) {
			e4.printStackTrace();
			fail();
		}
		
		assertEquals("Four individuals, instances of Animal, should be returned.", 4, result.size());
		
		//Let's add 2 more statements
		stmts.clear();
		stmts.add("sparrow eats seeds");
		stmts.add("sparrow eats butter");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements!");
			e.printStackTrace();
		}
		
		try {
			result =	onto.query("instances", who_is_an_animal);
		} catch (InvalidQueryException e3) {
			e3.printStackTrace();
			fail();
		}
		
		//Since sparrows are said to eat something, they are animals.
		assertEquals("Five individuals, instances of Animal, should be returned.", 5, result.size());

		String what_does_the_sparrow_eat = "SELECT ?food \n" +
		"WHERE { \n" +
		"oro:sparrow oro:eats ?food}\n";

		try {
			result = onto.query("food", what_does_the_sparrow_eat);
		} catch (InvalidQueryException e2) {
			e2.printStackTrace();
			fail();
		}		
		
		assertEquals("Two objects should be returned.", 2, result.size());
		
		//Let's clear infos about what the sparrow eats.
		Set<String> clearPattern = new HashSet<String>();
		clearPattern.add("sparrow eats ?food");
		
		try {
			oro.clear(clearPattern);
		} catch (IllegalStatementException e) {
			fail("Error while clearing statements!");
			e.printStackTrace();
		} catch (OntologyServerException e) {
				fail();
		}
		
		try {
			result = onto.query("food", what_does_the_sparrow_eat);
		} catch (InvalidQueryException e1) {
			e1.printStackTrace();
			fail();
		}		
	
		assertEquals("Nothing should be returned.", 0, result.size());
		
		try {
			result =	onto.query("instances", who_is_an_animal);
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}

		//There's no more assertions permitting to say that sparrows are animals, so they shouldn't appear.
		assertEquals("Four individuals, instances of Animal, should be returned.", 4, result.size());
	
	
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	

	
	@Test
	public void stmtConsistency() {
		
		System.out.println("[UNITTEST] ***** TEST: Checking consistency of a external set of statements *****");
		

		IOntologyBackend onto = new OpenRobotsOntology(conf);
		
		Statement s1 = null, s2 = null;
		Set<Statement> consistentOnes = new HashSet<Statement>();
		Set<Statement> inconsistentOnes = new HashSet<Statement>();
		
		try {
			s1 = onto.createStatement("cow drinks water");
			consistentOnes.add(s1);
			
			s2 = onto.createStatement("cow rdf:type Plant");
			inconsistentOnes.add(s2);
		} catch (IllegalStatementException e) {
			fail("Error while creating a set of statements in testConsistency!");
		}
		
		assertTrue(onto.checkConsistency(consistentOnes));
		assertFalse(onto.checkConsistency(inconsistentOnes));

		assertFalse("Statements should not be added to the model!!", onto.check(s1));
		assertFalse("Statements should not be added to the model!!", onto.check(s2));
						
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test try to match a given set of statements against the ontology, and to get back the class of an object.
	 */
	@Test
	public void find() {

		System.out.println("[UNITTEST] ***** TEST: Exact statements matching *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		Set<String> matchingResources = null;
				
		
		System.out.println("[UNITTEST] First part: only the resource we are looking for is unknown.");
		
		Set<String> empty_set = new HashSet<String>();
		Set<String> partial_statements = new HashSet<String>();
		
		try {
			oro.find("", partial_statements);
			fail("find() without specifying a variable should throw an exception");
		} catch (OntologyServerException e) {
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			matchingResources = oro.find("?mysterious", empty_set);
			assertTrue("Nothing should be returned, but it shouldn't fail", matchingResources.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
				

		partial_statements.add("?mysterious oro:eats oro:banana_tree");
		partial_statements.add("?mysterious oro:isFemale true^^xsd:boolean");  //Attention: "?mysterious oro:isFemale true" is valid, but "?mysterious oro:isFemale true^^xsd:boolean" is not!
		
		try {
			matchingResources = oro.find("?mysterious", partial_statements, empty_set);
			assertTrue("Nothing should be returned, but it shouldn't fail", matchingResources.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		try {
			matchingResources = oro.find("?mysterious", partial_statements);
			
			matchingResources = oro.find("mysterious", partial_statements);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		assertNotNull("find() didn't answered anything!",matchingResources);
		
		for ( String resource:matchingResources )
		{
			assertTrue("Only baboon should be returned.", resource.contains("baboon"));
		}		
		//TODO Add a test which returns the class of the resource.
		
		System.out.println("[UNITTEST] Second part: more complex resource description.");
		
		partial_statements.clear();
		matchingResources.clear();
		Set<String> filters = new HashSet<String>();

		partial_statements.add("?mysterious rdf:type oro:Monkey");
		partial_statements.add("?mysterious oro:weight ?value");

		filters.add("?value >= 48");
		
		try {
			matchingResources = oro.find("mysterious", partial_statements, filters);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertEquals("find() should answer 2 resources (baboon and gorilla)", 2, matchingResources.size());
		
		filters.add("?value < 75.8");
		
		try {
			matchingResources = oro.find("mysterious", partial_statements, filters);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertEquals("find() should now answer only 1 resources (baboon)", 1, matchingResources.size());
		
		
		// Checking that we can retrieve string literals
		partial_statements.add("baboon rdfs:label ?label");
		
		try {
			matchingResources = oro.find("label", partial_statements);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertEquals("find() should answer 2 resources (English + French labels)", 2, matchingResources.size());
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * Same as testFind(), but inside the AlteriteModule
	 */
	@Test
	public void findForAgent() {

		System.out.println("[UNITTEST] ***** TEST: findForAgent *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = null;
		
		try {
			oro.add(oro.createStatement("Agent rdfs:subClassOf owl:Thing"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			e1.printStackTrace();
			fail();
		}
		
		try {
			alterite = new AlteriteModule(oro, conf);
		} catch (InvalidModelException e) {
			fail();
		}
		
		try {
			oro.add(oro.createStatement("toto rdf:type Agent"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			e1.printStackTrace();
			fail();
		}
		
		Set<String> matchingResources = null;
				
		
		System.out.println("[UNITTEST] First part: only the resource we are looking for is unknown.");
		
		Set<String> partial_statements = new HashSet<String>();
		
		try {
			alterite.findForAgent("toto", "", partial_statements);
			fail("findForAgent() without specifying a variable should throw an exception");
		} catch (OntologyServerException e) {
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			matchingResources = alterite.findForAgent("toto", "?mysterious", partial_statements);
			assertTrue("Nothing should be returned, but it shouldn't fail", matchingResources.isEmpty());
		} catch (Exception e) {
			fail();
		}
		
		Set<String> stmts = new HashSet<String>();
		stmts.add("gorilla eats banana_tree");
		stmts.add("gorilla isFemale false");
		stmts.add("elephant weight 1547.32");
		
		try {
			alterite.addForAgent("toto", stmts);
		} catch (IllegalStatementException e1) {
			e1.printStackTrace();
			fail();
		} catch (AgentNotFoundException e) {
			fail();
		}

		partial_statements.add("?mysterious eats banana_tree");
		partial_statements.add("?mysterious isFemale false");  //Attention: "?mysterious oro:isFemale true" is valid, but "?mysterious oro:isFemale true^^xsd:boolean" is not!

		try {
			matchingResources = alterite.findForAgent("toto", "?mysterious", partial_statements);
			
			matchingResources = alterite.findForAgent("toto", "mysterious", partial_statements);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		assertNotNull("findForAgent() didn't answered anything!",matchingResources);
		
		for ( String resource:matchingResources )
		{
			assertTrue("Only gorilla should be returned.", resource.contains("gorilla"));
		}		
		
		System.out.println("[UNITTEST] Second part: more complex resource description.");
		
		partial_statements.clear();
		matchingResources.clear();
		Set<String> filters = new HashSet<String>();

		partial_statements.add("?mysterious oro:weight ?value");

		filters.add("?value >= 1000");
		
		try {
			matchingResources = alterite.findForAgent("toto", "mysterious", partial_statements, filters);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertFalse("findForAgent() didn't answered anything!",matchingResources.isEmpty());
		
		for ( String resource:matchingResources )
		{
			assertTrue("Only elephant should be returned.", resource.contains("elephant"));
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests the find() method when it returns several variables.
	 */
	@Test
	public void findMultipleVariables() {

		System.out.println("[UNITTEST] ***** TEST: Find several variables *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		Map<String, Set<String>> matchingResources = null;
		
		Set<String> variables = new HashSet<String>();
		variables.add("animal");
		variables.add("food");
		
		Set<String> partial_statements = new HashSet<String>();
		partial_statements.add("?animal eats ?food");

		try {
			matchingResources = oro.find(variables, partial_statements);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}	
		
		assertNotNull("find() didn't answered anything!",matchingResources);
		
		
		//TODO Add a tests!

		System.out.println("[UNITTEST] ***** Test successful *****");
	}


	/***********************************************************************
	 *                       ADVANCED TESTS                                *
	 ***********************************************************************/

	/**
	 * This test add several new statements and test basic inference mechanisms.
	 */
	@Test
	public void inference() {

		System.out.println("[UNITTEST] ***** TEST: Inference testing *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		//Add a statement
		try {
			onto.add(onto.createStatement("sheep eats grass"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		//Check it was added.

		Set<String> matchingResources = null;
		Set<String> partialStatements = new HashSet<String>();

		partialStatements.add("?animals rdf:type Animal");
		
		try {
			matchingResources = oro.find("animals", partialStatements);	
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}		
		
		
		Iterator<String> res = matchingResources.iterator();
		while(res.hasNext())
		{
			String currentRes = res.next();
			System.out.println(currentRes);
		}
		assertEquals("Four animals should be returned.", 4, matchingResources.size());
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests the similarities function that extracts common features between concepts.
	 */
	@Test
	public void similarities() {

		System.out.println("[UNITTEST] ***** TEST: Similarities test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);
		
		//Add a statement
		try {
			//oro.add(oro.createStatement("sheepy rdf:type Sheep"), MemoryProfile.DEFAULT);
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT, false); //we can infer that sheepy is an animal
			oro.add(oro.createStatement("baboon2 rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("baboon2 age 75"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
				
		Set<String> similarities = null;
		
		//**********************************************************************
		
		//check that we can not compare instances and classes.
		try {
			similarities = categorizationModule.getSimilarities("baboon", "Monkey");
			fail("We shouldn't be allowed to compare instances and classes!");
		} catch (NotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (NotComparableException e) {
		}
		
		//**********************************************************************
		
		System.out.println(" * Similarities between the cow and the sheep");
		try {
			similarities = categorizationModule.getSimilarities("cow", "sheepy");	//like the sheep, the cow eats grass and is an animal.
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}

		assertTrue("The cow and the sheep should be identified as animals that eat grass", similarities.contains("? rdf:type Animal") && similarities.contains("? eats grass") && similarities.size() == 2);
		
		//**********************************************************************
		
		System.out.println(" * Similarities between the cow and the gorilla");
		try {
			similarities = categorizationModule.getSimilarities("cow", "gorilla");	//Both the cow and the gorilla are males of 12.
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
		
		Iterator<String> itSim = similarities.iterator();
		while(itSim.hasNext())
		{
			String currentRes = itSim.next();
			System.out.println(currentRes);
		}
		assertTrue("Both the cow and the gorilla are male animals of 12.", similarities.contains("? rdf:type Animal") && similarities.contains("? age 12") && similarities.contains("? isFemale false") && similarities.size() == 3);
		
		//**********************************************************************
		
		System.out.println("Similarities between the baboon and another baboon");
		try {
			similarities = categorizationModule.getSimilarities("baboon", "baboon2");
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
	
		itSim = similarities.iterator();
		while(itSim.hasNext())
		{
			String currentRes = itSim.next();
			System.out.println(currentRes);
		}
		assertTrue("The 2 baboons should be identified as monkeys", similarities.contains("? rdf:type Monkey") && similarities.size() == 1);
		
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests the differences function that extracts different properties between concepts.
	 */
	@Test
	public void differences() {

		System.out.println("[UNITTEST] ***** TEST: Differences test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);
		
		//Add a statement
		try {
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT, false); //we can infer that sheepy is an animal
			oro.add(oro.createStatement("baboon2 rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("baboon2 rdf:type MyAnimals"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("baboon2 age 75"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
				
		Set<Set<String>> differences = null;
		
		//**********************************************************************
		
		//check that we can not compare instances and classes.
		try {
			differences = categorizationModule.getDifferences("baboon", "Monkey");
			fail("We shouldn't be allowed to compare instances and classes!");
		} catch (NotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (NotComparableException e) {
		}
		
		//**********************************************************************
		
		System.out.println(" * Differences between the cow and the sheep");
		try {
			differences = categorizationModule.getDifferences("cow", "sheepy");	//like the sheep, the cow eats grass and is an animal.
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
		
		for(Set<String> ss : differences)
			System.out.println(ss);

		assertTrue("The cow and the sheep shouldn't have visible differences", differences.isEmpty());
		
		//**********************************************************************
		
		System.out.println(" * Differences between the cow and the gorilla");
		try {
			differences = categorizationModule.getDifferences("cow", "gorilla");
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
		
		for(Set<String> ss : differences)
			System.out.println(ss);

		//TODO: improve error checking...
		assertTrue("Expected differences:[gorilla rdf:type Monkey, cow rdf:type Animal],  [gorilla weight 100.2, cow weight 150.7], [gorilla eats apple, cow eats grass]", differences.size() == 3);
		
		//**********************************************************************
		
		System.out.println(" * Differences between the baboon and another baboon");
		try {
			differences = categorizationModule.getDifferences("baboon", "baboon2");
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
	
		for(Set<String> ss : differences)
			System.out.println(ss);
		
		assertTrue("Expected differences:[baboon2 age 75, baboon age 35]", differences.size() == 1);
		
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests the diff and similar function in complex hierarchies of 
	 * classes.
	 */
	@Test
	public void advancedDiff() {

		System.out.println("[UNITTEST] ***** TEST: Complex hierarchies Diff test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		Set<Set<String>> diff1 = null;
		Set<Set<String>> diff2 = null;
		Set<Set<String>> diff3 = null;
		Set<Set<String>> diff4 = null;
		Set<String> sim1 = null;
		Set<String> sim2 = null;
		Set<String> sim3 = null;
		Set<String> sim4 = null;
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);
		
		long totalTimeDiff = 0;
		long totalTimeSim = 0;
		long startTime = System.currentTimeMillis();
		
		
		try {
			diff1 = categorizationModule.getDifferences("f", "j");
			diff2 = categorizationModule.getDifferences("f", "k");
			diff3 = categorizationModule.getDifferences("f", "e");
			diff4 = categorizationModule.getDifferences("f", "f");
			
			totalTimeDiff = (System.currentTimeMillis()-startTime);
			
			sim1 = categorizationModule.getSimilarities("f", "j");
			sim2 = categorizationModule.getSimilarities("f", "k");
			sim3 = categorizationModule.getSimilarities("f", "e");
			sim4 = categorizationModule.getSimilarities("f", "f");
			
			totalTimeSim = (System.currentTimeMillis()- startTime - totalTimeDiff);
			
		} catch (NotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}
		
		
		//TODO: improve error checking!!
		assertTrue("", diff1.size() == 2);
		assertTrue("", diff2.size() == 2);
		assertTrue("", diff3.size() == 1);
		assertTrue("", diff4.isEmpty());
		
		assertTrue("", sim1.size() == 2 && sim1.contains("? rdf:type B") && sim1.contains("? rdf:type D"));
		assertTrue("", sim2.size() == 2 && sim2.contains("? rdf:type B") && sim2.contains("? rdf:type D"));
		assertTrue("", sim3.size() == 1 && sim3.contains("? rdf:type E"));
		assertTrue("", sim4.size() == 1 && sim4.contains("? rdf:type F"));
		
		System.out.println("[UNITTEST] ***** Average time per differences comparison:" + totalTimeDiff / 4 + "ms");
		System.out.println("[UNITTEST] ***** Average time per similarities comparison:" + totalTimeSim / 4 + "ms");
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests ability for the cognitive kernel to find discriniment features
	 * in a set of concepts.
	 */
	@Test
	public void discriminent() {

		System.out.println("[UNITTEST] ***** TEST: Categorization test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);

		List<Set<Property>> discriminents = null;
		
		Set<OntResource> resources = new HashSet<OntResource>();
		
		
		try {
			oro.add(oro.createStatement("capucin rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("capucin rdf:type MyAnimals"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		resources.add(oro.getResource("capucin"));
		resources.add(oro.getResource("gorilla"));
		
		//In this case, we should find any way to discriminate these instances.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertNotNull(discriminents);
		
		assertEquals("No way to differenciate a capucin from a gorilla!",0,
				discriminents.get(0).size());

		//**********************************************************************
		
		try {
			oro.add(oro.createStatement("capucin eats grass"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		//In this case, we should find any way to discriminate these instances.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("Capucins eat grass and gorillas apples!",
				discriminents.get(0).size() == 1 &&
				discriminents.get(0).contains(oro.getModel().getProperty(Namespaces.format("eats"))));

		discriminents.clear();
		
		//**********************************************************************
		
		resources.add(oro.getResource("grass"));
		
		//This time, we expect 'rdf:type' property to be returned.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("The only common property is the type. But it's not a total discriminant.",
				discriminents.get(0).size() == 0 &&
				discriminents.get(1).size() == 2 &&
				discriminents.get(1).contains(oro.getModel().getProperty(Namespaces.format("rdf:type"))) &&
				discriminents.get(1).contains(oro.getModel().getProperty(Namespaces.format("eats"))));

		discriminents.clear();
		
		//**********************************************************************
		
		try {
			oro.add(oro.createStatement("capucin hasColor grey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("gorilla hasColor black"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("grass hasColor green"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		//Now, the concepts can be differenciated by the color. Only 'hasColor'
		//should be returned.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("Every object as a different color, isn't?",
				discriminents.get(0).size() == 1 &&
				discriminents.get(0).contains(oro.getModel().getProperty(Namespaces.format("hasColor"))));
		
		discriminents.clear();
			
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	
	
	/**
	 * This tests corner cases for the discrinimate method.
	 */
	@Test
	public void advancedDiscriminent() {

		System.out.println("[UNITTEST] ***** TEST: Advanced getDiscriminent test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);

		List<Set<Property>> discriminents = null;
		
		Set<OntResource> resources = new HashSet<OntResource>();
				
		try {
			oro.add(oro.createStatement("capucin rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("capucin rdf:type MyAnimals"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("capucin climbsOn sunflower"), MemoryProfile.DEFAULT, false);
			
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		resources.add(oro.getResource("capucin"));
		resources.add(oro.getResource("baboon"));
		
		//In this case, we should find any way to discriminate these instances.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("Capucin and Baboon climbs on different things, and climbsOn " +
				"is more accurate than isOn.",
				discriminents.get(0).size() == 1 &&
				discriminents.get(0).contains(oro.getModel().getProperty(Namespaces.format("climbsOn"))));

		discriminents.clear();
		
		//**********************************************************************
				
		//climbsOn is a sub-property of isOn
		try {
			oro.add(oro.createStatement("bonobo rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("bonobo isOn sunflower"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		resources.add(oro.getResource("bonobo"));
		
		//In this case, we should find any way to discriminate these instances.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		/*
		assertTrue("The best discriminents should be climbsOn AND isOn",
				discriminents.get(0).size() == 0 &&
				discriminents.get(1).size() == 2 &&
				discriminents.get(1).contains(oro.getModel().getProperty(Namespaces.format("isOn"))) &&
				discriminents.get(1).contains(oro.getModel().getProperty(Namespaces.format("climbsOn"))));
		*/
		discriminents.clear();
		
		//**********************************************************************
		
		resources.remove(oro.getResource("bonobo"));
		
		try {
			oro.add(oro.createStatement("gibbon rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("gibbon isOn radish"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		resources.add(oro.getResource("gibbon"));

		
		//This time, we expect 'isOn' super-property to be returned.
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		/*
		assertTrue("The only common property is 'isOn'.",
				discriminents.get(0).size() == 1 &&
				discriminents.get(0).contains(oro.getModel().getProperty(Namespaces.format("isOn"))));
		 */
		
		//**********************************************************************
		// Check the behaviour with super-/sub-properties
		
		try {
			oro.add(oro.createStatement("vervet climbsOn palmtree"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("macaque rdf:type Monkey"), MemoryProfile.DEFAULT, false);
			oro.add(oro.createStatement("macaque isOn palmtree"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		resources.clear();
		resources.add(oro.getResource("vervet"));
		resources.add(oro.getResource("macaque"));
		
		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue(discriminents.get(1).size() == 1 &&
				discriminents.get(1).contains(oro.getModel().getProperty(Namespaces.format("climbsOn"))));

		discriminents.clear();
		
		try {
			Set<Statement> s = new HashSet<Statement>();
			s.add(oro.createStatement("macaque isOn palmtree"));
			oro.remove(s);
			oro.add(oro.createStatement("macaque climbsOn coconut"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		} catch (OntologyServerException e) {
			fail();
		}

		try {
			discriminents = categorizationModule.getDiscriminent(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("Only climsOn and not isOn should be returned.",
				discriminents.get(0).size() == 1 &&
				discriminents.get(0).contains(oro.getModel().getProperty(Namespaces.format("climbsOn"))));

		//**********************************************************************
	
			
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests ability for the cognitive kernel to extract categories from a
	 * set of concept.
	 */
	@Test
	public void categorization() {

		System.out.println("[UNITTEST] ***** TEST: Categorization test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);

		Map<OntClass, Set<Individual>> categories = null;
		
		Set<OntResource> resources = new HashSet<OntResource>();
		
		resources.add(oro.getResource("apple"));
		resources.add(oro.getResource("baboon"));
		resources.add(oro.getResource("banana_tree"));
		resources.add(oro.getResource("cow"));
		resources.add(oro.getResource("gorilla"));
		resources.add(oro.getResource("grass"));
		
		//First try, with the instances defined in the testsuite ontology.
		try {
			categories = categorizationModule.makeCategories(resources);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertNotNull(categories);
		
		assertTrue("The two categories 'Animal' and 'Plant' were expected!", 
				categories.containsKey(oro.getResource("Animal").asClass()) && 
				categories.containsKey(oro.getResource("Plant").asClass()));

		//**********************************************************************
		
		Set<OntResource> animals = new HashSet<OntResource>();
		
		animals.add(oro.getResource("baboon"));
		animals.add(oro.getResource("cow"));
		animals.add(oro.getResource("gorilla"));
				
		//Add a statement
		try {
			oro.add(oro.createStatement("baboon eats grass"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		categories.clear();	
		try {
			categories = categorizationModule.makeCategories(animals);
		} catch (NotComparableException e) {
			fail();
		}
		
		assertTrue("Two categories should be returned: 'Monkey' and another one.", 
				categories.containsKey(oro.getResource("Monkey").asClass()) && 
				categories.size() == 2);
		
		categories.remove(oro.getResource("Monkey").asClass());
		
		//the second category is a new class, the class of animals that eat grass.
		OntClass grassEaters = null;
		for (OntClass c : categories.keySet())
			grassEaters = c;
		
		//this class should contain 2 individuals
		assertEquals("Right now, we have only 2 grass eaters.", 
				grassEaters.listInstances().toSet().size(), 2);
		
		//Add a new grass eater
		try {
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT, false); //we can infer that sheepy is an animal
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		//If the class is correct, we should have now 3 individuals.
		assertEquals("Now, we should have 3 grass eaters.", 
				grassEaters.listInstances().toSet().size(), 3);
		
		//**********************************************************************
				
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	
	/**
	 * This test checks that the Alterite module works as expected regarding
	 * addition of agents.
	 * @throws IllegalStatementException 
	 */
	@Test
	public void alteriteModule1() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Alterite Module *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = null;
		
		oro.add(oro.createStatement("myself rdf:type Agent"), MemoryProfile.DEFAULT, false);
		
		try {
			alterite = new AlteriteModule(oro, conf);
		} catch (InvalidModelException e) {
		}
		
		assertEquals("Only myself is an agent!", 1, alterite.listAgents().size());
		
		oro.add(oro.createStatement("gerard rdf:type Agent"), MemoryProfile.DEFAULT, false);
		
		assertEquals("Now we are two: myself and gerard", 2, alterite.listAgents().size());
		
		oro.add(oro.createStatement("Animal rdfs:subClassOf Agent"), MemoryProfile.DEFAULT, false);
		
		System.out.println("Oooh! A lot of new agents!");
		for (String s : alterite.listAgents())
			System.out.println(s);
		
		assertEquals("myself + all the animals are now agents!", 5, alterite.listAgents().size());
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks Alterite module general methods.
	 * @throws IllegalStatementException 
	 */
	@Test
	public void alteriteModule2() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Alterite Module 2 *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = null;
		
		oro.add(oro.createStatement("Agent rdfs:subClassOf owl:Thing"), MemoryProfile.DEFAULT, false);
		
		try {
			alterite = new AlteriteModule(oro, conf);
		} catch (InvalidModelException e) {
			fail();
		}

		
		oro.add(oro.createStatement("Animal rdfs:subClassOf Agent"), MemoryProfile.DEFAULT, false);
		
		
		try {
			alterite.getInfosForAgent("baboon", "banana");
			fail("A NotFoundException should be thrown since 'banana' doesn't exist in baboon model.");
		} catch (NotFoundException e1) {
		} catch (AgentNotFoundException e1) {
			fail("Agent 'baboon' should be found!");
		}

		Set<String> stmts = new HashSet<String>();
		stmts.add("baboon likes banana");
		
		try {
			alterite.addForAgent("baboon", stmts);
			
			Set<String> res = alterite.getInfosForAgent("baboon", "banana");

			assertEquals(2, res.size());
			
			for (String s : res)
				System.out.println(s);
						
		} catch (AgentNotFoundException e) {
			fail("Agent 'baboon' should be found!");
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks race condition issues that may arise in a concurrent
	 * execution.
	 */
	@Test
	public void races(){

		class Runner extends Thread {
			 BaseModule oro;
	         int msSleepPeriod;
	         String name;
	         CategorizationModule c;
	         boolean onlyDiscriminate;
	         
	         Runner(String name, int msSleepPeriod, BaseModule oro, CategorizationModule c, boolean onlyDiscriminate) {
	             this.msSleepPeriod = msSleepPeriod;
	             this.oro = oro;
	             this.name = name;
	             this.c = c;
	             this.onlyDiscriminate = onlyDiscriminate;
	         }
	         
	         void add() { 
	         
	        	 Set<String> stmts = new HashSet<String>();
	        	 stmts.add("Bird rdfs:subClassOf Animal");
	        	 
	            try {
	            	System.out.println(name + " starts to add stmts...");
	            	for (int i = 0; i < 50 ; i++) {
	    	    		stmts.add("fish" + i + " rdf:type Animal");
	    	    		stmts.add("fish" + i + " weight 18.0");
	    	    		stmts.add("fish" + i + " eats seaweed");
	    	    		stmts.add("sparrow" + i + " rdf:type Bird");
	    	    		stmts.add("sparrow" + i + " weight 18.0");
	    	    		stmts.add("sparrow" + i + " eats grass");
	            		oro.add(stmts);
	            		stmts.clear();
	            	}
					System.out.println(name + " done with stmts adding.");
				} catch (IllegalStatementException e) {
					fail();
				}
	         }
	        
	         void find() { 
	        	 Set<String> stmts = new HashSet<String>();
	        	 stmts.add("?a rdf:type Animal");
	        	 
	            try {
	            	System.out.println(name + " starts to look for stmts...");
	            	oro.find("a", stmts);
					System.out.println(name + " done with looking for stmts.");
				} catch (IllegalStatementException e) {
					fail();
				} catch (OntologyServerException e) {
					fail();
				}
	         }
	         
	         void discriminate() {
	        	 Set<String> concepts = new HashSet<String>();
	        	 System.out.println(name + " starts to discriminate...");
	        	 concepts.add("fish1");
	        	 concepts.add("sparrow1");
	        	 try {
					c.discriminate(concepts);
				} catch (NotFoundException e) {
					
				} catch (NotComparableException e) {
					
				}
	        	 System.out.println(name + " starts to discriminate");
	         }

	         
			public void run() {
				if (onlyDiscriminate)
					discriminate();
				else {
					add();
					find();
					discriminate();
				}
	         }	
		}
		
		System.out.println("[UNITTEST] ***** TEST: Race conditions *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		CategorizationModule c = new CategorizationModule(onto);
		
		Runner r1 = new Runner("thread1", 0, oro, c, false);
		Runner r2 = new Runner("thread2", 0, oro, c, true);
	    
		r1.start();
		r2.start();

		try {
			r1.join(1000);
			r2.join(1000);
		} catch (InterruptedException e) {
			fail();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks race condition issues that may arise in a concurrent
	 * execution, within the Alterite module.
	 */
	@Test
	public void racesForAgent(){

		class Runner extends Thread {
			AlteriteModule oro;
	         int msSleepPeriod;
	         String name;
	         boolean onlyDiscriminate;
	         
	         Runner(String name, int msSleepPeriod, AlteriteModule oro, boolean onlyDiscriminate) {
	             this.msSleepPeriod = msSleepPeriod;
	             this.oro = oro;
	             this.name = name;
	             this.onlyDiscriminate = onlyDiscriminate;
	         }
	         
	         void add() { 
	         
	        	 Set<String> stmts = new HashSet<String>();
	        	 stmts.add("Bird rdfs:subClassOf Animal");
	        	 
	            try {
	            	System.out.println(name + " starts to add stmts...");
	            	for (int i = 0; i < 50 ; i++) {
	    	    		stmts.add("fish" + i + " rdf:type Animal");
	    	    		stmts.add("sparrow" + i + " rdf:type Bird");
	            		oro.addForAgent("baboon", stmts);
	            		stmts.clear();
	            	}
					System.out.println(name + " done with stmts adding.");
				} catch (IllegalStatementException e) {
					fail();
				} catch (AgentNotFoundException e) {
					fail();
				}
	         }
	        
	         void find() { 
	        	 Set<String> stmts = new HashSet<String>();
	        	 stmts.add("?a rdf:type Animal");
	        	 
	            try {
	            	System.out.println(name + " starts to look for stmts...");
	            	oro.findForAgent("baboon", "a", stmts);
					System.out.println(name + " done with looking for stmts.");
				} catch (IllegalStatementException e) {
					fail();
				} catch (OntologyServerException e) {
					fail();
				}
	         }
	         
	         void discriminate() {
	        	 Set<String> concepts = new HashSet<String>();
	        	 System.out.println(name + " starts to discriminate...");
	        	 concepts.add("fish1");
	        	 concepts.add("sparrow1");
	        	 try {
					oro.discriminateForAgent("baboon", concepts);
				} catch (NotFoundException e) {
					fail();
				} catch (NotComparableException e) {
					fail();
				} catch (AgentNotFoundException e) {
					fail();
				} catch (OntologyServerException e) {
					fail();
				}
	        	 System.out.println(name + " starts to discriminate");
	         }

	         
			public void run() {
				if (onlyDiscriminate)
					discriminate();
				else {
					add();
					find();
					discriminate();
				}
	         }	
		}
		
		System.out.println("[UNITTEST] ***** TEST: Race conditions *****");
	
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = null;
		
	
		try {
			oro.add(oro.createStatement("Agent rdfs:subClassOf owl:Thing"), MemoryProfile.DEFAULT, false);
			alterite = new AlteriteModule(oro, conf);
			oro.add(oro.createStatement("Animal rdfs:subClassOf Agent"), MemoryProfile.DEFAULT, false);
		} catch (InvalidModelException e) {
			fail();
		} catch (IllegalStatementException e) {
			fail();
		}
						
		Runner r1 = new Runner("thread1", 0, alterite, false);
		Runner r2 = new Runner("thread2", 0, alterite, true);
	    
		r1.start();
		r2.start();

		try {
			r1.join(1000);
			r2.join(1000);
		} catch (InterruptedException e) {
			fail();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test partially covers the SocketConnector functionnalities.
	 * 
	 * It mainly tests request handling.
	 * 
	 * TODO: Really difficult to implement because of the SocketConnector structure...
	 */
	/*
	@Test
	public void socketConnector() {

		System.out.println("[UNITTEST] ***** TEST: Socket Connector *****");
		
		MethodTestHolder mth = new MethodTestHolder();
		
		HashMap<String, IService> registredServices = new HashMap<String, IService>();
		
		for (Method m : mth.getClass().getMethods()) {
			RPCMethod a = m.getAnnotation(RPCMethod.class);
			if (a != null) {				
				IService service = new ServiceImpl(
										m.getName(), 
										a.category(), 
										a.desc(), 
										m, 
										mth);
				
				String name = m.getName()+OroServer.formatParameters(m);
				registredServices.put(name, service);
			}
		}
		
		SocketConnector sc = new SocketConnector(null, registredServices);
		
		ClientWorker cw = null;
		try {
			cw = new SocketConnector.ClientWorker(null);
		}
		catch() {};
		

		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	*/
	
}
