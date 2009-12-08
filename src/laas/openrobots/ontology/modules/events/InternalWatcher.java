package laas.openrobots.ontology.modules.events;

import java.util.Set;

import laas.openrobots.ontology.modules.events.IWatcherProvider.TriggeringType;

/**
 * This abstract class is intended to be subclassed by all the watchers that 
 * rely on the {@link IEventConsumer} interface for notification.
 * 
 * @see IWatcher
 * @see IEventConsumer
 * @author slemaign
 *
 */
public abstract class InternalWatcher implements IWatcher {

	private IEventConsumer objToNotify;
	
	public InternalWatcher(IEventConsumer o) {
		this.objToNotify = o;
	}
	
	public abstract EventType getPatternType();
	
	public abstract TriggeringType getTriggeringType();

	public abstract Set<String> getWatchPattern();

	@Override
	public void notifySubscriber(OroEvent e) {
		objToNotify.consumeEvent(e);
	}

}
