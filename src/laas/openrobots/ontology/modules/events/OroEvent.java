package laas.openrobots.ontology.modules.events;

import java.util.Set;

public class OroEvent {

	Set<String> matchedId;
	boolean status = false;
	
	private IWatcher originalWatcher;
		
	public OroEvent(IWatcher originalWatcher, Set<String> matchedId) {
		super();
		this.matchedId = matchedId;
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
