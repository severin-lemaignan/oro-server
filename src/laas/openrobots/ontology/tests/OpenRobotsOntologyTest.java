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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import junit.framework.TestCase;
import laas.openrobots.ontology.IOntologyBackend;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.UnmatchableException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;


/**
 * This class holds unit tests that cover most of the {@code oro-server} features.<br/>
 * For the tests to be executed, the {@code oro_test.owl} ontology is required, and must be referenced by the {@code oro_test.conf} configuration file.<br/>
 * Currently implemented:
 * <ul>
 * <li> Basic tests
 * <ul>
 *  <li> {@link #testSave() testSave}: serialization of the ontology.</li>
 *  <li> {@link #testCheck() testCheck}: check that some facts are correctly asserted/infered in the ontology.</li>
 * 	<li> {@link #testQuery() testQuery}: simple query on the test ontology.</li>
 *  <li> {@link #testGetInfos() testGetInfos}: informations retrieval on a resource.</li>
 *  <li> {@link #testGetInfosDefaultNs() testGetInfosDefaultNs}: informations retrieval on a resource test namespace defaulting.</li>
 *  <li> {@link #testAddStmnt() testAddStmnt}: insertion of a simple statement.</li>
 *  <li> {@link #testAddStmntWithLiteral() testAddStmntWithLiteral}: insertion of a statement containing literals.</li>
 *  <li> {@link #testLiterals() testLiterals}: test support for various possible way of representing literals.</li>
 *  <li> {@link #testRemoveAndClear() testRemoveAndClear}: removal of statements from the ontology.</li>
 *  <li> {@link #testConsistency() testConsistency}: ontology consistency.</li>
 *  <li> {@link #testMatching()}: exact statements matching.</li>
 *  <li> {@link #testApproximateNumericMatching()}: approximate numerical statements matching.</li>
 * </ul>
 * </li>
 * <li> More advanced tests
 *  <ul>
 *  <li> {@link #testInference() testInference}: test basic inference mechanisms.</li>
 *  </ul>
 * </li>
 * </ul>
 * 
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class OpenRobotsOntologyTest extends TestCase {

	/***********************************************************************
	 *                          BASIC TESTS                                *
	 ***********************************************************************/
	
	public void testSave() {
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		System.out.println(" * Serializing the ontology to disk...");
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
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
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
	
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");

		
		/****************
		 *  First query *
		 ****************/
		long intermediateTime = System.currentTimeMillis();
		
		ResultSet result =	oro.query(
						"SELECT ?instances \n" +
						"WHERE { \n" +
						"?instances rdf:type owl:Thing}\n");
	
		
		
		System.out.println("[UNITTEST] First query executed in roughly "+ (System.currentTimeMillis() - intermediateTime) + "ms.");
	
		while (result.hasNext())
		{
			String name = result.nextSolution().getResource("instances").getLocalName();
			assertTrue("Only individuals should be returned. Got " + name, name.contains("apple") ||name.contains("cow") || name.contains("banana_tree") || name.contains("gorilla") || name.contains("baboon"));
		}
		//
		
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
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
				
		Model infos;

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
		//remove this test which depends on the type of reasonner
		//assertEquals("getInfos method should return 11 statements about baboon.", 11, infos.size());
		assertTrue("getInfos method should tell that baboon is an instance of Animal.", infos.contains(oro.createStatement("oro:baboon rdf:type oro:Animal")));
		assertTrue("getInfos method should tell that baboon has a data property set to \"true\".", infos.contains(oro.createStatement("oro:baboon oro:isFemale \"true\"^^xsd:boolean")));
		
		
		// Second test, to check getInfo(Resource) works as well as intended.
		infos = oro.getInfos(oro.createResource("oro:baboon"));
		assertNotNull("getInfos didn't answered anything!",infos);
		//remove this test which depends on the type of reasonner
		//assertEquals("getInfos method should return 11 statements about baboon.", 11, infos.size());
		assertTrue("getInfos method should tell that baboon is an instance of Animal.", infos.contains(oro.createStatement("oro:baboon rdf:type oro:Animal")));
		assertTrue("getInfos method should tell that baboon has a data property set to \"true\".", infos.contains(oro.createStatement("oro:baboon oro:isFemale \"true\"^^xsd:boolean")));
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test is exactly the same as {@link #testGetInfos()} except we don't specify any namespaces. It should use the default one.

	 * @throws IllegalStatementException 
	 */
	public void testGetInfosDefaultNs() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: Informations retrieval on a resource using default namespace *****");
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
				
		Model infos;

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
		assertTrue("getInfosDefaultNs method should tell that baboon is an instance of Animal.", infos.contains(oro.createStatement("baboon rdf:type Animal")));
		assertTrue("getInfosDefaultNs method should tell that baboon has a data property set to \"true\".", infos.contains(oro.createStatement("oro:baboon isFemale \"true\"^^xsd:boolean")));
			
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test add a new statement to the ontology (a new instance of the class Class1 and then query the ontology to check the individual was successfully added, with the right namespace.
	 */
	public void testAddStmnt() {
		
		System.out.println("[UNITTEST] ***** TEST: Insertion of a new statement in the ontology *****");
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		long startTime = System.currentTimeMillis();
		
		try {
			oro.add("oro:horse rdf:type oro:Animal");
		} catch (IllegalStatementException e) {
			fail("Error while adding a statement!");
			e.printStackTrace();
		}
		
		
		Vector<String> stmts = new Vector<String>();
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
		ResultSet result =	oro.query(
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
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
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
			oro.add("oro:fish oro:isFemale \"true\"^^xsd:boolean");
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
	 * This test try to create statements with various types of literals.
	 */
	public void testLiterals() {

		System.out.println("[UNITTEST] ***** TEST: Statements with literals *****");
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
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
	public void testRemoveAndClear() {
		
		System.out.println("[UNITTEST] ***** TEST: Remove & Clear *****");
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		String who_is_an_animal = "SELECT ?instances \n" +
				"WHERE { \n" +
				"?instances rdf:type oro:Animal}\n";
		
		
		Vector<String> stmts = new Vector<String>();
		stmts.add("fish rdf:type Animal");
		stmts.add("sparrow rdf:type Bird");
		stmts.add("Bird rdfs:subClassOf Animal");
		
		try {
			oro.add(stmts);
		} catch (IllegalStatementException e) {
			fail("Error while adding a set of statements!");
			e.printStackTrace();
		}
				
		ResultSet result =	oro.query(who_is_an_animal);
		
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
		
		result = oro.query(who_is_an_animal);
		
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
		
		result =	oro.query(who_is_an_animal);		
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

		result = oro.query(what_does_the_sparrow_eat);		
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
		
		result = oro.query(what_does_the_sparrow_eat);		
		count = 0;
		while (result.hasNext()){
			result.next();
			count++;
		}
		assertEquals("Nothing should be returned.", 0, count);
		
		result =	oro.query(who_is_an_animal);
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
		

		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
				
		try {
			oro.checkConsistency();
		} catch (InconsistentOntologyException e) {
			fail("Initial ontology should be detected as consistent!");
		}
		
				
		try {
			oro.add("cow rdf:type Plant");
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
			oro.add("cow climbsOn banana_tree");
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
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		System.out.println("[UNITTEST] First part: only the resource we are looking for is unknown.");
		
		Vector<PartialStatement> statements = new Vector<PartialStatement>();

		try {
			statements.add(oro.createPartialStatement("?mysterious oro:eats oro:banana_tree"));
			statements.add(oro.createPartialStatement("?mysterious oro:isFemale true^^xsd:boolean")); //Attention: "?mysterious oro:isFemale true" is valid, but "?mysterious oro:isFemale true^^xsd:boolean" is not!
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Vector<Resource> matchingResources = oro.find("mysterious", statements);
		assertNotNull("find() didn't answered anything!",matchingResources);
		
		for ( Resource resource:matchingResources )
		{
			assertTrue("Only baboon should be returned.", resource.getLocalName().contains("baboon"));
		}		
		//TODO Add a test which returns the class of the resource.
		
		System.out.println("[UNITTEST] Second part: more complex resource description.");
		
		statements.clear();
		matchingResources.clear();
		Vector<String> filters = new Vector<String>();

		try {
			statements.add(oro.createPartialStatement("?mysterious rdf:type oro:Monkey"));
			statements.add(oro.createPartialStatement("?mysterious oro:weight ?value"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		filters.add("?value >= 50");
		
		matchingResources = oro.find("mysterious", statements, filters);
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertTrue("find() should answer 2 resources (baboon and gorilla)", matchingResources.size() == 2);
		
		filters.add("?value < 75.8");
		
		matchingResources = oro.find("mysterious", statements, filters);
		assertFalse("find() didn't answered anything!",matchingResources.isEmpty());
		
		assertTrue("find() should now answer only 1 resources (baboon)", matchingResources.size() == 1);
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	/**
	 * This test try to identify a resource through one of its numerical property, but in an approximate way.
	 */
	public void testApproximateNumericMatching() {

		System.out.println("[UNITTEST] ***** TEST: Approximate numeric matching *****");
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
	
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
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		//Add a statement
		try {
			oro.add("sheep eats grass");
		} catch (IllegalStatementException e1) {
			fail("Error while adding a statement!");
			e1.printStackTrace();
		}
		
		//Check it was added.

		Vector<PartialStatement> partialStatements = new Vector<PartialStatement>();

		try {
			partialStatements.add(oro.createPartialStatement("?animals rdf:type Animal"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Vector<Resource> matchingResources = oro.find("animals", partialStatements);
		
		Iterator<Resource> res = matchingResources.iterator();
		while(res.hasNext())
		{
			Resource currentRes = res.next();
			System.out.println(currentRes);
		}
		assertEquals("Four animals should be returned.", 4, matchingResources.size());
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
}
