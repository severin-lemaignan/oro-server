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

package laas.openrobots.ontology.backends;

//Imports
///////////////
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.InvalidQueryException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.Pair;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.EventProcessor;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.modules.memory.MemoryManager;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.RPCMethod;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.utils.VersionInfo;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * The OpenRobotsOntology class is the main storage backend for oro-server.<br/>
 * 
 * It maps useful methods for knowledge access in a robotic context to a 
 * Jena-baked {@link com.hp.hpl.jena.ontology.OntModel ontology}.<br/>
 * <br/>
 * Amongst other feature, it offers an easy way to {@linkplain #query(String) 
 * query} the ontology with standard SPARQL requests, it can try to 
 * {@linkplain #find(String, Vector) find} resources matching a set of 
 * statements or {@linkplain #checkConsistency() check the consistency} of the
 * knowledge storage.<br/><br/>
 * 
 * Examples covering the various aspects of the API can be found in the 
 * {@linkplain laas.openrobots.ontology.tests Unit Tests}.
 *  
 * @author Severin Lemaignan <i>severin.lemaignan@laas.fr</i>
 */
public class OpenRobotsOntology implements IOntologyBackend {
	
	private OntModel onto;
	
	private ResultSet lastQueryResult;
	private String lastQuery;
	
	private boolean isInInconsistentState;
	
	private Properties parameters;

	
	private Map<String, Set<Pair<String, ResourceType>>> lookupTable;
	private boolean modelChanged = true;
	private boolean forceLookupTableUpdate = false; //useful to systematically rebuild the lookup table when a statement has been removed.
	
	/*This set stores *at initialization* the list of functional properties.
	* This is used by the update() method to quickly discard update on non-
	* functional properties.
	* 
	* \TODO Attention: if a functional property is added at runtime, it won't
	* be correctly handled (ie, update() won't work for this property. This
	* could be fixed by updating this set when a FunctionalProperty is added. Or
	* use OntProperty#hasSubproperty() (but this will be slower)
	*/
	Set<OntProperty> functionalProperties;
	
	private MemoryManager memoryManager;

	private EventProcessor eventProcessor;

	//True if this model has already been closed
	private boolean isClosed;
	
	/***************************************
	 *          Constructors               *
	 **************************************/
	
	public OpenRobotsOntology(){
		this(OroServer.ServerParameters);
	}
	
	/**
	 * Constructor which takes a config file as parameter.<br/>
	 * The constructor first opens the ontology, then loads it into memory and 
	 * eventually bounds it to Jena internal reasoner. Thus, the instanciation 
	 * of OpenRobotsOntology may take some time (several seconds, depending on 
	 * the size on the ontology).<br/>
	 * <br/>
	 * Available options:<br/>
	 * <ul>
	 * <li><em>verbose = [true|false]</em>: set it to <em>true</em> to get more infos from the engine.</li>
	 * <li><em>ontology = PATH</em>: the path to the OWL (or RDF) ontology to be loaded.</li> 
	 * <li><em>default_namespace = NAMESPACE</em>: set the default namespace. Don't forget the trailing #!</li>
	 * <li><em>short_namespaces = [true|false]</em> (default: true): if true, the 
	 * ontology engine will return resource with prefix instead of full URI, or 
	 * nothing if the resource is in the default namespace.</li>
	 * </ul>
	 * The file may contain other options, related to the server configuration. 
	 * See {@link laas.openrobots.ontology.OroServer}. Have a look as well at the 
	 * config file itself for more details.
	 * 
	 * @param parameters The set of parameters, read from the server configuration file.
	 */
	public OpenRobotsOntology(Properties parameters){
		if (parameters == null) throw new IllegalArgumentException();
		this.parameters = parameters;
		initialize();
	}
	
	/**
	 * Constructor which takes a Jena {@linkplain OntModel} as parameter.<br/>
	 * 
	 * @param onto An already built ontology model. The OpenRobotsOntology will 
	 * wrap it.
	 */
	public OpenRobotsOntology(OntModel onto){
		this(onto, OroServer.ServerParameters);
	}

	public OpenRobotsOntology(OntModel onto, Properties parameters){
		
		if (onto == null) throw new IllegalArgumentException();
		this.onto = onto;
		
		if (parameters == null) throw new IllegalArgumentException("No " + 
				"parameters provided in OroServer to instanciate agent models");
		this.parameters = parameters;
		
		isClosed = false;
		
		initialize();
	}
	
	/***************************************
	 *       Accessors and helpers         *
	 **************************************/
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#createProperty(java.lang.String)
	 */
	public OntProperty createProperty(String lex_property){

		OntProperty p = onto.createOntProperty(Namespaces.expand(lex_property));
		
		return p;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createResource(java.lang.String)
	 */
	public OntResource createResource(String lex_resource){
		
		OntResource r = onto.createOntResource(Namespaces.expand(lex_resource));
		
		return r;
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createStatement(java.lang.String)
	 */
	public Statement createStatement(String statement) throws IllegalStatementException {
	
		Resource subject;
		Property predicate;
		RDFNode object;
		
		List<String> tokens_statement = Helpers.tokenize(statement.trim(), ' ');
				
		if (tokens_statement.size() != 3)
			throw new IllegalStatementException(
					"Three tokens are expected in a statement, " +	
					tokens_statement.size() + " found in " + statement + ".");
		
		//expand the namespaces for subject and predicate.
		for (int i = 0; i<2; i++){
			tokens_statement.set(i, Namespaces.format(tokens_statement.get(i)));
		}
		
		
		subject = onto.getResource(tokens_statement.get(0));
		predicate = onto.getProperty(tokens_statement.get(1));

		//Handle objects
		
		object = Helpers.parseLiteral(tokens_statement.get(2), (ModelCom)onto);
		
		
		assert(object!=null);
		
		Statement s =new StatementImpl(subject, predicate, object);
	
		return s; 
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createPartialStatement(java.lang.String)
	 */
	public PartialStatement createPartialStatement(String statement) throws IllegalStatementException {
		
		PartialStatement p = new PartialStatement(statement, (ModelCom)getModel());
		
		return p;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#getModel()
	 */
	public OntModel getModel(){
		return onto;
	}
	
	/***************************************
	 *           Public methods            * 
	 *									   *
	 **************************************/
	
	/** Classify the underlying model.
	 * 
	 * Be careful, this method is not thread-safe but modifies the model.
	 * 
	 * Typically, it should be called inside of a
	 * model.enterCriticalSection(Lock.WRITE);
	 * ...
	 * model.leaveCriticalSection();
	 */
	public void classify() {
		try {
			((PelletInfGraph) onto.getGraph()).classify();
			
			// If we reach this point, the ontology is consistent
			if (isInInconsistentState) {
				Logger.log("The ontology is back in a consistent state\n ", 
						VerboseLevel.WARNING);
				isInInconsistentState = false;
			}

		}
		catch (org.mindswap.pellet.exceptions.InconsistentOntologyException ioe) {
			isInInconsistentState = true;
			Logger.log("The ontology is in an inconsistent state!\n ", 
					VerboseLevel.WARNING);
			Logger.log("Inconsistency causes:\n" + ioe.getMessage() + "\n", VerboseLevel.WARNING, false);
		}
	}
		
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#add(com.hp.hpl.jena.rdf.model.Statement, laas.openrobots.ontology.modules.memory.MemoryProfile)
	 */
	@Override
	public boolean add(Statement statement, MemoryProfile memProfile, boolean safe) throws IllegalStatementException
	{
		Set<Statement> toAdd = new HashSet<Statement>();
		toAdd.add(statement);
		
		return add(toAdd, memProfile, safe);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#add(com.hp.hpl.jena.rdf.model.Statement, laas.openrobots.ontology.modules.memory.MemoryProfile)
	 */
	@Override
	public boolean add(Set<Statement> statements, MemoryProfile memProfile, boolean safe) throws IllegalStatementException
	{
		if (statements.isEmpty()) return true;
		
		boolean allHaveBeenInserted = true;
		
		Logger.demo("Adding", statements);
		
		String ss = "";
		for (Statement s : statements) ss += "\n\t ["+ Namespaces.toLightString(s) + "]";
		Logger.log("Adding statements " + ((memProfile != MemoryProfile.DEFAULT) ? memProfile : "") + ss +"\n");
		
		for (Statement statement : statements) {
		
			try {
				onto.add(statement);
			}
			catch (ConversionException e) {
				Logger.log("Impossible to assert " + statement + ". A concept can not be a class " +
						"and an instance at the same time in OWL DL.\n", VerboseLevel.ERROR);
				throw new IllegalStatementException("Impossible to assert " + statement + 
						". A concept can not be a class and an instance at the same time in OWL DL.");

			}
		
			//If we are in safe mode, we check that the ontology is not inconsistent.
			if (safe) {
				if (!checkConsistency()) {
					onto.remove(statement);	
					Logger.log("...I won't add " + statement + " because it " +
							"leads to inconsistencies!\n", VerboseLevel.IMPORTANT);
					allHaveBeenInserted = false;
					continue;
				}
			}

				
			if (!(memProfile == MemoryProfile.LONGTERM || memProfile == MemoryProfile.DEFAULT)) //not LONGTERM memory
			{
				//create a name for this reified statement (concatenation of "rs" with hash made from S + P + O)
				String rsName = "rs_" + Math.abs(statement.hashCode()); 
				
				onto.createReifiedStatement(Namespaces.addDefault(rsName), statement);
				
				Statement metaStmt = createStatement(rsName + " stmtCreatedOn " + onto.createTypedLiteral(Calendar.getInstance()));
				//Statement metaStmt = oro.createStatement(rsName + " stmtCreatedOn " + toXSDDate(new Date())); //without timezone
				Statement metaStmt2 = createStatement(rsName + " stmtMemoryProfile " + memProfile + "^^xsd:string");
				onto.add(metaStmt);
				onto.add(metaStmt2);

			}
		}						
		
		//TODO: optimization possible for reified statement with onModelChange(rsName)
		//notify the events subscribers.
		if(!isInInconsistentState) onModelChange();
		
		return allHaveBeenInserted;
	}

	

	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#check(com.hp.hpl.jena.rdf.model.Statement, boolean)
	 */
	@Override
	public boolean check(Statement statement) {
		
		Logger.demo("Checking", statement);

		return onto.contains(statement);

	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#check(laas.openrobots.ontology.PartialStatement)
	 */
	@Override
	public boolean check(PartialStatement statement) {
		
		Logger.demo("Checking", statement);
		
		String resultQuery = "ASK { " + statement.asSparqlRow() + " }";
		
		try	{
			Query myQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			return myQueryExecution.execAsk();
		}
		catch (QueryParseException e) {
			Logger.log("internal error during query parsing while trying to check a partial statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		catch (QueryExecException e) {
			Logger.log("internal error during query execution while trying to check a partial statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}

	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#checkConsistency()
	 */
	@Override
	public boolean checkConsistency() {
		
		classify();
		
		return !isInInconsistentState;
		
	}
	

	@Override
	public boolean checkConsistency(Set<Statement> statements) {
		
		boolean consistent = true;
		
		Logger.log("Checking consistency of statements");
		
		for (Statement statement : statements) onto.add(statement);
		
		consistent = checkConsistency();		

		for (Statement statement : statements) onto.remove(statement);
	
		return consistent;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#query(java.lang.String)
	 */
	//public Set<RDFNode> query(String query) throws InvalidQueryException
	public Set<RDFNode> query(String key, String query) throws InvalidQueryException
	{
		//TODO: do some detection to check that the first param is the key, and throw nice exceptions when required.
		
		
		Set<RDFNode> res = new HashSet<RDFNode>();
		
		//Add the common prefixes.
		query = Namespaces.prefixes() + query;
		
		this.lastQuery = query;
		
		try	{
			Query myQuery = QueryFactory.create(query, Syntax.syntaxSPARQL );
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			this.lastQueryResult = myQueryExecution.execSelect();
			
			try {
				while (this.lastQueryResult.hasNext()) {
					QuerySolution s = this.lastQueryResult.nextSolution();
					res.add(s.get(key));
				}
			}
			catch (NoSuchElementException nsee) {} // TODO: workaround for a "NoSuchElementException" that is sometimes thrown by 'hasNext()'
		

		}
		catch (QueryParseException e) {
			Logger.log("Error during query parsing ! ("+ e.getLocalizedMessage() +").", VerboseLevel.ERROR);
			throw new InvalidQueryException("Error during query parsing ! ("+ e.getLocalizedMessage() +")");
		}
		catch (QueryExecException e) {
			Logger.log("Error during query execution ! ("+ e.getLocalizedMessage() +").", VerboseLevel.SERIOUS_ERROR);
			throw new InvalidQueryException("Error during query execution ! ("+ e.getLocalizedMessage() +")");
		}
		
		return res;
	}
	
	@Override
	public Set<RDFNode> find(	String varName,	Set<PartialStatement> statements, 
							Set<String> filters) throws InvalidQueryException {
		
		Logger.demo("Looking for '" + varName + "' such as:", statements);
		
		String query = "SELECT ?" + varName + "\n" +
		"WHERE {\n";
		for (PartialStatement ps : statements)
		{
			query += ps.asSparqlRow();
		}
		
		if (!(filters == null || filters.isEmpty())) 
		{
			for (String filter :filters)
			{
				query += "FILTER (" + filter + ") .\n";
			}
		}		
		query += "}";
		
		Set<RDFNode> res = query(varName, query);
		
		Logger.demo_nodes("Result", res);
		
		return res;
	}


	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getResource(String)
	 */
	@Override
	public OntResource getResource(String lex_resource) throws NotFoundException {
		
		lex_resource = Namespaces.format(lex_resource);
	
		OntResource node = onto.getOntResource(lex_resource);

		//TODO : is it necessary to check the node exists? if it doesn't exist, the SPARQL query will answer an empty resultset.
		// This check is only useful to throw an exception...
		if (node == null){
			throw new NotFoundException("The node " + lex_resource + " was not found in the ontology (tip: check the namespaces!).");
		}

		
		return node;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getSubmodel(com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public Model getSubmodel(Resource node) throws NotFoundException {
		
		Model resultModel;
		
		
		String resultQuery = Namespaces.prefixes();
		
		//we use the SPARQL query type "DESCRIBE" to get a RDF graph with all the links to this resource.
		// cf http://www.w3.org/TR/rdf-sparql-query/#describe for more details
		resultQuery += "DESCRIBE <" + node.getURI() +">";
		
		try	{
			Query myQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			resultModel = myQueryExecution.execDescribe();
		}
		catch (QueryParseException e) {
			Logger.log("internal error during query parsing while trying the get infos! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			return null;
		}
		catch (QueryExecException e) {
			Logger.log("internal error during query execution while try the get infos! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			return null;
		}
			
		return resultModel;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getSuperclassesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
	@Override
	public Set<OntClass> getSuperclassesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();
		
		
		ExtendedIterator<OntClass> it = type.listSuperClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		
		if (onlyDirect)
			Logger.demo_nodes("Retrieving direct superclasses of " + Namespaces.toLightString(type), result);
		else
			Logger.demo_nodes("Retrieving all superclasses of " + Namespaces.toLightString(type), result);
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getSubclassesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
	@Override
	public Set<OntClass> getSubclassesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();
			
	
		ExtendedIterator<OntClass> it = type.listSubClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon() && !tmp.getURI().equals("http://www.w3.org/2002/07/owl#Nothing") ){
				result.add(tmp);
			}
		}
		
		if (onlyDirect)
			Logger.demo_nodes("Retrieving direct subclasses of " + Namespaces.toLightString(type), result);
		else
			Logger.demo_nodes("Retrieving all subclasses of " + Namespaces.toLightString(type), result);
		
		
		return result;
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getInstancesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
	@Override
	public Set<OntResource> getInstancesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntResource> result = new HashSet<OntResource>();
		
		ExtendedIterator<? extends OntResource> it = type.listInstances(onlyDirect);
		while (it.hasNext())
		{
			OntResource tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		
		if (onlyDirect)
			Logger.demo_nodes("Retrieving direct instances of " + Namespaces.toLightString(type), result);
		else
			Logger.demo_nodes("Retrieving all instances of " + Namespaces.toLightString(type), result);
		
		
		return result;
	}
	
	
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getClassesOf(com.hp.hpl.jena.ontology.Individual, boolean)
	 */
	@Override
	public Set<OntClass> getClassesOf(OntResource resource, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();
		
		
		Individual individual = null;
		
		try {
			individual = resource.asIndividual();
		}
		catch (ConversionException ce) {
			throw new NotFoundException(Namespaces.toLightString(resource) + " is not an individual!");
		} 

		ExtendedIterator<OntClass> it = individual.listOntClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		

		if (onlyDirect)
			Logger.demo_nodes("Retrieving direct classes of " + Namespaces.toLightString(resource), result);
		else
			Logger.demo_nodes("Retrieving all classes of " + Namespaces.toLightString(resource), result);
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#lookup(java.lang.String)
	 */
	@Override
	public Set<List<String>> lookup(String id) {
		
		//if statements have been removed, we must force a rebuilt of the lookup
		//table else a former concept that doesn't exist anymore could be returned.
		//In any case, if we can not find the id, we try to rebuild the lookup 
		//table, in case some changes occurred since the last lookup.
		//if (forceLookupTableUpdate || !lookupTable.containsKey(id.toLowerCase())) {
		if (modelChanged) {
			forceLookupTableUpdate = true;
			rebuildLookupTable();			
		}
		
		Logger.log("Looking up for " + id + "...\n");
		
		Set<List<String>> result = new HashSet<List<String>>();		
		
		if (lookupTable.containsKey(id.toLowerCase()))
		{
			for (Pair<String, ResourceType> p : lookupTable.get(id.toLowerCase())) {
				List<String> l = new ArrayList<String>();
				l.add(p.getLeft());
				l.add(p.getRight().toString());
				result.add(l);
			}
		}
		
		String ss = "[";
		for (List<String> s : result) ss += s.get(0) + " -> " + s.get(1) + ", ";
		Logger.log("\t => found: " + ss + "]\n", false);
		
		return result;
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#lookup(java.lang.String, ResourceType)
	 */
	@Override
	public Set<String> lookup(String id, ResourceType type) {
		
		//if statements have been removed, we must force a rebuilt of the lookup
		//table else a former concept that doesn't exist anymore could be returned.
		//In any case, if we can not find the id, we try to rebuild the lookup 
		//table, in case some changes occurred since the last lookup.
		if (forceLookupTableUpdate || !lookupTable.containsKey(id.toLowerCase())) {
			forceLookupTableUpdate = true;
			rebuildLookupTable();			
		}
		
		Logger.log("Looking up for " + id + "of type " + type + "...\n");
				
		Set<String> result = new HashSet<String>();		
		
		if (lookupTable.containsKey(id.toLowerCase()))
		{
			for (Pair<String, ResourceType> p : lookupTable.get(id.toLowerCase()))
				if (p.getRight().equals(type))
					result.add(p.getLeft());
		}
		
		String ss = "[";
		for (String s : result) ss += s + ", ";
		Logger.log("\t => found: " + ss + "]\n", false);
		
		return result;
		
	}
	
	/**
	 * Returns the current set of parameters.
	 * 
	 * @return the current set of parameters, reflecting the content of the 
	 * configuration file.
	 */
	public Properties getParameters(){
		return parameters;		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#clear(laas.openrobots.ontology.PartialStatement)
	 */
	@Override
	public void clear(PartialStatement partialStmt) throws OntologyServerException {
		Logger.log("Clearing statements matching ["+ partialStmt + "]\n");
		
		Selector selector = new SimpleSelector(partialStmt.getSubject(), partialStmt.getPredicate(), partialStmt.getObject());
		
		StmtIterator stmtsToRemove = null;
		Set<Statement> setToRemove = new HashSet<Statement>();

		stmtsToRemove = onto.listStatements(selector);
		if (stmtsToRemove != null) setToRemove = stmtsToRemove.toSet();
		
		
		remove(setToRemove);
		
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#remove(com.hp.hpl.jena.rdf.model.Statement)
	 */
	@Override
	@Deprecated
	public void remove(Statement stmt) throws OntologyServerException {
		Set<Statement> toRemove = new HashSet<Statement>();
		toRemove.add(stmt);
		
		remove(toRemove);
	}
	
	@Override
	public void remove(Set<Statement> stmts) throws OntologyServerException {
		
		String ss = "";
		for (Statement s : stmts) ss += "\n\t ["+ Namespaces.toLightString(s) + "]";
		Logger.log("Removing statements " + ss +"\n");
		
		onto.remove(new ArrayList<Statement>(stmts));
		
		//notify the events subscribers.
		if (!isInInconsistentState) onModelChange();
		
		//force the rebuilt of the lookup table at the next lookup.
		forceLookupTableUpdate = true;

	}
	

	@Override
	public void update(Set<Statement> stmts) throws IllegalStatementException, InconsistentOntologyException, OntologyServerException {
		
		Set<Statement> stmtsToRemove = new HashSet<Statement>();
		
		for (Statement stmt : stmts) {
			if(functionalProperties.contains(stmt.getPredicate())) {
				Selector selector = new SimpleSelector(stmt.getSubject(), stmt.getPredicate(), (RDFNode)null);
				
				StmtIterator stmtsToRemoveIt = null;
				try {
					stmtsToRemoveIt = onto.listStatements(selector);
					stmtsToRemove.addAll(stmtsToRemoveIt.toSet());
				} catch (org.mindswap.pellet.exceptions.InconsistentOntologyException ioe) {
					Logger.log("The ontology is in an inconsistent state! I couldn't " +
							"update any statements.\n ", VerboseLevel.WARNING);
					throw new InconsistentOntologyException("The ontology is in an inconsistent state! I couldn't " +
							"update any statements.");
				}				
			}
		}

		remove(stmtsToRemove);
		add(stmts, MemoryProfile.DEFAULT, false);
		
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#save(java.lang.String)
	 */
	@Override
	@RPCMethod(
			category = "administration",
			desc="exports the current ontology model to an OWL file. The provided path must be writable by the server."
	)
	public void save(String path) throws OntologyServerException {
		Logger.log("Saving ontology to " + path +".\n", VerboseLevel.IMPORTANT);
		FileOutputStream file;
		try {
			file = new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			throw new OntologyServerException("Error while opening " + path + " to output the ontology. Check it's a valid filename and a writable location!");
		}

		onto.write(file);
		
	}
	
	@RPCMethod(
			category = "administration",
			desc="exports the current ontology model to an OWL file. The file " +
					"will be saved to the current directory with an automatically" +
					"generated name."
	)
	public void save() throws OntologyServerException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		save(sdf.format(Calendar.getInstance().getTime()) + "-snapshot.owl");
		
	}
	
	
	@RPCMethod(
			category = "administration",
			desc = "Lists on the serveur stdout all facts matching a given pattern."
	)
	public void list(String pattern) throws IllegalStatementException {
		
		PartialStatement partialStmt = createPartialStatement(pattern);
		
		Selector selector = new SimpleSelector(	partialStmt.getSubject(), 
												partialStmt.getPredicate(), 
												partialStmt.getObject());
		
		StmtIterator stmtsToList = null;
		
		String list = "List of statements matching [" + pattern + "]:\n";
		
		stmtsToList = onto.listStatements(selector);
		
		if (stmtsToList != null && stmtsToList.hasNext()) {
			while(stmtsToList.hasNext()) {
				list += "\t" + 
						Namespaces.toLightString(stmtsToList.nextStatement()) + 
						"\n";
			}
		}
		else list += "None\n";
		
		Logger.info(list);
		

	}

	@Override
	public void registerEvent(IWatcher watcher) throws EventRegistrationException {
		eventProcessor.add(watcher);
	}
	
	@Override
	public void clearEvents() {
		eventProcessor.clear();
	}
	
	@Override
	public void clearEvent(IWatcher watcher) throws OntologyServerException {
		eventProcessor.remove(watcher);
	}

	@Override
	public Set<EventType> getSupportedEvents() {
		return eventProcessor.getSupportedEvents();
	}
	
	/***************************************
	 *          Private methods            *
	 **************************************/
	

	private void initialize(){
		
		this.lastQuery = "";
		this.lastQueryResult = null;
		this.isInInconsistentState = true;
					
		Namespaces.loadNamespaces(parameters);
		
		if (onto == null) this.load();

		//Force these value to true to ensure the lookup table is build at 
		// startup
		modelChanged = true;
		forceLookupTableUpdate = true;
		this.lookupTable = new HashMap<String, Set<Pair<String, ResourceType>>>();
		this.rebuildLookupTable();
		
		this.functionalProperties = new HashSet<OntProperty>();
		this.rebuildFunctionalPropertiesList();
		
		// By default, don't enable the memory manager.
		if (parameters.getProperty("memory_manager", "false").equalsIgnoreCase("true")) {
			memoryManager = new MemoryManager(onto);
			memoryManager.start();
		}
		
		eventProcessor = new EventProcessor(this);
		
	}
	
	public void close() {
		
		if (isClosed)
			return;
		
		if (memoryManager != null) {
			memoryManager.close();
			try {
				memoryManager.join(1000);
			} catch (InterruptedException e) {
			}
		}

		onto.close();
		
		isClosed = true;
	}

	/** This protected method is called every time the ontology model changes 
	 * (ie upon addition or removal of statements in the ontology).
	 * 
	 * It is mainly responsible for testing the various watchPatterns as 
	 * provided by the set of active {@link IWatcher} against the ontology.
	 * 
	 * onModelChange() relies on a caching mechanism of requests to improve 
	 * performances. It remains however a serious performance bottleneck.
	 * 
	 * @param rsName the name of the reified statements whose creation triggered
	 *  the update. Can be null if it does not apply.
	 * 
	 * @see #onModelChange()
	 */
	protected void onModelChange(String rsName) {

		modelChanged = true;
		
		Logger.log("Model changed!\n", VerboseLevel.DEBUG);
		
			//Update the event notifiers
		eventProcessor.process();
		
		//TODO: do we need to update it every time?
		rebuildLookupTable();
		
		
		if (rsName != null)	memoryManager.watch(rsName);
			
	}

	
	/**
	 * Simply call {@link #onModelChange(String)} with a  {@code null} string.
	 */
	private void onModelChange() {
		onModelChange(null);		
	}

	
	/**
	 * Loads into memory the ontology which was specified in the constructor.
	 */
	private void load() {

		String oroCommonSenseUri = parameters.getProperty("oro_common_sense");
		String oroRobotInstanceUri = parameters.getProperty("oro_robot_instance");
		String oroScenarioUri = parameters.getProperty("oro_scenario");
		
		OntModelSpec onto_model_reasonner;
		String onto_model_reasonner_name = parameters.getProperty("reasonner", "jena_internal_owl_rule");
		
		//select the inference model and reasoner from the "reasonner" parameter specified in the configuration file.
		if ( onto_model_reasonner_name.equalsIgnoreCase("pellet")){
			onto_model_reasonner = PelletReasonerFactory.THE_SPEC;
			onto_model_reasonner_name = "Pellet " + VersionInfo.getInstance().getVersionString() + " reasonner";
		}
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_rdfs")){
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RDFS_INF;
			onto_model_reasonner_name = "Jena internal reasonner - RDFS inference engine -";
		}
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_owl_rule")){
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RULE_INF;
			onto_model_reasonner_name = "Jena internal reasonner - OWL rule inference engine -";
		}
		else {
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM;
			onto_model_reasonner_name = "No reasonner -";
		}
		
		// loading of the OWL ontologies through Jena	
		try {
			Model mainModel = null;
			Model scenarioModel = null;
					
			try {
				mainModel = FileManager.get().loadModel(oroCommonSenseUri);
				Logger.log("Common sense ontology initialized with "+ 
						oroCommonSenseUri +".\n", VerboseLevel.IMPORTANT);
								
				if (oroScenarioUri != null) 
				{
				scenarioModel = FileManager.get().loadModel(oroScenarioUri);
				Logger.log("Scenario-specific ontology loaded from " + 
						oroScenarioUri + ".\n", VerboseLevel.IMPORTANT);
				}
				
				
			} catch (NotFoundException nfe) {
				Logger.log("Could not find one of these files:\n" +
						"\t- " + oroCommonSenseUri + 
						",\n\t- " + oroRobotInstanceUri + 
						" or\n\t- " + oroScenarioUri + 
						".\nExiting.\n", VerboseLevel.FATAL_ERROR);
				System.exit(1);
			}
			//Ontology model and reasonner type
			// OWL_DL_MEM_RDFS_INF: RDFS reasoner -> quick and light
			// OWL_DL_MEM_RULE_INF: uses a more complete OWL reasonning scheme. REQUIRED for "useful" consistency checking
			// PelletReasonerFactory.THE_SPEC : uses Pellet as reasonner
			onto = ModelFactory.createOntologyModel(onto_model_reasonner, mainModel);
			
			// Strict mode set to 'false' because of Jena bug triggered in getClassesOf 
			// cf http://tech.groups.yahoo.com/group/jena-dev/message/47199
			//onto.setStrictMode(false);
			onto.setStrictMode(true);
			
			if (scenarioModel != null) onto.add(scenarioModel);
			
			String defaultRobotId = parameters.getProperty("robot_id");
			try {
				if (defaultRobotId != null) {
					//TODO workaround for https://softs.laas.fr/bugzilla/show_bug.cgi?id=171
					onto.add(this.createStatement("myself owl:sameAs " + defaultRobotId));
				}
				onto.add(this.createStatement("myself rdf:type Robot"));
			} catch (IllegalStatementException e1) {
				Logger.log("Invalid robot id in your configuration file! must be only on word name of letters, numbers and underscores. ID not added.", VerboseLevel.ERROR);
			}
						
			Logger.cr();
			Logger.log("Ontology successfully loaded (using Jena " + com.hp.hpl.jena.Jena.VERSION + ").\n", VerboseLevel.IMPORTANT);
			
			if (scenarioModel != null)
				Logger.log("\t- Scenario-specific knowledge loaded and merged.\n");
			
			Logger.log("\t- " + onto_model_reasonner_name + " initialized.\n");
			
			//Perform an initial classification
			
			if (checkConsistency()) {
				Logger.log("\t- Good news: the initial ontology is consistent.\n");
			} else {
				Logger.log("Attention! The initial ontology is inconsistent!", VerboseLevel.IMPORTANT);
			}
			
			Logger.cr();
			
		} catch (ReasonerException re){
			Logger.log("Fatal error at ontology initialization: error with the reasoner\n", VerboseLevel.FATAL_ERROR);
			re.printStackTrace();
			System.exit(1);
		} catch (JenaException je){
			Logger.log("Fatal error at ontology initialization: error with Jena\n", VerboseLevel.FATAL_ERROR);
			je.printStackTrace();
			System.exit(1);
		}
		

	}

	private void addToLookupTable(String key, String id, ResourceType type) {
		if (!lookupTable.containsKey(key)) {
			lookupTable.put(key, new HashSet<Pair<String,ResourceType>>());
		}
		lookupTable.get(key).add(
						new Pair<String, ResourceType>(id, type));
	}

	/**
	 * Rebuild the map that binds all the concepts to their labels and type 
	 * (instance, class, property...). This map is used for fast lookup of 
	 * concept, and rebuild only when (the model has changed AND an lookup failed) 
	 * OR (a concept has been removed AND a lookup is starting).
	 * 
	 *  @see {@link #lookup(String)}
	 */
	private void rebuildLookupTable() {
		if (modelChanged && forceLookupTableUpdate) {
			
			modelChanged = false;
			forceLookupTableUpdate = false;				
			lookupTable.clear();
					

			// if the ontology is inconsistent, do not update the lookup table.
			ValidityReport report = getModel().validate();
			
			if (report == null || !report.isValid()){
				return;
			}

			
			{
				ExtendedIterator<Individual> resources;
				
				resources = onto.listIndividuals();
	
			
				while(resources.hasNext()) {
					Individual res = resources.next();
					
					if (res.isAnon()) continue;
					
					ExtendedIterator<RDFNode> labels = res.listLabels(null);
					
					if (labels.hasNext())
						while(labels.hasNext()) {
							String keyword = labels.next().as(Literal.class).getLexicalForm().toLowerCase();
							addToLookupTable(keyword, Namespaces.toLightString(res), ResourceType.INSTANCE);
						}
					
					
					//Add the concept id itself.
					addToLookupTable(res.getLocalName().toLowerCase(), Namespaces.toLightString(res), ResourceType.INSTANCE);
					
				}
			}
		
			{
				ExtendedIterator<OntClass> resources = onto.listClasses();
				while(resources.hasNext()) {
					OntClass res = resources.next();
					
					if (res.isAnon()) continue;
					
					ExtendedIterator<RDFNode> labels = res.listLabels(null);
					
					if (labels.hasNext())
						while(labels.hasNext()) {
							String keyword = labels.next().as(Literal.class).getLexicalForm().toLowerCase();
							addToLookupTable(keyword, Namespaces.toLightString(res), ResourceType.CLASS);
						}
					else addToLookupTable(res.getLocalName().toLowerCase(), Namespaces.toLightString(res), ResourceType.CLASS);
					
				}
			}
		
			{
				ExtendedIterator<ObjectProperty> resources = onto.listObjectProperties();
				while(resources.hasNext()) {
					ObjectProperty res = resources.next();
					
					if (res.isAnon()) continue;
					
					ExtendedIterator<RDFNode> labels = res.listLabels(null);
					
					if (labels.hasNext())
						while(labels.hasNext()) {
							String keyword = labels.next().as(Literal.class).getLexicalForm().toLowerCase();
							addToLookupTable(keyword, Namespaces.toLightString(res), ResourceType.OBJECT_PROPERTY);
						}
					else addToLookupTable(res.getLocalName().toLowerCase(), Namespaces.toLightString(res), ResourceType.OBJECT_PROPERTY);
					
				}
			}
		
			{
				ExtendedIterator<DatatypeProperty> resources = onto.listDatatypeProperties();
				while(resources.hasNext()) {
					DatatypeProperty res = resources.next();
					
					if (res.isAnon()) continue;
					
					ExtendedIterator<RDFNode> labels = res.listLabels(null);
					
					if (labels.hasNext())
						while(labels.hasNext()) {
							String keyword = labels.next().as(Literal.class).getLexicalForm().toLowerCase();
							addToLookupTable(keyword, Namespaces.toLightString(res), ResourceType.DATATYPE_PROPERTY);
						}
					else addToLookupTable(res.getLocalName().toLowerCase(), Namespaces.toLightString(res), ResourceType.DATATYPE_PROPERTY);
				}
			
			}
		
		}

	}
	

	/**
	 * 	Initializes the list of functional properties
	 */
	private void rebuildFunctionalPropertiesList() {

		Set<PartialStatement> partialStatements = new HashSet<PartialStatement>();
		try {
			partialStatements.add(createPartialStatement("?f rdf:type owl:FunctionalProperty"));
		} catch (IllegalStatementException e1) {
			Logger.log("Serious error while fetching functional properties! 'update()' won't work. Please contact the mainteners!", VerboseLevel.SERIOUS_ERROR);
			return;
		}
		Set<RDFNode> functionalProps = null;
		try {
			functionalProps = find("f", partialStatements, null);
		} catch (OntologyServerException e) {
			Logger.log("Serious error while fetching functional properties! 'update()' won't work. Please contact the mainteners!", VerboseLevel.SERIOUS_ERROR);
			return;
		}
		
		for (RDFNode s : functionalProps)
			functionalProperties.add(s.as(OntProperty.class));
		
	}

	@Override
	public long size() {
		return onto.size();
	}



}
