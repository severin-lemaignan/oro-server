package laas.openrobots.ontology.modules.events;

import java.util.ArrayList;

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
public class NewClassInstanceWatcher extends GenericWatcher {
		
	public NewClassInstanceWatcher(String classToWatch, IEventConsumer o) {
		super(	EventType.NEW_CLASS_INSTANCE, 
				IWatcher.TriggeringType.ON_TRUE, 
				new ArrayList<String>(),
				o);
		
		this.eventPattern.add(classToWatch);
	}
}
