package laas.openrobots.ontology.backends;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import laas.openrobots.ontology.IServiceProvider;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.events.IEventsProvider;
import laas.openrobots.ontology.events.IWatcher;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.UnmatchableException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

/** This interface describes the abstract behaviour of an ontology backend. It presents the list of operation the "knowledge store" should provide to be used with the {@linkplain laas.openrobots.ontology.OroServer ontology server}.
 * @author slemaign
 *
 */
public interface IOntologyBackend extends IServiceProvider {

	/**
	 * Helper to create a {@link com.hp.hpl.jena.rdf.model.Property property} attached at the current OpenRobotOntology by mapping the method to the underlying ontology model.<br/>
	 * This is a shortcut for {@code OpenRobotOntology.getModel().createProperty(Namespaces.format(lex_property))}
	 * @param lex_property the lexical form of the property (eg {@code "rdf:type"}).
	 * @return the corresponding Jena property.
	 * @see com.hp.hpl.jena.rdf.model.Property
	 * @see com.hp.hpl.jena.rdf.model.Model#createProperty(String)
	 */
	public abstract Property createProperty(String lex_property);

	/**
	 * Helper to create a {@link com.hp.hpl.jena.rdf.model.Resource resource} attached at the current OpenRobotOntology by mapping the method to the underlying ontology model.<br/>
	 * This is a shortcut for {@code OpenRobotOntology.getModel().createResource(Namespaces.format(lex_resource))}
	 * @param lex_resource the lexical form of the resource.
	 * @return the corresponding Jena resource.
	 * @see com.hp.hpl.jena.rdf.model.Resource
	 * @see com.hp.hpl.jena.rdf.model.Model#createResource(String)
	 */
	public abstract Resource createResource(String lex_resource);

	/**
	 * This static method acts as a Statement factory. It does some pre-processing to convert a string to a valid statement relative to the given ontology.<br/>
	 * Formatting follows roughly the <a href="http://www.w3.org/TR/rdf-sparql-query/#QSynLiterals">SPARQL syntax</a> :
	 * <ul>
	 *   <li> Literals follows the {@code "value"^^type} rule for the general case. Simple or double quotes can be used. Cf examples below.</li>
	 *   <li> Resources can be either isolated ("{@code individual1}"). It will then use the default namespace as defined in the {@linkplain OpenRobotsOntology#OpenRobotsOntology(String) configuration file}.</li>
	 *   <li> Or prefixed with the namespace prefix ("{@code rdf:type}"),</li>
	 *   <li> Or complete URIs ("{@code <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>}"). In this case, the URI must be enclosed between &lt; and &gt;.
	 * </ul>
	 * <br/>
	 * Literals examples:
	 * <ul>
	 *   <li>The boolean {@code true} can be represented either as {@code "true"^^xsd:boolean} or as {@code true},</li>
	 *   <li>The integer {@code 123} can be represented either as {@code 123^^xsd:int} or as {@code 123}. {@code "123"^^xsd:int} is also acceptable.</li>
	 *   <li>The double {@code 1.23} can be represented either as {@code 1.23^^xsd:double} or as {@code 1.23}. {@code "1.23"^^xsd:double} is also acceptable.</li>
	 *   <li>User-defined dataypes can be represented with {@code "xyz"^^<http://example.org/ns/userDatatype>} or {@code "xyz"^^oro:userDatatype}.</li>
	 * </ul>
	 * The unit test {@link laas.openrobots.ontology.tests.OpenRobotsOntologyTest#testLiterals()} extensively tests the various literal representation possibilities.
	 * 
	 * @param statement a string containing a statement. For example, {@code "oro:individual rdf:type oro:Class1"} or {@code "oro:individual <> oro:Class1"} 
	 * @return a new Jena statement
	 * @throws IllegalStatementException 
	 * @see #createPartialStatement(String) Partial statements for partially unbound statements (like "?subject rdf:type oro:Class1").
	 * @see #add(String)
	 * @see Namespaces List of known namespace prefixes.
	 */
	public abstract Statement createStatement(String statement)
			throws IllegalStatementException;

	/**
	 * This static method acts as a PartialStatement factory. It does some pre-processing to convert a string to a valid statement relative to the given ontology.<br/>
	 * 
	 * See {@link #createStatement(String)} for sytax details regarding literals.<br/>
	 * To be valid, a partial statement must have at least one variable, prepended with a "?".
	 * 
	 * @param statement a string representing the partial statement. For instance: {@code "?mysterious oro:objProperty2 oro:individual2"}
	 * @return a new partially defined statement
	 * @throws IllegalStatementException
	 * @see #createStatement(String) 
	 * @see #find(String, Vector)
	 */
	public abstract PartialStatement createPartialStatement(String statement)
			throws IllegalStatementException;

