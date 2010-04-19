package laas.openrobots.ontology.backends;

import java.util.List;
import java.util.Set;
import java.util.Vector;

import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

/** This interface describes the abstract behaviour of an ontology backend. It 
 * presents the list of operation the "knowledge store" should provide to be 
 * used with the {@linkplain laas.openrobots.ontology.OroServer ontology server}.
 * <br/>
 * 
 * @author slemaign
 *
 */
public interface IOntologyBackend extends IServiceProvider {



	/**
	 * Helper to create a {@link com.hp.hpl.jena.rdf.model.OntProperty property} attached at the current OpenRobotOntology by mapping the method to the underlying ontology model.<br/>
	 * This is a shortcut for {@code OpenRobotOntology.getModel().createProperty(Namespaces.format(lex_property))}
	 * @param lex_property the lexical form of the property (eg {@code "rdf:type"}).
	 * @return the corresponding Jena property.
	 * @see com.hp.hpl.jena.rdf.model.OntModel#createOntProperty(String)
	 */
	public abstract OntProperty createProperty(String lex_property);

	/**
	 * Helper to create a {@link com.hp.hpl.jena.rdf.model.OntResource resource} attached at the current OpenRobotOntology by mapping the method to the underlying ontology model.<br/>
	 * This is a shortcut for {@code OpenRobotOntology.getModel().createResource(Namespaces.format(lex_resource))}.
	 * 
	 * If a resource with the same lexical form already exist, it is reused. The
	 * {@link #getResource(String)} method can be used to retrieve resource
	 * without creating a new one if it doesn't exist.
	 * 
	 * @param lex_resource the lexical form of the resource.
	 * @return the corresponding Jena resource.
	 * @see #getResource(String)
	 * @see com.hp.hpl.jena.rdf.model.OntModel#createOntResource(String)
	 */
	public abstract OntResource createResource(String lex_resource);
	
	/**
	 * Try to retrieve a resource from the ontology, based on its lexical form.
	 * 
	 * @param lex_resource The URI of a resource in the ontology.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the resource doesn't exist in the ontology.
	 * @see #getSubmodel(OntResource)
	 * @see #createResource(String)
	 */
	public abstract OntResource getResource(String lex_resource) throws NotFoundException;
	
	/**
	 * Returns the set of inferred and asserted statement involving a resource as a Jena Model..
	 * 
	 * @param resource A Jena resource.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the resource doesn't exist in the ontology.
	 * @see #getSubmodel(String)
	 */
	public abstract Model getSubmodel(Resource node) throws NotFoundException;

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
	 * Adds a set of new statements (assertion) to the ontology. If one of the
	 * statements already exists, it won't be inserted.<br/>
	 * A memory profile is associated to all the statements: statements 
	 * associated to {@link MemoryProfile.LONGTERM} or {@link MemoryProfile.DEFAULT} 
	 * are stored and never removed from the ontology while other memory 
	 * profiles allow the ontology to "forget" about certains facts after a 
	 * given amount of time.
	 *   
	 * @param statements A set of statements to be inserted in the model.
	 * @param memProfile The memory profile associated to this statement.
	 * @param safe If true, the statement is added only if it does not 
	 * @return True if all the statements have been actually added to the model 
	 * (actually useful only in conjunction with the {@code safe} parameter enabled).
	 * @throws IllegalStatementException Currently only thrown if a concept is asserted 
	 * to be both an instance and a class.
	 */
	boolean add(Set<Statement> statements, MemoryProfile memProfile,
			boolean safe) throws IllegalStatementException;

	/**
	 * Adds a new statement (assertion) to the ontology.
	 * 
	 * @see #add(Set, MemoryProfile, boolean)
	 */
	public abstract boolean add(Statement statement, MemoryProfile memProfile, 
			boolean safe) throws IllegalStatementException;
	
	/**
	 * Checks if a statement is asserted or can be inferred from the ontology. 
	 * The check is done in an open world (everything is true except if it's 
	 * explicitely false).
	 * 
	 * For instance, if only the following statement is asserted:
	 * <pre>
	 * bottle hasColor green
	 * </pre>
	 * {@code check("bottle hasColor orange")} would return {@code true}.
	 * 
	 * If the two following statements are added to the ontology:
	 * <pre>
	 * green owl:isDifferentFrom orange
	 * hasColor rdf:type owl:functionalProperty
	 * </pre>
	 * then {@code check("bottle hasColor orange")} would return {@code 
	 * false}.
	 * 
	 * @param statement the statement to be evaluated
	 * @return true if the statement is asserted in or can be inferred from the 
	 * ontology
	 */
	public abstract boolean check(Statement statement);
	
