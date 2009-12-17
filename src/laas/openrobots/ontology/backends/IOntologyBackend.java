package laas.openrobots.ontology.backends;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.exceptions.UnmatchableException;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.IServiceProvider;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

/** This interface describes the abstract behaviour of an ontology backend. It presents the list of operation the "knowledge store" should provide to be used with the {@linkplain laas.openrobots.ontology.OroServer ontology server}.<br/>
 * <br/>
 * Please note that, since annotation can not be inherited in Java 1.6, it is useless to link this interface to the {@link laas.openrobots.ontology.service.IServiceProvider} interface. However, all classes implementing the {@link IOntologyBackend} must implement as well {@link IServiceProvider}.
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
	 * Adds a new statement (assertion) to the ontology. Does nothing is the statement already exists.<br/>
	 * A memory profile is associated to the statement: statements associated to {@link MemoryProfile.LONGTERM} or {@link MemoryProfile.DEFAULT} are stored and never removed from the ontology while other memory profiles allow the ontology to "forget" about certains facts after a given amount of time.
	 *   
	 * @param statement The new statement.
	 * @param memProfile The memory profile associated to this statement.
	 * @param safe If true, the statement is added only if it does not 
	 * @return True is the statement has been actually added to the model 
	 * (actually useful only in conjunction with the {@code safe} parameter enabled).
	 */
	public abstract boolean add(Statement statement, MemoryProfile memProfile, boolean safe);
		
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
	 * @return The result of the query as a Jena ResultSet.
	 * @see #queryAsXML(String)
	 * @throws QueryParseException thrown if the argument is not a valid SPARQL query.
	 */
	public abstract ResultSet query(String query) throws QueryParseException,
			QueryExecException;

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
	 * @return a result set of resources that match the statements.
	 * @see {@link #guess(String, Vector, double)}
	 * @see BaseModule#find(String, Set, Set) Examples of use
	 */
	public abstract ResultSet find(	String varName,	
							Set<PartialStatement> statements, 
							Set<String> filters);
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
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract Hashtable<Resource, Double> guess(String varName,
			Vector<PartialStatement> partialStatements, double threshold)
			throws UnmatchableException;

	public abstract Set<OntClass> getSuperclassesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntClass> getSubclassesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntResource> getInstancesOf(OntClass type,
			boolean onlyDirect) throws NotFoundException;

	public abstract Set<OntClass> getClassesOf(Individual individual,
			boolean onlyDirect) throws NotFoundException;

	/**
	 * Returns the id and type (INSTANCE, CLASS, OBJECT_PROPERTY, DATATYPE_PROPERTY, UNDEFINED) of the concept whose label or id match the given parameter. If several concepts match, an randomly choosen one is returned.
	 * 
	 * @param label the label (in any language) or id to look for.
	 * @return A list made of the id of the concept whose label matchs the parameter followed by its type.
	 * @throws NotFoundException
	 * @see ResourceType
	 * @see SocketConnector General syntax of RPCs for the oro-server socket connector.
	 */
	public abstract List<String> lookup(String id) throws NotFoundException;

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