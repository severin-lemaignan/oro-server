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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import junit.framework.TestCase;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.modules.events.GenericWatcher;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.NewInstanceWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

/**
 * This class holds unit tests that cover the events feature in {@code oro-server}.<br/>
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class EventsTest extends TestCase {

	final String ORO_TEST_CONF = "oro_test.conf";
	Properties conf;
	
	public EventsTest() {
		String confFile = System.getProperty("ORO_TEST_CONF");
		if (confFile == null)
			confFile = ORO_TEST_CONF;
		
		conf = OroServer.getConfiguration(confFile);
	}

	/**
	 * This tests event framework on "FACT_CHECKING" type of events
	 */	
	private class FactCheckingEventConsumer implements IEventConsumer {

		public boolean hasBeenTriggered = false;
	
		@Override
		public void consumeEvent(UUID id, OroEvent e) {
			System.out.println("Unbelivable ! The event " + id + " was triggered!");
			hasBeenTriggered = true;			
		}
		
	}
	
	public void testEventsFactChecking() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: FACT_CHECKING events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		FactCheckingEventConsumer consumer = new FactCheckingEventConsumer();
		
		Set<String> set = new HashSet<String>();
		set.add("chicken has teeth");
		
		Set<String> set2 = new HashSet<String>();
		set2.add("?a rdf:type Monkey");
		set2.add("?a eats grass");
		
		try {
			oro.registerEvent(
					new GenericWatcher(	EventType.FACT_CHECKING,
										IWatcher.TriggeringType.ON_TRUE,
										set,
										consumer));
			
			oro.registerEvent(
					new GenericWatcher(	EventType.FACT_CHECKING,
										IWatcher.TriggeringType.ON_FALSE_ONE_SHOT,
										set,
										consumer));
			
			oro.registerEvent(
					new GenericWatcher(	EventType.FACT_CHECKING,
										IWatcher.TriggeringType.ON_TRUE,
										set2,
										consumer));

		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered since the model" +
				" wasn't updated", consumer.hasBeenTriggered);
		
		//triggered a model update
		oro.add(oro.createStatement("paris loves dancing"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken have not yet teeth! we should trigger once the 'ON_FALSE_ONE_SHOT event!", consumer.hasBeenTriggered);
		//reset the flag
		consumer.hasBeenTriggered = false;
		
		//re-triggered a model update
		oro.add(oro.createStatement("paris prefers listening_to_music"), MemoryProfile.DEFAULT, false);
		
		assertFalse("Chicken have not yet teeth but we already triggered the event!", consumer.hasBeenTriggered);
		
		oro.add(oro.createStatement("chicken has teeth"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken now should have teeth :-(", consumer.hasBeenTriggered);
		//reset the flaget
		consumer.hasBeenTriggered = false;
		
		oro.remove(oro.createStatement("chicken has teeth"));
		
		assertFalse("No events should be triggered there :-(", consumer.hasBeenTriggered);
		
		oro.add(oro.createStatement("baboon eats grass"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Event has not been triggered :-(", consumer.hasBeenTriggered);
		//reset the flag
		consumer.hasBeenTriggered = false;
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	
	
	/**
	 * This tests event framework on "NEW_INSTANCE" type of events
	 */	
	private class NewInstanceEventConsumer implements IEventConsumer {

		public boolean hasBeenTriggered = false;		

		@Override
		public void consumeEvent(UUID id, OroEvent e) {
			System.out.print("Unbelivable ! New Monkey:");
			System.out.print("\t" + e.getEventContext());
				
			hasBeenTriggered = true;			
		}
		
	}
	
	public void testEventsNewInstance() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: NEW_INSTANCE events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		NewInstanceEventConsumer consumer = new NewInstanceEventConsumer();
		
		try {
			oro.registerEvent(new NewInstanceWatcher("Monkey", consumer));
		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered", consumer.hasBeenTriggered);
		
		//trigger a model update
		oro.add(oro.createStatement("paris loves dancing"), MemoryProfile.DEFAULT, false);
		
		assertFalse("No new monkey, no reason to trigger the event", consumer.hasBeenTriggered);
				
		//re-trigger a model update
		oro.add(oro.createStatement("coco rdf:type Monkey"), MemoryProfile.DEFAULT, false);
		
		assertTrue("We just added Coco, but it wasn't detected", consumer.hasBeenTriggered);
		//reset the flag
		consumer.hasBeenTriggered = false;
		
		oro.add(oro.createStatement("bumbo climbsOn old_oak"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Bumbo should be inferred to be a monkey since it climbs on trees!", consumer.hasBeenTriggered);
		//reset the flag
		consumer.hasBeenTriggered = false;
				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
}