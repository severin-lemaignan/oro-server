package laas.openrobots.ontology.events;

import laas.openrobots.ontology.events.IEventsProvider.TriggeringType;

/** A class which implements IWatcher is expected to represent an "event trigger" for the ontology. A watcher has a "watch pattern" (which is similar from a syntaxical point of view to a {@link laas.openrobots.ontology.PartialStatement}). When this "watch pattern" matchs at least one statement in the ontology, its {@link #notifySubscriber()} method is called and expected to warn the event subscriber that the event it was watching occured.<br/>
 * The way the trigger is actually fired depends on the triggering mode, as returned by {@link #getTriggeringType()}. Supported trigger mode are defined in {@link TriggeringType}.<br/>
 * Pratically, every connector (ie transport middleware) must provide its own implementation of this interface, like {@link YarpWatcher} for YARP. 
 * @author slemaign
 *
 */
public interface IWatcher {

	public abstract String getWatchPattern();
	
	public abstract TriggeringType getTriggeringType();
	
	public abstract void notifySubscriber();

}