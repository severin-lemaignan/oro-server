package laas.openrobots.ontology.modules.events;

import laas.openrobots.ontology.modules.events.IWatcher.EventType;

public interface OroEvent {
	
	/**
	 * Returns the context of the event (for instance, the facts that triggered
	 * the event) in a serializable form. The actual content depends on the event 
	 * type, as described here: {@link EventType}
	 * 
	 * @return A serializable form of the event context.
	 * @see EventType
	 */
	public String getEventContext();

}