	/**
	 * Returns the underlying Jena ontology model.
	 * @return the current underlying Jena ontology model.
	 */
	public abstract OntModel getModel();

	/**
	 * Performs a consistency validation against the ontology. If the check fails, it throws an exception with details on the inconsistencies sources.
	 * @throws InconsistentOntologyException thrown if the ontology is currently inconsistent. The exception message contains details on the source of inconsistency.
	 */
	public abstract Boolean checkConsistency()
			throws InconsistentOntologyException;

	/**
	 * Checks if a statement is asserted or can be inferred from the ontology. If the method returns false, IT DOES NOT mean that the statement itself is false. Most probably, the fact expressed by the statement is simply not known.
	 * 
	 * @param statement the statement to be evaluated
	 * @return true if the statement is asserted in or can be inferred from the ontology
	 */
	public abstract boolean check(Statement statement);

	/** Checks if a pattern represented as a partial statement matches at least one asserted of inferred statement.<br/>
	 * 
	 * For instance:
	 * <ul>
	 * 	<li>A pattern like {@code [?object rdf:type Bottle]} would match all instances of the class {@code Bottle}.</li>
	 *  <li>{@code [anAgent sees ?something]} would match all objects seen by instance "{@code anAgent}".</li>
	 * </ul>
	 * 
	 * @param partial_statement the pattern to be evaluated
	 * @return true if the pattern matches at least one asserted or inferred statement of the ontology
	 * @see #find(String, Vector)
	 */
	public abstract boolean check(PartialStatement partial_statement);
	
	/**
	 * Performs a SPARQL query on the OpenRobots ontology.<br/>
	 * 
	 * For instance:
	 * <pre>
	 *	IOntologyServer myOntology = new OpenRobotsOntology();
	 *	ResultSet result =	myOntology.query(
	 *						"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
	 *						"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
	 *						"SELECT ?instances \n" +
	 *						"WHERE { \n" +
	 *						"?instances rdf:type owl:Thing}\n");
	 *
	 * for ( ; result.hasNext() ; )
	 *	{
	 *		System.out.println(result.nextSolution().toString());
	 *	}
	 * </pre>
	 * This example would print all the instances existing in the ontology.<br/>
	 * Attention! Unlike other methods which take string representation of statements or resource, namespaces or namespace prefixes CAN NOT be omitted: {@code SELECT ?instance WHERE {?instance eats bananas}} won't match the same things as {@code SELECT ?instance WHERE {?instance oro:eats oro:bananas}} even if {@code oro} is the prefix of the default namespace. 
	 * 
	 * @param query A well-formed SPARQL query to perform on the ontology. {@code PREFIX} statements may be omitted if they are the standard ones (namely, owl, rdf, rdfs) or the LAAS OpenRobots ontology (oro) one.
	 * @return The result of the query as a Jena ResultSet.
	 * @see #queryAsXML(String)
	 * @throws QueryParseException thrown if the argument is not a valid SPARQL query.
	 */
	public abstract ResultSet query(String query) throws QueryParseException;

	/**
	 * Like {@link #query(String) query} except it returns a XML-encoded SPARQL result.
	 * 
	 * @param query A well-formed SPARQL query to perform on the ontology. {@code PREFIX} statements may be omitted if they are the standard ones (namely, owl, rdf, rdfs) or the LAAS OpenRobots ontology (oro) one.
	 * @return The result of the query as SPARQL XML string.
	 * @see #query(String)
	 */
	public abstract String queryAsXML(String query);

	/**
	 * Adds a new statement (assertion) to the ontology. Does nothing is the statement already exists.<br/>
	 *   
	 * @param statement The new statement.
	 */
	public abstract void add(Statement statement);
	
	/**
	 * Adds a new statement (assertion) to the ontology from its string representation. Does nothing is the statement already exists.<br/>
	 * 
	 * To create literals, you must suffix the value with {@code ^^xsd:} and the XML schema datatype.</br>
	 * <br/>
	 * For instance:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 * myOntology.add("myIndividual myBooleanPredicate true^^xsd:boolean");
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
	 * @param statement The new statement.
	 * @see #createStatement(String) Syntax details regarding the string describing the statement.
	 * @see #add(Statement)
	 * @see #add(Vector<String>)
	 * @see #remove(Statement)
	 */
	public abstract void add(String statement) throws IllegalStatementException;
	
