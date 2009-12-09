package laas.openrobots.ontology.modules.events;

import java.util.HashSet;

import laas.openrobots.ontology.modules.alterite.AgentWatcher;


/**
 * This class specializes {@link GenericWatcher} to easily create event watchers
 * that monitor new instances of a given class.
 * 
 * See {@link AgentWatcher} for an example.
 * 
 * @see GenericWatcher
 * @author slemaign
 *
 */
public class NewInstanceWatcher extends GenericWatcher {
		
	public NewInstanceWatcher(String classToWatch, IEventConsumer o) {
		super(	EventType.NEW_INSTANCE, 
				IWatcher.TriggeringType.ON_TRUE, 
				new HashSet<String>(),
				o);
		
		this.eventPattern.add(classToWatch);
	}
}
