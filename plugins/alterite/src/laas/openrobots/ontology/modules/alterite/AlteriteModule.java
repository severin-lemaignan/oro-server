package laas.openrobots.ontology.modules.alterite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.AgentNotFoundException;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.exceptions.NotComparableException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.IModule;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.categorization.CategorizationModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

public class AlteriteModule implements IModule, IServiceProvider, IEventConsumer {
	
	private Map<String, AgentModel> agents;
		
	private Properties serverParameters;
	
	private IOntologyBackend oro;
		
	public AlteriteModule(IOntologyBackend oro) throws EventRegistrationException {
		this(oro, OroServer.serverParameters);
	}
	
	public AlteriteModule(IOntologyBackend oro, Properties serverParameters) throws EventRegistrationException {
		agents = new HashMap<String, AgentModel>();

		this.oro = oro;
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

	@Override
	public IServiceProvider getServiceProvider() {
		return this;
	}
	
	public void add(String id){
		if (!checkAlreadyPresent(id)) {
			agents.put(id, new AgentModel(id, serverParameters));
		}
	}
	
	public boolean checkAlreadyPresent(String id) {
		
		ExtendedIterator<OntResource> sameResource = oro.getResource(id).listSameAs();

		while (sameResource.hasNext()){
			if (sameResource.next()
			return true;
		}
		return false;
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
		
		for (String s : (Set<String>) Helpers.deserialize(e.getEventContext(), Set.class))
			add(s);
		
	}
	/**************************************************************************/
	
	/** Add statements in a specific agent cognitive model.
	 * 
	 * @param id The id of the agent
	 * @param rawStmts a set of statements
	 * @throws IllegalStatementException
	 * @throws AgentNotFoundException 
	 * @see BaseModule#add(Set)
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
	 * @see BaseModule#add(Set, String)
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
			desc="tries to identify a resource given a set of partially defined " +
					"statements and restrictions in an specific agent model."
	)
	public Set<String> findForAgent(String id, 
									String varName, 
									Set<String> statements, 
									Set<String> filters) 
						throws IllegalStatementException, AgentNotFoundException, InvalidQueryException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		Logger.log(id + ": ");
		

		Logger.log("Searching resources in the ontology...\n");
		
		Set<String> result = new HashSet<String>();
		Set<PartialStatement> stmts = new HashSet<PartialStatement>();
		
		if (varName.startsWith("?")) varName = varName.substring(1);
		
		Logger.log(" matching following statements:\n", VerboseLevel.VERBOSE);
		
		for (String ps : statements) {
			Logger.log("\t- " + ps + "\n", VerboseLevel.VERBOSE);
			stmts.add(oro.createPartialStatement(ps));
		}
		
		if (filters != null) {
			Logger.log("with these restrictions:\n", VerboseLevel.VERBOSE);
			for (String f : filters)
				Logger.log("\t- " + f + "\n", VerboseLevel.VERBOSE);
		}
			
		return oro.find(varName, stmts, filters);
	}
	
	@RPCMethod(
			category = "agents",
			desc="tries to identify a resource given a set of partially defined " +
					"statements in an specific agent model."
	)
	public Set<String> findForAgent(String id, 
									String varName, 
									Set<String> statements) 
						throws IllegalStatementException, AgentNotFoundException, InvalidQueryException
	{
		return findForAgent(id, varName, statements, null);
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
	
	@RPCMethod(
			category = "agents",
			desc="returns a list of properties that helps to differentiate individuals for a specific agent."
	)
	public List<Set<String>> discriminateForAgent(String id, Set<String> rawConcepts) throws AgentNotFoundException, OntologyServerException, NotFoundException, NotComparableException {
		
		IOntologyBackend oro = getModelForAgent(id);
		CategorizationModule catModule = new CategorizationModule(oro);
		
		Logger.log(id + ": ", VerboseLevel.IMPORTANT);
		return catModule.discriminate(rawConcepts);
		
	}
	
	/**************************************************************************/
	
	private IOntologyBackend getModelForAgent(String id) throws AgentNotFoundException {
		
		AgentModel a = agents.get(id);
		if (a == null) throw new AgentNotFoundException("I couldn't find the agent " + id + ".");
		
		IOntologyBackend oro = a.model;
			
		return oro;
	}

}
