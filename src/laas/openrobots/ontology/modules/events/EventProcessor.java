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

package laas.openrobots.ontology.modules.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.EventNotFoundException;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.Lock;

public class EventProcessor {
	
	private Set<EventType> supportedEventTypes;
	IOntologyBackend onto;
	
	/**
	 * The WatcherHolder class is a pre-compiled version of an {@link IWatcher}
	 * plus some caching mechanisms. 
	 * @author slemaign
	 *
	 */
	//TODO: rewrite (with subclasses?) this class to be more generic regarding type of events.
	private class WatcherHolder {
		public final IWatcher watcher;
				
		public Query cachedQuery;
		public OntClass referenceClass;
		public String varName;
		
		public boolean lastStatus;
		public Set<Resource> lastMatchedResources;

		public WatcherHolder(IWatcher watcher) throws EventRegistrationException {
			super();
			this.watcher = watcher;
			
			lastStatus = false;
			
			compilePattern();
		}
		
		public IWatcher getWatcher() {
			return watcher;
		}
		
		private void compilePattern() throws EventRegistrationException {
			
			switch(watcher.getPatternType()) {
			
			case FACT_CHECKING:
			{
				String statement = "";
				
				try {
					
					for (String s : watcher.getWatchPattern())
						if (PartialStatement.isPartialStatement(s))
							statement += onto.createPartialStatement(s).asSparqlRow();
						else statement += Helpers.asSparqlRow(onto.createStatement(s));
					
				} catch (IllegalStatementException e) {
					Logger.log("Error while parsing a new watch pattern! ("+ 
							e.getLocalizedMessage() +").\nCheck the syntax of " +
							"your statement.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Error while parsing a " +
							"new watch pattern! ("+ e.getLocalizedMessage() +").\n" +
							"Check the syntax of your statement.\n");
				}
					
				String resultQuery = "ASK { " + statement + " }";
				
				Logger.log("SPARQL query: " + resultQuery + "\n", VerboseLevel.DEBUG);
				
				try	{
					cachedQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);					
				}
				catch (QueryParseException e) {
					Logger.log("Internal error during query parsing while trying" +
							" to add an event hook! ("+ e.getLocalizedMessage() +
							").\nCheck the syntax of your statement.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Internal error during " +
							"query parsing while trying to add an event hook! ("+ 
							e.getLocalizedMessage() +").\nCheck the syntax of " +
							"your statement.\n");
				}
				
				Logger.log("New FACT_CHECKING event registered:" +
						"\n\tPattern: " +  watcher.getWatchPattern() +
						"\n\tTriggering type: " + watcher.getTriggeringType() + 
						"\n\tID: " + watcher.getId() + "\n");

				
				break;
			}
			case NEW_CLASS_INSTANCE:
			{
				//No query to compile: we use directly the IOntologyBackend.getInstancesOf() method
				
				if (watcher.getWatchPattern().size() != 1) {
					Logger.log("Wrong pattern for a NEW_INSTANCE type of event." +
							" Only one class name was expected.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Wrong pattern for a " +
							"NEW_INSTANCE type of event. Only one class name was " +
							"expected.");
				}

				//must use a "for" loop because getWatchPattern is a set, but it
				//contains actually only one element.
				for (String s : watcher.getWatchPattern()) {
					Logger.logConcurrency(Logger.LockType.ACQUIRE_READ);
					onto.getModel().enterCriticalSection(Lock.READ);
					referenceClass = onto.getModel().getOntClass(Namespaces.format(s));
					onto.getModel().leaveCriticalSection();
					Logger.logConcurrency(Logger.LockType.RELEASE_READ);
					
					if (referenceClass == null) {
						Logger.log("The class " + s + " does not exists in the " +
								"ontology. Cannot register the NEW_INSTANCE " +
								"event!\n", VerboseLevel.ERROR);
						throw new EventRegistrationException("The class " + s + 
								" does not exists in the ontology. Cannot " +
								"register the NEW_INSTANCE event!");
					}
				}
				
				//Initialize the list of matching instance from the current 
				//state on the ontology.
				lastMatchedResources = new HashSet<Resource>(onto.getInstancesOf(referenceClass, false));
				
				Logger.log("Initial matching instances: " + lastMatchedResources + 
						" (they won't be reported).\n", VerboseLevel.DEBUG);
				
				Logger.log("New NEW_CLASS_INSTANCE event registered." +
						"\n\tClass: " +  Namespaces.toLightString(referenceClass) +
						"\n\tID: " + watcher.getId() + "\n");

				break;
			}
			
			case NEW_INSTANCE:
			{
				Logger.log("Compiling new NEW_INSTANCE event.\n", VerboseLevel.VERBOSE);
				
				List<String> pattern = new ArrayList<String>(watcher.getWatchPattern());

				//TODO: Implement filters for NEW_INSTANCE events
				Set<String> filters = null;

				varName = pattern.remove(0);
				if (varName.length() > 0 && varName.charAt(0) == '?') varName = varName.substring(1);
				
				String query = "SELECT ?" + varName + "\n" +
				"WHERE {\n";
				
				try {
					
					for (String s : pattern)
						query += onto.createPartialStatement(s).asSparqlRow();
					
				} catch (IllegalStatementException e) {
					Logger.log("Error while parsing partial statement ("+ 
							e.getLocalizedMessage() +").\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Error while parsing a " +
							"new watch pattern! ("+ e.getLocalizedMessage() +").\n" +
							"Check the syntax of your partial statements.\n");
				}
				
				if (!(filters == null || filters.isEmpty())) 
				{
					Iterator<String> filtersItr = filters.iterator();
					while (filtersItr.hasNext())
					{
						query += "FILTER (" + filtersItr.next() + ") .\n";
					}
				}
				
				query += "}";
				
				Logger.log("SPARQL query:\n" + query + "\n", VerboseLevel.DEBUG);
									
				try	{
					cachedQuery = QueryFactory.create(query, Syntax.syntaxSPARQL);
				}
				catch (QueryParseException e) {
					Logger.log("Internal error during query parsing while trying " +
							"to add an event hook! ("+ e.getLocalizedMessage() +").\n" +
							"Check the syntax of your statement.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Internal error during " +
							"query parsing while trying to add an event hook! ("+ 
							e.getLocalizedMessage() +").\nCheck the syntax of " +
							"your statement.\n");
				}
				
				
				//Initialize the list of matching instance from the current state on the ontology.
				lastMatchedResources = new HashSet<Resource>();
				ResultSet rawResult = QueryExecutionFactory.create(cachedQuery, onto.getModel()).execSelect();
				
				if (rawResult != null) {				
					while (rawResult.hasNext())
					{
						QuerySolution row = rawResult.nextSolution();
						lastMatchedResources.add(row.getResource(varName));
					}
				}
				Logger.log("Initial matching instances: " + lastMatchedResources +
						" (they won't be reported).\n", VerboseLevel.DEBUG);

				
				Logger.log("New NEW_INSTANCE event registered " +
						"\n\tPattern: " +  watcher.getWatchPattern() +
						"\n\tTriggering type: " + watcher.getTriggeringType() + 
						"\n\tID: " + watcher.getId() + "\n");
			}
		}
		
		
	}
	}
	
	/**
	 * the watchersCache holds as keys the literal watch pattern and as value a
	 * pair of pre-processed query built from the watch pattern and a boolean
	 * holding the last known result of the query.
	 */
	private Set<WatcherHolder> watchers;
		
	public EventProcessor(IOntologyBackend onto) {
		this.onto = onto;

		this.watchers = new HashSet<WatcherHolder>();
		
		this.supportedEventTypes = new HashSet<EventType>();
		supportedEventTypes.add(EventType.FACT_CHECKING);
		supportedEventTypes.add(EventType.NEW_CLASS_INSTANCE);
		supportedEventTypes.add(EventType.NEW_INSTANCE);
		
	}

	public void process() {
				
		Set<WatcherHolder> watchersToBeRemoved = new HashSet<WatcherHolder>();
		
		//iterate over the various registered watchers and notify the subscribers 
		//when needed.
		synchronized (watchers) {
		
			for (WatcherHolder holder : watchers) {
				
				switch (holder.getWatcher().getPatternType()) {
					case FACT_CHECKING:
						processFactChecking(holder, watchersToBeRemoved);
						break;
						
					case NEW_CLASS_INSTANCE:
						processNewClassInstance(holder, watchersToBeRemoved);
						break;
						
					case NEW_INSTANCE:
						processNewInstance(holder, watchersToBeRemoved);
						break;
				
				}
		
			}
			
			watchers.removeAll(watchersToBeRemoved);
		
		}
			
	}


	private void processFactChecking(WatcherHolder holder, Set<WatcherHolder> watchersToBeRemoved) throws QueryExecException {
		
		boolean isAsserted = false;

		Logger.logConcurrency(Logger.LockType.ACQUIRE_READ);
		onto.getModel().enterCriticalSection(Lock.READ);
		
		try {
			isAsserted = QueryExecutionFactory.create(holder.cachedQuery, onto.getModel()).execAsk();
		}
		catch (QueryExecException e) {
			Logger.log("Internal error during query execution while " +
					"verifiying conditions for event handlers! " +
					"("+ e.getLocalizedMessage() +").\nPlease contact the " +
					"maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		finally {
			onto.getModel().leaveCriticalSection();
			Logger.logConcurrency(Logger.LockType.RELEASE_READ);
		}
		
		
		if (isAsserted) {
			
			OroEvent e = new OroEventImpl();
			
			switch(holder.watcher.getTriggeringType()){
				case ON_TRUE:
				case ON_TOGGLE:
					//if the last status for this query is NOT true, then, trigger the event.
					if (!holder.lastStatus) {
						Logger.log("Event triggered for pattern " + holder.watcher.getWatchPattern() + "\n");
						holder.watcher.notifySubscribers(e);
					}
					break;
				case ON_TRUE_ONE_SHOT:
					Logger.log("Event triggered for pattern " + holder.watcher.getWatchPattern() + "\n");
					holder.watcher.notifySubscribers(e);
					watchersToBeRemoved.add(holder);
					break;
			}
		} else {
			OroEvent e = new OroEventImpl();
			
			switch(holder.watcher.getTriggeringType()){					
				case ON_FALSE:
				case ON_TOGGLE:
					//if the last statut for this query is NOT false, then, trigger the event.
					if (holder.lastStatus) {
						Logger.log("Event triggered for pattern " + holder.watcher.getWatchPattern() + "\n");
						holder.watcher.notifySubscribers(e);
					}
					break;
				case ON_FALSE_ONE_SHOT:
					Logger.log("Event triggered for pattern " + holder.watcher.getWatchPattern() + "\n");
					holder.watcher.notifySubscribers(e);
					watchersToBeRemoved.add(holder);
					break;
				}				
		}
		
		holder.lastStatus = isAsserted;
		
	}
	
	private void processNewClassInstance(WatcherHolder holder, Set<WatcherHolder> watchersToBeRemoved) throws QueryExecException {
	
		
		Set<Resource> instances = new HashSet<Resource>(onto.getInstancesOf(holder.referenceClass, false));
		
		Set<Resource> futureResources = new HashSet<Resource>(instances);
		
		instances.removeAll(holder.lastMatchedResources);
		
		//Nothing changed
		if (instances.isEmpty() && futureResources.size() == holder.lastMatchedResources.size())
			return;
		
		//Instances have been removed
		if (instances.isEmpty() && futureResources.size() < holder.lastMatchedResources.size()) {
			holder.lastMatchedResources = futureResources;
			return;
		}
		
		assert (!(instances.isEmpty() && futureResources.size() > holder.lastMatchedResources.size()));
		
		//New instances have been added
		OroEvent e = new OroEventNewInstances(instances);
		
		switch (holder.watcher.getTriggeringType()) {
		case ON_TRUE:
			holder.watcher.notifySubscribers(e);
			break;
			
		case ON_TRUE_ONE_SHOT:
			holder.watcher.notifySubscribers(e);
			watchersToBeRemoved.add(holder);
			break;		
		}
		
		
		holder.lastMatchedResources = futureResources;
		
		
	}
	
	private void processNewInstance(WatcherHolder holder, Set<WatcherHolder> watchersToBeRemoved) throws QueryExecException {
	
		
		Set<Resource> instances = new HashSet<Resource>();
		
		ResultSet rawResult = null;

		Logger.logConcurrency(Logger.LockType.ACQUIRE_READ);
		onto.getModel().enterCriticalSection(Lock.READ);
		
		try {
			rawResult = QueryExecutionFactory.create(holder.cachedQuery, onto.getModel()).execSelect();
			
			// While iterating on the result, we may have internal errors from
			// the reasoner. To be on the safe side, we do it inside of the 'try'
			// to be sure to release the lock.
			if (rawResult != null) {				
				while (rawResult.hasNext())
				{
					QuerySolution row = rawResult.nextSolution();
					instances.add(row.getResource(holder.varName));
				}
			}
		}
		catch (QueryExecException e) {
			Logger.log("Internal error during query execution while " +
					"verifiying conditions for event handlers! " +
					"("+ e.getLocalizedMessage() +").\nPlease contact the " +
					"maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		finally {
			onto.getModel().leaveCriticalSection();
			Logger.logConcurrency(Logger.LockType.RELEASE_READ);
		}
				
		Set<Resource> addedResources = new HashSet<Resource>(instances);
		addedResources.removeAll(holder.lastMatchedResources);
		
		Set<Resource> removedResources = new HashSet<Resource>(holder.lastMatchedResources);
		removedResources.removeAll(instances);

		//Instances have been removed
		if (!removedResources.isEmpty()) {
			OroEvent e = new OroEventNewInstances(removedResources);
			switch (holder.watcher.getTriggeringType()) {
			case ON_FALSE:
			case ON_TOGGLE:
				holder.watcher.notifySubscribers(e);
				break;
				
			case ON_FALSE_ONE_SHOT:
				holder.watcher.notifySubscribers(e);
				watchersToBeRemoved.add(holder);
				break;		
			}
		}
		
		//New instances have been added
		if (!addedResources.isEmpty()) {
			OroEvent e = new OroEventNewInstances(addedResources);		
			switch (holder.watcher.getTriggeringType()) {
			case ON_TRUE:
			case ON_TOGGLE:
				holder.watcher.notifySubscribers(e);
				break;
				
			case ON_TRUE_ONE_SHOT:
				holder.watcher.notifySubscribers(e);
				watchersToBeRemoved.add(holder);
				break;		
			}
		}
				
		holder.lastMatchedResources = instances;
		
		
	}
	
	
	public Set<EventType> getSupportedEvents() {
		return supportedEventTypes;
	}

	public void add(IWatcher w) throws EventRegistrationException {
		
		
		if (!supportedEventTypes.contains(w.getPatternType())) {
			Logger.log("An unsupported type of event (" + 
					w.getPatternType() + ") has reached the compilation stage. It" +
					"shouldn't happen. Discarding it for now.\n", VerboseLevel.SERIOUS_ERROR);
		}
		else {
			synchronized (watchers) {
				watchers.add(new WatcherHolder(w));
			}			
		}
		
	}

	public void clear() {
		synchronized (watchers) {
			watchers.clear();
		}		
	}
	
	public void remove(IWatcher w) throws OntologyServerException {
		
		WatcherHolder wh = null;
		
		synchronized (watchers) {
			for (WatcherHolder e : watchers) {
				if (e.watcher.equals(w)) wh = e;
			}
			
			if (wh == null) throw new OntologyServerException("Internal error! Event " + 
					w.getId().toString() + " is absent from the EventProcessor watchers. Please " +
					"report this bug to openrobots@laas.fr.");
			
			
			watchers.remove(wh);	
		}
	}
}