	/** Checks if a pattern represented as a partial statement matches at least 
	 * one asserted of inferred statement.<br/>
	 * 
	 * For instance:
	 * <ul>
	 * 	<li>A pattern like {@code [?object rdf:type Bottle]} would match all 
	 * 		instances of the class {@code Bottle}.</li>
	 *  <li>{@code [anAgent sees ?something]} would match all objects seen by 
	 *  	instance "{@code anAgent}".</li>
	 * </ul>
	 * 
	 * @param statement the partial pattern to be evaluated
	 * @return true if the pattern matches at least one asserted or inferred 
	 * statement of the ontology.
	 * @see #find(String, Vector)
	 */
	public abstract boolean check(PartialStatement statement);

	/**
	 * Performs a consistency validation against the ontology. If the check 
	 * fails, it throws an exception with details on the inconsistencies sources.
	 * @throws InconsistentOntologyException thrown if the ontology is currently 
	 * inconsistent. The exception message contains details on the source of 
	 * inconsistency.
	 */
	public abstract void checkConsistency()
			throws InconsistentOntologyException;

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
	 * 
	 * Attention! Unlike other methods which take string representation of 
	 * statements or resource, namespaces or namespace prefixes CAN NOT be 
	 * omitted: {@code SELECT ?instance WHERE {?instance eats bananas}} won't 
	 * match the same things as {@code SELECT ?instance WHERE {?instance 
	 * oro:eats oro:bananas}} even if {@code oro} is the prefix of the default 
	 * namespace. 
	 * 
	 * @param query A well-formed SPARQL query to perform on the ontology. {@code PREFIX} statements may be omitted if they are the standard ones (namely, owl, rdf, rdfs) or the LAAS OpenRobots ontology (oro) one.
	 * @param query 
	 * @return The result of the query as a Jena ResultSet.
	 * @see #queryAsXML(String)
	 * @throws QueryParseException thrown if the argument is not a valid SPARQL query.
	 */
	public abstract Set<String> query(String key, String query) throws InvalidQueryException;

	/**
	 * Tries to identify a resource given a set of partially defined statements 
	 * (plus optional restrictions) about this resource.
	 * 
	 * @param varName The name of the variable to bind, as used in the partial 
	 * statements.
	 * @param statements A set of partial statements that globaly define a search
	 * pattern
	 * @param filters a vector of string containing the various filters to be 
	 * appended to the search. The syntax is the SPARQL one (as defined here:
	 *  http://www.w3.org/TR/rdf-sparql-query/#tests).
	 * @return a set of resources that match the statements.
	 * @throws InvalidQueryException 
	 * @see BaseModule#find(String, Set, Set) Examples of use
	 */
	public abstract Set<String> find(	String varName,	
							Set<PartialStatement> statements, 
							Set<String> filters) throws InvalidQueryException;

	public abstract Set<OntClass> getSuperclassesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntClass> getSubclassesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntResource> getInstancesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntClass> getClassesOf(OntResource individual,
			boolean onlyDirect) throws NotFoundException;

	/**
	 * Returns the set of all [id, type, label] (with type one of INSTANCE, CLASS, 
	 * OBJECT_PROPERTY, DATATYPE_PROPERTY, UNDEFINED) of concepts whose labels
	 *  or id match the given parameter.
	 * 
	 * @param label the label (in any language) or id to look for.
	 * @return A list made of the id of the concept whose label matches the 
	 * parameter followed by its type, or an empty set if nothing was found.
	 * @see ResourceType
	 * @see #lookup(String, ResourceType)
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract Set<List<String>> lookup(String id);

	/**
	 * Returns the set of all id of concepts whose labels or ids match the given
	 *  parameter and of the given type.
	 * 
	 * @param label the label (in any language) or id to look for.
	 * @param type the type of the resource that is looked for.
	 * @return A set of ids whose label matches the parameter or an empty set.
	 * @see ResourceType
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract Set<String> lookup(String id, ResourceType type);
	
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
	 * Remove a given statement from the ontology. Does nothing if the statement doesn't exist.
	 * 
	 * @param stmt The statement to remove from the ontology.
	 * @see #add(String)
	 */
	public abstract void remove(Statement stmt);

	/**
	 * Saves the in-memory ontology model to a RDF/XML file.
	 * 
	 * @param path The path and name of the OWL file to save to (for instance {@code ./ontos/saved.owl})
	 * @throws OntologyServerException thrown when the output path is not valid or not accessible.
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract void save(String path) throws OntologyServerException;

	/**
	 * Allows to register several <em>events providers</em> (typically, one by underlying middleware) which in turn provide access to <em>watchers</em>. Watchers expose a <em>watch expression</em> which is a SPARQL <code>ASK</code> query. Every time a change is made on the ontology, the ontology backend which implements this interface is expected to execute this query against the model and notify the watchers (through {@link IWatcher#notifySubscriber()}) if the result is positive.
	 * @param watcherProviders A set of event providers.
	 * @throws EventRegistrationException 
	 * @see IWatcher, IEventsProvider
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract void registerEvent(
			IWatcher watcher) throws EventRegistrationException;
	
	/**
	 * Return the list of event types implemented (hence usable) by this backend.
	 * 
	 * @return The list of event type supported by the backend
	 */
	public Set<EventType> getSupportedEvents();

}