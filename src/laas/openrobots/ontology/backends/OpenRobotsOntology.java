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

package laas.openrobots.ontology.backends;


//Imports
///////////////
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.events.IEventsProvider;
import laas.openrobots.ontology.events.IWatcher;
import laas.openrobots.ontology.exceptions.*;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;


/**
 * The OpenRobotsOntology class is the main container of liboro.<br/>
 * 
 * It maps a Jena {@link com.hp.hpl.jena.ontology.OntModel ontology} and provides helpers for the robotics context.<br/>
 * Amongst other feature, it offers an easy way to {@linkplain #query(String) query} the ontology with standard SPARQL requests, it can try to {@linkplain #find(String, Vector) find} resources matching a set of statements or even find resources which {@linkplain #guess(String, Vector, double) approximately match} a set of statements.<br/><br/>
 * 
 * Examples covering the various aspects of the API can be found in the {@linkplain laas.openrobots.ontology.tests Unit Tests}.
 *  
 * @author Severin Lemaignan <i>severin.lemaignan@laas.fr</i>
 */
public class OpenRobotsOntology implements IOntologyBackend {

	/**
	 * The default configuration file (set to {@value}).
	 */
	public static final String DEFAULT_CONFIG_FILE = "./ontorobot.conf";
	
	private OntModel onto;
	
	private HashSet<IEventsProvider> eventsProviders;
	
	private String owlUri;
	
	private ResultSet lastQueryResult;
	private String lastQuery;
	
	private boolean verbose;
	
	private Properties parameters;

	//the watchersCache holds as keys the literal watch pattern and as value a pair of pre-processed query built from the watch pattern and a boolean holding the last known result of the query.
	private HashMap<String, Pair<Query, Boolean>> watchersCache;
	
	/***************************************
	 *          Constructors               *
	 **************************************/
	
	/**
	 * Default constructor. Use the default configuration file.<br/>
	 * The default configuration file is defined by the {@code DEFAULT_CONFIG_FILE} static field (set to {@value #DEFAULT_CONFIG_FILE}).
	 * The constructor first opens the ontology, then loads it into memory and eventually bounds it to Jena internal reasonner. Thus, the instanciation of OpenRobotsOntology may take some time (several seconds, depending on the size on the ontology).
	 */
	public OpenRobotsOntology(){
		this.parameters =  getConfiguration(DEFAULT_CONFIG_FILE);
		initialize();
	}
	
	/**
	 * Constructor which takes a config file as parameter.<br/>
	 * The constructor first opens the ontology, then loads it into memory and eventually bounds it to Jena internal reasonner. Thus, the instanciation of OpenRobotsOntology may take some time (several seconds, depending on the size on the ontology).<br/>
	 * <br/>
	 * Available options:<br/>
	 * <ul>
	 * <li><em>verbose = [true|false]</em>: set it to <em>true</em> to get more infos from the engine.</li>
	 * <li><em>ontology = PATH</em>: the path to the OWL (or RDF) ontology to be loaded.</li> 
	 * <li><em>default_namespace = NAMESPACE</em>: set the default namespace. Don't forget the trailing #!</li>
	 * <li><em>short_namespaces = [true|false]</em> (default: true): if true, the ontology engine will return resource with prefix instead of full URI, or nothing if the resource is in the default namespace.</li>
	 * </ul>
	 * The file may contain other options, related to the server configuration. See {@link laas.openrobots.ontology.OroServer}. Have a look as well at the config file itself for more details.
	 * 
	 * @param configFileURI The path and filename of the configuration file.
	 */
	public OpenRobotsOntology(String configFileURI){
		this.parameters = getConfiguration(configFileURI);
		initialize();
	}
	
	/**
	 * Constructor which takes directly a Properties object as parameters.<br/>
	 * 
	 * @param parameters The set of parameters.
	 * @see OpenRobotsOntology#OpenRobotsOntology(String)
	 */
	public OpenRobotsOntology(Properties parameters){
		this.parameters = parameters;
		initialize();
	}

	/***************************************
	 *       Accessors and helpers         *
	 **************************************/
	
	/**
	 * Returns the result of the last successful query.
	 * @return The last successful query result or a {@code null} object if not query has been successfully executed yet.
	 */
	public ResultSet lastResult()
	{
		return lastQueryResult;
	}
	
