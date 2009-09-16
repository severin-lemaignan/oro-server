package laas.openrobots.ontology.tests;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import com.hp.hpl.jena.rdf.model.Resource;

import junit.framework.TestCase;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.UnmatchableException;
import laas.openrobots.ontology.memory.MemoryProfile;


/**
 * This class holds more advanced unit tests that tests load scalability and some more advanced reasonning feature.<br/>
 * @author slemaign
 *
 */
public class ReasoningTests extends TestCase {
	
	final String ORO_TEST_CONF = "etc/oro-server/oro_test.conf";
	Properties conf;
	
	public ReasoningTests() {
		conf = getConfiguration(ORO_TEST_CONF);
	}
	
	/**
	 * This test tries to stress the ontology with a lot of statements addition and queries with huge resultsets.
	 */
	public void testLoadScalability() {

		System.out.println("[UNITTEST] ***** TEST: Load scalability *****");
		
		
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   
		long max = 10000;
		for (long i = 0 ; i < max ; i++)
			try {
				oro.add(oro.createStatement("individual" + i +" eats flowers"), MemoryProfile.DEFAULT);
			} catch (IllegalStatementException e) {
				fail("Error while adding a statement " + i);
				e.printStackTrace();
			}
		
		System.out.println(max + " statements added in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		long mem2 = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));
		System.out.println("Memory used by addition of statements: " + (long)((mem-mem2) / (1024*1024)) + "MB (ie " + (long)((mem-mem2)/max) + "B by statments)." );

		Set<String> partialStatements = new HashSet<String>();
		partialStatements.add("?individual eats flowers");

		Set<String> matchingResources = null;
		
		startTime = System.currentTimeMillis();
		try {
			matchingResources = oro.find("individual", partialStatements);			
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		

		
		
		assertEquals(max + " individuals should be returned.", max, matchingResources.size());
		
		System.out.println(max + " statements retrieved in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		partialStatements.clear();
		matchingResources.clear();
		
		partialStatements.add("?animals rdf:type Animal");
		
		startTime = System.currentTimeMillis();
		
		try {
			matchingResources = oro.find("animals", partialStatements);			
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		
		System.out.println(max + " statements retrieved through inference in "+ (System.currentTimeMillis() - startTime) + "ms.");
		
		assertEquals((max + 3) + " animals should be returned.", max + 3, matchingResources.size());
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This test tries to stress the ontology with a lot of statements addition and guesses.
	 */
	public void testLoadScalability2() {

		System.out.println("[UNITTEST] ***** TEST: Load scalability 2 *****");
		
		
		IOntologyBackend oro = new OpenRobotsOntology(conf);
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   
		long max = 10000;
		for (long i = 0 ; i < max ; i++)
			try {
				oro.add(oro.createStatement("individual" + i +" age 10^^xsd:int"), MemoryProfile.DEFAULT);
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
		OpenRobotsOntology oro = new OpenRobotsOntology(conf);
		
		try {
			oro.checkConsistency();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	private Properties getConfiguration(String configFileURI){
		/****************************
		 *  Parsing of config file  *
		 ****************************/
		Properties parameters = new Properties();
        try
		{
        	FileInputStream fstream = new FileInputStream(configFileURI);
        	parameters.load(fstream);
			fstream.close();
			
			if (!parameters.containsKey("ontology"))
			{
				System.err.println("No ontology specified in the configuration file (\"" + configFileURI + "\"). Add smthg like ontology=openrobots.owl");
	        	System.exit(1);
			}
		}
        catch (FileNotFoundException fnfe)
        {
        	System.err.println("No config file. Check \"" + configFileURI + "\" exists.");
        	System.exit(1);
        }
        catch (Exception e)
		{
			System.err.println("Config file input error. Check config file syntax.");
			System.exit(1);
		}
        
        return parameters;
	}

}
