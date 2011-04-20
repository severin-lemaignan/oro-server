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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;

public class Logger {

	public enum LockType {ACQUIRE_READ, ACQUIRE_WRITE, RELEASE_READ, RELEASE_WRITE};
	
    static private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    
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
	 * Outputs server standard messages.
	 * 
	 * Calls {@link #log(String, VerboseLevel, boolean)} with a {@link VerboseLevel#INFO}
	 * level of verbosity.
	 * 
	 * @param msg The message to display.
	 * @param withPrefix Set it to false when a looging info is expected to
	 * continue the last line of log and you don't want a prefix (like the date) 
	 * to be added.
	 * @see #log(String, VerboseLevel)
	 */
	public static void log(String msg, boolean withPrefix) {
		log(msg, VerboseLevel.INFO, withPrefix);
	}
	
	/**
	 * Outputs server messages, formatting them according to their importance.
	 * 
	 * @param msg The message to display.
	 * @param level The level of importance of the message.
	 * @see VerboseLevel The list of verbosity levels.
	 */
	public static void log(String msg, VerboseLevel level) {
		log(msg, level, true);
	}
		
	/**
	 * Outputs server messages, formatting them according to their importance.
	 * 
	 * @param msg The message to display.
	 * @param level The level of importance of the message.
	 * @param withPrefix Set it to false when a looging info is expected to
	 * continue the last line of log and you don't want a prefix (like the date) 
	 * to be added.
	 * @see VerboseLevel The list of verbosity levels.
	 */
	public static synchronized void log(String msg, VerboseLevel level, boolean withPrefix) {
		
		//Displays only message with a superior level of verbosity.
		if (!verbosityMin(level))
			return;
		
		String prefix = "";
		if (withPrefix)
			 prefix = "[" + sdf.format(Calendar.getInstance().getTime()) + "] ";
		
	    
		switch(level) {
			case FATAL_ERROR:
			case SERIOUS_ERROR:				
				if (!OroServer.BLINGBLING)
					printInRed(prefix + (OroServer.HAS_A_TTY ? msg : "[ERROR] " + msg));
				else
					printInRed("U looser! :P " + msg);
				break;
				
			case ERROR:
				if (!OroServer.BLINGBLING)
					printInRed(prefix + msg);
				else {
					blingblingPower();
					printInRed(msg + " lol!!");
				}
			
				break;
			
			case WARNING:
				if (!OroServer.BLINGBLING)
					printInPurple(prefix + (OroServer.HAS_A_TTY ? msg : "[WARNING] " + msg));
				else {
					blingblingPower();
					printInPurple("(°_°) " + msg);
				}
				break;
				
			case IMPORTANT:
				if (!OroServer.BLINGBLING)
						printInGreen(prefix + (OroServer.HAS_A_TTY ? msg : "[!!] " + msg));
				else {
					blingblingPower();
					printInGreen("(^_^) " + msg);
				}
				
				break;
			
			case EMPHASIZE:
				printInRed(prefix + msg);
				break;
				
			case INFO:
				System.out.print(prefix + msg);
				break;
			
			case VERBOSE:
			case DEBUG:
			case DEBUG_CONCURRENCY:
				if (withPrefix)
					 prefix += "[DEBUG] ";
				printInBlue(prefix + msg);
				break;
		
		}
	}
	
	/**
	 * Tests if a given level of verbosity is superior or egal to the current,
	 * application-wide, level of verbosity.
	 * 
	 * @param level A level of verbosity
	 * @return true if the server is configured to be at least as verbose as the
	 * given level of verbosity.
	 */
	public static boolean verbosityMin(VerboseLevel level) {
		if (level.ordinal() > OroServer.VERBOSITY.ordinal())
			return false;
		return true;
	}
	
	public static void logConcurrency(LockType type) {
		logConcurrency(type, "");
	}
		
	public static void logConcurrency(LockType type, String info) {
		if (Logger.verbosityMin(VerboseLevel.DEBUG_CONCURRENCY)) {
			
			String msg = "";
			switch (type) {
				case ACQUIRE_READ:
					msg = "enterCS";
				case ACQUIRE_WRITE:
					msg = "enterCSw";
				case RELEASE_READ:
					msg = "leaveCS";
				case RELEASE_WRITE:
					msg = "leaveCSw";
			}
			
			if(!info.isEmpty()) info = " (" + info + ")";
			
			Logger.log(">>" + msg + info + ": " + 
					Thread.currentThread().getStackTrace()[3].getMethodName() + 
					" -> " + Thread.currentThread().getStackTrace()[2].getMethodName() + "\n", 
					VerboseLevel.DEBUG_CONCURRENCY, false);
		}
	}

	public static void cr(){
		if (OroServer.VERBOSITY != VerboseLevel.SILENT)
			System.out.print('\n');
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

	public static void info(String msg) {
		log(msg, VerboseLevel.INFO);
	}
	
	public static void debug(String msg) {
		log(msg, VerboseLevel.DEBUG);
	}
	
	private static void makeUpStatement(Statement stmt) {
		
		printInGreen(Namespaces.toLightString(stmt.getSubject()) + " ");
		printInBlue(Namespaces.toLightString(stmt.getPredicate()) + " ");
		printInPurple(Namespaces.toLightString(stmt.getObject()));
	}
	
	private static void makeUpPartialStatement(Statement stmt) {
		if (stmt.getSubject() == null)
			printInRed("* ");
		else {
			printInGreen(Namespaces.toLightString(stmt.getSubject()) + " ");
		}
		
		if (stmt.getPredicate() == null)
			printInRed("* ");
		else {
			printInBlue(Namespaces.toLightString(stmt.getPredicate()) + " ");
		}
		
		if (stmt.getObject() == null)
			printInRed("*");
		else {
			printInPurple(Namespaces.toLightString(stmt.getObject()));
		}
	}
	
	public static <E extends Statement> void demo(String string, Set<E> stmts) {
		
		if (!OroServer.DEMO_MODE) return;
		
		System.out.println(string);
		for (E stmt : stmts) {
			System.out.print("\t[");
			
			if (stmt.getSubject() == null ||
					stmt.getPredicate() == null ||
					stmt.getObject() == null)
					makeUpPartialStatement(stmt);
			else
					makeUpStatement(stmt);
			System.out.print("]\n");
		}
		
	}

	public static void demo(String string, Statement stmt) {
		
		if (!OroServer.DEMO_MODE) return;
		
		System.out.print(string + " ");
		makeUpStatement(stmt);		
	}
	
	public static void demo(String string, PartialStatement stmt) {
		
		if (!OroServer.DEMO_MODE) return;
		
		System.out.print(string + " ");
		makeUpStatement(stmt);		
	}

	public static void demo(String label, String value, boolean ok) {
		
		if (!OroServer.DEMO_MODE) return;
		
		System.out.print(label + " ");
			if (ok)
				printInGreen(value);
			else
				printInRed(value);
		
	}

	public static <E extends RDFNode> void demo_nodes(String string, Set<E> nodes) {
		
		if (!OroServer.DEMO_MODE) return;
		
		System.out.print(string + ": [");
		for (E node : nodes)
			Namespaces.toLightString(node);
		
		System.out.print("]\n");
		
	}
}
