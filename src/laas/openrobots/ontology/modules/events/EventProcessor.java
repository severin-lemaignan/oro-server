package laas.openrobots.ontology.modules.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.Pair;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;
import com.sun.org.apache.xml.internal.utils.NameSpace;

public class EventProcessor {
	
	private Set<EventType> supportedEventTypes;
	IOntologyBackend onto;
	
	//TODO: rewrite (with subclasses?) this class to be more generic regarding type of events.
	private class WatcherHolder {
		public final IWatcher watcher;
		
		public final IWatcherProvider watcherProvider;
		
		public Query cachedQuery;
		public OntClass referenceClass;
		
		public boolean lastStatus;
		public Set<OntResource> lastMatchedResources;

		public WatcherHolder(IWatcher watcher,
				IWatcherProvider watcherProvider) throws EventRegistrationException {
			super();
			this.watcher = watcher;
			this.watcherProvider = watcherProvider;
			
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
					Logger.log("Error while parsing a new watch pattern! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Error while parsing a new watch pattern! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n");
				}
					
				String resultQuery = "ASK { " + statement + " }";
					
				try	{
					cachedQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
					Logger.log("New watch expression compiled and registered: " + resultQuery + "\n");
				}
				catch (QueryParseException e) {
					Logger.log("Internal error during query parsing while trying to add an event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Internal error during query parsing while trying to add an event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n");
				}
				break;
			}
			case NEW_INSTANCE:
			{
				//No query to compile: we use directly the IOntologyBackend.getInstancesOf() method
				
				if (watcher.getWatchPattern().size() != 1) {
					Logger.log("Wrong pattern for a NEW_INSTANCE type of event. Only one class name was expected.\n", VerboseLevel.ERROR);
					throw new EventRegistrationException("Wrong pattern for a NEW_INSTANCE type of event. Only one class name was expected.");
				}

				//must use a "for" loop because getWatchPattern is a set, but it
				//contains actually only one element.
				for (String s : watcher.getWatchPattern()) {
					onto.getModel().enterCriticalSection(Lock.READ);
					referenceClass = onto.getModel().getOntClass(Namespaces.format(s));
					onto.getModel().leaveCriticalSection();
				
					if (referenceClass == null) {
						Logger.log("The class " + s + " does not exists in the ontology. Cannot register the NEW_INSTANCE event!\n", VerboseLevel.ERROR);
						throw new EventRegistrationException("The class " + s + " does not exists in the ontology. Cannot register the NEW_INSTANCE event!");
					}
				}
				
				//Initialize the list of matching instance from the current state on the ontology.
				lastMatchedResources = onto.getInstancesOf(referenceClass, false);

				
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
		supportedEventTypes.add(EventType.NEW_INSTANCE);
		
	}

	public void process() {
				
		Set<WatcherHolder> watchersToBeRemoved = new HashSet<WatcherHolder>();
		
		//iterate over the various registered watchers and notify the subscribers when needed.
		for (WatcherHolder holder : watchers) {
				
			onto.getModel().enterCriticalSection(Lock.READ);
			try	{
				switch (holder.getWatcher().getPatternType()) {
					case FACT_CHECKING:
						processFactChecking(holder, watchersToBeRemoved);
						break;
						
					case NEW_INSTANCE:
						processNewInstance(holder, watchersToBeRemoved);
						break;
				
				}	
			}
			catch (QueryExecException e) {
				Logger.log("Internal error during query execution while verifiying conditions for event handlers! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
				throw e;
			}
			finally {
				onto.getModel().leaveCriticalSection();
			}
			
		}
		
		watchers.removeAll(watchersToBeRemoved);
			
	}


	private void processFactChecking(WatcherHolder holder, Set<WatcherHolder> watchersToBeRemoved) throws QueryExecException {
		
		if (QueryExecutionFactory.create(holder.cachedQuery, onto.getModel()).execAsk()){
			
			Logger.log("Event triggered for pattern " + holder.watcher.getWatchPattern() + "\n");
			
			OroEvent e = new OroEvent(holder.getWatcher(), true);
			
			switch(holder.watcher.getTriggeringType()){
				case ON_TRUE:
				case ON_TOGGLE:
					//if the last statut for this query is NOT true, then, trigger the event.
					if (!holder.lastStatus) {
						holder.watcher.notifySubscriber(e);
					}
					break;
				case ON_TRUE_ONE_SHOT:
					holder.watcher.notifySubscriber(e);
					holder.watcherProvider.removeWatcher(holder.watcher);
					watchersToBeRemoved.add(holder);
					break;
			}
		} else {
			OroEvent e = new OroEvent(holder.getWatcher(), false);
			
			switch(holder.watcher.getTriggeringType()){					
				case ON_FALSE:
				case ON_TOGGLE:
					//if the last statut for this query is NOT false, then, trigger the event.
					if (holder.lastStatus) {
						holder.watcher.notifySubscriber(e);
					}
					break;
				case ON_FALSE_ONE_SHOT:
					holder.watcher.notifySubscriber(e);
					holder.watcherProvider.removeWatcher(holder.watcher);
					watchersToBeRemoved.add(holder);
					break;
				}				
		}
		
	}
	
	private void processNewInstance(WatcherHolder holder, Set<WatcherHolder> watchersToBeRemoved) throws QueryExecException {
	
		
		Set<OntResource> instances = onto.getInstancesOf(holder.referenceClass, false);
		
		Set<OntResource> futureResources = new HashSet<OntResource>(instances);
		
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
		OroEvent e = new OroEvent(holder.getWatcher(), instances);
		
		switch (holder.watcher.getTriggeringType()) {
		case ON_TRUE:
			holder.watcher.notifySubscriber(e);
			break;
			
		case ON_TRUE_ONE_SHOT:
			holder.watcher.notifySubscriber(e);
			holder.watcherProvider.removeWatcher(holder.watcher);
			watchersToBeRemoved.add(holder);
			break;		
		}
		
		
		holder.lastMatchedResources = futureResources;
		
		
	}
	
	
	public Set<EventType> getSupportedEvents() {
		return supportedEventTypes;
	}

	public void add(IWatcherProvider watcherProvider) throws EventRegistrationException {
		
		for (IWatcher w : watcherProvider.getPendingWatchers()) {
			if (!supportedEventTypes.contains(w.getPatternType())) {
				Logger.log("An unsupported type of event (" + w.getPatternType() + ") has been submitted for registration. Discarding it.\n", VerboseLevel.WARNING);
			}
			else {
				watchers.add(new WatcherHolder(w, watcherProvider));
			}
		}
		
	}

}
