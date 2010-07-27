/*
 * Copyright (c) 2008-2010 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

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
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.AgentNotFoundException;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidModelException;
import laas.openrobots.ontology.exceptions.NotComparableException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.IModule;
import laas.openrobots.ontology.modules.alterite.AgentModel;
import laas.openrobots.ontology.modules.alterite.AgentWatcher;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.categorization.CategorizationModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;
import laas.openrobots.ontology.exceptions.InvalidQueryException;


import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


public class AlteriteModule implements IModule, IServiceProvider, IEventConsumer {
	
	private Map<String, AgentModel> agents;
		
	private Properties serverParameters;
	
	private IOntologyBackend oro;
		
	public AlteriteModule(IOntologyBackend oro) throws EventRegistrationException, InvalidModelException {
		this(oro, OroServer.serverParameters);
	}
	
	public AlteriteModule(IOntologyBackend oro, Properties serverParameters) throws EventRegistrationException, InvalidModelException {
		agents = new HashMap<String, AgentModel>();

		this.oro = oro;
		this.serverParameters = serverParameters;

		try {
			oro.getResource("myself");
			
			//Add myself as the first agent.
			agents.put("myself", new AgentModel("myself", oro));
			
			//Add equivalent concept to 'myself'
			ExtendedIterator<? extends Resource> sameResource = oro.getResource("myself").listSameAs();
			while (sameResource.hasNext()) {
				String syn = sameResource.next().getLocalName();
				if (!syn.equals("myself")) {
					Logger.log("Alterite module: adding " + syn + " as a synonym for myself.\n", VerboseLevel.INFO);
					agents.put(syn, agents.get("myself"));
				}			
			}
		} catch(NotFoundException nfe) {
			Logger.log("AlteriteModule: couldn't find 'myself'. Starting without it.\n", VerboseLevel.IMPORTANT);
		}
		
		try {
			Set<PartialStatement> ps = new HashSet<PartialStatement>();
			ps.add(oro.createPartialStatement("?ag rdf:type Agent"));
			ps.add(oro.createPartialStatement("?ag owl:differentFrom myself")); //attention! ag must be computed to be actually different from myself!
			Set<RDFNode> existing_agents = oro.find("ag", ps, null);
			
			for (RDFNode ag : existing_agents) {
				String id = ((Resource) ag.as(Resource.class)).getLocalName();
				Logger.log("Alterite module: adding agent " + id + ".\n", VerboseLevel.INFO);
				agents.put(id, new AgentModel(id, serverParameters));
			}
		} catch (IllegalStatementException ise) {
			assert(false);
		} catch (InvalidQueryException iqe) {
			assert(false);
		}

		
		
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
	
	public void add(String id) throws InvalidModelException{
		if (!checkAlreadyPresent(id)) {
			Logger.log("A new agent appeared: " + id + "\n", VerboseLevel.INFO);
			agents.put(id, new AgentModel(id, serverParameters));
		}
	}
	
	public boolean checkAlreadyPresent(String id) {
		
		ExtendedIterator<? extends Resource> sameResource = oro.getResource(id).listSameAs();

		Set<String> agentList = agents.keySet();
		
		while (sameResource.hasNext()){
			if (agentList.contains(sameResource.next().getLocalName()))
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
		
		try {
			
			for (String s : (Set<String>) Helpers.deserialize(e.getEventContext(), Set.class))
				add(s);
			
		} catch (OntologyServerException ose) //thrown if an unparsable unicode character is encountered
		{
			Logger.log("\nYou hit a serious bug! Better to exit now until proper handling of this " +
				"exception is added by maintainers! You can help by sending a " +
				"mail to openrobots@laas.fr with the exception stack.\n ", 
				VerboseLevel.FATAL_ERROR);
			System.exit(-1);
		} catch (IllegalArgumentException iae) //thrown if the string couldn't be deserialized to the expect object.
		{
			Logger.log("\nYou hit a serious bug! Better to exit now until proper handling of this " +
					"exception is added by maintainers! You can help by sending a " +
					"mail to openrobots@laas.fr with the exception stack.\n ", 
					VerboseLevel.FATAL_ERROR);
				System.exit(-1);
		}
		
		//JAVA7: Note that in Java7, both exception can be grouped in one catch clause.
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
			desc="adds one or several statements (triplets S-P-O) to a specific " +
				"agent model, in long term memory."
	)
	public void addForAgent(String id, Set<String> rawStmts) 
						throws 	IllegalStatementException, 
								AgentNotFoundException
	{		
		addForAgent(id, rawStmts, MemoryProfile.DEFAULT.toString());
	}

	@RPCMethod(
			category = "agents",
			desc="try to add news statements to a specific agent model in long " +
				"term memory, if they don't lead to inconsistencies (return false " +
				"if at least one stmt wasn't added)."
	)
	public boolean safeAddForAgent(String id, Set<String> rawStmts) 
						throws 	IllegalStatementException, 
								AgentNotFoundException
	{
		return safeAddForAgent(id, rawStmts, MemoryProfile.DEFAULT.toString());
	}
	
	/** Add statements in a specific agent cognitive model with a specific 
	 * memory model.
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
			desc="adds one or several statements (triplets S-P-O) to a specific " +
					"agent model associated with a memory profile."
	)
	public void addForAgent(String id, Set<String> rawStmts, String memProfile) 
						throws 	IllegalStatementException, 
								AgentNotFoundException
	{
		
		IOntologyBackend oro = getModelForAgent(id);
		
		Set<Statement> stmtsToAdd = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			stmtsToAdd.add(oro.createStatement(rawStmt));			
		}
		
		Logger.log(id + ": ");
		oro.add(stmtsToAdd, MemoryProfile.fromString(memProfile), false);

	}
	
	/** Adds statements in a specific agent cognitive model with a specific 
	 * memory model, but only if the statement doesn't cause any inconsistency.
	 * 
	 * If one statement cause an inconsistency, it won't be added, the return
	 * value will be "false", and the process continue with the remaining 
	 * statements. 
	 * 
	 * @param id The id of the agent
	 * @param rawStmts a set of statements
	 * @param memProfile the memory profile
	 * @return false if at least one statement was not added because it would 
	 * lead to inconsistencies.
	 * @throws IllegalStatementException
	 * @throws AgentNotFoundException 
	 */
	@RPCMethod(
			category = "agents",
			desc="try to add news statements to a specific agent model with a " +
				"specific memory profile, if they don't lead to inconsistencies " +
				"(return false if at least one stmt wasn't added)."
	)
	public boolean safeAddForAgent(String id, Set<String> rawStmts, String memProfile) 
							throws 	IllegalStatementException, 
									AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		Set<Statement> stmtsToAdd = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			stmtsToAdd.add(oro.createStatement(rawStmt));			
		}
		
		Logger.log(id + ": ");
		return oro.add(stmtsToAdd, MemoryProfile.fromString(memProfile), true);
	}
	
	@RPCMethod(
			category = "agents",
			desc="removes one or several statements (triplets S-P-O) from a specific agent model, in long term memory."
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
			desc="removes statements from a specific matching any pattern in the given set."
	)
	public void clearForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		for (String rawStmt : rawStmts) {
			Logger.log(id + ": ");
			oro.remove(oro.createStatement(rawStmt));
		}
	}
		
	@RPCMethod(
			category = "agents",
			desc="updates one or several statements (triplets S-P-O) in a specific agent model, in long term memory."
	)
	public void updateForAgent(String id, Set<String> rawStmts) throws IllegalStatementException, AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		Set<Statement> stmtsToUpdate = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to update!");
			stmtsToUpdate.add(oro.createStatement(rawStmt));			
		}
		
		Logger.log(id + ": ");
		oro.update(stmtsToUpdate);

	}
	
	/**
	 * Returns the set of asserted and inferred statements whose the given node 
	 * is part of, in the specifi agent model. It represents the "usages" of a 
	 * resource.<br/>
	 * 
	 * Usage example:<br/>
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 * Model results = myOntology.getInfos("ns:individual1");
	 * 
	 * NodeIterator types = results.listObjectsOfProperty(myOntology.createProperty("rdf:type"));
	 *
	 * for ( ; types.hasNext() ; )
	 * {
	 *	System.out.println(types.nextNode().toString());
	 * }
	 * </pre>
	 * This example would print all the types (classes) of the instance {@code ns:individual1}.
	 * 
	 * @param id the agent model to query.
	 * @param lex_resource the lexical form of an existing resource.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the lex_resource doesn't exist in the ontology.
	 * @throws AgentNotFoundException 
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category="agent",
			desc = "returns the set of asserted and inferred statements whose the given node is part of. It represents the usages of a resource."
	)
	public Set<String> getInfosForAgent(String id, String lex_resource) 
								throws NotFoundException, AgentNotFoundException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		Logger.log(id + ": ");
		
		
		Logger.log("Looking for statements about " + lex_resource + ".\n");
		
		Set<String> result = new HashSet<String>();
		
		Model infos = oro.getSubmodel(oro.getResource(lex_resource));

		StmtIterator stmts = infos.listStatements();

		while (stmts.hasNext()) {
			Statement stmt = stmts.nextStatement();
			RDFNode obj = stmt.getObject();
			//Property p = stmt.getPredicate();
			
			String objString;
		
			if (obj.isResource())
				objString = (obj.as(Resource.class)).getLocalName();
			else if (obj.isLiteral())
				objString = (obj.as(Literal.class)).getLexicalForm();
			else
				objString = obj.toString();

			result.add(	stmt.getSubject().getLocalName() + " " + 
						stmt.getPredicate().getLocalName() + " " +
						objString);
		}
		
		return result;
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
						throws IllegalStatementException, OntologyServerException
	{
		IOntologyBackend oro = getModelForAgent(id);
		
		Logger.log(id + ": ");
		

		Logger.log("Searching resources in the ontology...\n");
		
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
			
		Set<RDFNode> raw = oro.find(varName, stmts, filters);
		Set<String> res = new HashSet<String>();
		
		for (RDFNode n : raw) {
			try {
				Resource node = n.as(Resource.class);
				if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
					res.add(Namespaces.toLightString(node));
			} catch (ResourceRequiredException e) {
				try {
					Literal l = n.as(Literal.class);
					res.add(l.getLexicalForm());
				} catch (ClassCastException e2) {
					Logger.log("Failed to convert the result of the 'find' to a string!",
							VerboseLevel.SERIOUS_ERROR);
					throw new OntologyServerException("Failed to convert the " +
							"result of the 'find' to a string!");
				}
			}
		}

		return res;
	}
	
	@RPCMethod(
			category = "agents",
			desc="tries to identify a resource given a set of partially defined " +
					"statements in an specific agent model."
	)
	public Set<String> findForAgent(String id, 
									String varName, 
									Set<String> statements) 
						throws IllegalStatementException, OntologyServerException
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
