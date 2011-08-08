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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.AgentNotFoundException;
import laas.openrobots.ontology.exceptions.EventNotFoundException;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidEventDescriptorException;
import laas.openrobots.ontology.exceptions.InvalidModelException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.modules.alterite.AlteriteModule;
import laas.openrobots.ontology.modules.events.EventModule;
import laas.openrobots.ontology.modules.events.GenericWatcher;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.NewClassInstanceWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 * This class holds unit tests that cover the events feature in {@code oro-server}.<br/>
 * 
 * @author Severin Lemaignan severin.lemaignan@laas.fr
 *
 */
public class EventsTest {

	final String ORO_TEST_CONF = "/home/slemaign/openrobots/etc/oro-server/oro_test.conf";
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
	
	@Test
	public void eventsFactChecking() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: FACT_CHECKING events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		FactCheckingEventConsumer consumer = new FactCheckingEventConsumer();
		
		List<String> set = new ArrayList<String>();
		set.add("chicken has teeth");
		
		List<String> set2 = new ArrayList<String>();
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
		
		try {
			oro.remove(oro.createStatement("chicken has teeth"));
		} catch (OntologyServerException e) {
			fail();
		}
		
		assertFalse("No events should be triggered there :-(", consumer.hasBeenTriggered);
		
		oro.add(oro.createStatement("baboon eats grass"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Event has not been triggered :-(", consumer.hasBeenTriggered);
		//reset the flag
		consumer.hasBeenTriggered = false;
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	

	private class NewInstanceEventConsumer implements IEventConsumer {

		public boolean hasBeenTriggered = false;		

		@Override
		public void consumeEvent(UUID id, OroEvent e) {
			System.out.print("Unbelivable ! New instances:");
			System.out.print("\t" + e.getEventContext());
				
			hasBeenTriggered = true;			
		}
		
	}
	
	/**
	 * This tests event framework on "NEW_INSTANCE" type of events
	 */	
	//TODO Unit-test not very complete!
	@Test
	public void eventsNewInstance() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: NEW_INSTANCE events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		NewInstanceEventConsumer consumer = new NewInstanceEventConsumer();
		
		List<String> list = new ArrayList<String>();
		list.add("b");
		list.add("?a rdf:type Monkey");
		list.add("?a eats ?b");
		
		try {
			oro.registerEvent(
					new GenericWatcher(	EventType.NEW_INSTANCE,
										IWatcher.TriggeringType.ON_TRUE,
										list,
										consumer));
		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered", consumer.hasBeenTriggered);
		
		//trigger a model update
		oro.add(oro.createStatement("paris loves dancing"), MemoryProfile.DEFAULT, false);
		
		assertFalse("No new monkey eats smthg new, no reason to trigger the event", consumer.hasBeenTriggered);
				
		//re-trigger a model update
		oro.add(oro.createStatement("coco rdf:type Monkey"), MemoryProfile.DEFAULT, false);
		
		assertFalse("A new monkey, but it doesn't eat anything, no reason to trigger the event", consumer.hasBeenTriggered);

		//re-trigger a model update
		oro.add(oro.createStatement("coco eats banana"), MemoryProfile.DEFAULT, false);

		assertTrue("Coco eats babana", consumer.hasBeenTriggered);

				
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	/**
	 * This tests event "NEW_INSTANCE" with owl:sameAs predicate
	 */	
	@Test
	public void eventsNewInstanceWithSameAs() throws IllegalStatementException {
		System.out.println("[UNITTEST] ***** TEST: NEW_INSTANCE events test with owl:sameAs *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		NewInstanceEventConsumer consumer = new NewInstanceEventConsumer();
		
		List<String> list = new ArrayList<String>();
		list.add("b");
		list.add("?a rdf:type Monkey");
		list.add("?a eats ?b");
		
		try {
			oro.registerEvent(
					new GenericWatcher(	EventType.NEW_INSTANCE,
										IWatcher.TriggeringType.ON_TRUE,
										list,
										consumer));
		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		}
		
		//re-trigger a model update
		oro.add(oro.createStatement("coco rdf:type Monkey"), MemoryProfile.DEFAULT, false);
		oro.add(oro.createStatement("coco eats banana"), MemoryProfile.DEFAULT, false);
		
		assertTrue("Coco eats babana", consumer.hasBeenTriggered);

		consumer.hasBeenTriggered = false;
		
		oro.add(oro.createStatement("coco eats banana"), MemoryProfile.DEFAULT, false);
		
	}
	
	/**
	 * This tests event framework on "NEW_CLASS_INSTANCE" type of events
	 */	
	@Test
	public void eventsNewClassInstance() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: NEW_CLASS_INSTANCE events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		NewInstanceEventConsumer consumer = new NewInstanceEventConsumer();
		
		try {
			oro.registerEvent(new NewClassInstanceWatcher("Monkey", consumer));
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
	
	@Test
	public void eventsFactCheckingAlternateModel() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: FACT_CHECKING events test on alternate models *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		FactCheckingEventConsumer consumer = new FactCheckingEventConsumer();
		
		AlteriteModule alterite = null;
		
		oro.add(oro.createStatement("myself rdf:type Agent"), MemoryProfile.DEFAULT, false);
		
		try {
			alterite = new AlteriteModule(oro, conf);
		} catch (InvalidModelException e) {
		}
		
		oro.add(oro.createStatement("gerard rdf:type Agent"), MemoryProfile.DEFAULT, false);
						
		List<String> set = new ArrayList<String>();
		set.add("chicken has teeth");
		
		List<String> set2 = new ArrayList<String>();
		set2.add("?a rdf:type Monkey");
		set2.add("?a eats grass");
		
		try {
			alterite.registerEventForAgent("gerard",
								   "FACT_CHECKING",
								   "ON_TRUE",
								   set,
								   consumer);
			
			alterite.registerEventForAgent("gerard",
										   "FACT_CHECKING",
										   "ON_FALSE_ONE_SHOT",
										   set,
										   consumer);
			
			alterite.registerEventForAgent("gerard",
										   "FACT_CHECKING",
										   "ON_TRUE",
										   set2,
										   consumer);

		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		} catch (AgentNotFoundException e) {
			fail("Error while registering an event: unknown agent!");
		} catch (InvalidEventDescriptorException e) {
			fail("Error while registering an event: invalid event descriptor!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered since the model" +
				" wasn't updated", consumer.hasBeenTriggered);
		
		//triggered a model update
		Set<String> tmp = new HashSet<String>();
		
		try {
			tmp.add("paris loves dancing");
			alterite.addForAgent("gerard", tmp);

			assertTrue("Chicken have not yet teeth! we should trigger once the 'ON_FALSE_ONE_SHOT event!", consumer.hasBeenTriggered);
			//reset the flag
			consumer.hasBeenTriggered = false;
			
			//re-triggered a model update
			tmp.clear();
			tmp.add("paris prefers listening_to_music");
			alterite.addForAgent("gerard", tmp);
			
			assertFalse("Chicken have not yet teeth but we already triggered the event!", consumer.hasBeenTriggered);
			
			tmp.clear();
			tmp.add("chicken has teeth");
			alterite.addForAgent("gerard", tmp);
			
			assertTrue("Chicken now should have teeth :-(", consumer.hasBeenTriggered);
			//reset the flaget
			consumer.hasBeenTriggered = false;
			
			try {
				alterite.clearForAgent("gerard", tmp);
			} catch (OntologyServerException e) {
				fail();
			}
			
			assertFalse("No events should be triggered there :-(", consumer.hasBeenTriggered);
			
			tmp.clear();
			tmp.add("baboon eats grass");
			alterite.addForAgent("gerard", tmp);
			
			assertTrue("Event has not been triggered :-(", consumer.hasBeenTriggered);
			//reset the flag
			consumer.hasBeenTriggered = false;
		
		} catch (AgentNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	@Test
	public void clearEvents() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: clear events test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		FactCheckingEventConsumer consumer = new FactCheckingEventConsumer();
		
		EventModule evtModule = new EventModule(oro);
		
		List<String> set = new ArrayList<String>();
		set.add("chicken has teeth");
		
		List<String> set2 = new ArrayList<String>();
		set2.add("?a rdf:type Monkey");
		set2.add("?a eats grass");
		
		try {
			evtModule.registerEvent("FACT_CHECKING",
									"ON_TRUE",
									set,
									consumer);
			
			evtModule.registerEvent("FACT_CHECKING",
									"ON_TRUE",
									set2,
									consumer);

		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		} catch (InvalidEventDescriptorException e) {
			fail("Error while registering an event!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered since the model" +
				" wasn't updated", consumer.hasBeenTriggered);
	
		Statement s1 = oro.createStatement("chicken has teeth");
		Statement s2 = oro.createStatement("baboon eats grass");
		
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken now should have teeth :-(", consumer.hasBeenTriggered);
		//reset the flaget
		consumer.hasBeenTriggered = false;
			
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertTrue("Event has not been triggered :-(", consumer.hasBeenTriggered);
		
		try {
			oro.remove(s1);
		} catch (OntologyServerException e1) {
			fail();
		}
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken now should have teeth again :-(", consumer.hasBeenTriggered);
		//reset the flaget
		consumer.hasBeenTriggered = false;
		
		try {
			oro.remove(s2);
		} catch (OntologyServerException e) {
			fail();
		}
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertTrue("Baboon eats grass again :-(", consumer.hasBeenTriggered);

		//reset the flag
		consumer.hasBeenTriggered = false;
		
		evtModule.clearEvents();
		
		try {
			oro.remove(s1);
		} catch (OntologyServerException e) {
			fail();
		}
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertFalse("No more event, baby! :-(", consumer.hasBeenTriggered);
		
		try {
			oro.remove(s2);
		} catch (OntologyServerException e) {
			fail();
		}
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertFalse("No more event, baby! :-(", consumer.hasBeenTriggered);
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}
	
	@Test
	public void clearEvent() throws IllegalStatementException {

		System.out.println("[UNITTEST] ***** TEST: clear one single event test *****");
		IOntologyBackend oro = new OpenRobotsOntology(conf);

		FactCheckingEventConsumer consumer = new FactCheckingEventConsumer();
		
		EventModule evtModule = new EventModule(oro);
		
		List<String> set = new ArrayList<String>();
		set.add("chicken has teeth");
		
		List<String> set2 = new ArrayList<String>();
		set2.add("?a rdf:type Monkey");
		set2.add("?a eats grass");
		
		UUID evt1 = null;
		UUID evt2 = null;
		
		try {
			evt1 = evtModule.registerEvent("FACT_CHECKING",
									"ON_TRUE",
									set,
									consumer);
			
			evt2 = evtModule.registerEvent("FACT_CHECKING",
									"ON_TRUE",
									set2,
									consumer);

		} catch (EventRegistrationException e) {
			fail("Error while registering an event!");
		} catch (InvalidEventDescriptorException e) {
			fail("Error while registering an event!");
		}
		
		assertFalse("Initially, the event shouldn't be triggered since the model" +
				" wasn't updated", consumer.hasBeenTriggered);
	
		Statement s1 = oro.createStatement("chicken has teeth");
		Statement s2 = oro.createStatement("baboon eats grass");
		
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken now should have teeth :-(", consumer.hasBeenTriggered);
		//reset the flaget
		consumer.hasBeenTriggered = false;
			
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertTrue("Event has not been triggered :-(", consumer.hasBeenTriggered);
		
		try {
			oro.remove(s1);
		} catch (OntologyServerException e1) {
			fail();
		}
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertTrue("Chicken now should have teeth again :-(", consumer.hasBeenTriggered);
		//reset the flaget
		consumer.hasBeenTriggered = false;
		
		try {
			oro.remove(s2);
		} catch (OntologyServerException e1) {
			fail();
		}
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertTrue("Baboon eats grass again :-(", consumer.hasBeenTriggered);

		//reset the flag
		consumer.hasBeenTriggered = false;
		
		try {
			evtModule.clearEvent(evt1.toString());
		} catch (EventNotFoundException e) {
			fail("We should find the event!!");
		} catch (OntologyServerException e) {
			fail("We should find the event!!");
		}
		
		try {
			oro.remove(s1);
		} catch (OntologyServerException e1) {
			fail();
		}
		oro.add(s1, MemoryProfile.DEFAULT, false);
		
		assertFalse("No more event, baby! :-(", consumer.hasBeenTriggered);
		
		try {
			oro.remove(s2);
		} catch (OntologyServerException e1) {
			fail();
		}
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertTrue("Baboons still eat grass! :-(", consumer.hasBeenTriggered);
		
		//reset the flag
		consumer.hasBeenTriggered = false;
		
		try {
			evtModule.clearEvent(evt2.toString());
		} catch (EventNotFoundException e) {
			fail("We should find the event!!");
		} catch (OntologyServerException e) {
			fail("We should find the event!!");
		}
		
		try {
			oro.remove(s2);
		} catch (OntologyServerException e) {
			fail();
		}
		oro.add(s2, MemoryProfile.DEFAULT, false);
		
		assertFalse("No more event, baby! :-(", consumer.hasBeenTriggered);
		
		System.out.println("[UNITTEST] ***** Test successful *****");
	}

	
}
