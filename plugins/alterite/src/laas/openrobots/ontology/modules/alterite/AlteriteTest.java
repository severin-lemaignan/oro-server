/*
 * ©LAAS-CNRS (2008-2010)
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