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

package laas.openrobots.ontology;

import java.util.ArrayList;
import java.util.List;

import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Namespaces;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Alt;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceF;
import com.hp.hpl.jena.rdf.model.Seq;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;

/** A partial statement is a statement whose at least one element (subject, predicate or object) is unknown.</br>
 * 
 * To be valid, a partial statement must have at least one variable, prepended with a "?". For instance: {@code "?mysterious oro:objProperty oro:individual"}
 *
 */
public class PartialStatement implements Statement {

	private List<String> stmtTokens;
	
	private StatementImpl baseStmt;
	
	/** Creates a partial statement from a partial triplet (subject, predicate, object).
	 * 
	 * At least one of the parameter is assumed to be null.
	 * 
	 * @param subject The subject of the statement
	 * @param predicate The predicate of the statement
	 * @param object The object of the statement
	 * @param model The ontology this partial statement refers to.
	 */
	public PartialStatement(Resource subject, Property predicate, RDFNode object, ModelCom model) {
		
		stmtTokens = new ArrayList<String>();
		
		if (subject == null) {
			stmtTokens.add("?subject");
			subject = model.createResource("nullSubject");
		}
		else
			stmtTokens.add(Namespaces.toLightString(subject));
		
		if (predicate == null) {
			stmtTokens.add("?predicate");
			predicate = model.createProperty("nullPredicate");
		}
		else
			stmtTokens.add(Namespaces.toLightString(predicate));
		
		if (object == null) {
			stmtTokens.add("?object");
			object = model.createResource("nullObject");
		}
		else
			stmtTokens.add(Namespaces.toLightString(object));
		
		baseStmt = new StatementImpl(subject, predicate, object, model);
	}
	
	/**
	 * Create a new partial statement from its string representation.<br/>
	 * Works as {@link OpenRobotsOntology#createStatement(String)} except at least one variable, prepended with a "?", is expected.</br>
	 * 
	 * This class implements {@link Statement}, but the {@link #getSubject()}, {@link #getPredicate()} and {@link #getObject()} method will return {@code null} if the corresponding part of the statement is unbounded. 
	 * 
	 * @param partialStatement The string representing the partial statement. For example, {@code "?mysterious oro:objProperty2 ?object"} or {@code "?subject oro:dataProperty1 true"} are valid.
	 * @param model The ontology this partial statement refers to.
	 * @throws IllegalStatementException Currently thrown only if the statement doesn't contain three tokens.
	 * @see OpenRobotsOntology#createStatement(String) Syntax details
	 */
	public PartialStatement(String partialStatement, ModelCom model) throws IllegalStatementException {
				
		Resource subject = null;
		Property predicate = null;
		RDFNode object = null;
		
		stmtTokens = Helpers.tokenize(partialStatement.trim(), ' ');
		
		if (stmtTokens.size() != 3)
			throw new IllegalStatementException("Three tokens are expected in a partial statement, " + stmtTokens.size() + " found in " + partialStatement + ".");
		
				
		//checks that at least one token starts with a "?".
		if (!((stmtTokens.get(0).length() > 0 && stmtTokens.get(0).charAt(0) == '?') || (stmtTokens.get(1).length() > 0 && stmtTokens.get(1).charAt(0) == '?') || (stmtTokens.get(2).length() > 0 && stmtTokens.get(2).charAt(0) == '?')) )
			throw new IllegalStatementException("At least one token should be marked as unbounded (starting with a \"?\").");
		
		if (stmtTokens.get(0).length() == 0 || stmtTokens.get(0).charAt(0) != '?')
			subject = model.getResource(Namespaces.format(stmtTokens.get(0)));
		else
			subject = model.createResource("nullSubject"); //if the subject is unbounded (a variable), creates an "nullSubject" resource to replace the subject.
		
		if (stmtTokens.get(1).length() == 0 || stmtTokens.get(1).charAt(0) != '?')
			predicate = model.getProperty(Namespaces.format(stmtTokens.get(1)));
		else
			predicate = model.createProperty("nullPredicate"); //if the predicate is unbounded (a variable), creates an "nullPredicate" property to replace the predicate.
		

		if (stmtTokens.get(2).length() == 0 || stmtTokens.get(2).charAt(0) != '?')
		{
			object = Helpers.parseLiteral(stmtTokens.get(2), model);				
			assert(object!=null);
		}
		else
			object = model.createResource("nullObject"); //if the object is unbounded (a variable), creates an "nullObject" object to replace the object.
		
		baseStmt = new StatementImpl(subject, predicate, object, model);
		
	}
	
	/**Formats a partial statement for inclusion in a SPARQL query.</br>
	 * 
	 * Unbounded resources of the statement are rendered with "?" as prefixes, other parts are rendered as resources with their full URIs.
	 * 
	 * @return the formatted partial statement.
	 */
	public String asSparqlRow(){	
		return (
				stmtTokens.get(0).length() > 0 && stmtTokens.get(0).charAt(0) == '?' ? 
						stmtTokens.get(0) : 
						"<" + getSubject().toString() + ">"
				) + " " + 
				(stmtTokens.get(1).length() > 0 && stmtTokens.get(1).charAt(0) == '?' ? 
						stmtTokens.get(1) : 
						"<" + getPredicate().toString() + ">"
				) + " " + 
				(stmtTokens.get(2).length() > 0 && stmtTokens.get(2).charAt(0) == '?' ? 
						stmtTokens.get(2) : 
						(getObject().isLiteral()? 
								Helpers.literalToSparqlSyntax((Literal)getObject()) : 
								"<" + getObject().toString() + ">")
				) + " .\n";
	}
	
