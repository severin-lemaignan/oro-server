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

package laas.openrobots.ontology.modules.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;
import laas.openrobots.ontology.types.ResourceDescription;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;

public class BaseModule implements IServiceProvider {

	IOntologyBackend oro;
		
	public BaseModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
	}
	
	/**
	 * Like {@link #add(Set<String>, String)} with the {@link MemoryProfile#DEFAULT} memory profile.
	 * 
	 * @param statements A set of string representing statements to be inserted in the ontology.
	 * @throws IllegalStatementException
	 * 
	 * @see #add(Set, String)
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the robot model, in long term memory."
	)
	public void add(Set<String> rawStmts) throws IllegalStatementException
	{
		add(rawStmts, MemoryProfile.DEFAULT.toString());
			
	}
	
	@RPCMethod(
			desc="try to add news statements in long term memory, if they don't lead to inconsistencies (return false if at least one stmt wasn't added)."
	)
	public boolean safeAdd(Set<String> rawStmts) throws IllegalStatementException
	{
		return safeAdd(rawStmts, MemoryProfile.DEFAULT.toString());
	}

	/**
	 * Update the value of a property.
	 * 
	 * This method is equivalent to a {@link #remove(Set)} followed by an {@link #add(Set)}.
	 * 
	 * ATTENTION: this method works only on <em>functional</em> properties (ie, 
	 * properties that are subclasses of <pre>owl:FunctionalProperty</pre>.
	 * 
	 * For non-functional properties (or if the subject or predicate does not
	 * exist), this method behaves like {@link #add(Set)}.
	 * 
	 * Example:
	 * <pre>
	 * add(["alice isLost true"])
	 * -> ontology contains the fact "alice isLost true"
	 * 
	 * update(["alice isLost false"])
	 * -> ontology contains the fact "alice isLost false"
	 * </pre>
	 * 
	 * Please note that the newly created statement is put in the DEFAULT memory
	 * profile, usually the long-term one.
	 * 
	 * @param statements A set of string representing statements to be updated.
	 * @throws IllegalStatementException
	 * 
	 * @see {@link IOntologyBackend#update(Set)}
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="update the value of a functional property."
	)
	public void update(Set<String> rawStmts) throws IllegalStatementException
	{
		Set<Statement> stmtsToUpdate = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			stmtsToUpdate.add(oro.createStatement(rawStmt));			
		}
		
		oro.update(stmtsToUpdate);
			
	}
	
	/**
	 * Adds a set of statements (assertions) to the ontology from their string representation in the given memory profile.<br/>
	 * This method does nothing if the statements already exist with the same memory profile. If the same statements are added with a different memory profile, the shortest term memory container has priority.
	 * 
	 * To create literals, you must suffix the value with {@code ^^xsd:} and the XML schema datatype.</br>
	 * <br/>
	 * For instance:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 * myOntology.add("myIndividual myBooleanPredicate true^^xsd:boolean", MemoryProfile.SHORTTERM);
	 * </pre>
	 * 
	 * Examples of statements: <br/>
	 * <ul>
	 * 	<li>{@code "oro:instance1 rdf:type oro:Class1"}</li>
	 *  <li>{@code "oro:instance1 oro:dataProperty1 true^^xsd:boolean"}</li>
	 *  <li>{@code "instance1 dataProperty1 true"} (if no namespace is specified, it uses the default one)</li>
	 * </ul>
	 *  
	 * C++ code snippet with {@code liboro} library:
	 *  
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;socket_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 
	 * 		SocketConnector connector(&quot;localhost&quot;, &quot;6969&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		onto->add(Statement("gorilla rdf:type Monkey"));
	 * 		onto->add(Statement("gorilla age 12^^xsd:int"));
	 * 		onto->add(Statement("gorilla weight 75.2"));
	 * 
	 * 		// You can as well send a set of statement. The transport will be optimized (all the statements are sent in one time).
	 * 		vector<Statement> stmts;
	 * 
	 * 		stmts.push_back("gorilla rdf:type Monkey");
	 * 		stmts.push_back("gorilla age 12^^xsd:int");
	 * 		stmts.push_back("gorilla weight 75.2");
	 * 
	 * 		onto->add(stmts);
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * This method does nothing if the statement already exists with the same memory profile. If the same statement is added with a different memory profile, the shortest term memory container has priority.
	 * 
	 * @param rawStmts A set of new statements.
	 * @param memProfile The memory profile associated with this statement.
	 * @throws IllegalStatementException
	 * 
	 * @see #createStatement(String) Syntax details regarding the string describing the statement.
	 * @see IOntologyBackend#add(Statement, MemoryProfile, boolean)
	 * @see #remove(Set)
	 */
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the robot model associated with a memory profile."
	)
	public void add(Set<String> rawStmts, String memProfile) throws IllegalStatementException
	{
		Set<Statement> stmtsToAdd = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			Statement s = oro.createStatement(rawStmt);
			stmtsToAdd.add(s);		
		}
		
		oro.add(stmtsToAdd, MemoryProfile.fromString(memProfile), false);
	}
	
	/** Adds statements with a specific memory model, but only if the statement 
	 * doesn't cause any inconsistency.
	 * 
	 * If one statement cause an inconsistency, it won't be added, the return
	 * value will be "false", and the process continue with the remaining 
	 * statements. 
	 * 
	 * @param rawStmts a set of statements
	 * @param memProfile the memory profile
	 * @return false if at least one statement was not added because it would 
	 * lead to inconsistencies.
	 * @throws IllegalStatementException
	 */
	@RPCMethod(
			desc="try to add news statements with a specific memory profile, if they don't lead to inconsistencies (return false if at least one stmt wasn't added)."
	)
	public boolean safeAdd(Set<String> rawStmts, String memProfile) throws IllegalStatementException
	{
		Set<Statement> stmtsToAdd = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			Statement s = oro.createStatement(rawStmt);
			stmtsToAdd.add(s);			
		}

		return oro.add(stmtsToAdd, MemoryProfile.fromString(memProfile), true);
	}
	
	/**
	 * Remove a given statement (represented as a string) from the ontology. Does nothing if the statement doesn't exist.
	 * 
	 * @param stmt A string representing the statement to remove from the ontology.
	 * @deprecated {@link #remove(Set)} should be used instead.
	 * @see #add(Set)
	 * @see IOntologyBackend#remove(Statement)
	 * @see IOntologyBackend#createStatement(String) Syntax details regarding the string describing the statement.
	 */
	public void remove(String stmt) throws IllegalStatementException {
		Statement s = oro.createStatement(stmt);
		oro.remove(s);
	}
	
	/**
	 * Remove a set of statements (represented as a strings) from the ontology. Does nothing if the statements don't exist.
	 * 
	 * @param stmts A vector of strings representing the statements to remove from the ontology.
	 * @see #add(Set)
	 * @see #remove(String)
	 */
	@RPCMethod(
			desc="removes one or several statements (triplets S-P-O) from the ontology."
	)
	public void remove(Set<String> stmts) throws IllegalStatementException {
		for (String stmt : stmts) remove(stmt);		
	}
		
	/**
	 * Removes all statements matching any partial statements in a set.
	 * 
	 * Attention, the implicit relation between each of the partial statements
	 * in the set is a OR: the ontology is matched against each of the provided
	 * partial statements, and for each of them, all matching statements are 
	 * removed.
	 * 
	 * If need, the "AND" behaviour can be added. Please drop a mail to openrobots@laas.fr
	 * 
	 * @param partialStmts A set of partial statements (in their lexical form) representing a "mask" of statements to delete.
	 * @throws IllegalStatementException thrown if the string does not represent a valid partial statement.
	 * @see PartialStatement
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="removes statements matching any pattern in the given set"
	)
	public void clear(Set<String> partialStmts) throws IllegalStatementException {
		
		for (String s : partialStmts) 
			oro.clear(oro.createPartialStatement(s));
	}
	
	@RPCMethod(
			desc="checks that one or several statements are asserted or can be inferred from the ontology"
	)
	public Boolean check(Set<String> stmts) throws IllegalStatementException{
	
		Logger.log("Checking facts: "+ stmts + "\n");
		
		for (String s : stmts)
		{
			if (PartialStatement.isPartialStatement(s)) {
				if (!oro.check(oro.createPartialStatement(s))) return false;
			}
			else {
				Statement stmt = oro.createStatement(s);
				if (!oro.check(stmt)) return false;
			}
		}
		
		return true;
	}
	

	/**
	 * Checks if the ontology is consistent.
	 * 
	 * @return true if the ontology is consistent, false otherwise.
	 */
	@RPCMethod(
			desc="checks that the ontology is semantically consistent"
	)
	public Boolean checkConsistency() {
		Logger.log("Checking ontology consistency...", VerboseLevel.IMPORTANT);
		
		try {
			oro.checkConsistency();
		}
		catch (InconsistentOntologyException e){
			Logger.log("ontology inconsistent!\n", VerboseLevel.WARNING, false);
			return false;
		}
		
		Logger.log("no problems.\n", false);
		return true;
		
	}
	
	/**
	 * Checks that a set of statements are consistent with the current model.
	 * 
	 * Statements are not added to the model.
	 * 
	 * @return true if the set of given statement is consistent with the ontology, false otherwise.
	 * @throws IllegalStatementException 
	 */
	@RPCMethod(
			desc="checks that a set of statements are consistent with the current model"
	)
	public Boolean checkConsistency(Set<String> rawStmts) throws IllegalStatementException {
		Logger.log("Checking stmts consistency against current model...\n", VerboseLevel.IMPORTANT);
		
		Set<Statement> stmtsToCheck = new HashSet<Statement>();
		
		for (String rawStmt : rawStmts) {
			if (rawStmt == null)
				throw new IllegalStatementException("Got a null statement to add!");
			Statement s = oro.createStatement(rawStmt);
			stmtsToCheck.add(s);		
		}
		
		return oro.checkConsistency(stmtsToCheck);
		
	}
	
	/**
	 * Maps {@link IOntologyBackend#query(String, String)} into a RPC call
	 * 
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#query(String, String)
	 */
	@RPCMethod(
			category="querying",
			desc="performs one SPARQL query on the ontology"
	)
	public Set<String> query(String key, String q) throws InvalidQueryException, OntologyServerException
	{
		Logger.log("Processing query:\n" + q + "\n");
		
		Set<RDFNode> raw = oro.query(key, q);
		Set<String> res = new HashSet<String>();
		
		for (RDFNode n : raw) {
			try {
				Resource node = n.as(Resource.class);
				if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
					res.add(Namespaces.toLightString(node));
			} catch (ClassCastException e) {
				try {
					Literal l = n.as(Literal.class);
					res.add(l.getLexicalForm());
				} catch (ClassCastException e2) {
					Logger.log("Failed to convert the result of the query to a string!",
							VerboseLevel.SERIOUS_ERROR);
					throw new OntologyServerException("Failed to convert the " +
							"result of the query to a string!");
				}
			}
		}
		return res;
	}

	/**
	 * Like {@link #query(String) query} except it returns a XML-encoded SPARQL result.
	 * 
	 * @param query A well-formed SPARQL query to perform on the ontology. {@code PREFIX} statements may be omitted if they are the standard ones (namely, owl, rdf, rdfs) or the LAAS OpenRobots ontology (oro) one.
	 * @return The result of the query as SPARQL XML string.
	 * @see #query(String)
	 */
	/**TODO: REIMPLEMENT queryAsXML!!
	@RPCMethod(
			category="querying",
			desc = "performs one or several SPARQL queries on the ontology and returns a XML-formatted result set"
	)
	public String queryAsXML(String query){
		
		ResultSet result = oro.query(query);
		if (result != null)
			return ResultSetFormatter.asXMLString(result);
		else
			return null;
	}
	**/

	/**
	 * Tries to identify a resource given a set of partially defined statements (plus optional restrictions) about this resource.<br/>
	 * 
	 * First simple example:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 *
	 * Set<String> knowledge = new Set<String>();
	 * knowledge.add("?mysterious_object ns:isEdibleBy ns:monkey");
	 * knowledge.add("?mysterious_object ns:color "yellow"^^ns:color");
	 *
	 * Set<String> results = myOntology.find("mysterious_object", knowledge);
	 * for (String res:results)
	 * 		System.out.println(res);
	 * </pre>
	 * Supposing your ontology defines the right properties and instances, you can expect this example to output something like {@code ns:banana}.<br/>
	 * <br/>
	 * Example with restrictions:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 *
	 * Set<String> knowledge = new Set<String>();
	 * Set<String> filters = new Set<String>();
	 * 
	 * knowledge.add("?mysterious_object ns:isEdibleBy ns:monkey");
	 * knowledge.add("?mysterious_object ns:color "yellow"^^ns:color");
	 * knowledge.add("?mysterious_object ns:size ?size);
	 *
	 * filters.add("?size >= 200.0");
	 * filters.add("?size < 250.0");
	 *
	 * Set<String> results = myOntology.find("mysterious_object", knowledge, filters);
	 * for (String res:results)
	 * 		System.out.println(res);
	 * </pre>
	 * This example would output all the {@code ns:banana}s whose size is comprised between 200 and 250mm (assuming mm is the unit you are using...).
	 * <br/>
	 * C++ code snippet using liboro:  
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 		set&lt;Concept&gt; result;
	 * 		set&lt;string&gt; partial_stmts;
	 * 		set&lt;string&gt; filters;
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		partial_stmts.insert("?mysterious rdf:type oro:Monkey");
	 * 		partial_stmts.insert("?mysterious oro:weight ?value");
	 * 
	 * 		filters.insert("?value >= 50");
	 * 
	 * 		onto->find(&quot;mysterious&quot;, partial_stmts, filters, result);
	 * 
	 * 		//display the result
	 * 		copy(result.begin(), result.end(), ostream_iterator<Concept>(cout, "\n"));
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * @param varName The name of the variable to identify, as used in the statements.
	 * @param statements The partial statement statements defining (more or less) the resource your looking for.
	 * @param filters a vector of string containing the various filters you want to append to your search. The syntax is the SPARQL one (as defined here: http://www.w3.org/TR/rdf-sparql-query/#tests).
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @throws IllegalStatementException 
	 * @throws OntologyServerException 
	 * @see PartialStatement Syntax of partial statements
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category="querying",
			desc="tries to identify a resource given a set of partially defined statements plus restrictions about this resource."
	)	
	public Set<String> find(String varName,	Set<String> statements, Set<String> filters) throws IllegalStatementException, OntologyServerException {
		
		Set<String> res = new HashSet<String>();
		
		if (varName.isEmpty()) {
			Logger.log("Calling the find() method with an empty variable.\n", VerboseLevel.ERROR);
			throw new OntologyServerException("Calling the find() method with an empty variable.");
		}
		
		if (statements.isEmpty()) {
			Logger.log("Calling the find() method without partial statement. Returning an empty set of result.\n", VerboseLevel.WARNING);
			return res;
		}
		
		Logger.log("Searching resources in the ontology...\n");
				
		Set<PartialStatement> stmts = new HashSet<PartialStatement>();
		
		if (varName.length() > 0 && varName.charAt(0) == '?') varName = varName.substring(1);
		
		Logger.log(" matching following statements:\n", VerboseLevel.VERBOSE);
		
		for (String ps : statements) {
			Logger.log("\t- " + ps + "\n", VerboseLevel.VERBOSE);
			stmts.add(oro.createPartialStatement(ps));
		}
		
		if (filters != null) {
			Logger.log("with these restrictions:\n", VerboseLevel.VERBOSE);
			for (String f : filters)
				Logger.log("\t- " + f + "\n", VerboseLevel.VERBOSE);
		}

		Set<RDFNode> raw = oro.find(varName, stmts, filters);
				
		for (RDFNode n : raw) {
			try {
				Resource node = n.as(Resource.class);
				if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
					res.add(Namespaces.toLightString(node));
			} catch (ResourceRequiredException e) {
				try {
					Literal l = n.as(Literal.class);
					res.add(l.getLexicalForm());
				} catch (ClassCastException e2) {
					Logger.log("Failed to convert the result of the 'find' to a string!",
							VerboseLevel.SERIOUS_ERROR);
					throw new OntologyServerException("Failed to convert the " +
							"result of the 'find' to a string!");
				}
			}
		}

		return res;
	}

	/**
	 * Tries to identify a resource given a set of partially defined statements about this resource.<br/>
	 * 
	 * This is a simpler form for {@link #find(String, Set, Set)}, without filters.
	 * 
	 * <br/>
	 * C++ code snippet using liboro: 
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 		set&lt;Concept&gt; result;
	 * 		set&lt;string&gt; partial_stmts;
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		partial_stmts.insert("?mysterious oro:eats oro:banana_tree");
	 * 		partial_stmts.insert("?mysterious oro:isFemale true^^xsd:boolean");
	 * 
	 * 		onto->find(&quot;mysterious&quot;, partial_stmts, result);
	 * 
	 * 		//display the result
	 * 		copy(result.begin(), result.end(), ostream_iterator<Concept>(cout, "\n"));
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * @param varName The name of the variable to identify, as used in the statements.
	 * @param statements The partial statement statements defining (more or less) the resource your looking for.
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @throws IllegalStatementException 
	 * @throws OntologyServerException 
	 * @see #find(String, Vector, Vector)
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category="querying",
			desc="tries to identify a resource given a set of partially defined statements about this resource."
	)	
	public Set<String> find(String varName, Set<String> statements) 
				throws 	IllegalStatementException, 
						OntologyServerException {
		return find(varName, statements, null);
	}

	public Map<String, Set<String>> find(Set<String> variables,
			Set<String> partialStatements) {
		return null;
	}
	
	/**
	 * Returns the set of asserted and inferred statements whose the given node is part of. It represents the "usages" of a resource.<br/>
	 * Usage example:<br/>
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 * Model results = myOntology.getInfos("ns:individual1");
	 * 
	 * NodeIterator types = results.listObjectsOfProperty(myOntology.createProperty("rdf:type"));
	 *
	 * for ( ; types.hasNext() ; )
	 * {
	 *	System.out.println(types.nextNode().toString());
	 * }
	 * </pre>
	 * This example would print all the types (classes) of the instance {@code ns:individual1}.
	 * 
	 * @param lex_resource the lexical form of an existing resource.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the lex_resource doesn't exist in the ontology.
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category="querying",
			desc = "returns the set of asserted and inferred statements whose the given node is part of. It represents the usages of a resource."
	)
	public Set<String> getInfos(String lex_resource) throws NotFoundException {
		
		Logger.log("Looking for statements about " + lex_resource + ".\n");
		
		Set<String> result = new HashSet<String>();
		
		Model infos = oro.getSubmodel(oro.getResource(lex_resource));

		StmtIterator stmts = infos.listStatements();

		while (stmts.hasNext()) {
			Statement stmt = stmts.nextStatement();
			RDFNode obj = stmt.getObject();
			//Property p = stmt.getPredicate();
			
			String objString;

			//returns only statement involving properties from the ORO namespace
			//or stating the type/subtype of the instance.
			/*
			if (!	(p.getNameSpace().equals(Namespaces.DEFAULT_NS) || 
					p.getLocalName().equals("type") ||
					p.getLocalName().equals("subClassOf")))
				continue;
			*/
			
			//TODO: if changing that, change it as well in getInfosForAgent 
			
			if (obj.isResource())
				objString = (obj.as(Resource.class)).getLocalName();
			else if (obj.isLiteral())
				objString = (obj.as(Literal.class)).getLexicalForm();
			else
				objString = obj.toString();

			result.add(	stmt.getSubject().getLocalName() + " " + 
						stmt.getPredicate().getLocalName() + " " +
						objString);
		}
		
		return result;
	}
	
	/** Returns all the super classes of a given class, as asserted or inferred from the ontology.
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of classe ids associated to their labels (in the default language, as defined in the configuration file). 
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred superclasses of a given class."
	)
	public Map<String, String> getSuperclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking up for superclasses of " + type + ".\n");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSuperclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		return result;
	}
	
	/** Returns all the direct super-classes of a given class (ie, the classes whose the given class is a direct descendant), as asserted or inferred from the ontology.
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of classe ids associated to their labels (in the default language, as defined in the configuration file).
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred direct superclasses of a given class."
	)
	public Map<String, String> getDirectSuperclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking for direct superclasses of " + type + ".\n");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSuperclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		return result;
	}
	
	/** Returns all the sub-classes of a given class, as asserted or inferred from the ontology.
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of classe ids associated to their labels (in the default language, as defined in the configuration file).
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred subclasses of a given class."
	)
	public Map<String, String> getSubclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking up for subclasses of " + type + ".\n");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSubclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		return result;
	}
	
	/** Returns all the direct sub-classes of a given class (ie, the classes whose the given class is the direct parent), as asserted or inferred from the ontology.
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of classe ids associated to their labels (in the default language, as defined in the configuration file).
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred direct subclasses of a given class."
	)
	public Map<String, String> getDirectSubclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking for direct subclasses of " + type + ".\n");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
				
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSubclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		return result;
	}
	
	/** Returns all the instances of a given class, as asserted or inferred from the ontology. 
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of classe ids associated to their labels (in the default language, as defined in the configuration file).
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {instance name, label} (or {instance name, instance name without namespace} is no label is available) of asserted and inferred instances of a given class."
	)
	public Map<String, String> getInstancesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking up for instances of " + type + "...");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getInstancesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	
	/**  Returns all the direct instances of a given class (ie, the instances whose the given class is the direct parent), as asserted or inferred from the ontology.
	 * 
	 * @param type A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
	 * @return A map of instances ids (individuals) associated to their labels (in the default language, as defined in the configuration file).
	 * @throws NotFoundException
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {instance name, label} (or {instance name, instance name without namespace} is no label is available) of asserted and inferred direct instances of a given class."
	)
	public Map<String, String> getDirectInstancesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking for direct instances of " + type + "...");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getInstancesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	

	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of asserted and inferred classes of a given individual."
	)
	public Map<String, String> getClassesOf(String individual) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking for classes of " + individual + "...");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		Individual myClass = oro.getModel().getIndividual(Namespaces.format(individual));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + individual + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getClassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	
	@RPCMethod(
			category = "taxonomy",
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of asserted and inferred direct classes of a given individual."
	)
	public Map<String, String> getDirectClassesOf(String individual) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		Logger.log("Looking for direct classes of " + individual + "...");
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		Individual myClass = oro.getModel().getIndividual(Namespaces.format(individual));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myClass == null) throw new NotFoundException("The class " + individual + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getClassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	
	/**
	 * Returns a complete description of a resource identified by {@code id}, 
	 * including its type, label (in a specified language, if available), plus: 
	 *  - superclasses, subclasses and instances for classes
	 *  - class for instances
	 * 
	 * These data are sent as a JSON serialization.
	 * 
	 * Reference for language code is available in RFC4646.
	 * 
	 * @see ResourceDescription
	 * @param id the concept whose description is queried
	 * @param language_code a standard language code.
	 * @return a ResourceDescription object, serializable as a JSON string
	 * @throws NotFoundException
	 */
	@RPCMethod(
			category="querying",
			desc = "returns a serialized ResourceDescription object that " +
					"describe all the links of this resource with others " +
					"resources (sub and superclasses, instances, properties, " +
					"etc.). The second parameter specify the desired language " +
					"(following RFC4646)."
	)
	public ResourceDescription getResourceDetails(String id, String language_code) throws NotFoundException {
		
		Logger.log(">>enterCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		oro.getModel().enterCriticalSection(Lock.READ);
		//TODO: check if the resource exists!!!
		OntResource myResource = oro.getModel().getOntResource(Namespaces.format(id));
		oro.getModel().leaveCriticalSection();
		Logger.log(">>leaveCS: " + Thread.currentThread().getStackTrace()[2].getMethodName() + " -> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "\n", VerboseLevel.DEBUG, false);
		
		if (myResource == null) throw new NotFoundException("The resource " + 
				id + " does not exists in the ontology (tip: if this resource " +
						"is not in the default namespace, be sure to add the " +
						"namespace prefix!)");
		
		return new ResourceDescription(myResource, language_code);
	}
		
	/**
	 * Like {@link #getResourceDetails(String, String)} with language set to
	 * default language (as set in the server config file).
	 * 
	 * @see #getResourceDetails(String, String)
	 * @param id the concept whose description is queried
	 * @return a ResourceDescription object, serializable as a JSON string
	 * @throws NotFoundException
	 */
	@RPCMethod(
			category="querying",
			desc = "returns a serialized ResourceDescription object that " +
					"describe all the links of this resource with others " +
					"resources (sub and superclasses, instances, properties, " +
					"etc.)."
	)
	public ResourceDescription getResourceDetails(String id) throws NotFoundException {
				
		return getResourceDetails(id, OroServer.DEFAULT_LANGUAGE);
	}
	
	@RPCMethod(
			desc = "try to identify a concept from its id or label, and return " +
					"it, along with its type (class, instance, object_property, " +
					"datatype_property)."
	)
	public Set<List<String>> lookup(String id) {	
		return oro.lookup(id);
	}
	
	@RPCMethod(
			desc = "try to identify a concept from its id or label and its type " +
					"(class, instance, object_property, datatype_property)."
	)
	public Set<String> lookup(String id, String type) {
		return oro.lookup(id, ResourceType.fromString(type));
	}
	

	/**
	 * Returns the label associated to a concept whose name is 'id'.
	 * If the concept has several labels, a random one is picked.
	 * If the concept has no label, the concept id is returned.
	 * 
	 * @param the id to look for.
	 * @return One of the labels associated to this id or the id itself if no 
	 * label has been defined.
	 * @throws OntologyServerException 
	 */
	@RPCMethod(
			desc = "return the label of a concept, if available."
	)
	public String getLabel(String id) throws OntologyServerException {
		
		Set<String> q = new HashSet<String>();
		q.add(id + " rdfs:label ?label");
		
		Set<String> labels;
		
		try {
			labels = find("?label", q);
		} catch (InvalidQueryException e) {
			Logger.log("Unable to query the ontology for the label of id: " + id +
					". Will return the id and continue.",
					VerboseLevel.SERIOUS_ERROR);			
			return id;
		} catch (IllegalStatementException e) {
			Logger.log("Unable to query the ontology for the label of id: " + id +
					". Will return the id and continue.",
					VerboseLevel.SERIOUS_ERROR);			
			return id;
		}
		
		if (labels.isEmpty()) return id;
		
		return Helpers.pickRandom(labels);
	}

	
}
