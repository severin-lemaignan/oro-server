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
				if (!OroServer.BLINGBLING)
					printInRed(OroServer.HAS_A_TTY ? msg : "[ERROR] " + msg);
				else
					printInRed("U looser! :P " + msg);
				break;
				
			case ERROR:
				if (!OroServer.BLINGBLING)
					printInRed(msg);
				else {
					blingblingPower();
					printInRed(msg + " lol!!");
				}
			
				break;
			
			case WARNING:
				if (!OroServer.BLINGBLING)
					printInPurple(OroServer.HAS_A_TTY ? msg : "[WARNING] " + msg);
				else {
					blingblingPower();
					printInPurple("(°_°) " + msg);
				}
				break;
				
			case IMPORTANT:
				if (!OroServer.BLINGBLING)
						printInGreen(OroServer.HAS_A_TTY ? msg : "[!!] " + msg);
				else {
					blingblingPower();
					printInGreen("(^_^) " + msg);
				}
				
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
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}

	
	public static void printlnInBlue(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[34m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void printlnInGreen(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[32m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	public static void printInGreen(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[32m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void printInRed(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[31m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void printlnInRed(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[31m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void printlnInPurple(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[35m");
		System.out.println (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void printInPurple(String msg){
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[35m");
		System.out.print (msg);
		if (OroServer.HAS_A_TTY) System.out.print ((char)27 + "[39m");
	}
	
	public static void blingblingPower(){
		if (!OroServer.HAS_A_TTY) return;
		
		for (int i = 0; i < 10 ; i++) {
			long x = Math.round(Math.random() * 80);
			long y = Math.round(Math.random() * 40);
			long col = Math.round(Math.random() * 6) + 1;
			
			//save cursor position
			System.out.print((char)27 + "[s");
			//move cursor
			System.out.print((char)27 + "[" + y + ";" + x + "H");
			System.out.print((char)27 + "[3" + col + ";5m");
			printAsciiStar();
			System.out.print((char)27 + "[39;25m");
			//restore cursor position
			System.out.print((char)27 + "[u");
			
		}
	}
	
	private static void printAsciiStar(){
		System.out.print((char)27 + "[2D");		
		System.out.print(" <o> ");
		System.out.print((char)27 + "[4D" + (char)27 + "[1A");		
		System.out.print(" | ");
		System.out.print((char)27 + "[3D" + (char)27 + "[2B");		
		System.out.print(" | ");		
		System.out.print((char)27 + "[3D");		
	}
}
