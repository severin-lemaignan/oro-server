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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

import junit.framework.TestCase;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;


/**
 * This class holds more advanced unit tests that tests load scalability and some more advanced reasonning feature.<br/>
 * @author slemaign
 *
 */
public class ReasoningTests extends TestCase {
	
	final String ORO_TEST_CONF = "/home/slemaign/openrobots/etc/oro-server/oro_bench.conf";
	Properties conf;
	List<Long> results = new ArrayList<Long>();
	
	public ReasoningTests() {
		String confFile = System.getProperty("ORO_TEST_CONF");
		if (confFile == null)
			confFile = ORO_TEST_CONF;
		
		conf = OroServer.getConfiguration(confFile);
	}
	
	
	public void testBench1Insert() {

		System.out.println("[UNITTEST] ***** TEST: Benchmark 1 - 1000 inserts *****");
		
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   
		long max = 1000;
		for (long i = 0 ; i < max ; i++)
			try {
				//onto.add(onto.createStatement("individual" + i +" eats flowers"), MemoryProfile.DEFAULT, false);
				onto.add(onto.createStatement("RED_BOTTLE hasWeight " + i), MemoryProfile.DEFAULT, false);
			} catch (IllegalStatementException e) {
				fail("Error while adding a statement " + i);
				e.printStackTrace();
			}
		
		long duration = (System.currentTimeMillis() - startTime);
		System.out.println(max + " statements added in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).");
		
		runtime.gc();
		long mem2 = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));
		System.out.println("Memory used by addition of statements: " + ((mem-mem2) / (1024*1024)) + "MB (ie " + ((mem-mem2)/max) + "B by statments)." );

	}
	
	public void testBench2InsertQuery() {

		long max = 10000;
		
		System.out.println("[UNITTEST] ***** TEST: Benchmark 2 - " + max + " grouped inserts *****");
		
		
		IOntologyBackend onto = new OpenRobotsOntology(conf);
		
		Set<Statement> stmts = new HashSet<Statement>();
			
		System.out.println("Starting to stress the ontology...");

		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long mem = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));  
		   

		long startTime = System.currentTimeMillis();
		
		for (long i = 0 ; i < max ; i++) {
			try {
				stmts.add(onto.createStatement("individual" + i +" isOn apple"));
			} catch (IllegalStatementException e) {
				fail("Error while adding statement " + i);
				e.printStackTrace();
			}
		}		
		
		try {
			onto.add(stmts, MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			fail();
		}
		
		long duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println(max + " statements added in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).");
		
		runtime.gc();
		long mem2 = (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory()));
		System.out.println("Memory used by addition of statements: " + ((mem-mem2) / (1024*1024)) + "MB (ie " + ((mem-mem2)/max) + "B by statments)." );
		
		/********* CHECK CONSISTENCY **************/
		startTime = System.currentTimeMillis();
		
		try {
			onto.checkConsistency();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Checked consistency in "+ duration + "ms.\n");

		/********* RETRIEVE STATEMENTS **************/
		
		Set<PartialStatement> partialStatements = new HashSet<PartialStatement>();

		/*** A/Direct instances ***/
		try {
			partialStatements.add(onto.createPartialStatement("?individual rdf:type Animal"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		startTime = System.currentTimeMillis();
		
		Set<RDFNode> res = null;
		try {
			res = onto.find("individual", partialStatements, null);
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Found " + res.size() + " individuals.");
		System.out.println("Reasoning level 1: " + max + " statements retrieved in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).\n");

		
		res.clear();
		
		startTime = System.currentTimeMillis();

		try {
			res = onto.find("individual", partialStatements, null);
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Found " + res.size() + " individuals.");
		System.out.println("Reasoning level 1 (second time): " + max + " statements retrieved in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).\n");
	
		/*** B/More complex inference ***/
		partialStatements.clear();
		try {
			partialStatements.add(onto.createPartialStatement("?individual isAt ?plant"));
			partialStatements.add(onto.createPartialStatement("?plant rdf:type Plant"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		res.clear();
		startTime = System.currentTimeMillis();
	

		try {
			res = onto.find("individual", partialStatements, null);
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Found " + res.size() + " individuals.");
		System.out.println("Reasoning level 2: " + max + " statements retrieved in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).\n");

		/*** C/Even more complex inference ***/
		partialStatements.clear();
		try {
			partialStatements.add(onto.createPartialStatement("apple isUnder ?individual"));
		} catch (IllegalStatementException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		

		res.clear();
		
		startTime = System.currentTimeMillis();
		
		try {
			res = onto.find("individual", partialStatements, null);
		} catch (InvalidQueryException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Found " + res.size() + " individuals.");
		System.out.println("Reasoning level 3: " + max + " statements retrieved in "+ duration + "ms (" + Math.ceil(1000 * max / duration) + " stmt/sec).\n");
	
		/********* CHECK CONSISTENCY **************/
		startTime = System.currentTimeMillis();
		
		try {
			onto.checkConsistency();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
			fail();
		}
		
		duration = (System.currentTimeMillis() - startTime);
		results.add(duration);
		System.out.println("Checked consistency in "+ duration + "ms.\n");
		
		
		System.out.println("\nRESULT SUMMARY\n");
		for (Long i : results)
			System.out.println(i);
			

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
