package laas.openrobots.ontology.modules.alterite;

import java.util.Properties;

import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.utils.VersionInfo;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;

public class AgentModel {

	public String id;
	public IOntologyBackend model;
	
	public AgentModel(String id, IOntologyBackend model) {
		super();
		this.id = id;
		this.model = model;
	}
	
	public AgentModel(String id) {
		super();
		this.id = id;
		this.model = createAgentModel(OroServer.serverParameters);
	}
	
	public AgentModel(String id, Properties parameters) {
		super();
		this.id = id;
		this.model = createAgentModel(parameters);
	}
	
	
	private IOntologyBackend createAgentModel(Properties parameters) 
	{
		OntModel onto = null;
		
		String oroCommonSenseUri = parameters.getProperty("oro_common_sense");
		String oroAgentInstanceUri = parameters.getProperty("oro_agent_instance");
		String oroScenarioUri = parameters.getProperty("oro_scenario");
		
		OntModelSpec onto_model_reasonner;
		String onto_model_reasonner_name = parameters.getProperty("reasonner", "jena_internal_owl_rule");
		
		//select the inference model and reasonner from the "reasonner" parameter specified in the configuration file.
		if ( onto_model_reasonner_name.equalsIgnoreCase("pellet"))
			onto_model_reasonner = PelletReasonerFactory.THE_SPEC;
		
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_rdfs"))
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RDFS_INF;
		
		else onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RULE_INF;
		
		// loading of the OWL ontologies thanks Jena	
		try {
			Model mainModel = null;
			Model agentInstancesModel = null;
			Model scenarioModel = null;
					
			try {
				mainModel = FileManager.get().loadModel(oroCommonSenseUri);
				
				if (oroAgentInstanceUri != null) 
					agentInstancesModel = FileManager.get().loadModel(oroAgentInstanceUri);
				
				if (oroScenarioUri != null) 
					scenarioModel = FileManager.get().loadModel(oroScenarioUri);
				
			} catch (NotFoundException nfe) {
				Logger.log("Unexpected error while initializing a new cognitive " +
						"model for agent " + id + ": could not find one of these" +
						" files:\n\t- " + oroCommonSenseUri + ",\n\t- " + 
						oroAgentInstanceUri + " or\n\t- " + oroScenarioUri + 
						".\nExiting.", VerboseLevel.SERIOUS_ERROR);
			}

				
			onto = ModelFactory.createOntologyModel(onto_model_reasonner, mainModel);
			
			onto.enterCriticalSection(Lock.WRITE);
			if (agentInstancesModel != null) onto.add(agentInstancesModel);
			if (scenarioModel != null) onto.add(scenarioModel);
			onto.leaveCriticalSection();

			Logger.log("New cognitive model for agent " + id + " created.\n", VerboseLevel.IMPORTANT);
			
		} catch (ReasonerException re){
			Logger.log("Unexpected error while initializing a new cognitive " +
					"model for agent " + id + ": error with the reasonner. I'm " +
					"going on, but this agent's model won't be added.\n", 
					VerboseLevel.SERIOUS_ERROR);
			
		} catch (JenaException je){
			Logger.log("Unexpected error while initializing a new cognitive " +
					"model for agent " + id + ": error with Jena. I'm going on," +
					" but this agent's model won't be added.\n", 
					VerboseLevel.SERIOUS_ERROR);
		}
		
		if (onto == null) return null;
		
		IOntologyBackend agentModel = new OpenRobotsOntology(onto);
		
		//Add a first assertion: in this model, 'myself' is the agent.
		try {
			agentModel.add(agentModel.createStatement("myself owl:sameAs " + id), MemoryProfile.DEFAULT);
		} catch (IllegalStatementException e) {
			Logger.log("Unexpected error while creating a new cognitive model " +
					"for agent " + id + ": impossible to set it a 'myself'. I'm " +
					"going on, but something is wrong with the semantic " +
					"model.\n", VerboseLevel.SERIOUS_ERROR);
		}
		
		return agentModel;
		
	}
		
}
