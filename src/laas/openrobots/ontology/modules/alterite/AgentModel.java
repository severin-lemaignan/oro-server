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

import java.util.Properties;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidModelException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.EventModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;

public class AgentModel {

	public String id;
	public IOntologyBackend model;
	private EventModule events; 
	
	public AgentModel(String id, IOntologyBackend model) {
		super();
		this.id = id;
		this.model = model;
		
		setupEventModule();
	}
	
	public AgentModel(String id, Properties parameters) throws InvalidModelException {
		super();
		this.id = id;
		this.model = createAgentModel(parameters);
		
		setupEventModule();
	}
	
	
	private IOntologyBackend createAgentModel(Properties parameters) throws InvalidModelException 
	{
		OntModel onto = null;
		
		if (parameters == null) 
			throw new InvalidModelException("Couldn't create " +
				"a model: no parameters given");
		
		String oroCommonSenseUri = parameters.getProperty("oro_common_sense");
		String oroScenarioUri = parameters.getProperty("oro_scenario");
		
		OntModelSpec onto_model_reasonner;
		String onto_model_reasonner_name = parameters.getProperty("reasonner", "jena_internal_owl_rule");
		
		//select the inference model and reasonner from the "reasonner" parameter specified in the configuration file.
		if ( onto_model_reasonner_name.equalsIgnoreCase("pellet"))
			onto_model_reasonner = PelletReasonerFactory.THE_SPEC;
		
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_rdfs"))
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RDFS_INF;
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_owl_rule"))
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RULE_INF;
		else onto_model_reasonner = OntModelSpec.OWL_DL_MEM;

		// loading of the OWL ontologies thanks Jena	
		try {
			Model mainModel = null;
			Model scenarioModel = null;
					
			try {
				mainModel = FileManager.get().loadModel(oroCommonSenseUri);

				if (oroScenarioUri != null) 
					scenarioModel = FileManager.get().loadModel(oroScenarioUri);
				
			} catch (NotFoundException nfe) {
				Logger.log("Unexpected error while initializing a new cognitive " +
						"model for agent " + id + ": could not find one of these" +
						" files:\n\t- " + oroCommonSenseUri + ",\n\t- " + 
						oroScenarioUri + ".\nExiting.", VerboseLevel.SERIOUS_ERROR);
			}

				
			onto = ModelFactory.createOntologyModel(onto_model_reasonner, mainModel);
						
			if (scenarioModel != null) onto.add(scenarioModel);
			
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
		
		IOntologyBackend agentModel = new OpenRobotsOntology(onto, parameters);
		
		//Add a first assertion: in this model, 'myself' is the agent.
		try {
			Logger.log(id + ": ");
			agentModel.add(agentModel.createStatement("myself owl:sameAs " + id), MemoryProfile.DEFAULT, false);
		} catch (IllegalStatementException e) {
			Logger.log("Unexpected error while creating a new cognitive model " +
					"for agent " + id + ": impossible to set it a 'myself'. I'm " +
					"going on, but something is wrong with the semantic " +
					"model.\n", VerboseLevel.SERIOUS_ERROR);
		}
		
		// Performs an initial classification.
		agentModel.checkConsistency();
		
		
		String defaultRobotId = parameters.getProperty("robot_id");
		if (defaultRobotId != null) {
			try {
				onto.add(agentModel.createStatement(defaultRobotId + " rdf:type Robot"));
			} catch (IllegalStatementException e1) {
				Logger.log("Invalid robot id in your configuration file! must be only on word name of letters, numbers and underscores. ID not added.", VerboseLevel.ERROR);
			}
		}
		
		return agentModel;
		
	}
	
	private void setupEventModule() {
		
		events = new EventModule(model);

	}
	
	public EventModule getEventModule() {return events;}
}
