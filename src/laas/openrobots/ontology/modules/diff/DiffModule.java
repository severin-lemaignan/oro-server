package laas.openrobots.ontology.modules.diff;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;
import laas.openrobots.ontology.exceptions.NotComparableException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;

public class DiffModule {
	
	IOntologyBackend oro;
	
	Individual indivA;
	Individual indivB;
	OntModel modelA;
	OntModel modelB;
		
	public DiffModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
	}
	
	private void loadModels(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		if (Helpers.getType(conceptA) != Helpers.getType(conceptB)) throw new NotComparableException("Can not compare resources of different kind (a " + Helpers.getType(conceptA) + " and a " + Helpers.getType(conceptB) + " were provided).");
		
		//TODO: Implement a kind of comparison between resources different from individuals
		if (Helpers.getType(conceptA) != ResourceType.INSTANCE) throw new NotComparableException("Only the comparison between instances (individuals) is currently implemented");
		
		//We recreate two ontology models from details we get for each concepts.
		modelA = ModelFactory.createOntologyModel(oro.getModel().getSpecification(), oro.getSubmodel(conceptA));
		modelB = ModelFactory.createOntologyModel(oro.getModel().getSpecification(), oro.getSubmodel(conceptB));
		
		indivA = oro.getModel().getIndividual(conceptA.getURI());
		indivB = oro.getModel().getIndividual(conceptB.getURI());
	}
	
	/** Returns the classes that concept A and B have in common, as close as
	 * possible from concept A and B.
	 * 
	 * Algo principle:
	 * 
	 * We want S2 such as:
	 * S1 = superclasses(A) ∩ superclasses(B)
	 * S2 = { c / c ∈ S1 AND subclasses(c) ∩ S1 = ∅ }
	 * 
	 * @return the closest common ancestors of concept A and concept B
	 */
	private Set<OntClass> commonAncestors(){
			
		//get the list of types for concept A
		Set<OntClass> typesA = modelA.listNamedClasses().toSet();
		
		//get the list of types for concept B and remove the ones that are not in A
		Set<OntClass> commonAncestors = modelB.listNamedClasses().toSet();
		commonAncestors.retainAll(typesA);
		
		//commonAncestors shouldn't be empty: at least owl:Thing should be in common.
		assert(!commonAncestors.isEmpty());
		
		
		//**********************************************************************
		// Now, some heuristics to optimize the method in common cases.
		
		commonAncestors.remove(modelA.getResource(Namespaces.format("owl:Nothing")));
		
		if (commonAncestors.size() == 1) //owl:Thing
			return commonAncestors;
		
		commonAncestors.remove(modelA.getResource(Namespaces.format("owl:Thing")));
		
		
		//**********************************************************************
		
		Set<OntClass> result = new HashSet<OntClass>(commonAncestors);
		
		for (OntClass c : commonAncestors) {
			
			if (result.size() == 1)
				return result;
			
			//We need to retrieve the class c IN the main ontology model to be
			//able to get the subclasses.
			oro.getModel().enterCriticalSection(Lock.READ);
			OntClass classInOro = oro.getModel().getOntClass(c.getURI());
			oro.getModel().leaveCriticalSection();
			
			assert(c.equals(classInOro));
			
			Set<OntClass> subClassesC = oro.getSubclassesOf(classInOro, false);
			
			subClassesC.retainAll(commonAncestors);
			
			if (!subClassesC.isEmpty())
				result.remove(c);				
			
		}
		
		return result;
	}
		
	/***************** DIFFERENCES *********************/

	public Set<String> getDifferences(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<String> result = new HashSet<String>();
		
		loadModels(conceptA, conceptB);

		Set<OntClass> commonAncestors = commonAncestors();
		
		for (OntClass c : commonAncestors) {
			result.add(c.getLocalName());
		}
		
		return result;
	}

	public Set<String> getDifferences(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
		oro.getModel().enterCriticalSection(Lock.READ);
			OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
			OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		oro.getModel().leaveCriticalSection();
		
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getDifferences(resA, resB);
	}
	
	
	/***************** SIMILARITIES *********************/
	
	public Set<String> getSimilarities(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<String> result = new HashSet<String>();
		
		loadModels(conceptA, conceptB);

		Set<OntClass> commonAncestors = commonAncestors();
		
		for (OntClass c : commonAncestors) {
			result.add("? rdf:type " + c.getURI());
		}
		
		return result;
	}
	
	public Set<String> getSimilarities(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
		oro.getModel().enterCriticalSection(Lock.READ);
			OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
			OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		oro.getModel().leaveCriticalSection();
			
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getSimilarities(resA, resB);
	}
}
