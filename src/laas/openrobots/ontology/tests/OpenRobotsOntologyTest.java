/*
 * ©LAAS-CNRS (2008-2009)
 * 
 * contributor(s) : Séverin Lemaignan <severin.lemaignan@laas.fr>
 * 
 * This software is a computer program whose purpose is to interface
 * with an ontology in a robotics context.
 * 
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 * 
*/

package laas.openrobots.ontology.tests;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.NotComparableException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.exceptions.UnmatchableException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.modules.alterite.AlteriteModule;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.categorization.CategorizationModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.shared.PropertyNotFoundException;

/**
 * This class holds unit tests that cover most of the {@code oro-server} features.<br/>
 * For the tests to be executed, the {@code oro_test.owl} ontology is required, and must be referenced by the {@code oro_test.conf} configuration file.<br/>
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class OpenRobotsOntologyTest extends TestCase {

	final String ORO_TEST_CONF = "oro_test.conf";
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
	
	public void testHelpersFunction() {
	
		System.out.println("[UNITTEST] ***** TEST: Testing helper functions *****");
		
		System.out.println("Tokenization...");
		// with a comma separator, the string below should be tokenized in "{}", "f\'\", " \}", " [r ,t,\]" ",r ] ", "a" (5 tokens)
		// with a space as separator, the string below should be tokenized in "{},f\'\,", "\},", "[r ,t,\]" ",r ]", ",a" (4 tokens)
		String strToTokenize = "{},f\\\'\\, \\}, [r ,t,\\]\" \",r ] ,a";
		assertEquals("Wrong tokenization using commas.", 5, Helpers.tokenize(strToTokenize, ',').size());
		assertEquals("Wrong tokenization using spaces.", 4, Helpers.tokenize(strToTokenize, ' ').size());
		
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
		t2.add("toto");
		t2.add("titi");
		String res = Helpers.stringify(t2);
		assertTrue(res.equals("[\"toto\",\"titi\"]") || res.equals("[\"titi\",\"toto\"]"));
		
		Map<Integer, String> t3 = new HashMap<Integer, String>();
		t3.put(1, "toto");
		t3.put(2, "tata");
		assertEquals("{1:\"toto\",2:\"tata\"}", Helpers.stringify(t3));
		
		List<String> t1bis = new ArrayList<String>();
		t1bis.add("tutu");		
		
		Set<List<String>> t4 = new HashSet<List<String>>();
		t4.add(t1);
		t4.add(t1bis);
		res = Helpers.stringify(t4);
		assertTrue(res.equals("[[\"toto\",\"tata\"],[\"tutu\"]]") || res.equals("[[\"tutu\"],[\"toto\",\"tata\"]]"));
		
		Map<Set<String>, List<String>> t5 = new HashMap<Set<String>, List<String>>();
		t5.put(t2, t1);
		res = Helpers.stringify(t5);
		assertTrue(res.equals("{[\"toto\",\"titi\"]:[\"toto\",\"tata\"]}") || res.equals("{[\"titi\",\"toto\"]:[\"toto\",\"tata\"]}"));
		
		System.out.println("OK.");
	}
	
	
	public void testSave() {
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
	public void testCheck() {
		
		System.out.println("[UNITTEST] ***** TEST: Check facts in the ontology *****");
		
		long startTime = System.currentTimeMillis();
		
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		System.out.println("[UNITTEST] Ontology loaded in roughly "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		/****************
		 *  First check *
		 ****************/
		
		try {
			assertTrue("The fact that gorillas are monkey IS an asserted fact.", oro.check(oro.createStatement("gorilla rdf:type Monkey")));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		/****************
		 *  Second check *
		 ****************/
		
		try {
			assertTrue("The fact that apples are plants should be infered!", oro.check(oro.createStatement("apple rdf:type Plant")));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail();
		}
		
		/****************
		 *  Third check *
		 ****************/
		
		try {
			assertFalse("The fact that gorillas are plants is false!", oro.check(oro.createStatement("gorilla rdf:type Plant")));
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
	public void testQuery() {
		
		System.out.println("[UNITTEST] ***** TEST: Query of the test ontology *****");
	
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);

		
		/****************
		 *  First query *
		 ****************/
		long intermediateTime = System.currentTimeMillis();
		
		ResultSet result =	onto.query(
						"SELECT ?instances \n" +
						"WHERE { \n" +
						"?instances rdf:type oro:Monkey}\n");
	
		
		
		System.out.println("[UNITTEST] First query executed in roughly "+ (System.currentTimeMillis() - intermediateTime) + "ms.");
	
	
		int counter = 0;
		while (result.hasNext())
		{
			counter ++;
			String name = result.nextSolution().getResource("instances").getLocalName();
			assertTrue("Monkeys should be returned. Got " + name, name.contains("baboon") ||name.contains("gorilla"));
		}
		assertEquals("Two monkeys should be returned", 2, counter);
		
		
		/*****************
		 *  Second query *
		 *****************/
		
		long intermediateTime2 = System.currentTimeMillis();
		
		//Second test in XML mode
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
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test retrieve in the ontology details about a resource. Result should be similar to Protege's [class|property|individual usage panel.

	 * @throws IllegalStatementException 
	 */
	public void testGetInfos() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Informations retrieval on a resource *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
				
		Set<String> infos;

		//Check the behaviour of the method in case of inexistant resource
		try {
			infos = oro.getInfos("oro:i_d_ont_exist");
			fail("A NotFoundException should have been triggered!");
		} catch (NotFoundException e) {
		}
		
		long startTime = System.currentTimeMillis();
		
		infos = oro.getInfos("oro:baboon");
		
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
	public void testGetInfosDefaultNs() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Informations retrieval on a resource using default namespace *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
				
		Set<String> infos;

		//Check the behaviour of the method in case of inexistant resource
		try {
			infos = oro.getInfos("i_d_ont_exist");
			fail("A NotFoundException should have been triggered!");
		} catch (NotFoundException e) {
		}
		
		long startTime = System.currentTimeMillis();
		
		infos = oro.getInfos("baboon");
		
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
	public void testAddStmnt() {
		
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
		ResultSet result =	onto.query(
				"SELECT ?instances \n" +
				"WHERE { \n" +
				"?instances rdf:type oro:Animal}\n");
		assertTrue("query didn't answer anything!",result.hasNext());

		System.out.println("[UNITTEST] One statement added in roughly "+ ((intermediateTime - startTime) / 4) + "ms, and ontology queried in roughly "+(System.currentTimeMillis()-intermediateTime)+"ms.");
		
		int count = 0;
		while (result.hasNext()){			
			result.next();
			count++;
		}
		assertEquals("Six individuals, instances of Animal, should be returned.", 6, count);
		
	
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test add a new statement with a literal as object to the ontology (a new instance of the class Class1 and then query the ontology to check the individual was successfully added, with the right namespace.
	 */
	public void testAddStmntWithLiteral() {

		System.out.println("[UNITTEST] ***** TEST: Insertion of statements with literals *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		//First test a request before altering the ontology. 
		String xmlResult =	oro.queryAsXML(
				"SELECT ?value \n" +
				"WHERE { \n" +
				"?instances rdf:type owl:Thing .\n" +
				"?instances oro:isFemale ?value}\n");
		assertNotNull("query didn't answered anything!",xmlResult);
		
		int next = 0; int count = -1;
		while (next != -1){
			next = xmlResult.indexOf("true", next + 1);
			count++;
		}
		assertEquals("The value of isFemale for baboon should be true. It's the only one.", 1, count);
		
		//Add a statement
		try {
			onto.add(onto.createStatement("oro:fish oro:isFemale \"true\"^^xsd:boolean"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		//Check it was added.
		xmlResult =	oro.queryAsXML(
				"SELECT ?value \n" +
				"WHERE { \n" +
				"?instances rdf:type owl:Thing .\n" +
				"?instances oro:isFemale ?value}\n");
		assertNotNull("query didn't answered anything!",xmlResult);
		
		next = 0; count = -1;
		while (next != -1){
			next = xmlResult.indexOf("true", next + 1);
			count++;
		}
		assertEquals("The value of isFemale for baboon and fish is now true. We should get two positives.", 2, count);
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test tests the "safe" add that avoid leading the ontology in a 
	 * inconsistent state.
	 */
	public void testSafeAddStmnt() {
		
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
	public void testAddStmntInMemory() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Insertion of statements with different memory profile *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		MemoryProfile.timeBase = 100; //we accelerate 10 times the behaviour of the memory container.
	
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

		int nbSeconds = 1;
		Date now = new Date();
		
		Set<Statement> rs_stmts = new HashSet<Statement>();
		Set<Statement> rs_short_term = new HashSet<Statement>();
		
		onto.getModel().enterCriticalSection(Lock.READ);
	
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
                    System.out.println(" * Recent stmt found. Elapsed time -> " + elapsedTime);
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
 
       	onto.getModel().leaveCriticalSection();
       	
		assertEquals("Two recently added statements should be returned.", 2, rs_stmts.size());
		
		assertEquals("One short term statements should be returned.", 1, rs_short_term.size());
	
		try {
			onto.save("./before_cleaning.owl");
		} catch (OntologyServerException e) {
			fail();
		}
		
		
		System.out.print(" * Waiting a bit (" + (MemoryProfile.SHORTTERM.duration() + 500) + "ms)...");
		Thread.sleep(MemoryProfile.SHORTTERM.duration() + 500);
		
		
		try {
			onto.save("./after_cleaning.owl");
		} catch (OntologyServerException e) {
			fail();
		}

		onto.getModel().enterCriticalSection(Lock.READ);
		rsIter = onto.getModel().listReifiedStatements() ;
		int nb = rsIter.toSet().size();
		onto.getModel().leaveCriticalSection();
		
		assertEquals("Only one reified statement should now remain.", 1, nb);
				
	
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks that statements that are addable can be removed as well.
	 * 
	 * @throws InterruptedException
	 */
	public void testRemove() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Remove 2 *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
	
		Set<String> stmts = new HashSet<String>();
		
		stmts.add("gorilla rdfs:label KingKong"); //This statement create de facto a KingKong concept.
		stmts.add("gorilla rdfs:comment \"KingKong is nice\"");
		
		try {

			int nbStmt = onto.getModel().listStatements().toList().size();
			
			oro.add(stmts);
			
			try {
				onto.save("./after_add.owl");
			} catch (OntologyServerException e) {
				fail();
			}
			
			oro.remove(stmts);
			
			try {
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
	 * This test checks that concept can be retrieved by their labels. 
	 * @throws InterruptedException 
	 */
	public void testLookup() throws InterruptedException {
		
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

		long startTime = System.currentTimeMillis();
        
		try {
			oro.lookup("princess Daisy");
			fail("No label \"princess Daisy\" exists in the ontology.");
		} catch (NotFoundException e) {}
		
		assertEquals("The \"baboon\" instance should be retieved.", "baboon", oro.lookup("BabouIn").get(0));
		assertEquals("The \"baboon\" type should be INSTANCE.", ResourceType.INSTANCE.toString(), oro.lookup("BabouIn").get(1));
		assertEquals("The \"baboon\" instance should be retieved.", "baboon", oro.lookup("Baboon monkey").get(0));
		assertEquals("The \"gorilla\" instance should be retrieved.", "gorilla", oro.lookup("king kong").get(0));
		assertEquals("The \"iddebile\" instance should be retrieved.", "iddebile", oro.lookup("iddebile").get(0));
		
		assertEquals("\"Monkey\" should be a CLASS.", ResourceType.CLASS.toString(), oro.lookup("Monkey").get(1));
		
		try {
			oro.clear("gorilla ?a ?b");
		} catch (IllegalStatementException e1) {
			e1.printStackTrace();
		}
		
		try {
			oro.lookup("king kong");
			fail("No label \"king kong\" should exist anymore in the ontology.");
		} catch (NotFoundException e) {}
	
		long totalTime = (System.currentTimeMillis()-startTime);
		System.out.println("[UNITTEST] ***** Total time elapsed: "+ totalTime +"ms. Average by query:" + totalTime / 5 + "ms");
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test checks that sub- and superclasses are correctly inferred. 
	 * @throws InterruptedException 
	 */
	public void testSubSuperClasses() throws InterruptedException {
		
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

		long startTime = System.currentTimeMillis();
		
		//for (String k : oro.getInstancesOf("Monkey").keySet()){
		//	System.out.println(k);
		//}
          
		assertEquals("Three subclasses should be returned (Monkey, Insect and Ladybird).", 3, oro.getSubclassesOf("Animal").size());
		assertEquals("Two direct subclasses should be returned (Monkey and Insect).", 2, oro.getDirectSubclassesOf("Animal").size());
		
		assertTrue("These superclasses should be returned (Insect, Animal, owl:Thing (and possibly rdfs:Resource, depending on the reasonner).", (3 <= oro.getSuperclassesOf("Ladybird").size()) && (4 >= oro.getSuperclassesOf("Ladybird").size()));
		assertEquals("One direct superclass should be returned (Insect).", 1, oro.getDirectSuperclassesOf("Ladybird").size());
		
		assertEquals("Three instances of animal should be returned (cow, baboon and gorilla).", 3, oro.getInstancesOf("Animal").size());
		assertEquals("One direct instance of animal should be returned (cow).", 1, oro.getDirectInstancesOf("Animal").size());
		
		assertEquals("Eight subclasses of A should be returned.", 8, oro.getSubclassesOf("A").size());
	
		long totalTime = (System.currentTimeMillis()-startTime);
		System.out.println("[UNITTEST] ***** Total time elapsed: "+ totalTime +"ms. Average by query:" + totalTime / 8 + "ms");
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test try to create statements with various types of literals.
	 */
	public void testLiterals() {

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
	public void testAdvancedRemoveAndClear() {
		
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
				
		ResultSet result =	onto.query(who_is_an_animal);
		
		int count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		assertEquals("Five individuals, instances of Animal, should be returned.", 5, count);
		
		//Let's remove a statement: Birds are no more animals, and thus, the sparrow shouldn't be counted as an animal.
		try {
			oro.remove("Bird rdfs:subClassOf Animal");
		} catch (IllegalStatementException e) {
			fail("Error while removing a set of statements!");
			e.printStackTrace();
		}
		
		result = onto.query(who_is_an_animal);
		
		count = 0;
		while (result.hasNext()){
			System.out.println(result.next());
			count++;
		}
		assertEquals("Four individuals, instances of Animal, should be returned.", 4, count);
		
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
		
		result =	onto.query(who_is_an_animal);		
		count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		//Since sparrows are said to eat something, they are animals.
		assertEquals("Five individuals, instances of Animal, should be returned.", 5, count);

		String what_does_the_sparrow_eat = "SELECT ?food \n" +
		"WHERE { \n" +
		"oro:sparrow oro:eats ?food}\n";

		result = onto.query(what_does_the_sparrow_eat);		
		count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		assertEquals("Two objects should be returned.", 2, count);
		
		//Let's clear infos about what the sparrow eats.
		try {
			oro.clear("sparrow eats ?food");
		} catch (IllegalStatementException e) {
			fail("Error while clearing statements!");
			e.printStackTrace();
		}
		
		result = onto.query(what_does_the_sparrow_eat);		
		count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		assertEquals("Nothing should be returned.", 0, count);
		
		result =	onto.query(who_is_an_animal);
		count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		//There's no more assertions permitting to say that sparrows are animals, so they shouldn't appear.
		assertEquals("Four individuals, instances of Animal, should be returned.", 4, count);
	
	
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
	/**
	 * Tests ontology consistency checking.
	 */
	public void testConsistency() {
		
		System.out.println("[UNITTEST] ***** TEST: Ontology consistency checking *****");
		

		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
				
		try {
			oro.checkConsistency();
		} catch (InconsistentOntologyException e) {
			fail("Initial ontology should be detected as consistent!");
		}
		
				
		try {
			onto.add(onto.createStatement("cow rdf:type Plant"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements in testConsistency!");
		}
		
		//This time, the consistency check should fail since we assert that a cow is both an animal and a plant which contradict the assert axiom (Animal disjointWith Plant)
		try {
			oro.checkConsistency();
			fail("Ontology should be detected as inconsistent! Cows are not plants!");
		} catch (InconsistentOntologyException e) {			
		}
		
		try {
			oro.remove("cow rdf:type Plant");
			oro.checkConsistency();
		} catch (InconsistentOntologyException e) {
			fail();
		} catch (IllegalStatementException e) {
			fail();
		}
		
		try {
			onto.add(onto.createStatement("cow climbsOn banana_tree"), MemoryProfile.DEFAULT, false);
			oro.checkConsistency();
			fail("Ontology should be detected as inconsistent! Cows can not climb on banana trees because they are explicitely not monkeys!");
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements in testConsistency!");
		} catch (InconsistentOntologyException e) {
		}
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test try to match a given set of statements against the ontology, and to get back the class of an object.
	 */
	public void testMatching() {

		System.out.println("[UNITTEST] ***** TEST: Exact statements matching *****");
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		Set<String> matchingResources = null;
		
		System.out.println("[UNITTEST] First part: only the resource we are looking for is unknown.");
		
		Set<String> partial_statements = new HashSet<String>();
		partial_statements.add("?mysterious oro:eats oro:banana_tree");
		partial_statements.add("?mysterious oro:isFemale true^^xsd:boolean");  //Attention: "?mysterious oro:isFemale true" is valid, but "?mysterious oro:isFemale true^^xsd:boolean" is not!

		try {
			matchingResources = oro.find("?mysterious", partial_statements);
			
			matchingResources = oro.find("mysterious", partial_statements);
		} catch (IllegalStatementException e) {
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
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertEquals("find() should answer 2 resources (baboon and gorilla)", 2, matchingResources.size());
		
		filters.add("?value < 75.8");
		
		try {
			matchingResources = oro.find("mysterious", partial_statements, filters);
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertEquals("find() should now answer only 1 resources (baboon)", 1, matchingResources.size());
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test try to identify a resource through one of its numerical property, but in an approximate way.
	 */
	public void testApproximateNumericMatching() {

		System.out.println("[UNITTEST] ***** TEST: Approximate numeric matching *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
	
		Vector<PartialStatement> partialStatements = new Vector<PartialStatement>();

		try {
			partialStatements.add(oro.createPartialStatement("?mysterious age \"40\"^^xsd:int"));
			partialStatements.add(oro.createPartialStatement("?mysterious weight \"60\"^^xsd:double"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Hashtable<Resource, Double> matchingResources = null;
		
		try {
			matchingResources = oro.guess("mysterious", partialStatements, 0.6);
		} catch (UnmatchableException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		
		assertFalse("guess() didn't answered anything!",matchingResources.isEmpty());
		
		Iterator<Resource> res = matchingResources.keySet().iterator();
		while(res.hasNext())
		{
			Resource currentRes = res.next();
			assertTrue("Only baboon should be returned.", currentRes.getLocalName().contains("baboon"));
			//assertTrue("The matching quality should be about 0.7.", matchingResources.get(currentRes) == 0.725);
		}
		//TODO Add a test which returns the class of the resource.
		
		try {
			matchingResources = oro.guess("mysterious", partialStatements, 0.9);
		} catch (UnmatchableException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		
		assertTrue("guess() shouldn't have answered anything!",matchingResources.isEmpty());
		
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/***********************************************************************
	 *                       ADVANCED TESTS                                *
	 ***********************************************************************/

	/**
	 * This test add several new statements and test basic inference mechanisms.
	 */
	public void testInference() {

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
		} catch (IllegalStatementException e) {
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
	public void testSimilarities() {

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
	public void testDifferences() {

		System.out.println("[UNITTEST] ***** TEST: Differences test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);
		
		//Add a statement
		try {
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT, false); //we can infer that sheepy is an animal
			oro.add(oro.createStatement("baboon2 rdf:type Monkey"), MemoryProfile.DEFAULT, false);
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
	public void testAdvancedDiff() {

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
	public void testDiscriminent() {

		System.out.println("[UNITTEST] ***** TEST: Categorization test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);

		List<Set<Property>> discriminents = null;
		
		Set<OntResource> resources = new HashSet<OntResource>();
		
		
		try {
			oro.add(oro.createStatement("capucin rdf:type Monkey"), MemoryProfile.DEFAULT, false);
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
	 * This tests croner cases for the discrinimate method.
	 */
	public void testAdvancedDiscriminent() {

		System.out.println("[UNITTEST] ***** TEST: Advanced getDiscriminent test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		CategorizationModule categorizationModule = new CategorizationModule(oro);

		List<Set<Property>> discriminents = null;
		
		Set<OntResource> resources = new HashSet<OntResource>();
				
		try {
			oro.add(oro.createStatement("capucin rdf:type Monkey"), MemoryProfile.DEFAULT, false);
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
			oro.remove(oro.createStatement("macaque isOn palmtree"));
			oro.add(oro.createStatement("macaque climbsOn coconut"), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
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
	public void testCategorization() {

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
	 * This test checks that the Alterite module works as expected.
	 * @throws IllegalStatementException 
	 */
	public void testAlteriteModule() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Alterite Module *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = null;
		
		oro.add(oro.createStatement("Agent rdfs:subClassOf owl:Thing"), MemoryProfile.DEFAULT, false);
		
		try {
			alterite = new AlteriteModule(oro);
		} catch (EventRegistrationException e) {
			fail("We should be able to register the AgentWatcher event!");
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
	
}