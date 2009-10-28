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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.RPCMethod;
import laas.openrobots.ontology.exceptions.*;
import laas.openrobots.ontology.modules.events.IEventsProvider;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.memory.MemoryManager;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.types.ResourceDescription;

import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.utils.VersionInfo;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
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
	
	public static enum ResourceType {CLASS, INSTANCE, OBJECT_PROPERTY, DATATYPE_PROPERTY, UNDEFINED}
	
	private OntModel onto;
	
	private HashSet<IEventsProvider> eventsProviders;
	
	private ResultSet lastQueryResult;
	private String lastQuery;
	
	
	private boolean verbose;
	
	private Properties parameters;

	//the watchersCache holds as keys the literal watch pattern and as value a pair of pre-processed query built from the watch pattern and a boolean holding the last known result of the query.
	private HashMap<String, Pair<Query, Boolean>> watchersCache;
	
	private Map<String, Pair<String, ResourceType> > lookupTable;
	private boolean modelChanged = true;
	private boolean forceLookupTableUpdate = false; //useful to systematically rebuild the lookup table when a statement has been removed.
	
	private MemoryManager memoryManager;
	
	/***************************************
	 *          Constructor                *
	 **************************************/
	
	/**
	 * Constructor which takes a config file as parameter.<br/>
	 * The constructor first opens the ontology, then loads it into memory and 
	 * eventually bounds it to Jena internal reasonner. Thus, the instanciation 
	 * of OpenRobotsOntology may take some time (several seconds, depending on 
	 * the size on the ontology).<br/>
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
	 * @param parameters The set of parameters, read from the server configuration file.
	 */
	public OpenRobotsOntology(Properties parameters){
		this.parameters = parameters;
		initialize();
	}

	/***************************************
	 *       Accessors and helpers         *
	 **************************************/
		
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
		
		ArrayList<String> tokens_statement = Helpers.tokenize(statement.trim(), ' ');
				
		if (tokens_statement.size() != 3)
			throw new IllegalStatementException("Three tokens are expected in a statement, " +	tokens_statement.size() + " found in " + statement + ".");
		
		//expand the namespaces for subject and predicate.
		for (int i = 0; i<2; i++){
			tokens_statement.set(i, Namespaces.format(tokens_statement.get(i)));
		}
		
		subject = onto.getResource(tokens_statement.get(0));
		predicate = onto.getProperty(tokens_statement.get(1));

		//Handle objects
		
		object = Helpers.parseLiteral(tokens_statement.get(2), (ModelCom)onto);
		
		
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

	@Override
	public void add(Statement statement, MemoryProfile memProfile)
	{
		
		try {
			
			if (memProfile == MemoryProfile.LONGTERM || memProfile == MemoryProfile.DEFAULT) //LONGTERM memory
			{
				if (verbose) System.out.print(" * Adding new statement in long term memory ["+Namespaces.toLightString(statement)+"]...");
				
				onto.enterCriticalSection(Lock.WRITE);
				onto.add(statement);
				onto.leaveCriticalSection();
				
				//notify the events subscribers.
				onModelChange();
			}
			else
			{
				if (verbose) System.out.print(" * Adding new statement in " + memProfile + " memory ["+Namespaces.toLightString(statement)+"]...");
				
				onto.enterCriticalSection(Lock.WRITE);
				
				onto.add(statement);
				
				//create a name for this reified statement (concatenation of "rs" with hash made from S + P + O)
				String rsName = "rs_" + Math.abs(statement.hashCode()); 
				
				onto.createReifiedStatement(Namespaces.addDefault(rsName), statement);
				
				Statement metaStmt = createStatement(rsName + " stmtCreatedOn " + onto.createTypedLiteral(Calendar.getInstance()));
				//Statement metaStmt = oro.createStatement(rsName + " stmtCreatedOn " + toXSDDate(new Date())); //without timezone
				Statement metaStmt2 = createStatement(rsName + " stmtMemoryProfile " + memProfile + "^^xsd:string");
				onto.add(metaStmt);
				onto.add(metaStmt2);
				onto.leaveCriticalSection();
				
				//notify the events subscribers.
				onModelChange(rsName);
				
			}
			
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
	
		if (verbose) System.out.println("done.");
	}

	@Override
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the ontology, in long term memory."
	)
	public void add(Set<String> rawStmts) throws IllegalStatementException
	{
		for (String rawStmt : rawStmts) add(createStatement(rawStmt), MemoryProfile.DEFAULT);
	}

	@Override
	@RPCMethod(
			desc="adds one or several statements (triplets S-P-O) to the ontology associated with a memory profile."
	)
	public void add(Set<String> rawStmts, String memProfile) throws IllegalStatementException
	{
		for (String rawStmt : rawStmts) add(createStatement(rawStmt), MemoryProfile.fromString(memProfile));
	}

	
	@Override
	public boolean check(Statement statement) {
		if (verbose) System.out.print(" * Checking a fact: ["+ statement + "]...");
		
		onto.enterCriticalSection(Lock.READ);
		
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
		finally {
			onto.leaveCriticalSection();
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

	@RPCMethod(
			desc="checks that one or several statements are asserted or can be inferred from the ontology"
	)
	public Boolean check(Vector<String> stmts) throws IllegalStatementException{
	
		for (String s : stmts)
		{
			if (PartialStatement.isPartialStatement(s))
				if (!check(createPartialStatement(s))) return false;
			else
				if (!check(createStatement(s))) return false;
		}
		
		return true;

	}
	
	@Override
	@RPCMethod(
			desc="checks that the ontology is semantically consistent"
	)
	public Boolean checkConsistency() throws InconsistentOntologyException {
		if (verbose) System.out.print(" * Checking ontology consistency...");
		
		onto.enterCriticalSection(Lock.READ);
		ValidityReport report = onto.validate();
		onto.leaveCriticalSection();
		
		String cause = "";
		
		if (!report.isValid())
		{
			if (verbose) System.out.println("ontology inconsistent!");
			for (Iterator<Report> i = report.getReports(); i.hasNext(); ) {
	            cause += " - " + i.next();
			}

			throw new InconsistentOntologyException(cause);
		}
		
		if (verbose) System.out.println("no problems.");
		return true;
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#query(java.lang.String)
	 */
	public ResultSet query(String query) throws QueryParseException, QueryExecException
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
			throw e;
		}
		catch (QueryExecException e) {
			System.err.println("[ERROR] error during query execution ! ("+ e.getLocalizedMessage() +").");
			throw e;
		}
		
		if (verbose) System.out.println("done.");
		
		
		return this.lastQueryResult;
		
	}
	
	@RPCMethod(
			desc="performs one SPARQL query on the ontology"
	)
	public Set<String> query(String key, String q) throws QueryParseException, QueryExecException
	{
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
		
		queryResults = query(q);
		while (queryResults.hasNext()) {
			RDFNode node = queryResults.nextSolution().getResource(key);
			if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
				result.add(Namespaces.toLightString(node));
		}
				
		
		return result;
	}


	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#queryAsXML(java.lang.String)
	 */
	@RPCMethod(
			rpc_name = "query_as_xml", 
			desc = "performs one or several SPARQL queries on the ontology and returns a XML-formatted result set"
	)
	public String queryAsXML(String query){
		
		ResultSet result = query(query);
		if (result != null)
			return ResultSetFormatter.asXMLString(result);
		else
			return null;
	}
	

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
			PartialStatement stmt = createPartialStatement(stmts.next());
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
			result.add(Namespaces.toLightString(row.getResource(varName)));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.IOntologyServer#find(java.lang.String, java.util.Vector)
	 */
	@RPCMethod(
			desc="tries to identify a resource given a set of partially defined statements about this resource."
	)	
	public Set<String> find(String varName, Set<String> statements) throws IllegalStatementException {
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
		
		if (varName.startsWith("?")) varName = varName.substring(1);
		
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

	@RPCMethod( 
			desc = "returns the set of asserted and inferred statements whose the given node is part of. It represents the \"usages\" of a resource."
	)
	public Set<String> getInfos(String lex_resource) throws NotFoundException {
		
		Set<String> result = new HashSet<String>();
		
		Model infos = getSubmodel(lex_resource);

		StmtIterator stmts = infos.listStatements();

		while (stmts.hasNext()) {
			Statement stmt = stmts.nextStatement();
			RDFNode obj = stmt.getObject();
			String objString;

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
		return result;
		
	}

	/**
	 * Like {@link #getSubmodel(OntResource)} a string (the lexical form of a resource) as input.
	 * 
	 * @param lex_resource The URI of a resource in the ontology.
	 * @return a RDF model containing all the statements related the the given resource.
	 * @throws NotFoundException thrown if the resource doesn't exist in the ontology.
	 * @see #getSubmodel(OntResource)
	 */
	public Model getSubmodel(String lex_resource) throws NotFoundException {
		
		if (verbose) System.out.print(" * Looking for statements about " + lex_resource + "...");
		
		lex_resource = Namespaces.format(lex_resource);
		
		//TODO : is it necessary to check the node exists? if it doesn't exist, the SPARQL query will answer an empty resultset.
		// This check is only useful to throw an exception...
		
		onto.enterCriticalSection(Lock.READ);
		Resource node = onto.getOntResource(lex_resource);
		onto.leaveCriticalSection();
		
		if (node == null){
			if (verbose) System.out.println("resource not found!");
			throw new NotFoundException("The node " + lex_resource + " was not found in the ontology (tip: check the namespaces!).");
		}
		
		if (verbose) System.out.println("done.");
		
		return getSubmodel(node);
	}
	
	@Override
	public Model getSubmodel(Resource node) throws NotFoundException {
		
		Model resultModel;
		
		
		String resultQuery = Namespaces.prefixes();
		
		//we use the SPARQL query type "DESCRIBE" to get a RDF graph with all the links to this resource.
		// cf http://www.w3.org/TR/rdf-sparql-query/#describe for more details
		resultQuery += "DESCRIBE <" + node.getURI() +">";
		
		onto.enterCriticalSection(Lock.READ);
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
		finally {
			onto.leaveCriticalSection();
		}
			
		return resultModel;
	}
	
	public Set<OntClass> getSuperclassesOf(OntClass type) throws NotFoundException {
		return getSuperclassesOf(type, false);
	}
	
	@Override
	public Set<OntClass> getSuperclassesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();
		
		
		onto.enterCriticalSection(Lock.READ);
		ExtendedIterator<OntClass> it = type.listSuperClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		onto.leaveCriticalSection();
				
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred superclasses of a given class."
	)
	public Map<String, String> getSuperclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking up for superclasses of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : getSuperclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred direct superclasses of a given class."
	)
	public Map<String, String> getDirectSuperclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking for direct superclasses of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : getSuperclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		if (verbose) System.out.println("done.");
		
		return result;
	}
	

	
	public Set<OntClass> getSubclassesOf(OntClass type) throws NotFoundException {
		return getSubclassesOf(type, false);
	}
	
	@Override
	public Set<OntClass> getSubclassesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();
			
		onto.enterCriticalSection(Lock.READ);
		
		ExtendedIterator<OntClass> it = type.listSubClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon() && !tmp.getURI().equals("http://www.w3.org/2002/07/owl#Nothing") ){
				result.add(tmp);
			}
		}
		
		onto.leaveCriticalSection();
				
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred subclasses of a given class."
	)
	public Map<String, String> getSubclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking up for subclasses of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : getSubclassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of all asserted and inferred direct subclasses of a given class."
	)
	public Map<String, String> getDirectSubclassesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking for direct subclasses of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntClass c : getSubclassesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));
		
		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	public Set<OntResource> getInstancesOf(OntClass type) throws NotFoundException {
		return getInstancesOf(type, false);
	}


	public Set<OntResource> getInstancesOf(OntClass type, boolean onlyDirect) throws NotFoundException {
		
		Set<OntResource> result = new HashSet<OntResource>();
		
		onto.enterCriticalSection(Lock.READ);
		
		ExtendedIterator<? extends OntResource> it = type.listInstances(onlyDirect);
		while (it.hasNext())
		{
			OntResource tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		
		onto.leaveCriticalSection();
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {instance name, label} (or {instance name, instance name without namespace} is no label is available) of asserted and inferred instances of a given class."
	)
	public Map<String, String> getInstancesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking up for instances of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : getInstancesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {instance name, label} (or {instance name, instance name without namespace} is no label is available) of asserted and inferred direct instances of a given class."
	)
	public Map<String, String> getDirectInstancesOf(String type) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking for direct instances of " + type + "...");
		
		onto.enterCriticalSection(Lock.READ);
		OntClass myClass = onto.getOntClass(Namespaces.format(type));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + type + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : getInstancesOf(myClass, true) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@Override
	public Set<OntClass> getClassesOf(Individual individual, boolean onlyDirect) throws NotFoundException {
		
		Set<OntClass> result = new HashSet<OntClass>();

		
		onto.enterCriticalSection(Lock.READ);
		ExtendedIterator<OntClass> it = individual.listOntClasses(onlyDirect);
		while (it.hasNext())
		{
			OntClass tmp = it.next();
			if (tmp != null && !tmp.isAnon()){
				result.add(tmp);
			}
		}
		onto.leaveCriticalSection();
		
		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a map of {class name, label} (or {class name, class name without namespace} is no label is available) of asserted and inferred classes of a given individual."
	)
	public Map<String, String> getClassesOf(String individual) throws NotFoundException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		if (verbose) System.out.print(" * Looking for classes of " + individual + "...");
		
		onto.enterCriticalSection(Lock.READ);
		Individual myClass = onto.getIndividual(Namespaces.format(individual));
		onto.leaveCriticalSection();
		
		if (myClass == null) throw new NotFoundException("The class " + individual + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		for (OntResource c : getClassesOf(myClass, false) )
			result.put(Namespaces.contract(c.getURI()), Helpers.getLabel(c));

		if (verbose) System.out.println("done.");
		
		return result;
	}
	
	@RPCMethod(
			desc = "returns a serialized ResourceDescription object that describe all the links of this resource with others resources (sub and superclasses, instances, properties, etc.). The second parameter specify the desired language (following RFC4646)."
	)
	public ResourceDescription getResourceDetails(String id, String language_code) throws NotFoundException {
				
		onto.enterCriticalSection(Lock.READ);
		//TODO: check if the resource exists!!!
		OntResource myResource = onto.getOntResource(Namespaces.format(id));
		onto.leaveCriticalSection();
		
		if (myResource == null) throw new NotFoundException("The resource " + id + " does not exists in the ontology (tip: if this resource is not in the default namespace, be sure to add the namespace prefix!)");
		
		return new ResourceDescription(myResource, language_code);
	}
		
	@RPCMethod(
			desc = "returns a serialized ResourceDescription object that describe all the links of this resource with others resources (sub and superclasses, instances, properties, etc.)."
	)
	public ResourceDescription getResourceDetails(String id) throws NotFoundException {
				
		return getResourceDetails(id, OroServer.DEFAULT_LANGUAGE);
	}

	@Override
	@RPCMethod(
			desc = "try to identify a concept from its id or label, and return it, along with its type (class, instance, object_property, datatype_property)."
	)
	public List<String> lookup(String id) throws NotFoundException {
		
		if (forceLookupTableUpdate) rebuildLookupTable(); //if statements have been removed, we must force a rebuilt of the lookup table else a former concept that doesn't exist anymore could be returned.
				
		List<String> result = new ArrayList<String>();
		
		if (lookupTable.containsKey(id.toLowerCase())) {
			result.add(lookupTable.get(id.toLowerCase()).getLeft());
			result.add(lookupTable.get(id.toLowerCase()).getRight().toString());
		}
		else {
			
			//we try to rebuild the lookup table, in case some changes occured since the last lookup.
			rebuildLookupTable();
			if (lookupTable.containsKey(id.toLowerCase())) {
				result.add(lookupTable.get(id.toLowerCase()).getLeft());
				result.add(lookupTable.get(id.toLowerCase()).getRight().toString());
			}
			else throw new NotFoundException("The resource (or label) " + id + " could not be found in the ontology.");
		}
		
		return result;
		
	}
	
	/**
	 * Returns the current set of parameters.
	 * 
	 * @return the current set of parameters, reflecting the content of the configuration file.
	 */
	public Properties getParameters(){
		return parameters;		
	}
	
	
	/**
	 * 
	 */
	@RPCMethod(
		desc = "Reload the base ontologies, discarding all inserted of " +
				"removed statements" 
	)
	public void reload() {
		load();
	}
	/***************************************
	 *          Private methods            *
	 **************************************/
	

	private void initialize(){
				
		this.eventsProviders = new HashSet<IEventsProvider>();
		this.watchersCache = new HashMap<String, Pair<Query, Boolean>>();
		this.lookupTable = new HashMap<String, Pair<String, ResourceType>>();
		this.lastQuery = "";
		this.lastQueryResult = null;
		this.verbose  = Boolean.parseBoolean(parameters.getProperty("verbose", "true"));
		Namespaces.setDefault(parameters.getProperty("default_namespace"));
		
		this.load();
		
		this.rebuildLookupTable();
		
		memoryManager = new MemoryManager(onto);
		memoryManager.start();
		
	}

	
	/** This protected method is called every time the ontology model changes (ie upon addition or removal of statements in the ontology).<br/>
	 * It is mainly responsible for testing the various watchPatterns as provided by the set of active {@link IWatcher} against the ontology.<br/>
	 * 
	 * onModelChange() relies on a caching mechanism of requests to improve performances. It remains however a serious performance bottleneck.
	 * 
	 * @param rsName the name of the reified statements whose creation triggered the update. Can be null if it does not apply.
	 * 
	 * @see #onModelChange()
	 */
	protected void onModelChange(String rsName){
		//System.out.println("Model changed!");
		
		modelChanged = true;
		
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
				
				onto.enterCriticalSection(Lock.READ);
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
				finally {
					onto.leaveCriticalSection();
				}
				
			}
			
		}
		
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
		
		//select the inference model and reasonner from the "reasonner" parameter specified in the configuration file.
		if ( onto_model_reasonner_name.equalsIgnoreCase("pellet")){
			onto_model_reasonner = PelletReasonerFactory.THE_SPEC;
			onto_model_reasonner_name = "Pellet " + VersionInfo.getInstance().getVersionString() + " reasonner";
		}
		else if(onto_model_reasonner_name.equalsIgnoreCase("jena_internal_rdfs")){
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RDFS_INF;
			onto_model_reasonner_name = "Jena internal reasonner - RDFS inference engine -";
		}
		else {
			onto_model_reasonner = OntModelSpec.OWL_DL_MEM_RULE_INF;
			onto_model_reasonner_name = "Jena internal reasonner - OWL rule inference engine -";
		}
		
		// loading of the OWL ontologies thanks Jena	
		try {
			Model mainModel = null;
			Model robotInstancesModel = null;
			Model scenarioModel = null;
					
			try {
				mainModel = FileManager.get().loadModel(oroCommonSenseUri);
				if (verbose) System.out.print(" * Common sense ontology initialized with "+ oroCommonSenseUri +".\n");
				
				if (oroRobotInstanceUri != null) 
					{
					robotInstancesModel = FileManager.get().loadModel(oroRobotInstanceUri);
					if (verbose) System.out.println(" * Robot-specific ontology loaded from " + oroRobotInstanceUri + ".");
					}
				else if (verbose) System.out.print("\n");
				
				if (oroScenarioUri != null) 
				{
				scenarioModel = FileManager.get().loadModel(oroScenarioUri);
				if (verbose) System.out.println(" * Scenario-specific ontology loaded from " + oroScenarioUri + ".");
				}
				else if (verbose) System.out.print("\n");
				
				
			} catch (NotFoundException nfe) {
				System.err.println("[ERROR] Could not find one of these files:\n\t- " + oroCommonSenseUri + ",\n\t- " + oroRobotInstanceUri + " or\n\t- " + oroScenarioUri + ".\nExiting.");
				System.exit(1);
			}
			//Ontology model and reasonner type
			// OWL_DL_MEM_RDFS_INF: RDFS reasoner -> quick and light
			// OWL_DL_MEM_RULE_INF: uses a more complete OWL reasonning scheme. REQUIRED for "useful" consistency checking
			// PelletReasonerFactory.THE_SPEC : uses Pellet as reasonner
			onto = ModelFactory.createOntologyModel(onto_model_reasonner, mainModel);
			
			onto.enterCriticalSection(Lock.WRITE);
			if (robotInstancesModel != null) onto.add(robotInstancesModel);
			if (scenarioModel != null) onto.add(scenarioModel);
			onto.leaveCriticalSection();
			
			if (verbose) {
				Helpers.printInGreen(" * Ontology successfully loaded");
				System.out.print(" (");
				if (robotInstancesModel != null) System.out.print("Robot-specific knowledge loaded and merged. ");
				if (scenarioModel != null) System.out.print("Scenario-specific knowledge loaded and merged. ");
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
		
		onto.enterCriticalSection(Lock.READ);
		StmtIterator stmtsToRemove = onto.listStatements(selector);		
		onto.leaveCriticalSection();
		
		for (Statement s : stmtsToRemove.toList())
			remove(s);		
	}
	
	@Override
	public void clear(String partialStmt) throws IllegalStatementException {		
		clear(createPartialStatement(partialStmt));
	}

	@Override
	public void remove(Statement stmt) {
		if (verbose) System.out.println(" * Removing statement ["+ Namespaces.toLightString(stmt) + "]...");
		
		onto.enterCriticalSection(Lock.WRITE);		
		onto.remove(stmt);	
		onto.leaveCriticalSection();
		
		//force the rebuilt of the lookup table at the next lookup.
		forceLookupTableUpdate = true;
		
		//notify the events subscribers.
		onModelChange();

	}
	
	@Override
	public void remove(String stmt) throws IllegalStatementException {
		remove(createStatement(stmt));
	}
	
	@Override
	@RPCMethod(
			desc="removes one or several statements (triplets S-P-O) from the ontology."
	)
	public void remove(Set<String> stmts) throws IllegalStatementException {
		for (String stmt : stmts) remove(stmt);		
	}

	@Override
	@RPCMethod(
			desc="exports the current ontology model to an OWL file. The provided path must be writable by the server."
	)
	public void save(String path) throws OntologyServerException {
		if (verbose) System.out.print(" * Saving ontology to " + path +"...");
		FileOutputStream file;
		try {
			file = new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			throw new OntologyServerException("Error while opening " + path + " to output the ontology. Check it's a valid filename and a writable location!");
		}
		onto.enterCriticalSection(Lock.READ);
		onto.write(file);
		onto.leaveCriticalSection();
		
		if (verbose) System.out.println("done");
		
	}


	public void registerEventsHandlers(Set<IEventsProvider> eventsProviders) {
		this.eventsProviders.addAll(eventsProviders);
		
	}

	/**
	 * Rebuild the map that binds all the concepts to their labels and type (instance, class, property...). This map is used for fast lookup of concept, and rebuild only when (the model has changed AND an lookup failed) OR (a concept has been removed AND a lookup is starting).
	 * 
	 *  @see {@link #lookup(String)}
	 */
	private void rebuildLookupTable() {
		{
			if (modelChanged) {
				
				modelChanged = false;
				forceLookupTableUpdate = false;				
				lookupTable.clear();
				
				ExtendedIterator<Individual> resources = onto.listIndividuals();
				
				while(resources.hasNext()) {
					Individual res = resources.next();
					
					if (res.isAnon()) continue;
					
					ExtendedIterator<RDFNode> labels = res.listLabels(null);
					
					if (labels.hasNext())
						while(labels.hasNext()) {							
							lookupTable.put(	labels.next().as(Literal.class).getLexicalForm().toLowerCase(),
											new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.INSTANCE));
						}
					
					lookupTable.put(res.getLocalName().toLowerCase(), 
							new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.INSTANCE));
					
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
							lookupTable.put(	labels.next().as(Literal.class).getLexicalForm().toLowerCase(),
											new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.CLASS));
						}
					else lookupTable.put(	res.getLocalName().toLowerCase(),
							new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.CLASS));
					
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
							lookupTable.put(	labels.next().as(Literal.class).getLexicalForm().toLowerCase(), 
											new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.OBJECT_PROPERTY));
						}
					else lookupTable.put(	res.getLocalName().toLowerCase(),
							new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.OBJECT_PROPERTY));
					
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
							lookupTable.put(  labels.next().as(Literal.class).getLexicalForm().toLowerCase(), 
											new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.DATATYPE_PROPERTY));
						}
					else lookupTable.put(	res.getLocalName().toLowerCase(),
							new Pair<String, ResourceType>(Namespaces.toLightString(res), ResourceType.DATATYPE_PROPERTY));
				}
			}
		}
	}



}
