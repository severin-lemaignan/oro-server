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

package laas.openrobots.ontology.modules.alterite;

import java.util.Properties;

import junit.framework.TestCase;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.modules.alterite.AlteriteModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;


/**
 * This class holds unit tests that cover most of the {@code oro-server} features.<br/>
 * For the tests to be executed, the {@code oro_test.owl} ontology is required, and must be referenced by the {@code oro_test.conf} configuration file.<br/>
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class AlteriteTest extends TestCase {

	final String ORO_TEST_CONF = "/home/slemaign/openrobots/etc/oro-server/oro_test.conf";
	Properties conf;
	
	public AlteriteTest() {
		String confFile = System.getProperty("ORO_TEST_CONF");
		if (confFile == null)
			confFile = ORO_TEST_CONF;
		
		conf = OroServer.getConfiguration(confFile);
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