	/** Performs basic tests to determine if a statement is a partial statement, ie if the given string contains three tokens and at least one token starting with ?.
	 * 
	 * @param lex a string to test
	 * @return whether the statement is a partial statement or not.
	 */
	static public boolean isPartialStatement(String lex)
	{
		List<String> stmtTokens = Helpers.tokenize(lex.trim(), ' ');
		
		if (stmtTokens.size() != 3)
			return false;
		
				
		//checks that at least one token starts with a "?".
		if (!((stmtTokens.get(0).length() > 0 && stmtTokens.get(0).charAt(0) == '?') || (stmtTokens.get(1).length() > 0 && stmtTokens.get(1).charAt(0) == '?') || (stmtTokens.get(2).length() > 0 && stmtTokens.get(2).charAt(0) == '?')) )
			return false;
		
		return true;
		
	}
	
	@Override
	public String toString(){
		
		String result="";
		
		for (String token : stmtTokens){
			result += token + " ";
		}
		return result.trim();
	}

	@Override
	public Resource getSubject() 
	{
		Resource result = baseStmt.getSubject();
		if (result.toString().equals("nullSubject"))
			return null;
		else
			return result;
	}
	
	@Override
	public Property getPredicate() 
	{
		Property result = baseStmt.getPredicate();
		if (result.toString().equals("nullPredicate"))
			return null;
		else
			return result;
	}

	@Override
	public RDFNode getObject() 
	{
		RDFNode result = baseStmt.getObject();
		if (result.isResource() && ((Resource)result).toString().equals("nullObject"))
			return null;
		else
			return result;
	}

	@Override
	public Statement changeLiteralObject(boolean o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeLiteralObject(long o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeLiteralObject(int o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeLiteralObject(char o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeLiteralObject(float o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeLiteralObject(double o) {
		return baseStmt.changeLiteralObject(o);
	}

	@Override
	public Statement changeObject(String o) {
		return baseStmt.changeObject(o);
	}

	@Override
	public Statement changeObject(RDFNode o) {
		return baseStmt.changeObject(o);
	}

	@Override
	public Statement changeObject(String o, boolean wellFormed) {
		return baseStmt.changeObject(o, wellFormed);
	}

	@Override
	public Statement changeObject(String o, String l) {
		return baseStmt.changeObject(o, l);	
	}

	@Override
	public Statement changeObject(String o, String l, boolean wellFormed) {
		return baseStmt.changeObject(o, l, wellFormed);
	}

	@Override
	public ReifiedStatement createReifiedStatement() {
		throw new com.hp.hpl.jena.shared.CannotCreateException("Partial statements can not be reified.");
	}

	@Override
	public ReifiedStatement createReifiedStatement(String uri) {
		throw new com.hp.hpl.jena.shared.CannotCreateException("Partial statements can not be reified.");
	}

	@Override
	public Alt getAlt() {
		return baseStmt.getAlt();
	}

	@Override
	public Bag getBag() {
		return baseStmt.getBag();
	}

	@Override
	public boolean getBoolean() {
		return baseStmt.getBoolean();
	}

	@Override
	public byte getByte() {
		return baseStmt.getByte();
	}

	@Override
	public char getChar() {
		return baseStmt.getChar();
	}

	@Override
	public double getDouble() {
		return baseStmt.getDouble();
	}

	@Override
	public float getFloat() {
		return baseStmt.getFloat();
	}

	@Override
	public int getInt() {
		return baseStmt.getInt();
	}

	@Override
	public String getLanguage() {
		return baseStmt.getLanguage();
	}

	@Override
	public Literal getLiteral() {
		return baseStmt.getLiteral();
	}

	@Override
	public long getLong() {
		return baseStmt.getLong();
	}

	@Override
	public Model getModel() {
		return baseStmt.getModel();
	}

	@Override
	public Statement getProperty(Property p) {
		return baseStmt.getProperty(p);
	}

	@Override
	public Resource getResource() {
		return baseStmt.getResource();
	}

	@Override
	public Seq getSeq() {
		return baseStmt.getSeq();
	}

	@Override
	public short getShort() {
		return baseStmt.getShort();
	}

	@Override
	public Statement getStatementProperty(Property p) {
		return baseStmt.getStatementProperty(p);
	}

	@Override
	public String getString() {
		return baseStmt.getString();
	}

	@Override
	public boolean hasWellFormedXML() {
		return baseStmt.hasWellFormedXML();
	}

	@Override
	public boolean isReified() {
		return baseStmt.isReified();
	}

	@Override
	public RSIterator listReifiedStatements() {
		return baseStmt.listReifiedStatements();
	}

	@Override
	public Statement remove() {
		return baseStmt.remove();
	}

	@Override
	public void removeReification() {
		baseStmt.removeReification();
		
	}

	@Override
	public Triple asTriple() {
		throw new com.hp.hpl.jena.shared.CannotCreateException("Partial statements can not be viewed as triple.");
	}

	@Override
	public Resource getResource(ResourceF f) {
		return baseStmt.getResource(f);
	}

}
