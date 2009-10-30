/*
 * ©LAAS-CNRS (2008-2010)
 * 
 * contributor(s) : Séverin Lemaignan <severin.lemaignan@laas.fr>
 * 
 * This software is a computer program whose purpose is to interface
 * with an ontology in a robotics context.
 * 
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 * 
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
	DEBUG;
}
