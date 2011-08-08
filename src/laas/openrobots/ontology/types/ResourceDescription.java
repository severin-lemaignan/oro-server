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

package laas.openrobots.ontology.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.helpers.Helpers;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;


public class ResourceDescription implements Serializable {

	private final String propertiesToRemove[] = {	
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", 
			"http://www.w3.org/2002/07/owl#sameAs", 
			"http://www.w3.org/2002/07/owl#differentFrom",
			"http://www.w3.org/2002/07/owl#topDataProperty",
			"http://www.w3.org/2002/07/owl#bottomDataProperty",
			"http://www.w3.org/2000/01/rdf-schema#label"};
	
	private final String resourcesToRemove[] = {
			"http://www.w3.org/2002/07/owl#Nothing"};
    
	private String languageCode = "en"; //default language for labels set to English
	private OntResource resource;
			
	public ResourceDescription(OntResource resource){
		this.resource = resource;
	}
	
	public ResourceDescription(OntResource resource, String languageCode){
		this.resource = resource;
		this.languageCode = languageCode;
	}

	@Override
	public String toString() {
		
		ResourceType type = Helpers.getType(resource);
		
		String result = "{";
		
			result += "\"name\":\"" + Helpers.getLabel(resource, languageCode) + "\",";
			result += "\"id\":\"" + Helpers.getId(resource) + "\",";
			result += "\"type\":\"" + type.toString().toLowerCase() + "\",";
			result += "\"sameAs\":[";
			
			if (type == ResourceType.CLASS){
				// equivalent classes
				result += listOntResource(resource.asClass().listEquivalentClasses().filterDrop(
						new Filter<OntClass>() {
							@Override
				            public boolean accept(OntClass c) {
				                if (c.isAnon() || c.getURI().equalsIgnoreCase(resource.getURI()))                
				                	return true;
				                return false;
				            }
						}
				), true);
			}
			else if (type == ResourceType.INSTANCE){
				result += listOntResource(resource.asIndividual().listSameAs().filterDrop(
						new Filter() {
							@Override
				            public boolean accept(Object c) {
				                if (((OntResource)c).isAnon() || ((OntResource)c).getURI().equalsIgnoreCase(resource.getURI()))                
				                	return true;
				                return false;
				            }
						}
				), true);
			}
			
			result += "],";			
			result += "\"attributes\": [";
			

			if (type == ResourceType.CLASS){
				
				result +="{";
				
					//(direct) super classes
					result += "\"name\":\"Parents\",";
					result += "\"id\":\"superClasses\",";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listSuperClasses(true));
					result += "]";
				
				result +="},{";
				
					//(direct) subclasses
					result += "\"name\":\"Children\",";
					result += "\"id\":\"subClasses\",";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listSubClasses(true));
					result += "]";

				result +="},{";
				
					//(direct) instances
					result += "\"name\":\"Instances\",";
					result += "\"id\":\"instances\",";
					result += "\"values\":[";
					result += listOntResource(resource.asClass().listInstances(true));
					result += "]";
				
				result +="}";
			}
			
			else if (type == ResourceType.INSTANCE){
				
				result +="{";
				
					//(direct) super classes
					result += "\"name\":\"Classes\",";
					result += "\"id\":\"classes\",";
					result += "\"values\":[";
					result += listOntResource(resource.asIndividual().listOntClasses(true));
					result += "]";
					
				result +="},";
					
				//link with other resources
				result += listLinks(resource.listProperties());
				
				result = result.substring(0, result.length() -1); //remove the last comma

			}
			
			
			
			result += "]";
		result += "}";
		
		return result;
	}
	
	private String listOntResource(ExtendedIterator<? extends Resource> it) {
		return listOntResource(it, false);
	}
	
	private String listOntResource(ExtendedIterator<? extends Resource> it, boolean only_id) {
		String result = "";
		
		while (it.hasNext())
		{
			OntResource tmp = (OntResource) it.next(); // A bit dangerous. Did it because of 'listSameAs' that returns an iterator on resources that are said to be OntResource by the doc
			
			if (!tmp.isAnon()) {
				for (String rToRemove : resourcesToRemove) {
                	if (!tmp.getURI().equalsIgnoreCase(rToRemove)) {
                		if (only_id) {
                			result += "\"" + Helpers.getId(tmp) + "\"";
                		}
                		else {
                			result += "{\"name\":\"" + Helpers.getLabel(tmp, languageCode) + "\", ";
                			result += "\"id\":\"" + Helpers.getId(tmp) + "\"}";
                		}
    					if (it.hasNext()) result += ",";
                	}
            	}
				
			}			
			
		}
		
		return result;
	}
		
	/**
	 * This method scans all the given statements and build a list of present 
	 * predicate with their associated objects. This list is then output as a 
	 * JSON string.
	 * 
	 * @param rawStmtList
	 * @return a JSON string describing, for each property, all the linked resources.
	 */
	private String listLinks(ExtendedIterator<Statement> rawStmtList) {
		String linkValues = "", result = "";
		
		Map<Property, Set<RDFNode>> propertiesList = new HashMap<Property, Set<RDFNode>>();
		
		//Clean a bit the list of statements.	
		ExtendedIterator<Statement> stmtList = rawStmtList.filterKeep(
				new Filter<Statement>() {
			            @Override
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
		
		//Convert it to a string.
		Iterator<Entry<Property, Set<RDFNode>>> links = propertiesList.entrySet().iterator();
	    
		while (links.hasNext()) {
	        Entry<Property, Set<RDFNode>> pairs = links.next();
	        
	        linkValues = "";
	        for (RDFNode n : pairs.getValue())
			{
			
				if (n.canAs(Individual.class)) {
					Individual obj = n.as(Individual.class);
					linkValues += "{\"name\":\"" + Helpers.getLabel(obj, languageCode) + "\", ";
					linkValues += "\"id\":\"" + Helpers.getId(obj) + "\"}";
					
					linkValues += ",";
				}
				else if(n.isLiteral()) {
					linkValues += "{\"name\":\"" + n.toString().split("\\^\\^")[0] + "\", ";
					linkValues += "\"id\":\"literal\"}";
					
					linkValues += ",";
				}
			}
			
	        //linkValues is empty if all the objects are anonymous.
	        if (!linkValues.isEmpty()) {
		        
		        result += "{\"name\":\"" + pairs.getKey().getLocalName() + "\",";
				result += "\"id\":\"properties\",";
				result += "\"values\":[";
				
				result += linkValues.substring(0, linkValues.length() -1); //remove the last comma				
				
				result += "]},";
	        }

		}
				
		return result;
	}
}
