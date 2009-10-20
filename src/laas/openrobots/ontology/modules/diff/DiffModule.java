package laas.openrobots.ontology.modules.diff;

import java.util.Set;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;
import laas.openrobots.ontology.exceptions.NotComparableException;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.shared.NotFoundException;

public class DiffModule {
	
	IOntologyBackend oro;
		
	public DiffModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
	}
	
	
	/***************** DIFFERENCES *********************/

	public Set<String> getDifferences(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		if (Helpers.getType(conceptA) != Helpers.getType(conceptB)) throw new NotComparableException("Can not compare resources of different kind (a " + Helpers.getType(conceptA) + " and a " + Helpers.getType(conceptB) + " were provided).");
		
		//TODO: Implement a kind of comparison between resources different from individuals
		if (Helpers.getType(conceptA) != ResourceType.INSTANCE) throw new NotComparableException("Only the comparison between instances (individuals) is currently implemented");
		
		
		return null;
	}

	public Set<String> getDifferences(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
		OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getDifferences(resA, resB);
	}
	
	
	/***************** SIMILARITIES *********************/
	
	public Set<String> getSimilarities(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		if (Helpers.getType(conceptA) != Helpers.getType(conceptB)) throw new NotComparableException("Can not compare resources of different kind (a " + Helpers.getType(conceptA) + " and a " + Helpers.getType(conceptB) + " were provided).");
		
		//TODO: Implement a kind of comparison between resources different from individuals
		if (Helpers.getType(conceptA) != ResourceType.INSTANCE) throw new NotComparableException("Only the comparison between instances (individuals) is currently implemented");
		
		return null;
	}
	
	public Set<String> getSimilarities(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
		OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getSimilarities(resA, resB);
	}
}
