/**  
 * Represents the different available profile of memory.
 */
package laas.openrobots.ontology.memory;

/**
 * @author slemaign
 *
 */
public enum MemoryProfile {
	/**
	 * This represents short term memory (or working memory). A statement added in short term memory is held for 10 seconds.
	 */
	SHORTTERM (10),
	/**
	 * This represents episodic memory (memory of personal experience). A statement added in episodic memory is held for 5 minutes.
	 */
	EPISODIC (300),
	/**
	 * This represents long term memory. A statement added in long term memory is never forgotten.
	 */
	LONGTERM (-1),
	/**
	 * Default memory profile is equivalent to the long term memory profile.
	 * @see MemoryProfile.LONGTERM
	 */
	DEFAULT (-1);
	
	/**
	 * The time base is how ling a second lasts in milliseconds.<br/>
	 * The default value is obviously 1000, but you can alter this value to accelerate (or slow down) the behaviour of the memory storage.<br/>
	 * 
	 * For instance, if you set {@code timeBase = 500}, the actual duration a short term statement is divided by to (ie 5 seconds instead of the default 10 seconds)
	 * 
	 */
	public static int timeBase = 1000;
	
	private final int duration_seconds; //in seconds
	
	MemoryProfile( int duration_seconds) {
		this.duration_seconds = duration_seconds;
	}
	
	/**
	 * Returns the lifespan associated to the memory profile, in milliseconds.
	 * @return the lifespan associated to the memory profile, in milliseconds
	 */
	public int duration(){return duration_seconds * timeBase;}
	
	/**
	 * Returns a MemoryProfile constant from its string representation, or {@link #DEFAULT} if the string is not recognized.
	 * @param literalMemProfile the string representation of a MemoryProfile
	 * @return The corresponding MemoryProfile constant.
	 */
	public static MemoryProfile fromString(String literalMemProfile) {
		try {
			return Enum.valueOf(MemoryProfile.class, literalMemProfile);
		} catch (IllegalArgumentException e) {
			return DEFAULT;
		}
	}
	
}
