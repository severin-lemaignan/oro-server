package laas.openrobots.ontology.modules.alterite;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.AgentNotFoundException;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcherProvider;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

public class AlteriteModule implements IServiceProvider, IWatcherProvider, IEventConsumer {

	private Map<String, AgentModel> agents;
	
	private Set<IWatcher> eventsPatterns;
	
	private Properties serverParameters;
	
	public AlteriteModule(IOntologyBackend oro) throws EventRegistrationException {
		this(oro, OroServer.serverParameters);
	}
	
	public AlteriteModule(IOntologyBackend oro, Properties serverParameters) throws EventRegistrationException {
		agents = new HashMap<String, AgentModel>();
		eventsPatterns = new HashSet<IWatcher>();
		
		this.serverParameters = serverParameters;
		
		//Add myself as the first agent.
		agents.put("myself", new AgentModel("myself", oro));
		
		//Register a new event that waits of appearance of new agents.
		//Each time a new agent appears, the this.consumeEvent() method is called.
		IWatcher w = new AgentWatcher(this);
		eventsPatterns.add(w);
		
		oro.registerEvents(this);
	}
	
	public void add(String id){
		agents.put(id, new AgentModel(id, serverParameters));
	}
	
	public Map<String, AgentModel> getAgents() {
		return agents;
	}
	
	@RPCMethod (
			category = "agents",
			desc = "Returns the set of agents I'm aware of (ie, for whom I have " +
					"a cognitive model)."
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
		for (String s : e.getMatchingIds())
			add(s);
		
	}
	/**************************************************************************/
	
	/** Add statements in a specific agent cognitive model.
	 * 
	 * @param id The id of the agent
	 * @param rawStmts a set of statements
	 * @throws IllegalStatementException
	 * @throws AgentNotFoundException 
	 * @see {@link BaseModule#add(Set)}
	 */
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to a specific agent model, in long term memory."
	)
	public void addForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		addForAgent(id, rawStmts, MemoryProfile.DEFAULT.toString());
	}

	/** Add statements in a specific agent cognitive model with a specific memory model.
	 * 
	 * @param id The id of the agent
	 * @param rawStmts a set of statements
	 * @param memProfile the memory profile
	 * @throws IllegalStatementException
	 * @throws AgentNotFoundException 
	 * @see {@link BaseModule#add(Set, String)}
	 * @see MemoryProfile Available memory profile
	 */
	@RPCMethod(
			category = "agents",
			desc="adds one or several statements (triplets S-P-O) to a specific agent model associated with a memory profile."
	)
	public void addForAgent(String id, Set<String> rawStmts, String memProfile) throws IllegalStatementException, AgentNotFoundException
	{
		
		IOntologyBackend oro = getModelForAgent(id);
		
		for (String rawStmt : rawStmts) oro.add(oro.createStatement(rawStmt), MemoryProfile.fromString(memProfile));
	}
	
	@RPCMethod(
			category = "agents",
			desc="adds one or several statements (triplets S-P-O) to a specific agent model, in long term memory."
	)
	public void removeForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		for (String rawStmt : rawStmts) oro.remove(oro.createStatement(rawStmt));
	}

	@RPCMethod(
			category = "agents",
			desc="exports the cognitive model of a given agent to an OWL file. The provided path must be writable by the server."
	)
	public void save(String id, String path) throws AgentNotFoundException, OntologyServerException {
		
		IOntologyBackend oro = getModelForAgent(id);
		
		oro.save(path);
		
	}
	
	/**************************************************************************/
	
	private IOntologyBackend getModelForAgent(String id) throws AgentNotFoundException {
		IOntologyBackend oro = agents.get(id).model;
		
		if (oro == null) throw new AgentNotFoundException("I couldn't find the agent " + id + ".");
		
		return oro;
	}
}
