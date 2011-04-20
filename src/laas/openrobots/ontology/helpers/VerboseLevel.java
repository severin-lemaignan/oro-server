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

package laas.openrobots.ontology.helpers;

/**
 * This enum defines the various possible verbosity levels for server messages.
 * Theses values can be used in the code (with the {@link Logger#log(String, VerboseLevel)}
 * method in particular) of in the configuration file.
 * 
 * Note that the order of the enum constant is important since for a given level
 * of verbosity, all messages with a superior level of verbosity will be displayed
 * as well.
 *  
 * @author slemaign
 *
 */
public enum VerboseLevel {

	/**
	 * Only for use in the config file. Indicates that nothing must be displayed.
	 */
	SILENT,
	
	/**
	 * Error that trigger the direct interuption of the application
	 */
	FATAL_ERROR, 
	
	/**
	 * Error that "shouldn't happen" but the server can live with.
	 */
	SERIOUS_ERROR, 
	
	/**
	 * "Normal" error, that will be reported to the clients
	 */
	ERROR, 

	/**
	 * Not an error, but an important information that can alter the way the server works.
	 */
	WARNING, 
	
	/**
	 * Important information that should be emphasized in a log
	 */
	IMPORTANT,
	
	/**
	 * Emphasized standard information
	 */
	EMPHASIZE,
	
	/**
	 * Standard information
	 */
	INFO,
	
	/**
	 * Important debug info
	 */
	VERBOSE,
	
	/**
	 * Not very important debug info.
	 */
	DEBUG,
	
	/**
	 * Very verbose debug information, used to track concurrency errors.
	 */
	DEBUG_CONCURRENCY;
}
