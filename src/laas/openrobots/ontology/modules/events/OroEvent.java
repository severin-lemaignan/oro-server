package laas.openrobots.ontology.modules.events;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.helpers.Helpers;

import com.hp.hpl.jena.ontology.OntResource;

//TODO: rewrite (with subclasses?) this dumb OroEvent class.
public class OroEvent {

	private Set<String> matchedId;
	private boolean status = false;
	
	private IWatcher originalWatcher;
		
	public OroEvent(IWatcher originalWatcher, Set<OntResource> matchedId) {
		super();
		this.matchedId = new HashSet<String>();
		
		for (OntResource r : matchedId)
			this.matchedId.add(Helpers.getId(r));
		
		this.originalWatcher = originalWatcher;
	}
	
	public OroEvent(IWatcher originalWatcher, boolean status) {
		super();
		this.status = status;
		this.originalWatcher = originalWatcher;
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
	
	public IWatcher getWatcher() {
		
		return originalWatcher;
	}
}
