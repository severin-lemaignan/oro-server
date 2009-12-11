package laas.openrobots.ontology.modules.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.InvalidEventDescriptorException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

/**
 * This class allows the registration of event by <b>external</b> client through
 * the {@link #registerEvent(String, String, Set, IEventConsumer)} RPC method.
 *  
 * @author slemaign
 * @see SocketConnector The SocketConnector class documentation has examples of 
 * event registration with the socket connector.
 * 
 * @since 0.6.8
 */
public class EventModule implements IServiceProvider {


	private Set<IWatcher> registredEvents;
	private IOntologyBackend oro;
	
	public EventModule(IOntologyBackend oro) {
		registredEvents = new HashSet<IWatcher>();
		
		this.oro = oro;
	}
	
	/**
	 * Creates a new event bind to a connector.
	 * 
	 * @param eventType
	 * @param eventPattern
	 * @param triggeringType
	 * @param client
	 * @return The unique ID of the newly generated event watcher.
	 * @throws EventRegistrationException 
	 * @see IWatcher
	 */
	private UUID addEvent(	EventType eventType, 
							IWatcher.TriggeringType triggeringType,
							String variable,
							List<String> eventPattern, 
							IEventConsumer client) 
			throws EventRegistrationException
	{
		if (variable != null) eventPattern.add(0, variable);
		
		IWatcher e = new GenericWatcher(eventType, triggeringType, eventPattern, client);
		registredEvents.add(e);

		oro.registerEvent(e);


		return e.getId();
	}

	@RPCMethod(
			category = "events",
			desc = "registers an event. Expected parameters are: type, triggering " +
					"type, event pattern. The last parameter is set automatically."
	)
	public UUID registerEvent(String type, String triggeringType, List<String> pattern, IEventConsumer consumer)
				throws InvalidEventDescriptorException, EventRegistrationException
	{
		return registerEvent(type, triggeringType, null, pattern, consumer);
	}
				
				
	@RPCMethod(
			category = "events",
			desc = "registers an event. Expected parameters are: type, triggering " +
					"type, variable, event pattern. The last parameter is set automatically."
	)
	public UUID registerEvent(String type, String triggeringType, String variable, List<String> pattern, IEventConsumer consumer)
				throws InvalidEventDescriptorException, EventRegistrationException
	{
		EventType eType = null;
		IWatcher.TriggeringType eTriggeringType = null;
		
		try {
			eType = EventType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException iae) {
			Logger.log("Invalid event type. Got " + type, VerboseLevel.WARNING);
			throw new InvalidEventDescriptorException("Invalid event type. One " +
					"of " + Arrays.asList(EventType.values()).toString() + " was expected. Got " + type + 
					" instead.");
		}
		
		if (eType == EventType.NEW_INSTANCE && variable == null){
			Logger.log("No variable provided for NEW_INSTANCE event", VerboseLevel.WARNING);
			throw new InvalidEventDescriptorException("NEW_INSTANCE events " +
					"require as first parameter a variable name to bind.");
		}
		
		if (eType != EventType.NEW_INSTANCE) variable = null;
			
		
		try {
			eTriggeringType = IWatcher.TriggeringType.valueOf(triggeringType.toUpperCase());
		} catch (IllegalArgumentException iae) {
			Logger.log("Invalid event triggering type. Got " + triggeringType, 
					VerboseLevel.WARNING);
			
			throw new InvalidEventDescriptorException("Invalid event triggering " +
					"type. One of " + Arrays.asList(IWatcher.TriggeringType.values()).toString() + " was expected. " +
					"Got " + triggeringType +	" instead.");
		}
		
		return addEvent(eType, eTriggeringType, variable, pattern, consumer);
	}

}
