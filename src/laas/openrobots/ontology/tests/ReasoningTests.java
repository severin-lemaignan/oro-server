package laas.openrobots.ontology.tests;

import java.util.Hashtable;
import java.util.Vector;

import com.hp.hpl.jena.rdf.model.Resource;

import junit.framework.TestCase;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.UnmatchableException;


/**
 * This class holds more advanced unit tests that tests load scalability and some more advanced reasonning feature.<br/>
 * @author slemaign
 *
 */
public class ReasoningTests extends TestCase {
	
	
	/**
	 * This test tries to stress the ontology with a lot of statements addition and queries with huge resultsets.
	 */
	public void testLoadScalability() {

		System.out.println("[UNITTEST] ***** TEST: Load scalability *****");
		
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   
		long max = 10000;
		for (long i = 0 ; i < max ; i++)
			try {
				oro.add("individual" + i +" eats flowers");
			} catch (IllegalStatementException e) {
				fail("Error while adding a statement " + i);
				e.printStackTrace();
			}
		
		System.out.println(max + " statements added in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		long mem2 = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));
		System.out.println("Memory used by addition of statements: " + (long)((mem-mem2) / (1024*1024)) + "MB (ie " + (long)((mem-mem2)/max) + "B by statments)." );

		Vector<PartialStatement> partialStatements = new Vector<PartialStatement>();

		try {
			partialStatements.add(oro.createPartialStatement("?individual eats flowers"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		startTime = System.currentTimeMillis();
		
		Vector<Resource> matchingResources = oro.find("individual", partialStatements);
		
		assertEquals(max + " individuals should be returned.", max, matchingResources.size());
		
		System.out.println(max + " statements retrieved in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		partialStatements.clear();
		matchingResources.clear();
		
		try {
			partialStatements.add(oro.createPartialStatement("?animals rdf:type Animal"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		startTime = System.currentTimeMillis();
		
		matchingResources = oro.find("animals", partialStatements);
		
		System.out.println(max + " statements retrieved through inference in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		assertEquals((max + 3) + " animals should be returned.", max + 3, matchingResources.size());
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test tries to stress the ontology with a lot of statements addition and guesses.
	 */
	public void testLoadScalability2() {

		System.out.println("[UNITTEST] ***** TEST: Load scalability 2 *****");
		
		
		IOntologyBackend oro = new OpenRobotsOntology("oro_test.conf");
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   
		long max = 10000;
		for (long i = 0 ; i < max ; i++)
			try {
				oro.add("individual" + i +" age 10^^xsd:int");
			} catch (IllegalStatementException e) {
				fail("Error while adding statement "+i);
				e.printStackTrace();
			}
		
		System.out.println(max + " statements added in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		long mem2 = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));
		System.out.println("Memory used by addition of statements: " + (long)((mem-mem2) / (1024*1024)) + "MB (ie " + (long)((mem-mem2)/max) + "B by statments)." );

		Vector<PartialStatement> partialStatements = new Vector<PartialStatement>();

		try {
			partialStatements.add(oro.createPartialStatement("?individual age 12^^xsd:int"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		startTime = System.currentTimeMillis();
		
		Hashtable<Resource, Double> matchingResources = new Hashtable<Resource, Double>();
		try {
			matchingResources = oro.guess("individual", partialStatements, 0.5);
		} catch (UnmatchableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(max + 1 + " individuals should be returned.", max + 1, matchingResources.size());
		
		System.out.println(max + " statements guessed in "+ (System.currentTimeMillis() - startTime) + "ms.");
						
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
	public void testInstanceClassification(){
		OpenRobotsOntology oro = new OpenRobotsOntology("oro_test.conf");
		
		try {
			oro.checkConsistency();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
			fail();
		}
	}

}
