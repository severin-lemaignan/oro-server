package laas.openrobots.ontology.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;


public class ResourceDescription {

	private final String propertiesToRemove[] = {	"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", 
													"http://www.w3.org/2002/07/owl#sameAs", 
													"http://www.w3.org/2002/07/owl#differentFrom",
													"http://www.w3.org/2000/01/rdf-schema#label"};
	
	private final String resourcesToRemove[] = {	"http://www.w3.org/2002/07/owl#Nothing"};
    
	private String languageCode = "en"; //default language for labels set to English
	private OntResource resource;
			
	public ResourceDescription(OntResource resource){
		this.resource = resource;
	}
	
	public ResourceDescription(OntResource resource, String languageCode){
		this.resource = resource;
		this.languageCode = languageCode;
	}

	public String getJson() {
		
		ResourceType type = Helpers.getType(resource);
		
		String result = "{\n";
		
			result += "\"name\":\"" + Helpers.getLabel(resource, languageCode) + "\",\n";
			result += "\"id\":\"" + Helpers.getId(resource) + "\",\n";
			result += "\"type\":\"" + type.toString().toLowerCase() + "\",\n";
			
			result += "\"attributes\": [\n";
			

			if (type == ResourceType.CLASS){
			
				result +="{\n";
				
					//(direct) super classes
					result += "\"name\":\"Parents\",\n";
					result += "\"id\":\"superClasses\",\n";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listSuperClasses(true));
					result += "\n]\n";
				
				result +="},{\n";
				
					//(direct) subclasses
					result += "\"name\":\"Children\",\n";
					result += "\"id\":\"subClasses\",\n";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listSubClasses(true));
					result += "\n]\n";

				result +="},{\n";
				
					//(direct) instances
					result += "\"name\":\"Instances\",\n";
					result += "\"id\":\"instances\",\n";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listInstances(true));
					result += "\n]\n";
				
				result +="}\n";
			}
			
			else if (type == ResourceType.INSTANCE){
				
				result +="{\n";
				
					//(direct) super classes
					result += "\"name\":\"Classes\",\n";
					result += "\"id\":\"classes\",\n";
					result += "\"values\":[";
					result += listOntResource(resource.asIndividual().listOntClasses(true));
					result += "\n]\n";
					
				result +="},";
					
				//link with other resources
				result += listLinks(resource.listProperties());
				
				result = result.substring(0, result.length() -1); //remove the last comma

			}
			
			
			
			result += "]\n";
		result += "}\n";
		
		return result;
	}
	
	private String listOntResource(ExtendedIterator<? extends OntResource> it) {
		String result = "";
		
		while (it.hasNext())
		{
			OntResource tmp = it.next();
			
			if (!tmp.isAnon()) {
				result += "\n{\"name\":\"" + Helpers.getLabel(tmp, languageCode) + "\", ";
				result += "\"id\":\"" + Helpers.getId(tmp) + "\"}";
				if (it.hasNext()) result += ",";
			}			
			
		}
		
		return result;
	}
	
	/**
	 * This method scans all the given statements and build a list of present predicate with their associated objects. This list is then output as a JSON string.
	 * @param rawStmtList
	 * @return a JSON string describing, for each property, all the linked resources.
	 */
	private String listLinks(ExtendedIterator<Statement> rawStmtList) {
		String linkValues = "", result = "";
		
		Map<Property, Set<RDFNode>> propertiesList = new HashMap<Property, Set<RDFNode>>();
		
		//Clean a bit the list of statements.

	
		ExtendedIterator<Statement> stmtList = rawStmtList.filterKeep(new Filter<Statement>() {
													            public boolean accept(Statement stmt) {
													                Property p = stmt.getPredicate();
													                RDFNode o = stmt.getObject();
													                
													                for (String pToRemove : propertiesToRemove) {
													                	if (p.getURI().equalsIgnoreCase(pToRemove))
													                			return false;
													                }
													                	
													                if (o.isURIResource()) {
													                	for (String rToRemove : resourcesToRemove) {
														                	if (o.as(Resource.class).getURI().equalsIgnoreCase(rToRemove))
														                			return false;
													                	}
													                }
													                
													                return true;
													            }
												         });
            
		//lists all the properties for a given subject and add, for each property, the corresponding objects.
		while (stmtList.hasNext())
		{
						
			Statement tmp = stmtList.next();
			
			if(!propertiesList.containsKey(tmp.getPredicate()))
				propertiesList.put(tmp.getPredicate(), new HashSet<RDFNode>());
			
			propertiesList.get(tmp.getPredicate()).add(tmp.getObject());
		}
		
		Iterator<Entry<Property, Set<RDFNode>>> links = propertiesList.entrySet().iterator();
	    
		while (links.hasNext()) {
	        Entry<Property, Set<RDFNode>> pairs = links.next();
	        
	        linkValues = "";
	        for (RDFNode n : pairs.getValue())
			{
			
				if (n.canAs(Individual.class)) {
					Individual obj = n.as(Individual.class);
					linkValues += "\n{\"name\":\"" + Helpers.getLabel(obj, languageCode) + "\", ";
					linkValues += "\"id\":\"" + Helpers.getId(obj) + "\"}";
					
					linkValues += ",";
				}
				else if(n.isLiteral()) {
					linkValues += "\n{\"name\":\"" + n.toString() + "\", ";
					linkValues += "\"id\":\"literal\"}";
					
					linkValues += ",";
				}
			}
			
	        //linkValues is empty if all the objects are anonymous.
	        if (!linkValues.isEmpty()) {
		        
		        result += "{\n\"name\":\"" + pairs.getKey().getLocalName() + "\",\n";
				result += "\"id\":\"properties\",\n";
				result += "\"values\":[";
				
				result += linkValues.substring(0, linkValues.length() -1); //remove the last comma				
				
				result += "\n]\n},";
	        }

		}
				
		return result;
	}
}
