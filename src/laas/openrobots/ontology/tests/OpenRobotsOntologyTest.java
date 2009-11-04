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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
import laas.openrobots.ontology.modules.diff.DiffModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcherProvider;
import laas.openrobots.ontology.modules.events.InternalWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

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
		
		// with a comma separator, the string below should be tokenized in "{}", "f\'\", " \}", " [r ,t,\]" ",r ] ", "a" (5 tokens)
		// with a space as separator, the string below should be tokenized in "{},f\'\,", "\},", "[r ,t,\]" ",r ]", ",a" (4 tokens)
		String strToTokenize = "{},f\\\'\\, \\}, [r ,t,\\]\" \",r ] ,a";
		assertEquals("Wrong tokenization using commas.", 5, Helpers.tokenize(strToTokenize, ',').size());
		assertEquals("Wrong tokenization using spaces.", 4, Helpers.tokenize(strToTokenize, ' ').size());
	}
	
	public void testSave() {
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		System.out.println("[UNITTEST] ***** TEST: Serializing the ontology to disk *****");
		try {
			oro.save("./test.owl");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
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
			onto.add(onto.createStatement("oro:horse rdf:type oro:Animal"), MemoryProfile.DEFAULT);
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
			onto.add(onto.createStatement("oro:fish oro:isFemale \"true\"^^xsd:boolean"), MemoryProfile.DEFAULT);
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
	 * This test add statements to the ontology with different memory models and checks that everything behave as expected (for instance, short term statements must be removed after a while). 
	 * @throws InterruptedException 
	 */
	public void testAddStmntInMemory() throws InterruptedException {
		
		System.out.println("[UNITTEST] ***** TEST: Insertion of statements with different memory profile *****");
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		BaseModule oro = new BaseModule(onto);
		
		MemoryProfile.timeBase = 100; //we accelerate 10 times the behaviour of the memory container.
	
		try {
			onto.add(onto.createStatement("snail rdf:type Animal"), MemoryProfile.DEFAULT);
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}

		try {
			onto.add(onto.createStatement("snail eats grass"), MemoryProfile.EPISODIC);
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
			onto.add(onto.createStatement("cow rdf:type Plant"), MemoryProfile.DEFAULT);
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
			onto.add(onto.createStatement("cow climbsOn banana_tree"), MemoryProfile.DEFAULT);
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
			onto.add(onto.createStatement("sheep eats grass"), MemoryProfile.DEFAULT);
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
		
		DiffModule diffModule = new DiffModule(oro);
		
		//Add a statement
		try {
			//oro.add(oro.createStatement("sheepy rdf:type Sheep"), MemoryProfile.DEFAULT);
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT); //we can infer that sheepy is an animal
			oro.add(oro.createStatement("baboon2 rdf:type Monkey"), MemoryProfile.DEFAULT);
			oro.add(oro.createStatement("baboon2 age 75"), MemoryProfile.DEFAULT);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
				
		Set<String> similarities = null;
		
		//**********************************************************************
		
		//check that we can not compare instances and classes.
		try {
			similarities = diffModule.getSimilarities("baboon", "Monkey");
			fail("We shouldn't be allowed to compare instances and classes!");
		} catch (NotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (NotComparableException e) {
		}
		
		//**********************************************************************
		
		System.out.println(" * Similarities between the cow and the sheep");
		try {
			similarities = diffModule.getSimilarities("cow", "sheepy");	//like the sheep, the cow eats grass and is an animal.
		} catch (NotFoundException e) {
			fail(e.getMessage());
		} catch (NotComparableException e) {
			fail();
		}

		assertTrue("The cow and the sheep should be identified as animals that eat grass", similarities.contains("? rdf:type Animal") && similarities.contains("? eats grass") && similarities.size() == 2);
		
		//**********************************************************************
		
		System.out.println(" * Similarities between the cow and the gorilla");
		try {
			similarities = diffModule.getSimilarities("cow", "gorilla");	//Both the cow and the gorilla are males of 12.
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
			similarities = diffModule.getSimilarities("baboon", "baboon2");
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
		
		DiffModule diffModule = new DiffModule(oro);
		
		//Add a statement
		try {
			oro.add(oro.createStatement("sheepy eats grass"), MemoryProfile.DEFAULT); //we can infer that sheepy is an animal
			oro.add(oro.createStatement("baboon2 rdf:type Monkey"), MemoryProfile.DEFAULT);
			oro.add(oro.createStatement("baboon2 age 75"), MemoryProfile.DEFAULT);
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
				
		Set<Set<String>> differences = null;
		
		//**********************************************************************
		
		//check that we can not compare instances and classes.
		try {
			differences = diffModule.getDifferences("baboon", "Monkey");
			fail("We shouldn't be allowed to compare instances and classes!");
		} catch (NotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (NotComparableException e) {
		}
		
		//**********************************************************************
		
		System.out.println(" * Differences between the cow and the sheep");
		try {
			differences = diffModule.getDifferences("cow", "sheepy");	//like the sheep, the cow eats grass and is an animal.
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
			differences = diffModule.getDifferences("cow", "gorilla");
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
			differences = diffModule.getDifferences("baboon", "baboon2");
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
		
		DiffModule diffModule = new DiffModule(oro);
		
		long totalTimeDiff = 0;
		long totalTimeSim = 0;
		long startTime = System.currentTimeMillis();
		
		
		try {
			diff1 = diffModule.getDifferences("f", "j");
			diff2 = diffModule.getDifferences("f", "k");
			diff3 = diffModule.getDifferences("f", "e");
			diff4 = diffModule.getDifferences("f", "f");
			
			totalTimeDiff = (System.currentTimeMillis()-startTime);
			
			sim1 = diffModule.getSimilarities("f", "j");
			sim2 = diffModule.getSimilarities("f", "k");
			sim3 = diffModule.getSimilarities("f", "e");
			sim4 = diffModule.getSimilarities("f", "f");
			
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
	 * This test checks that the Alterite module works as expected.
	 * @throws IllegalStatementException 
	 */
	public void testAlteriteModule() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Alterite Module *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		AlteriteModule alterite = new AlteriteModule(oro);
		
		try {
			oro.registerEvents(alterite);
			fail("The agent class doesn't exist yet: we can not register the event!");
		} catch (EventRegistrationException e) {}

		oro.add(oro.createStatement("Agent rdfs:subClassOf owl:Thing"), MemoryProfile.DEFAULT);
		
		try {
			oro.registerEvents(alterite);
		} catch (EventRegistrationException e) {
			fail("The agent class now exist: we should be able to register the AgentWatcher event!");
		}
		
		assertEquals("Only myself is an agent!", 1, alterite.listAgents().size());
		
		oro.add(oro.createStatement("Animal rdfs:subClassOf Agent"), MemoryProfile.DEFAULT);
		
		System.out.println("Oooh! A lot of new agents!");
		for (String s : alterite.listAgents())
			System.out.println(s);
		
		assertEquals("myself + all the animals are now agents!", 4, alterite.listAgents().size());
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
}