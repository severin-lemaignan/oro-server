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
 * NaiveNaturalLanguageModule plugin for ORO
 */
package laas.openrobots.ontology.modules.naturallanguage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;


import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.IModule;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;


/**
 * Implement a very simple natural language processor, based on regex.
 * 
 * It can currently handle these kinds of sentence:
 * <ul>
 * <li><tt>? [learn|remember|add] ? [that|:] {subject} {predicate} {object}</tt>: the 
 * module will add accordingly the statement in the knowledge base.</li>
 * <li><tt>? [learn|remember|add] ? [that|:] {concept_label} [type|is|are] {class_name}</tt>: asserts
 * that <tt>concept</tt> belongs to <tt>class</tt></li>
 * <li><tt>? [learn|remember|add] ? [that|:] {class_name}s are [a kind|kinds] of 
 * {superclass}</tt>: asserts that <tt>class</tt> is a subclass of <tt>superclass</tt></li>
 * <li><tt>[what|which] [{object_type}|] [do|does] {subject} {predicate}</tt>: will query 
 * the knowledge base and return the list of objects that matches <tt>{subject} 
 * {predicate} ?obj</tt> (plus <tt>?obj rdf:type {object_type}</tt> is it's defined).</li>
 * <li><tt>what is {object}?</tt>: will query the knowledge base and return the 
 * type of the object.</li>
 * <li><tt>{object_id} [has name|name is|is called] {object_name}</tt> or <tt>?? 
 * name of {object_id} is {object_name}</tt>: will add to the knowledge base a 
 * label to the concept.</li>
 * </ul>
 * 
 * The method will try its best (ie, invoke {@link IOntologyBackend#lookup(String, ResourceType)})
 * to reuse existing names and concept in the knowledge base.
 * 
 * @author Séverin Lemaignan <severin.lemaignan@laas.fr>
 *
 */
public class NaiveNaturalLanguageModule implements IModule, IServiceProvider {
	
	enum doCleaning {NO_CLEANING, CLEANING};

	IOntologyBackend oro;
	
	Pattern addPattern;
	Matcher addMatcher;
	
	Pattern whatPattern;
	Matcher whatMatcher;
	
	Pattern whatIsPattern;
	Matcher whatIsMatcher;
	
	Pattern nameIsPattern;
	Matcher nameIsMatcher;
	Pattern nameIsPattern2;
	Matcher nameIsMatcher2;
	
	Pattern whereIsPattern;
	Matcher whereIsMatcher;
	
	/**
	 * Initializes the plugin with the server instance of the knowledge storage.
	 */
	public NaiveNaturalLanguageModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
		
		//should match sentence like: Please remember that ice-creams are cold.
		addPattern = Pattern.compile(
				"[\\w,\\' ]*?(?:remember|add|learn)[\\w,\\' ]*?(?:that|:) ?([\\w ]+)", 
				Pattern.CASE_INSENSITIVE);
		
		addMatcher = addPattern.matcher("");
		
		//should match sentence like: Which objects does Toto like? or What do you see?
		whatPattern = Pattern.compile(
				"[Ww](?:hat|hich)(?: ([\\w]+))? do(?:es)? ([\\w]+) ([\\w]+) ?\\?");
		
		whatMatcher = whatPattern.matcher("");
		
		//should match sentence like: What is blue_bottle?
		whatIsPattern = Pattern.compile(
				"[Ww]hat is ([\\w]+) ?\\?");
		
		whatIsMatcher = whatIsPattern.matcher("");
		
		//should match sentence like: blue_bottle is called My bottle.
		nameIsPattern = Pattern.compile(
				"[\\w,\\' ]*?name of ([\\w]+) is ([\\w ]+)");
		nameIsPattern2 = Pattern.compile(
		"([\\w]+) [\\w,\\' ]*?(?:is called|has name|name is) ([\\w ]+)");
		
		nameIsMatcher = nameIsPattern.matcher("");
		nameIsMatcher2 = nameIsPattern2.matcher("");
		
		//should match sentence like: What is blue_bottle?
		whereIsPattern = Pattern.compile(
				"[Ww]here is ([\\w]+) ?\\?");
		