	/**
	 * Returns the contain of the last query.
	 * @return The last performed query or an empty string if the ontology has not been queried yet.
	 */
	public String lastQuery()
	{
		return lastQuery;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createProperty(java.lang.String)
	 */
	public Property createProperty(String lex_property){
		return onto.createProperty(Namespaces.expand(lex_property));
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createResource(java.lang.String)
	 */
	public Resource createResource(String lex_resource){
		return onto.createResource(Namespaces.expand(lex_resource));
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createStatement(java.lang.String)
	 */
	public Statement createStatement(String statement) throws IllegalStatementException {
	
		Resource subject;
		Property predicate;
		RDFNode object;
	
		//TODO: We limit the split to 3 tokens to allow spaces in the object when it is a literal string. A better solution would be to properly detect quotes and count only spaces that are not inside quotes.
		String tokens_statement[] = statement.trim().split(" ", 3);
				
		if (tokens_statement.length != 3)
		{
			throw new IllegalStatementException("Three tokens are expected in a statement, " + tokens_statement.length + " found in " + statement + ".");
		}
		
		//expand the namespaces for subject and predicate.
		for (int i = 0; i<2; i++){
			tokens_statement[i] = Namespaces.format(tokens_statement[i]);
		}
		
		subject = onto.getResource(tokens_statement[0]);
		predicate = onto.getProperty(tokens_statement[1]);

		//Handle objects
		
		object = Helpers.parseLiteral(tokens_statement[2], (ModelCom)onto);
		
		
		assert(object!=null);
	
		return new StatementImpl(subject, predicate, object);
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#createPartialStatement(java.lang.String)
	 */
	public PartialStatement createPartialStatement(String statement) throws IllegalStatementException {
		return new PartialStatement(statement, (ModelCom)getModel());
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#getModel()
	 */
	public OntModel getModel(){
		return onto;
	}
	
	
	/***************************************
	 *           Public methods            *
	 **************************************/
	

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#query(java.lang.String)
	 */
	public ResultSet query(String query) throws QueryParseException
	{
		
		//Add the common prefixes.
		query = Namespaces.prefixes() + query;
		
		this.lastQuery = query;
		
		if (verbose) System.out.print(" * Processing query...");
		
		try	{
			Query myQuery = QueryFactory.create(query, Syntax.syntaxSPARQL );
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			this.lastQueryResult = myQueryExecution.execSelect();
		}
		catch (QueryParseException e) {
			System.err.println("[ERROR] error during query parsing ! ("+ e.getLocalizedMessage() +").");
			return null;
		}
		catch (QueryExecException e) {
			System.err.println("[ERROR] error during query execution ! ("+ e.getLocalizedMessage() +").");
			return null;
		}
		
		if (verbose) System.out.println("done.");
		
		
		return this.lastQueryResult;
		
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#queryAsXML(java.lang.String)
	 */
	public String queryAsXML(String query){
		
		ResultSet result = query(query);
		if (result != null)
			return ResultSetFormatter.asXMLString(result);
		else
			return null;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#add(Statement)
	 */
	public void add(Statement statement)
	{
		if (verbose) System.out.print(" * Adding new statement ["+Namespaces.toLightString(statement)+"]...");
		
		try {
			onto.add(statement);
		}
		catch (Exception e)
		{
			if (verbose) {
				System.err.println("\n[ERROR] Couldn't add the statement for an unknown reason. \n Details:\n ");
				e.printStackTrace();
				System.err.println("\nBetter to exit now until proper handling of this exception is added by mainteners! You can help by sending a mail to openrobots@laas.fr with the exception stack.\n ");
				System.exit(1);
			}			
		}
		
		//notify the events subscribers.
		onModelChange();
		
		if (verbose) System.out.println("done.");
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#add(String)
	 */
	public void add(String rawStmt) throws IllegalStatementException
	{			
			add(createStatement(rawStmt));
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#add(Vector<String>)
	 */
	public void add(Vector<String> rawStmts) throws IllegalStatementException
	{
		for (String rawStmt : rawStmts) add(rawStmt);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#find(java.lang.String, java.util.Vector, java.util.Vector)
	 */
	public Vector<Resource> find(String varName,	Vector<PartialStatement> statements, Vector<String> filters) {
		
		Vector<Resource> result = new Vector<Resource>();
		Iterator<PartialStatement> stmts = statements.iterator();
		
		String query = "SELECT ?" + varName + "\n" +
		"WHERE {\n";
		while (stmts.hasNext())
		{
			PartialStatement stmt = stmts.next();
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
		
		ResultSet rawResult = query(query);
		
		if (rawResult == null) return null;
		
		while (rawResult.hasNext())
		{
			QuerySolution row = rawResult.nextSolution();
			result.add(row.getResource(varName));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#find(java.lang.String, java.util.Vector)
	 */
	public Vector<Resource> find(String varName, Vector<PartialStatement> statements) {
		return find(varName, statements, null);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#guess(java.lang.String, java.util.Vector, double)
	 */
	public Hashtable<Resource, Double> guess(String varName, Vector<PartialStatement> partialStatements, double threshold) throws UnmatchableException {

		Hashtable<Resource, Double> result = new Hashtable<Resource, Double>();
		Hashtable<Resource, Integer> nbMatches = new Hashtable<Resource, Integer>();
		
		Iterator<PartialStatement> stmts = partialStatements.iterator();
		
		String query = "";
		
		System.out.println(" * Trying to guess what \""+ varName + "\" could be...");
		
		//TODO don't forget to check that properties are functional!
		
		while (stmts.hasNext())
		{
			PartialStatement stmt = stmts.next();
			query = "SELECT ?" + varName + " ?value\n" +
					"WHERE {\n" +
					"?" + varName + " <" + stmt.getPredicate().getURI() + "> ?value}\n";
			ResultSet individuals = query(query);
			
			while (individuals.hasNext())
			{
				QuerySolution row = individuals.nextSolution();
								
				if (!result.containsKey(row.getResource(varName)))
				{
					nbMatches.put(row.getResource(varName),1);
					
					result.put(row.getResource(varName),
								getMatchQuality(stmt.getObject(), row.get("value")));
				}
				else
				{
					nbMatches.put(row.getResource(varName),nbMatches.get(row.getResource(varName))+1);
					
					result.put(row.getResource(varName),
							result.get(row.getResource(varName)) + getMatchQuality(stmt.getObject(), row.get("value")));
				}
					
			}
			
		}
		
		
		//Compute mean values and discard resources whose matching quality is smaller than the threshold.
		Enumeration<Resource> objects = result.keys();
		
		if (verbose) System.out.println("  -> Results (threshold="+threshold+"):");
		
		while (objects.hasMoreElements())
		{
			Resource current = (Resource)objects.nextElement();
						
			result.put(current, result.get(current) / nbMatches.get(current));
			
			if (verbose) System.out.println("\t" + current.toString() + " -> " + result.get(current));
			
			if (result.get(current) < threshold) result.remove(current);
		}
				
		return result;
	}
	

	//TODO: Implement comparison between object. The clean way to do so would be to define a "isMatchable" interface and Java classes linked to relevant ontology classes (like "Color").
	private Double getMatchQuality(RDFNode object1, RDFNode object2) throws UnmatchableException {
		
		Double matchQuality=0.0;
		
		if (!(object1.isLiteral() && object2.isLiteral())) throw new UnmatchableException("Match comparison between object is not yet implemented.");
		
		Literal a, b;
		a = (Literal)object1.as(Literal.class);
		b = (Literal)object2.as(Literal.class);
		
		Class aClass = a.getDatatype().getJavaClass();
		Class bClass = b.getDatatype().getJavaClass();
		
		if (aClass == null || bClass == null) throw new UnmatchableException("Couldn't cast the literal datatype while comparing statements (tip: check the syntax of your literal!).");
				
		if(aClass.getSuperclass() == Number.class) {
				try {
					Double aD = ((Number)a.getValue()).doubleValue();
					Double bD = ((Number)b.getValue()).doubleValue();
					//matchQuality = (Math.exp(-Math.pow(((aD-bD)/Math.max(aD, bD)), 2) * 5.0));
					matchQuality = Math.max(0.1, 1-Math.abs(aD - bD)/aD);
				} catch (DatatypeFormatException dte) {
					throw new UnmatchableException("Datatype mismatch while estimating match quality (between" + a.getLexicalForm() + " and "+ b.getLexicalForm() + ").");
					//The datatype of the second object is not compatible with the first one. We can safely ignore this exception, and set the match quality to zero.
				}
		}

		else if(aClass == Boolean.class && bClass == Boolean.class) {
			if ((Boolean)a.getValue() & (Boolean)b.getValue()) matchQuality = 1.0;
			else matchQuality = 0.1;
		}
		else throw new UnmatchableException("Datatype mismatch while estimating match quality (between" + a.getLexicalForm() + " and "+ b.getLexicalForm() + ").");
							
		return matchQuality;
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#getInfos(java.lang.String)
	 */
	public Model getInfos(String lex_resource) throws NotFoundException {
		
		Model resultModel;
		
		if (verbose) System.out.print(" * Looking for statements about " + lex_resource + "...");
		
		lex_resource = Namespaces.format(lex_resource);
		
		//TODO : is it necessary to check the node exists? if it doesn't exist, the SPARQL query will answer an empty resultset.
		// This check is only useful to throw an exception...
		Resource node = onto.getOntResource(lex_resource);
		if (node == null){
			if (verbose) System.out.println("resource not found!");
			throw new NotFoundException("The node " + lex_resource + " was not found in the ontology (tip: check the namespaces!).");
		}
	
		
		String resultQuery = Namespaces.prefixes();
		
		//we use the SPARQL query type "DESCRIBE" to get a RDF graph with all the links to this resource.
		// cf http://www.w3.org/TR/rdf-sparql-query/#describe for more details
		resultQuery += "DESCRIBE <" + lex_resource +">";
		
		try	{
			Query myQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			resultModel = myQueryExecution.execDescribe();
		}
		catch (QueryParseException e) {
			if (verbose) System.err.println("[ERROR] internal error during query parsing while trying the get infos! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			return null;
		}
		catch (QueryExecException e) {
			if (verbose) System.err.println("[ERROR] internal error during query execution while try the get infos! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			return null;
		}
		
		if (verbose) System.out.println("done.");
		
		//resultModel.write(System.out);
				
		//for (StmtIterator stmts = resultModel.listStatements(); stmts.hasNext() ; ){
		//	System.out.println(stmts.nextStatement().toString());
		//}
		
		return resultModel;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#getInfos(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public Model getInfos(Resource resource) throws NotFoundException {
		return getInfos(resource.toString());
	}

	/**
	 * Returns the current set of parameters.
	 * 
	 * @return the current set of parameters, reflecting the content of the configuration file.
	 */
	public Properties getParameters(){
		return parameters;		
	}
	
	/***************************************
	 *          Private methods            *
	 **************************************/
	
	/**
	 * Read a configuration file and return to corresponding "Properties" object.
	 * The configuration file contains the path to the ontology to be loaded and several options regarding the server configuration.
	 * @param configFileURI The path and filename of the configuration file.
	 * @return A Java.util.Properties instance containing the application configuration.
	 */
	private Properties getConfiguration(String configFileURI){
		/****************************
		 *  Parsing of config file  *
		 ****************************/
		Properties parameters = new Properties();
        try
		{
        	FileInputStream fstream = new FileInputStream(configFileURI);
        	parameters.load(fstream);
			fstream.close();
			
			if (!parameters.containsKey("ontology"))
			{
				System.err.println("No ontology specified in the configuration file (\"" + configFileURI + "\"). Add smthg like ontology=openrobots.owl");
	        	System.exit(1);
			}
		}
        catch (FileNotFoundException fnfe)
        {
        	System.err.println("No config file. Check \"" + configFileURI + "\" exists.");
        	System.exit(1);
        }
        catch (Exception e)
		{
			System.err.println("Config file input error. Check config file syntax.");
			System.exit(1);
		}
        
        return parameters;
	}

	
	private void initialize(){
				
		this.eventsProviders = new HashSet<IEventsProvider>();
		this.watchersCache = new HashMap<String, Pair<Query, Boolean>>(); 
		this.lastQuery = "";
		this.lastQueryResult = null;
		this.verbose  = Boolean.parseBoolean(parameters.getProperty("verbose", "true"));
		Namespaces.setDefault(parameters.getProperty("default_namespace"));
		
		
		
		this.load();
		
	}


	/** This protected method is called every time the ontology model changes (ie upon addition or removal of statements in the ontology).<br/>
	 * It is mainly responsible for testing the various watchPatterns as provided by the set of active {@link IWatcher} against the ontology.<br/>
	 * 
	 * onModelChange() relies on a caching mechanism of requests to improve performances. It remains however a serious performance bottleneck. 
	 */
	protected void onModelChange(){
		//System.out.println("Model changed!");
		
		//iterate over the various registered watchers and notify the subscribers when needed.
		for (IEventsProvider ep : eventsProviders) {
			for (IWatcher w : ep.getPendingWatchers()) {
				
				//First time we see this watch expression: we convert it to a nice QueryExecution object, ready to be executed against the ontology.
				if (watchersCache.get(w.getWatchPattern()) == null) {
					PartialStatement statement;
					
					try {
						statement = createPartialStatement(w.getWatchPattern());
					} catch (IllegalStatementException e) {
						if (verbose) System.err.println("[ERROR] Error while parsing a new watch pattern! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.");
						return;
					}
						
					String resultQuery = "ASK { "+statement.asSparqlRow() +" }";
										
					try	{
						Query query = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
						watchersCache.put(w.getWatchPattern(), Pair.create(query, false));
						if (verbose) System.out.println("\n * New watch expression added to cache: " + resultQuery);
					}
					catch (QueryParseException e) {
						if (verbose) System.err.println("[ERROR] internal error during query parsing while trying to add an event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.");
						return;
					}
					
					
				}
				
				
				try	{
					Pair<Query, Boolean> currentQuery = watchersCache.get(w.getWatchPattern());
				
					if (QueryExecutionFactory.create(currentQuery.getLeft(), onto).execAsk()){
						
						if (verbose) System.out.println("\n * Event triggered for pattern " + w.getWatchPattern());
						
						switch(w.getTriggeringType()){
						case ON_TRUE:
						case ON_TOGGLE:
							//if the last statut for this query is NOT true, then, trigger the event.
							if (!currentQuery.getRight()) {
								w.notifySubscriber();
							}
							break;
						case ON_TRUE_ONE_SHOT:
							w.notifySubscriber();
							ep.removeWatcher(w);
							watchersCache.remove(currentQuery);
							break;
						}
					} else {
						switch(w.getTriggeringType()){
						
						case ON_FALSE:
						case ON_TOGGLE:
							//if the last statut for this query is NOT false, then, trigger the event.
							if (currentQuery.getRight()) {
								w.notifySubscriber();
							}
							break;
						case ON_FALSE_ONE_SHOT:
							w.notifySubscriber();
							ep.removeWatcher(w);
							watchersCache.remove(currentQuery);
							break;
						}
						
					
					}
					
				}
				catch (QueryExecException e) {
					if (verbose) System.err.println("[ERROR] internal error during query execution while verifiying conditions for event handlers! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
					throw e;
				}
				
			}
			
		}
			
	}
	/**
	 * Loads into memory the ontology which was specified in the constructor.
	 */
	private void load() {

		String owlUri = parameters.getProperty("ontology");
		String owlInstanceUri = parameters.getProperty("instances");
		
		OntModelSpec onto_model_reasonner;
		String onto_model_reasonner_name = parameters.getProperty("reasonner", "jena_internal_owl_rule");
		
		//select the inference model and reasonner from the "reasonner" parameter specified in the configuration file.
		if ( onto_model_reasonner_name.equalsIgnoreCase("pellet")){
			onto_model_reasonner = PelletReasonerFactory.THE_SPEC;
			onto_model_reasonner_name = "Pellet reasonner";
		}
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_rdfs")){
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RDFS_INF;
			onto_model_reasonner_name = "Jena internal reasonner - RDFS inference engine -";
		}
		else {
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RULE_INF;
			onto_model_reasonner_name = "Jena internal reasonner - OWL rule inference engine -";
		}
		
		//		loading of the OWL ontology thanks Jena	
		try {
			Model mainModel = null;
			Model instancesModel = null;
					
			try {
				mainModel = FileManager.get().loadModel(owlUri);
				if (verbose) System.out.print(" * Ontology initialized with "+ owlUri +".");
				
				if (owlInstanceUri != null) 
					{
					instancesModel = FileManager.get().loadModel(owlInstanceUri);
					if (verbose) System.out.println(" Instances loaded from " + owlInstanceUri + ".");
					}
				else if (verbose) System.out.print("\n");
				
				
			} catch (NotFoundException nfe) {
				System.err.println("[ERROR] Could not find " + owlUri + " or " + owlInstanceUri + ". Exiting.");
				System.exit(1);
			}
			//Ontology model and reasonner type
			// OWL_DL_MEM_RDFS_INF: RDFS reasoner -> quick and light
			// OWL_DL_MEM_RULE_INF: uses a more complete OWL reasonning scheme. REQUIRED for "useful" consistency checking
			// PelletReasonerFactory.THE_SPEC : uses Pellet as reasonner
			onto = ModelFactory.createOntologyModel(onto_model_reasonner, mainModel);
			
			if (instancesModel != null) onto.add(instancesModel);
			
			if (verbose) {
				System.out.print(" * Ontology successfully loaded (");
				if (instancesModel != null) System.out.print("domain instances loaded and merged. ");
				System.out.println(onto_model_reasonner_name + " initialized).");
			}
			
			
		} catch (ReasonerException re){
			re.printStackTrace();
			System.exit(1);
		} catch (JenaException je){
			je.printStackTrace();
			System.exit(1);
		}
		

	}

	@Override
	public void clear(PartialStatement partialStmt) {
		if (verbose) System.out.println(" * Clearing statements matching ["+ partialStmt + "]...");
		
		Selector selector = new SimpleSelector(partialStmt.getSubject(), partialStmt.getPredicate(), partialStmt.getObject());
		StmtIterator stmtsToRemove = onto.listStatements(selector);
		onto.remove(stmtsToRemove.toList());
		
	}
	
	@Override
	public void clear(String partialStmt) throws IllegalStatementException {		
		clear(createPartialStatement(partialStmt));
	}

	@Override
	public void remove(Statement stmt) {
		if (verbose) System.out.println(" * Removing statement ["+ Namespaces.toLightString(stmt) + "]...");
		onto.remove(stmt);	
		
		//notify the events subscribers.
		onModelChange();

	}
	
	@Override
	public void remove(String stmt) throws IllegalStatementException {
		remove(createStatement(stmt));
	}
	
	@Override
	public void remove(Vector<String> stmts) throws IllegalStatementException {
		for (String stmt : stmts) remove(stmt);		
	}

	@Override
	public void save(String path) {
		if (verbose) System.out.print(" * Saving ontology to " + path +"...");
		FileOutputStream file;
		try {
			file = new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			System.err.println("[ERROR] Error while opening " + path + " to output the ontology. Check it's a valid filename!");
			return;
		}
		onto.write(file);
		
		if (verbose) System.out.println("done");
		
	}

	@Override
	public boolean checkConsistency() throws InconsistentOntologyException {
		if (verbose) System.out.println(" * Checking ontology consistency...");
		
		ValidityReport report = onto.validate();
		
		String cause = "";
		
		if (!report.isValid())
		{
			for (Iterator i = report.getReports(); i.hasNext(); ) {
	            cause += " - " + i.next();
			}

			throw new InconsistentOntologyException(cause);
		}
		
		return true;
		
	}

	@Override
	public boolean check(Statement statement) {
		if (verbose) System.out.print(" * Checking a fact: ["+ statement + "]...");
		
		//trivial to answer true is the statement has been asserted.
		if (onto.contains(statement)) return true;
		
		String resultQuery = "ASK { <" + statement.getSubject().getURI() +"> <" + statement.getPredicate().getURI() + "> <" + statement.getObject().toString() + "> }";
		
		try	{
			Query myQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			return myQueryExecution.execAsk();
		}
		catch (QueryParseException e) {
			if (verbose) System.err.println("[ERROR] internal error during query parsing while trying to check a statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			throw e;
		}
		catch (QueryExecException e) {
			if (verbose) System.err.println("[ERROR] internal error during query execution while trying to check a statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			throw e;
		}

	}
	
	@Override
	public boolean check(PartialStatement statement) {
		if (verbose) System.out.print(" * Checking a fact: ["+ statement + "]...");
						
		String resultQuery = "ASK { " + statement.asSparqlRow() + " }";
		
		try	{
			Query myQuery = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			return myQueryExecution.execAsk();
		}
		catch (QueryParseException e) {
			if (verbose) System.err.println("[ERROR] internal error during query parsing while trying to check a partial statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			throw e;
		}
		catch (QueryExecException e) {
			if (verbose) System.err.println("[ERROR] internal error during query execution while trying to check a partial statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)");
			throw e;
		}

	}

	public void registerEventsHandlers(HashSet<IEventsProvider> eventsProviders) {
		this.eventsProviders.addAll(eventsProviders);
		
	}




}
