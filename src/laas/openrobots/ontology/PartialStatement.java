/*
 * ©LAAS-CNRS (2008-2009)
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

package laas.openrobots.ontology;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Alt;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ObjectF;
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

import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.exceptions.IllegalStatementException;

/** A partial statement is a statement whose at least one element (subject, predicate or object) is unknown.</br>
 * 
 * To be valid, a partial statement must have at least one variable, prepended with a "?". For instance: {@code "?mysterious oro:objProperty oro:individual"}
 *
 */
public class PartialStatement implements Statement {

	private String stmtTokens[];
	
	private StatementImpl baseStmt;
	
	/**
	 * Create a new partial statement from its string representation.<br/>
	 * Works as {@link OpenRobotsOntology#createStatement(String)} except at least one variable, prepended with a "?", is expected.</br>
	 * 
	 * This class implements {@link Statement}, but the {@link #getSubject()}, {@link #getPredicate()} and {@link #getObject()} method will return {@code null} if the corresponding part of the statement is unbounded. 
	 * 
	 * @param partialStatement The string represtentating the partial statement. For example, {@code "?mysterious oro:objProperty2 ?object"} or {@code "?subject oro:dataProperty1 true"} are valid.
	 * @param model The ontology this partial statement refers to.
	 * @throws IllegalStatementException Currently thrown only if the statement doesn't contain three tokens.
	 * @see OpenRobotsOntology#createStatement(String) Syntax details
	 */
	public PartialStatement(String partialStatement, ModelCom model) throws IllegalStatementException {
				
		Resource subject = null;
		Property predicate = null;
		RDFNode object = null;
		
		//TODO: We limit the split to 3 tokens to allow spaces in the object when it is a literal string. A better solution would be to properly detect quotes and count only spaces that are not inside quotes.
		stmtTokens = partialStatement.trim().split(" ", 3);
		
		if (stmtTokens.length != 3)
					throw new IllegalStatementException("Three tokens are expected in a partial statement, " + stmtTokens.length + " found in " + partialStatement + ".");
				
		//checks that at least one token starts with a "?".
		if (!(stmtTokens[0].startsWith("?") || stmtTokens[1].startsWith("?") || stmtTokens[2].startsWith("?")) ) throw new IllegalStatementException("At least one token should be marked as unbounded (starting with a \"?\").");
		
		if (!stmtTokens[0].startsWith("?"))
			subject = model.getResource(Namespaces.format(stmtTokens[0]));
		else
			subject = model.createResource("nullSubject"); //if the subject is unbounded (a variable), creates an "nullSubject" resource to replace the subject.
		
		if (!stmtTokens[1].startsWith("?"))
			predicate = model.getProperty(Namespaces.format(stmtTokens[1]));
		else
			predicate = model.createProperty("nullPredicate"); //if the predicate is unbounded (a variable), creates an "nullPredicate" property to replace the predicate.
		

		if (!stmtTokens[2].startsWith("?"))
		{
			object = Helpers.parseLiteral(stmtTokens[2], model);				
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
				stmtTokens[0].startsWith("?") ? 
						stmtTokens[0] : 
						"<" + getSubject().toString() + ">"
				) + " " + 
				(stmtTokens[1].startsWith("?") ? 
						stmtTokens[1] : 
						"<" + getPredicate().toString() + ">"
				) + " " + 
				(stmtTokens[2].startsWith("?") ? 
						stmtTokens[2] : 
						(getObject().isLiteral()? 
								Helpers.literalToSparqlSyntax((Literal)getObject()) : 
								"<" + getObject().toString() + ">")
				) + " .\n";
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
	public Object getObject(ObjectF f) {
		return baseStmt.getObject(f);
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
	public Resource getResource(ResourceF f) {
		return baseStmt.getResource(f);
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

}
