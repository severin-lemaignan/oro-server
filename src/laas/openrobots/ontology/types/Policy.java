package laas.openrobots.ontology.types;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.exceptions.InvalidPolicyException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.json.JSONArray;
import laas.openrobots.ontology.json.JSONException;
import laas.openrobots.ontology.json.JSONObject;
import laas.openrobots.ontology.modules.memory.MemoryProfile;

public class Policy {

	/**
	 * Revision method
	 */
	private PolicyMethods method;
	
	/**
	 * List of the models to be impacted by the revision.
	 * 
	 * An empty set bears the special meaning "all models"
	 */
	private Set<String> models;
	
	private MemoryProfile memoryProfile;
	
	public static final Policy ADD_TO_MYSELF;
	public static final Policy SAFE_ADD_TO_MYSELF;
	public static final Policy RETRACT_FROM_MYSELF;
	public static final Policy UPDATE_MYSELF;
	public static final Policy REVISE_MYSELF;
	
	public static final Policy ADD_TO_ALL = new Policy(PolicyMethods.ADD, new HashSet<String>());
	public static final Policy SAFE_ADD_TO_ALL = new Policy(PolicyMethods.SAFE_ADD, new HashSet<String>());
	public static final Policy RETRACT_FROM_ALL = new Policy(PolicyMethods.RETRACT, new HashSet<String>());
	public static final Policy UPDATE_ALL = new Policy(PolicyMethods.UPDATE, new HashSet<String>());
	public static final Policy REVISE_ALL = new Policy(PolicyMethods.REVISION, new HashSet<String>());
	
	static {
		Set<String> myself = new HashSet<String>();
		myself.add("default");
		ADD_TO_MYSELF = new Policy(PolicyMethods.ADD, myself);
		SAFE_ADD_TO_MYSELF = new Policy(PolicyMethods.SAFE_ADD, myself);
		RETRACT_FROM_MYSELF = new Policy(PolicyMethods.RETRACT, myself);
		UPDATE_MYSELF = new Policy(PolicyMethods.UPDATE, myself);
		REVISE_MYSELF = new Policy(PolicyMethods.REVISION, myself);
	}
	
	public static final Policy DEFAULT = Policy.ADD_TO_ALL;
	
	public Policy(JSONObject policy) throws InvalidPolicyException {
		
		models = new HashSet<String>();
		
		try {
			method = PolicyMethods.valueOf(policy.getString("method").toUpperCase());
		} catch (JSONException e) {
			throw new InvalidPolicyException("No method field specified, or unknown method");
		}
		
		JSONArray m = policy.optJSONArray("models");
		
		if (m == null) {
			//Do we have only one value?
			String model = policy.optString("models");
			
			if (model == null || model.equals("all")) { 
				// Not specifying the model is equivalent to 'all'
				models.add("myself");
			}
			else models.add(model);
		}
		else {
			for (int i = 0; i < m.length(); i++) {
				try {
					models.add(m.getString(i));
				} catch (JSONException e) {
					Logger.log("Incorrect policy: got invalid value in the model list!", VerboseLevel.ERROR);
				}
			}
		}
	
		String mem = policy.optString("memory_profile");
		if (mem == null || mem.isEmpty()) memoryProfile = MemoryProfile.DEFAULT;
		else memoryProfile = MemoryProfile.valueOf(mem.toUpperCase());			
	}
	
	public Policy(PolicyMethods method, Set<String> models) {
		this(method, models, MemoryProfile.DEFAULT);
	}
	
	public Policy(PolicyMethods method, Set<String> models, MemoryProfile memoryProfile) {
		this.method = method;
		this.models = models;
		this.memoryProfile = memoryProfile;
	}

	public PolicyMethods getMethod() {
		return method;
	}
	
	public Set<String> getModels() {
		return models;
	}	

	public MemoryProfile getMemoryProfile() {
		return memoryProfile;
	}
}