	/**
	 * Adds a set of statements (assertions) to the ontology from their string representation.<br/>
	 * If the statements are already present, nothing is done.
	 * 
	 * @param statements A vector of string representing statements to be inserted in the ontology.
	 * @see #add(String) for details.
	 */
	public abstract String add(Vector<String> statements) throws IllegalStatementException;
	
	/**
	 * Remove a given statement from the ontology. Does nothing if the statement doesn't exist.
	 * 
	 * @param stmt The statement to remove from the ontology.
	 * @see #add(String)
	 */
	public abstract void remove(Statement stmt);

	/**
	 * Remove a given statement (represented as a string) from the ontology. Does nothing if the statement doesn't exist.
	 * 
	 * @param stmt A string representing the statement to remove from the ontology.
	 * @see #add(String)
	 * @see #remove(Statement)
	 * @see #createStatement(String) Syntax details regarding the string describing the statement.
	 */
	public abstract void remove(String stmt) throws IllegalStatementException;
	
	/**
	 * Remove a set of statements (represented as a strings) from the ontology. Does nothing if the statements don't exist.
	 * 
	 * @param stmts A vector of strings representing the statements to remove from the ontology.
	 * @see #add(Vector)
	 * @see #remove(String)
	 */
	public abstract void remove(Vector<String> stmts) throws IllegalStatementException;
	
	/**
	 * Tries to identify a resource given a set of partially defined statements (plus optional restrictions) about this resource.<br/>
	 * 
	 * First simple example:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 *
	 * Vector<Statement> knowledge = new Vector<Statement>();
	 *	try {
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:isEdibleBy ns:monkey"));
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:color "yellow"^^ns:color"));
	 *	} catch (IllegalStatementException e) {
	 *		e.printStackTrace();
	 *	}
	 *
	 * Vector<Resource> results = myOntology.find("mysterious_object", knowledge);
	 * for (Resource res:results)
	 * 		System.out.println(res.getLocalName());
	 * </pre>
	 * Supposing your ontology defines the right properties and instances, you can expect this example to output something like {@code ns:banana}.<br/>
	 * <br/>
	 * Example with restrictions:
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 *
	 * Vector<Statement> knowledge = new Vector<Statement>();
	 * Vector<String> filters = new Vector<String>();
	 * 
	 *	try {
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:isEdibleBy ns:monkey"));
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:color "yellow"^^ns:color"));
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:size ?size));
	 *	} catch (IllegalStatementException e) {
	 *		e.printStackTrace();
	 *	}
	 *
	 * filters.add("?size >= 200.0");
	 * filters.add("?size < 250.0");
	 *
	 * Vector<Resource> results = myOntology.find("mysterious_object", knowledge, filters);
	 * for (Resource res:results)
	 * 		System.out.println(res.getLocalName());
	 * </pre>
	 * This example would output all the {@code ns:banana}s whose size is comprised between 200 and 250mm (assuming mm is the unit you are using...).
	 * 
	 * @param varName The name of the variable to identify, as used in the statements.
	 * @param statements The partial statement statements defining (more or less) the resource your looking for.
	 * @param filters a vector of string containing the various filters you want to append to your search. The syntax is the SPARQL one (as defined here: http://www.w3.org/TR/rdf-sparql-query/#tests).
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @see #guess(String, Vector, double)
	 * @see PartialStatement
	 */
	public abstract Vector<Resource> find(String varName,
			Vector<PartialStatement> statements, Vector<String> filters);

	/**
	 * Tries to identify a resource given a set of partially defined statements about this resource.<br/>
	 * 
	 * This is a simpler form for {@link #find(String, Vector, Vector)}, without filters.
	 * 
	 * @param varName The name of the variable to identify, as used in the statements.
	 * @param statements The partial statement statements defining (more or less) the resource your looking for.
	 * @return A vector of resources which match the statements. An empty vector is no matching resource is found.
	 * @see #find(String, Vector, Vector)
	 */
	public abstract Vector<Resource> find(String varName,
			Vector<PartialStatement> statements);

