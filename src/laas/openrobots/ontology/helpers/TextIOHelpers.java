package laas.openrobots.ontology.helpers;

import laas.openrobots.ontology.OroServer;

public class TextIOHelpers {


	
	public static void log(String msg) {
		log(msg, VerboseLevel.INFO);
	}
	
	public static void log(String msg, VerboseLevel level) {
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
