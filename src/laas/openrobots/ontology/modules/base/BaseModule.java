package laas.openrobots.ontology.modules.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;

import laas.openrobots.ontology.IServiceProvider;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.RPCMethod;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.types.ResourceDescription;

public class BaseModule implements IServiceProvider {

	IOntologyBackend oro;
	
	public BaseModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
	}
	
	/**
	 * Like {@link #add(Set<String>, MemoryProfile)} with the {@link MemoryProfile.DEFAULT} memory profile.

	 * @param statements A vector of string representing statements to be inserted in the ontology.
	 * @throws IllegalStatementException
	 * 
	 * @see #add(Set<String>, MemoryProfile)
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the ontology, in long term memory."
	)
	public void add(Set<String> rawStmts) throws IllegalStatementException
	{
		for (String rawStmt : rawStmts) oro.add(oro.createStatement(rawStmt), MemoryProfile.DEFAULT);
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
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
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
	 * @param statement The new statement.
	 * @param memProfile The memory profile associated with this statement.
	 * @throws IllegalStatementException
	 * 
	 * @see #createStatement(String) Syntax details regarding the string describing the statement.
	 * @see #add(Statement, MemoryProfile)
	 * @see #remove(Statement)
	 */
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the ontology associated with a memory profile."
	)
	public void add(Set<String> rawStmts, String memProfile) throws IllegalStatementException
	{
		for (String rawStmt : rawStmts) oro.add(oro.createStatement(rawStmt), MemoryProfile.fromString(memProfile));
	}
	
	/**
	 * Remove a given statement (represented as a string) from the ontology. Does nothing if the statement doesn't exist.
	 * 
	 * @param stmt A string representing the statement to remove from the ontology.
	 * @see #add(String)
	 * @see #remove(Statement)
	 * @see #createStatement(String) Syntax details regarding the string describing the statement.
	 */
	public void remove(String stmt) throws IllegalStatementException {
		oro.remove(oro.createStatement(stmt));
	}
	
	/**
	 * Remove all statements matching the partial statement.
	 * 
	 * @param partialStmt The lexical form of a partial statement representing a "mask" of statements to delete.
	 * @throws IllegalStatementException thrown if the string does not represent a valid partial statement.
	 * @see {@link #clear(PartialStatement)} for an example.
	 * @see PartialStatement
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public void clear(String partialStmt) throws IllegalStatementException {		
		oro.clear(oro.createPartialStatement(partialStmt));
	}
	
	/**
	 * Remove a set of statements (represented as a strings) from the ontology. Does nothing if the statements don't exist.
	 * 
	 * @param stmts A vector of strings representing the statements to remove from the ontology.
	 * @see #add(Vector)
	 * @see #remove(String)
	 */
	@RPCMethod(
			desc="removes one or several statements (triplets S-P-O) from the ontology."
	)
	public void remove(Set<String> stmts) throws IllegalStatementException {
		for (String stmt : stmts) remove(stmt);		
	}
	
	@RPCMethod(
			desc="checks that one or several statements are asserted or can be inferred from the ontology"
	)
	public Boolean check(Vector<String> stmts) throws IllegalStatementException{
	
		Logger.log("Checking facts: "+ stmts + "\n");
		
		for (String s : stmts)
		{
			if (PartialStatement.isPartialStatement(s))
				if (!oro.check(oro.createPartialStatement(s))) return false;
			else
				if (!oro.check(oro.createStatement(s))) return false;
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#checkConsistency()
	 */
	@RPCMethod(
			desc="checks that the ontology is semantically consistent"
	)
	public Boolean checkConsistency() throws InconsistentOntologyException {
		Logger.log("Checking ontology consistency...", VerboseLevel.IMPORTANT);
		
		try {
			oro.checkConsistency();
		}
		catch (InconsistentOntologyException e){
			Logger.log("ontology inconsistent!\n", VerboseLevel.WARNING);
			throw e;
		}
		
		Logger.log("no problems.\n", VerboseLevel.IMPORTANT);
		return true;
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#query(java.lang.String)
	 */
	@RPCMethod(
			desc="performs one SPARQL query on the ontology"
	)
	public Set<String> query(String key, String q) throws QueryParseException, QueryExecException
	{
		Logger.log("Processing query...");
		
		Set<String> result = new HashSet<String>();

		//String key = "";
		
		//The variable we want to bind.
		/*if (params.size() == 1) {
			String q = params.firstElement();
			int iQmark = q.indexOf("?");
			int iSpace = (q.indexOf(" ", iQmark) == -1) ? q.length() : q.indexOf(" ", iQmark);
			int iReturn = (q.indexOf('\n', iQmark) == -1) ? q.length() : q.indexOf("\n", iQmark);
			key = q.substring(iQmark+1, Math.min(iSpace, iReturn));
		}		
		else key = params.remove(0); //if more than one string is given, we assume that the first param is the variable to bind.
		*/
		//TODO: do some detection to check that the first param is the key, and throw nice exceptions when required.
		
		ResultSet queryResults = null;
		
		queryResults = oro.query(q);
		while (queryResults.hasNext()) {
			RDFNode node = queryResults.nextSolution().getResource(key);
			if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
				result.add(Namespaces.toLightString(node));
		}
		
		Logger.log("done.");
		
		return result;
	}
	
	/**
	 * Like {@link #query(String) query} except it returns a XML-encoded SPARQL result.
	 * 
	 * @param query A well-formed SPARQL query to perform on the ontology. {@code PREFIX} statements may be omitted if they are the standard ones (namely, owl, rdf, rdfs) or the LAAS OpenRobots ontology (oro) one.
	 * @return The result of the query as SPARQL XML string.
	 * @see #query(String)
	 */
	@RPCMethod(
			desc = "performs one or several SPARQL queries on the ontology and returns a XML-formatted result set"
	)
	public String queryAsXML(String query){
		
		ResultSet result = oro.query(query);
		if (result != null)
			return ResultSetFormatter.asXMLString(result);
		else
			return null;
	}
	

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
	 * @param partial_statements The partial statement statements defining (more or less) the resource your looking for.
	 * @param filters a vector of string containing the various filters you want to append to your search. The syntax is the SPARQL one (as defined here: http://www.w3.org/TR/rdf-sparql-query/#tests).
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @throws IllegalStatementException 
	 * @see #guess(String, Vector, double)
	 * @see PartialStatement Syntax of partial statements
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="tries to identify a resource given a set of partially defined statements plus restrictions about this resource."
	)	
	public Set<String> find(String varName,	Set<String> statements, Set<String> filters) throws IllegalStatementException {
		
		Set<String> result = new HashSet<String>();
		Iterator<String> stmts = statements.iterator();
		
		if (varName.startsWith("?")) varName = varName.substring(1);
		
		String query = "SELECT ?" + varName + "\n" +
		"WHERE {\n";
		while (stmts.hasNext())
		{
			PartialStatement stmt = oro.createPartialStatement(stmts.next());
			query += stmt.asSparqlRow();
		}
		
		if (!(filters == null || filters.isEmpty())) 
		{
			Iterator<String> filtersItr = filters.iterator();
			while (filtersItr.hasNext())
			{
				query += "FILTER (" + filtersItr.next() + ") .\n";
			}
		}
		
		
		query += "}";
		
		ResultSet rawResult = oro.query(query);
		
		if (rawResult == null) return null;
		
		while (rawResult.hasNext())
		{
			QuerySolution row = rawResult.nextSolution();
			result.add(Namespaces.toLightString(row.getResource(varName)));
		}
		
		return result;
	}

	/**
	 * Tries to identify a resource given a set of partially defined statements about this resource.<br/>
	 * 
	 * This is a simpler form for {@link #find(String, Vector, Vector)}, without filters.
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
	 * @param partial_statements The partial statement statements defining (more or less) the resource your looking for.
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @throws IllegalStatementException 
	 * @see #find(String, Vector, Vector)
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	@RPCMethod(
			desc="tries to identify a resource given a set of partially defined statements about this resource."
	)	
	public Set<String> find(String varName, Set<String> statements) throws IllegalStatementException {
		return find(varName, statements, null);
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
			desc = "returns the set of asserted and inferred statements whose the given node is part of. It represents the \"usages\" of a resource."
	)
	public Set<String> getInfos(String lex_resource) throws NotFoundException {
		
		Logger.log("Looking for statements about " + lex_resource + "...");
		
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
			
			if (obj.isResource())
				objString = ((Resource) obj.as(Resource.class)).getLocalName();
			else if (obj.isLiteral())
				objString = ((Literal) obj.as(Literal.class)).getLexicalForm();
			else
				objString = obj.toString();

			result.add(	stmt.getSubject().getLocalName() + " " + 
						stmt.getPredicate().getLocalName() + " " +
						objString);
		}

		Logger.log("done.\n");
		
		return result;
	}
	
	/** Returns all the super classes of a given class, as asserted or inferred from the ontology.
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		Logger.log("Looking up for superclasses of " + type + "...");
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSuperclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		Logger.log("done.\n");
		
		return result;
	}
	
	/** Returns all the direct super-classes of a given class (ie, the classes whose the given class is a direct descendant), as asserted or inferred from the ontology.
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		Logger.log("Looking for direct superclasses of " + type + "...");
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSuperclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		Logger.log("done.\n");
		
		return result;
	}
	
	/** Returns all the sub-classes of a given class, as asserted or inferred from the ontology.
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		Logger.log("Looking up for subclasses of " + type + "...");
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSubclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		Logger.log("done.\n");
		
		return result;
	}
	
	/** Returns all the direct sub-classes of a given class (ie, the classes whose the given class is the direct parent), as asserted or inferred from the ontology.
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		Logger.log("Looking for direct subclasses of " + type + "...");
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : oro.getSubclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		Logger.log("done.\n");
		
		return result;
	}
	
	/** Returns all the instances of a given class, as asserted or inferred from the ontology. 
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getInstancesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	
	/**  Returns all the direct instances of a given class (ie, the instances whose the given class is the direct parent), as asserted or inferred from the ontology.
	 * 
	 * @param A class, in its namespace (if no namespace is specified, the default namespace is assumed, as defined in the configuration file)
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
		
		oro.getModel().enterCriticalSection(Lock.READ);
		OntClass myClass = oro.getModel().getOntClass(Namespaces.format(type));
		oro.getModel().leaveCriticalSection();
		
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
		
		oro.getModel().enterCriticalSection(Lock.READ);
		Individual myClass = oro.getModel().getIndividual(Namespaces.format(individual));
		oro.getModel().leaveCriticalSection();
		
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
		
		oro.getModel().enterCriticalSection(Lock.READ);
		Individual myClass = oro.getModel().getIndividual(Namespaces.format(individual));
		oro.getModel().leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + individual + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : oro.getClassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		Logger.log("done.\n");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a serialized ResourceDescription object that describe all the links of this resource with others resources (sub and superclasses, instances, properties, etc.). The second parameter specify the desired language (following RFC4646)."
	)
	public ResourceDescription getResourceDetails(String id, String language_code) throws NotFoundException {
				
		oro.getModel().enterCriticalSection(Lock.READ);
		//TODO: check if the resource exists!!!
		OntResource myResource = oro.getModel().getOntResource(Namespaces.format(id));
		oro.getModel().leaveCriticalSection();
		
		if (myResource == null) throw new NotFoundException("The resource " + id + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		return new ResourceDescription(myResource, language_code);
	}
		
	@RPCMethod(
			desc = "returns a serialized ResourceDescription object that describe all the links of this resource with others resources (sub and superclasses, instances, properties, etc.)."
	)
	public ResourceDescription getResourceDetails(String id) throws NotFoundException {
				
		return getResourceDetails(id, OroServer.DEFAULT_LANGUAGE);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#lookup(java.lang.String)
	 */
	@RPCMethod(
			desc = "try to identify a concept from its id or label, and return it, along with its type (class, instance, object_property, datatype_property)."
	)
	public List<String> lookup(String id) throws NotFoundException {
		
		return oro.lookup(id);
		
	}
	
}