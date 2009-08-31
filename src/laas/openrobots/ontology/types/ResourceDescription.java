package laas.openrobots.ontology.types;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public class ResourceDescription implements JsonSerializable {

	private OntResource resource;
			
	public ResourceDescription(OntResource resource){
		this.resource = resource;
	}
	
	@Override
	public String getJson() {
		
		ResourceType type = Helpers.getType(resource);
		
		String result = "{\n";
		
			result += "\"name\":\"" + Helpers.getLabel(resource) + "\",\n";
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
					result += "\"name\":\"Class\",\n";
					result += "\"id\":\"class\",\n";
					result += "\"values\":[";
					result += listOntResource(resource.asIndividual().listOntClasses(true));
					result += "\n]\n";
					
				result +="},{\n";
					
					//link with other resources
					result += "\"name\":\"Links\",\n";
					result += "\"id\":\"links\",\n";
					result += "\"values\":[";
					
					//Retrieve the list of statements involving this individual, and extract the objects.
					/*ExtendedIterator<RDFNode> otherIndividuals = resource.listProperties().mapWith( new Map1<Statement, RDFNode>() {
																		                        public RDFNode map1( Statement s ) {
																		                            return s.getObject();
																		                        }}
																		                      );			
					*/
					result += listLinks(resource.listProperties());
					result += "\n]\n";
					
				result +="}\n";
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
			result += "\n{\"name\":\"" + Helpers.getLabel(tmp) + "\", ";
			result += "\"id\":\"" + Helpers.getId(tmp) + "\"}";
			
			if (it.hasNext()) result += ",";
		}
		
		return result;
	}
	
	private String listLinks(ExtendedIterator<Statement> it) {
		String result = "";
		
		while (it.hasNext())
		{
			Statement tmp = it.next();
			if (tmp.getObject().canAs(Individual.class)) {
				Individual obj = tmp.getObject().as(Individual.class);
				result += "\n{\"name\":\"" + Helpers.getLabel(obj) + "\", ";
				result += "\"id\":\"" + Helpers.getId(obj) + "\", ";
				result += "\"link\":\"" + tmp.getPredicate().getLocalName() + "\"}";
				
				if (it.hasNext()) result += ",";
			}
			else if(tmp.getObject().isLiteral()) {
				result += "\n{\"name\":\"literal\", ";
				result += "\"value\":\"" + tmp.getObject().toString() + "\", ";
				result += "\"id\":\"" + tmp.getObject().toString() + "\", ";
				result += "\"link\":\"" + tmp.getPredicate().getLocalName() + "\"}";
				
				if (it.hasNext()) result += ",";
			}
			
			// do nothing for anonymous objects.
		}
		
		return result;
	}
}
