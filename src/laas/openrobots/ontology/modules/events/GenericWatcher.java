package laas.openrobots.ontology.modules.events;

import java.util.Set;

import laas.openrobots.ontology.modules.events.IWatcherProvider.TriggeringType;

public class GenericWatcher implements IWatcher {

	EventType eventType;
	Set<String> eventPattern;
	TriggeringType triggeringType;
	
	IEventConsumer client;
	
	
	public GenericWatcher(EventType eventType, 
						Set<String> eventPattern, 
						TriggeringType triggeringType, 
						IEventConsumer client) {
		super();
		this.eventType = eventType;
		this.eventPattern = eventPattern;
		this.triggeringType = triggeringType;
		
		this.client = client;
	}

	@Override
	public EventType getPatternType() {
		return eventType;
	}

	@Override
	public Set<String> getWatchPattern() {
		return eventPattern;
	}
	
	@Override
	public TriggeringType getTriggeringType() {
		return triggeringType;
	}

	@Override
	public void notifySubscriber(OroEvent e) {
		//TODO Improve the return value of an event
		client.consumeEvent(e);

	}

}