	/**
	 * Tries to approximately identify an individual given a set of known statements about this resource.<br/>
	 * The individual which is looked for must be currently the <b>subject</b> of all these statements.<br/><br/>
	 * 
	 * Following steps are achieved:
	 * <ol>
	 * <li>For each statement, a SPARQL query is issued by replacing the object by a variable.</li>
	 * <li>This query returns a first list of individuals bound with the statement predicate to some object.</li>
	 * <li>For each member of this list, a "distance" to the original statement is measured and "matching quality level" is stored. The relevant metrics are delegated to...</li>
	 * TODO ...what delegates ?<br/>
	 * <li>These steps are reiterated over the vector of given statements. If new matching individuals are discover, we append them to the individuals list. If they already exist in the list, their previous matching quality level is multiplied with the new one.</li>
	 * <li>At the end, all individuals whose matching quality level is below the threshold are discarded.</li>
	 * </ol>
	 * <br/>
	 * Please note that:
	 * <ul>
	 * <li>If a statement involves a non-functional property (ie the subject may have several time this property with different objects), the statement is currently ignored.</li>
	 * </ul>
	 * 
	 * <b>For example:</b><br/>
	 * If your ontology defines such a statement:<br/>
	 * {@code ns:bottle ns:size "120"^^xsd:integer"}<br/>
	 * Then: 
	 * <pre>
	 * IOntologyServer myOntology = new OpenRobotsOntology();
	 *
	 * Vector<Statement> knowledge = new Vector<Statement>();
	 *	try {
	 *		knowledge.add(myOntology.createStatement("?mysterious_object ns:size "100"^^xsd:integer"));
	 *	} catch (IllegalStatementException e) {
	 *		e.printStackTrace();
	 *	}
	 *
	 * Hashtable<Resource, Double> matchingResources = myOntology.guess("mysterious", statements, 0.6);
	 *
	 *	Iterator<Resource> res = matchingResources.keySet().iterator();
	 *	while(res.hasNext())
	 *	{
	 *		Resource currentRes = res.next();
	 *		System.out.println(currentRes.getLocalName() + " (match quality: " + matchingResources.get(currentRes) + ").");
	 *	}
	 * </pre>
	 *  should output something like {@code "ns:bottle (match quality: 0.8)"}.
	 * 
	 * @param varName The name of the variable to identify, as used in the statements.
	 * @param partialStatements The statements defining (more or less) the resource your looking for.
	 * @param threshold the match quality threshold to hold and return a resource.
	 * @return a table containing the matching individuals and the corresponding match quality level (from 0.0: no match to 1.0: perfect match).
	 * @throws UnmatchableException thrown if we don't know how to compare (ie to calculate a distance) two nodes. TODO : improve this description.
	 * @see #find(String, Vector)
	 */
	public abstract Hashtable<Resource, Double> guess(String varName,
			Vector<PartialStatement> partialStatements, double threshold)
			throws UnmatchableException;

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
	 */
	public abstract Model getInfos(String lex_resource)
			throws NotFoundException;

	/**
	 * Like {@link #getInfos(String)} but using a Jena {@link com.hp.hpl.jena.rdf.model.Resource resource} instead of a string as input.
	 * 
	 * @param resource A Jena resource.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the resource doesn't exist in the ontology.
	 * @see #getInfos(String)
	 */
	public abstract Model getInfos(Resource resource) throws NotFoundException;

	
	/**
	 * Remove all statements matching the partial statement.
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
	 * 
	 * @param partialStmt The partial statement representing a "mask" of statements to delete.
	 */
	public abstract void clear(PartialStatement partialStmt);
	
	/**
	 * Remove all statements matching the partial statement.
	 * 
	 * @param partialStmt The lexical form of a partial statement representing a "mask" of statements to delete.
	 * @throws IllegalStatementException thrown if the string does not represent a valid partial statement.
	 * @see {@link #clear(PartialStatement)} for an example.
	 * @see PartialStatement
	 */
	public abstract void clear(String partialStmt) throws IllegalStatementException;
	
	
	/**
	 * Saves the in-memory ontology model to a RDF/XML file.
	 * 
	 * @param path The path and name of the OWL file to save to (for instance {@code ./ontos/saved.owl})
	 */
	public abstract void save(String path);
	
	/**
	 * Allows to register several <em>events providers</em> (typically, one by underlying middleware) which in turn provide access to <em>watchers</em>. Watchers expose a <em>watch expression</em> which is a SPARQL <code>ASK</code> query. Every time a change is made on the ontology, the ontology backend which implements this interface is expected to execute this query against the model and notify the watchers (through {@link IWatcher#notifySubscriber()}) if the result is positive.
	 * @param eventsProviders A set of event providers.
	 * @see IWatcher, IEventsProvider
	 */
	public void registerEventsHandlers(HashSet<IEventsProvider> eventsProviders);


}