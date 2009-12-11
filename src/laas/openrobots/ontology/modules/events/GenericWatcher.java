package laas.openrobots.ontology.modules.events;

import java.util.List;
import java.util.UUID;

public class GenericWatcher implements IWatcher {

	protected EventType eventType;
	protected List<String> eventPattern;
	protected IWatcher.TriggeringType triggeringType;
	
	protected IEventConsumer client;
	
	protected UUID watcherId; 
	
	public GenericWatcher(EventType eventType, 
						IWatcher.TriggeringType triggeringType,
						List<String> eventPattern,  
						IEventConsumer client) {
		super();
		this.eventType = eventType;
		this.eventPattern = eventPattern;
		this.triggeringType = triggeringType;
		
		this.client = client;
		
		this.watcherId = UUID.randomUUID();
	}

	@Override
	public EventType getPatternType() {
		return eventType;
	}

	@Override
	public List<String> getWatchPattern() {
		return eventPattern;
	}
	
	@Override
	public IWatcher.TriggeringType getTriggeringType() {
		return triggeringType;
	}
	
	@Override
	public UUID getId() {
		return watcherId;
	}

	@Override
	public void notifySubscriber(OroEvent e) {
		client.consumeEvent(watcherId, e);

	}

}
