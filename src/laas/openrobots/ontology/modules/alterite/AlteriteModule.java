package laas.openrobots.ontology.modules.alterite;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

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
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

public class AlteriteModule implements IServiceProvider, IEventConsumer {

	private Map<String, AgentModel> agents;
		
	private Properties serverParameters;
	
	public AlteriteModule(IOntologyBackend oro) throws EventRegistrationException {
		this(oro, OroServer.serverParameters);
	}
	
	public AlteriteModule(IOntologyBackend oro, Properties serverParameters) throws EventRegistrationException {
		agents = new HashMap<String, AgentModel>();

		
		this.serverParameters = serverParameters;
		
		//Add myself as the first agent.
		agents.put("myself", new AgentModel("myself", oro));
		
		//Register a new event that waits of appearance of new agents.
		//Each time a new agent appears, the this.consumeEvent() method is called.
		IWatcher w = new AgentWatcher(this);

		
		try {
			oro.registerEvent(w);
		}
		catch (EventRegistrationException ere)
		{
			Logger.log("Alterite module won't work because the \"new agent\" event " +
					"couldn't be registered.\n", VerboseLevel.WARNING);
		}
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
	public void consumeEvent(UUID watcherId, OroEvent e) {
		if (OroServer.BLINGBLING)
			Logger.log("22, v'la les agents!\n", VerboseLevel.WARNING);
		
		for (String s : e.getEventContext().split("\n"))
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
			category = "agents",
			desc="adds one or several statements (triplets S-P-O) to a specific agent model, in long term memory."
	)
	public void addForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		addForAgent(id, rawStmts, MemoryProfile.DEFAULT.toString());
	}

	/** Add statements in a specific agent cognitive model with a specific memory model.
	 * Statements are added in "safe mode", ie, only if they don't cause an 
	 * inconsistency in the agent model.
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
		
		for (String rawStmt : rawStmts) {
			Logger.log(id + ": ");
			oro.add(oro.createStatement(rawStmt), MemoryProfile.fromString(memProfile), true);
		}
	}
	
	@RPCMethod(
			category = "agents",
			desc="adds one or several statements (triplets S-P-O) to a specific agent model, in long term memory."
	)
	public void removeForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		for (String rawStmt : rawStmts) {
			Logger.log(id + ": ");
			oro.remove(oro.createStatement(rawStmt));
		}
	}

	@RPCMethod(
			category = "agents",
			desc="exports the cognitive model of a given agent to an OWL file. The provided path must be writable by the server."
	)
	public void save(String id, String path) throws AgentNotFoundException, OntologyServerException {
		
		IOntologyBackend oro = getModelForAgent(id);
		
		Logger.log(id + ": ", VerboseLevel.IMPORTANT);
		oro.save(path);
		
	}
	
	/**************************************************************************/
	
	private IOntologyBackend getModelForAgent(String id) throws AgentNotFoundException {
		IOntologyBackend oro = agents.get(id).model;
		
		if (oro == null) throw new AgentNotFoundException("I couldn't find the agent " + id + ".");
		
		return oro;
	}
}
