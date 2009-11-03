package laas.openrobots.ontology.modules.events;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.modules.alterite.AgentWatcher;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.events.IWatcherProvider.TriggeringType;

/**
 * This abstract class is intended to be subclassed by all the watchers that 
 * monitors the apparition of new instances of a given type and rely on the 
 * {@link IEventConsumer} interface for notification.
 * 
 * See {@link AgentWatcher} for an example.
 * 
 * @see IWatcher
 * @author slemaign
 *
 */
public class NewInstanceWatcher implements IWatcher {

	private Set<String> classToWatch;
	private IEventConsumer objToNotify;
		
	public NewInstanceWatcher(String classToWatch, IEventConsumer o) {
		Set<String> set = new HashSet<String>();
		set.add(classToWatch);
		this.classToWatch = set;
	}
	

	@Override
	public EventType getPatternType() {
		return EventType.NEW_INSTANCE;
	}
	
	@Override
	public TriggeringType getTriggeringType() {
		return TriggeringType.ON_TRUE;
	}

	@Override
	public Set<String> getWatchPattern() {
		return classToWatch;
	}

	@Override
	public void notifySubscriber(OroEvent e) {
		objToNotify.consumeEvent(e);
	}

}
