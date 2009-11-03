package laas.openrobots.ontology.modules.alterite;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcherProvider;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

public class AlteriteModule implements IServiceProvider, IWatcherProvider, IEventConsumer {

	private Map<String, AgentModel> agents;
	
	private Set<IWatcher> eventsPatterns;
	
	public AlteriteModule(IOntologyBackend oro) {
		agents = new HashMap<String, AgentModel>();
		
		//Add myself as the first agent.
		agents.put("myself", new AgentModel("myself", oro));
		
		initializeWatchers();
	}
	
	public void add(String id){
		agents.put(id, new AgentModel(id));
	}
	
	public Map<String, AgentModel> getAgents() {
		return agents;
	}
	
	@RPCMethod (
			category = "agents",
			desc = "Returns the set of agents I'm aware of (ie, for whom I have " +
					"a cognitive model."
			)
	public Set<String> listAgents() {
		return agents.keySet();
	}

	@Override
	public Set<IWatcher> getPendingWatchers() {
		return eventsPatterns;
	}

	@Override
	public void removeWatcher(IWatcher watcher) {
		eventsPatterns.remove(watcher);
		
	}

	@Override
	public void consumeEvent(OroEvent e) {
		if (e.getMatchingId() != null)
			add(e.getMatchingId());
		
	}
	/**************************************************************************/
	private void initializeWatchers() {
		IWatcher w = new AgentWatcher(this);
		eventsPatterns.add(w);
	}
}
