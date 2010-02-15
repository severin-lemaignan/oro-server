/**
 * 
 */
package laas.openrobots.ontology.backends;

public enum ResourceType {
	CLASS, 
	INSTANCE, 
	OBJECT_PROPERTY, 
	DATATYPE_PROPERTY, 
	UNDEFINED;
	
	/**
	 * Returns a ResourceType constant from its string representation, or {@link #UNDEFINED} if the string is not recognized.
	 * @param type the string representation of a ResourceType
	 * @return The corresponding ResourceType constant.
	 */
	public static ResourceType fromString(String type) {
		try {
			return Enum.valueOf(ResourceType.class, type.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return UNDEFINED;
		}
	}
}