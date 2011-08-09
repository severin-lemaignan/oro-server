package laas.openrobots.ontology.types;

public enum PolicyMethods {
	/**
	 * The statements are added to the knowledge base, without ensuring consistency
	 */
	ADD,
	
	/**
	 * The statements are added only if they (individually) do not lead to 
	 * inconsistencies
	 */
	SAFE_ADD,
	
	/**
	 * The statements are removed from the model
	 */
	RETRACT,
	
	/**
	 * Updates objects of one or several statements in the specified model. If 
	 * the predicate is not inferred to be functional (ie, it accept only one 
	 * single value), behaves like 'add'
	 */
	UPDATE,
	
	/**
	 * Updates objects of one or several statements in the specified model if it
	 *  does not (individually) lead to inconsistencies. If the predicate is not
	 *   inferred to be functional (ie, it accept only one single value), 
	 *   behaves like 'safe_add'
	 */
	REVISION,
	
	/**
	 * Synonym for 'revision'
	 */
	SAFE_UPDATE
}
