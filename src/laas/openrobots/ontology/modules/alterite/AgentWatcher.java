package laas.openrobots.ontology.modules.alterite;

import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.NewClassInstanceWatcher;


public class AgentWatcher extends NewClassInstanceWatcher {

	public AgentWatcher(IEventConsumer o) {
		//Extend the NewInstanceWatcher superclass by specifying we are monitoring
		//the appartion of new agents.
		super("Agent", o);
	}


}