		whereIsMatcher = whereIsPattern.matcher("");
		
	}
	
	/**
	 * try to parse a natural language sentence
	 * 
	 * If it succeed and is understood, it will try execute the command or answer
	 * the question, an will return an natural answer.
	 * 
	 * @param sentence the sentence to parse, in natural language
	 * @return an answer in natural language if the sentence was understood, else
	 * an empty string.
	 */
	@RPCMethod (
			category = "natural language",
			desc = "try to parse a natural language sentence, and, if it succeed" +
					", execute and answer something meaningful."
			)
	public String processNL(String sentence) {
		
		String res;
		
		addMatcher.reset(sentence);
		whatMatcher.reset(sentence);
		whatIsMatcher.reset(sentence);
		nameIsMatcher.reset(sentence);
		nameIsMatcher2.reset(sentence);
		whereIsMatcher.reset(sentence);
		
		if (addMatcher.matches())
			res = handleAdd(addMatcher.group(1));
		else if (whatMatcher.matches())
			res = handleFind(whatMatcher.group(2), whatMatcher.group(3), whatMatcher.group(1));
		else if (whatIsMatcher.matches())
			res = handleGetType(whatIsMatcher.group(1));
		else if (nameIsMatcher.matches())
			res = handleAdd(nameIsMatcher.group(1) + " rdfs:label " + nameIsMatcher.group(2));
		else if (nameIsMatcher2.matches())
			res = handleAdd(nameIsMatcher2.group(1) + " rdfs:label " + nameIsMatcher2.group(2));
		else if (whereIsMatcher.matches())
			res = handleFind(nameIsMatcher2.group(1), " isAt " , nameIsMatcher2.group(2));
		else {
			Logger.log("Didn't understand the sentence.\n", VerboseLevel.VERBOSE);
			res = "";
		}
		
		return res;
	}
	
	private String handleGetType(String concept) {
		String res="";
		boolean foundSeveralConcepts = false;
		
		Logger.log("Trying to find out the type of " + concept + "\n", 
				VerboseLevel.VERBOSE);
		
		Set<String> ids = oro.lookup(concept, ResourceType.INSTANCE);
		
		if (ids.size() == 0) return "I don't know " + concept + "! :-(";
		
		if (ids.size() > 1) foundSeveralConcepts = true;
		
		
		for (OntClass c : oro.getClassesOf(oro.getResource(Helpers.pickRandom(ids)), true)) {
			res += Helpers.getLabel(c) + ", ";
		}
		
		//TODO: this formatting regex won't match - ' and other similar characters
		res = res.replaceAll("([\\w:\\-, ]+),([\\w:s\\- ]+), ", "$1 and$2, isn't it?");
		res = res.replaceFirst("(.*)(?:, )$", "A $1 I think.");
		
		if (foundSeveralConcepts) res += " But I must say I'm a bit confused " +
				"because I know several things that are called " + concept +"...";
		
		
		return res;
	}
	
	private String handleFind(String subj, String pred, String typeObj) {
		String res="";
		
		Logger.log("Processing a \"find\" command with subject: " + subj + 
				", predicate: " + pred + ((typeObj == null) ? "\n": (" and type of object: " + typeObj + "\n")), 
				VerboseLevel.VERBOSE);
		
		Set<PartialStatement> stmts = new HashSet<PartialStatement>();
		
		// Try to find is the subject of the query already exists
		if (subj.equalsIgnoreCase("you")) subj = "myself";
		else {
			Set<String> possibleSubj = oro.lookup(subj, ResourceType.INSTANCE);
			if (!possibleSubj.isEmpty())
				subj = Helpers.pickRandom(possibleSubj);
		}
		
		// Try to find is the subject of the query already exists
		
		//TODO Rather crude stemming :-)
		/*actually, I remove it because it's annoying for predicates like knowsAbout
		  if (!pred.endsWith("s") && !pred.equals("rdf:type") && !pred.equals("rdfs:subClassOf"))
			if (pred.matches("[aeiou]$")) pred = pred + "es";
			else pred = pred + "s";
		*/

		Set<String> possiblePred = oro.lookup(pred, ResourceType.OBJECT_PROPERTY);
		possiblePred.addAll(oro.lookup(pred, ResourceType.DATATYPE_PROPERTY));
		
		if (!possiblePred.isEmpty())
			pred = Helpers.pickRandom(possiblePred);
		
		
		// Try to find is the subject of the query already exists			
		if (typeObj != null) {
			
			//TODO: Rather crude lemming -> just remove trailing 's' or 'es'
			typeObj = typeObj.replaceAll("(es|s)$", "");
			
			Set<String> possibleObject = oro.lookup(typeObj, ResourceType.CLASS);
			
			if (!possibleObject.isEmpty())
				typeObj = Helpers.pickRandom(possibleObject);

		}
		
		try {
			stmts.add(oro.createPartialStatement(subj + " " + pred + " ?obj"));
			stmts.add(oro.createPartialStatement("?obj rdfs:label ?label"));
			
			if (typeObj != null)
				stmts.add(oro.createPartialStatement("?obj rdf:type " + typeObj));
			
			Logger.log("Sending expression " +
					stmts.toString() + "\n", VerboseLevel.DEBUG);
			
			//Try to get the labels
			//TODO prbl here: if 2 concepts matches but only one have a label, we will silently
			//miss the other.
			Set<RDFNode> rawResult = oro.find("label", stmts, null);
			//if (rawResult.isEmpty()) rawResult = oro.find("obj", stmts, null); 

			if (rawResult.isEmpty()) res = "Nothing!";
			else {
				for (RDFNode r : rawResult)					
					res += r.as(Resource.class).getLocalName() + ", ";
				
				//TODO: this formatting regex won't match - ' and other similar characters
				res = res.replaceAll("([\\w:\\-, ]+),([\\w:s\\- ]+), ", "$1 and$2, I think.");
				res = res.replaceFirst("(.*)(?:, )$", "Only $1.");
			}
			
		} catch (IllegalStatementException e) {
			res = "I think you asked me a question, but I didn't understand what" +
					" you were looking for...";
		} catch (InvalidQueryException e) {
			res = "I think you asked me a question, but I didn't understand what" +
			" you were looking for...";
		}
		
		return res;
	}

	private String handleAdd(String stmt) {
		
		String res="";
		
		//replace "you" by "myself"
		stmt = stmt.replaceFirst("you (\\w+) ", "myself $1s ");
		stmt = stmt.replaceFirst(" you", " myself");
		
		//replace "is kind of" by "rdfs:subClassOf"
		stmt = stmt.replaceFirst("([\\w]+)s are (?:a kind|kinds) of ([\\w]+)", "$1 rdfs:subClassOf $2");
		
		//replace "is/are" by "rdf:type"
		stmt = stmt.replaceFirst("([\\w]+) (?:type|is|am|are) ([\\w]+)", "$1 rdf:type $2");
		
		//put into quote the 3 parameters. 
		Matcher tokenizer= Pattern.compile("([\\w:\\-]+) ([\\w:\\-]+) ([\\w:\\- ]+)\\.?").matcher(stmt);
		tokenizer.matches();
		String object = "";
		Set<List<String>> lookup = oro.lookup(tokenizer.group(3));
		if (lookup.isEmpty()) {
			//if we don't know the object of the statement, we create a new object
			//with the given name as label.
			object = tokenizer.group(3).replace(' ', '_').toLowerCase();
			try {
				oro.add(oro.createStatement(object + " rdfs:label " + Helpers.protectValue(tokenizer.group(3))), 
						MemoryProfile.DEFAULT,
						false);
			} catch (IllegalStatementException e) {
				Logger.log("Couldn't add label \"" + tokenizer.group(3) + "\" " +
						"for object " + object + ". Continuing.", VerboseLevel.SERIOUS_ERROR);
			}
		}
		else
			object = Helpers.pickRandom(lookup).get(0);
			
		stmt = tokenizer.group(1) + " " + tokenizer.group(2) + " " + object;
		
		Logger.log("Processing a \"add\" command with statement [" +
				stmt + "]\n", VerboseLevel.VERBOSE);
		try {			
			oro.add(oro.createStatement(stmt), 
					MemoryProfile.DEFAULT,
					false);
			
			res = "Ok! I now know something new!";
		} catch (IllegalStatementException e) {
			res = "I couldn't add what you said: I only understand sentence" +
				" of type <subject> <predicate> <object>";
		}
		
		return res;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.modules.IModule#getServiceProvider()
	 */
	@Override
	public IServiceProvider getServiceProvider() {
		return this;
	}

	@Override
	public void step() {
	}

}

