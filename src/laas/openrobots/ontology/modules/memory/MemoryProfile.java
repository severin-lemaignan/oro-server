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

/**  
 * Represents the different available profile of memory.
 */
package laas.openrobots.ontology.modules.memory;

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
	 * @see MemoryProfile#LONGTERM
	 */
	DEFAULT (-1);
	
	/**
	 * The time base states "how long a second lasts in milliseconds".<br/>
	 * The default value is obviously 1000, but you can alter this value to 
	 * accelerate (or slow down) the behaviour of the memory storage.<br/>
	 * 
	 * For instance, if you set {@code timeBase = 500}, the actual duration a 
	 * short term statement is divided by 2 (ie 5 seconds instead of the default
	 *  10 seconds)
	 * 
	 */
	public static int TimeBase = 1000;
	
	private final int duration_seconds; //in seconds
	
	MemoryProfile( int duration_seconds) {
		this.duration_seconds = duration_seconds;
	}
	
	/**
	 * Returns the lifespan associated to the memory profile, in milliseconds, 
	 * scaled with the TimeBase.
	 * 
	 * @return the lifespan associated to the memory profile, in milliseconds
	 * @see TimeBase
	 */
	public int duration(){return duration_seconds * TimeBase;}
	
	/**
	 * Returns a MemoryProfile constant from its string representation, or {@link #DEFAULT} if the string is not recognized.
	 * @param literalMemProfile the string representation of a MemoryProfile
	 * @return The corresponding MemoryProfile constant.
	 */
	public static MemoryProfile fromString(String literalMemProfile) {
		try {
			return Enum.valueOf(MemoryProfile.class, literalMemProfile.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return DEFAULT;
		}
	}
	
}
