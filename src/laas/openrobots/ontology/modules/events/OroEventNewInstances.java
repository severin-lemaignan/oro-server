package laas.openrobots.ontology.modules.events;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.helpers.Namespaces;

import com.hp.hpl.jena.ontology.OntResource;

public class OroEventNewInstances extends OroEventImpl {

	private Set<String> matchedId;
		
	public OroEventNewInstances(Set<OntResource> matchedId) {
		super();
		
		this.matchedId = new HashSet<String>();
		
		for (OntResource r : matchedId)
			this.matchedId.add(Namespaces.toLightString(r));

	}
	
	@Override
	public String getEventContext() {
		String res = "";
		for (String s : matchedId) {
			res += s = "\n";
		}
		
		return res;
	}

	public String getMatchingId() {
		if (matchedId.size() != 1) return null;
		
		String res = "";
		
		for (String s : matchedId)
			res = s;
		
		return res;
	}
	
	public Set<String> getMatchingIds() {
		
		return matchedId;
	}

}
