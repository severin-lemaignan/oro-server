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

import laas.openrobots.ontology.OroServer;

public class Logger {

	/**
	 * Outputs server standard messages.
	 * 
	 * Calls {@link #log(String, VerboseLevel)} with a {@link VerboseLevel#INFO}
	 * level of verbosity.
	 * 
	 * @param msg The message to display.
	 * @see #log(String, VerboseLevel)
	 */
	public static void log(String msg) {
		log(msg, VerboseLevel.INFO);
	}
	
	/**
	 * Outputs server messages, formatting them according to their importance.
	 * 
	 * @param msg The message to display.
	 * @param level The level of importance of the message.
	 * @see VerboseLevel The list of verbosity levels.
	 */
	public static void log(String msg, VerboseLevel level) {
		
		//Displays only message with a superior level of verbosity.
		if (level.ordinal() > OroServer.VERBOSITY.ordinal())
			return;
		
		switch(level) {
			case FATAL_ERROR:
			case SERIOUS_ERROR:				
				printInRed(OroServer.HAS_A_TTY ? msg : "[ERROR] " + msg);
				break;
				
			case ERROR:
				printInRed(msg);
				break;
			
			case WARNING:
				printInPurple(OroServer.HAS_A_TTY ? msg : "[WARNING] " + msg);
				break;
				
			case IMPORTANT:
				printInGreen(OroServer.HAS_A_TTY ? msg : "[!!] " + msg);
				break;
			
			case EMPHASIZE:
				printInRed(msg);
				break;
				
			case INFO:
				System.out.print(msg);
				break;
			
			case DEBUG:
			case VERBOSE:
				printInBlue("[DEBUG] " + msg);
				break;
		
		}
	}

	public static void cr(){
		System.out.print("\n");
	}
	
	public static void printInBlue(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[34m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}

	
	public static void printlnInBlue(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[34m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	
	public static void printlnInGreen(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[32m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	public static void printInGreen(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[32m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	
	public static void printInRed(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[31m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	
	public static void printlnInRed(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[31m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	
	public static void printlnInPurple(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[35m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
	
	public static void printInPurple(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[35m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[0m");
	}
}
