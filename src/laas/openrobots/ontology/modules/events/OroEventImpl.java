package laas.openrobots.ontology.modules.events;


import java.util.UUID;

public class OroEventImpl implements OroEvent {

	private UUID eventId; 
	
	public OroEventImpl() {
		super();
		
		this.eventId = UUID.randomUUID();
	}

	public String getEventId() {
		return eventId.toString();
	}
	
	public String getEventContext() {
		return "";
	}
	
	
}
