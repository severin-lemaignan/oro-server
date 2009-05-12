package laas.openrobots.ontology.events;

import laas.openrobots.ontology.events.IEventsProvider.TriggeringType;

public interface IWatcher {

	public abstract String getWatchQuery();
	
	public abstract TriggeringType getTriggeringType();
	
	public abstract void notifySubscriber();

}