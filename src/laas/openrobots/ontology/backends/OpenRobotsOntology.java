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

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.*;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.Pair;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.IEventsProvider;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.memory.MemoryManager;
import laas.openrobots.ontology.modules.memory.MemoryProfile;
import laas.openrobots.ontology.service.RPCMethod;
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
import com.hp.hpl.jena.ontology.OntProperty;
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
import com.hp.hpl.jena.util.iterator.Filter;


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
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#createProperty(java.lang.String)
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
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#add(com.hp.hpl.jena.rdf.model.Statement, laas.openrobots.ontology.modules.memory.MemoryProfile)
	 */
	@Override
	public void add(Statement statement, MemoryProfile memProfile)
	{
		
		try {
			
			if (memProfile == MemoryProfile.LONGTERM || memProfile == MemoryProfile.DEFAULT) //LONGTERM memory
			{
				Logger.log("Adding new statement in long term memory ["+Namespaces.toLightString(statement)+"]...");
				
				onto.enterCriticalSection(Lock.WRITE);
				onto.add(statement);
				onto.leaveCriticalSection();
				
				//notify the events subscribers.
				onModelChange();
			}
			else
			{
				Logger.log("Adding new statement in " + memProfile + " memory ["+Namespaces.toLightString(statement)+"]...");
				
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
			Logger.log("\nCouldn't add the statement for an unknown reason. \n Details:\n ", VerboseLevel.ERROR);
			e.printStackTrace();
			Logger.log("\nBetter to exit now until proper handling of this exception is added by mainteners! You can help by sending a mail to openrobots@laas.fr with the exception stack.\n ", VerboseLevel.FATAL_ERROR);
			System.exit(1);
		}			

		Logger.log("done.\n");
	}

	

	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#check(com.hp.hpl.jena.rdf.model.Statement)
	 */
	@Override
	public boolean check(Statement statement) {
			
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
			Logger.log("internal error during query parsing while trying to check a statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		catch (QueryExecException e) {
			Logger.log("internal error during query execution while trying to check a statement! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		finally {
			onto.leaveCriticalSection();
		}

	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#check(laas.openrobots.ontology.PartialStatement)
	 */
	@Override
	public boolean check(PartialStatement statement) {
						
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
	public Boolean checkConsistency() throws InconsistentOntologyException {
		
		onto.enterCriticalSection(Lock.READ);
		ValidityReport report = onto.validate();
		onto.leaveCriticalSection();
		
		String cause = "";
		
		if (!report.isValid())
		{
			for (Iterator<Report> i = report.getReports(); i.hasNext(); ) {
	            cause += " - " + i.next();
			}

			throw new InconsistentOntologyException(cause);
		}
		
		return true;
		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#query(java.lang.String)
	 */
	public ResultSet query(String query) throws QueryParseException, QueryExecException
	{
		
		//Add the common prefixes.
		query = Namespaces.prefixes() + query;
		
		this.lastQuery = query;
				
		try	{
			Query myQuery = QueryFactory.create(query, Syntax.syntaxSPARQL );
		
			QueryExecution myQueryExecution = QueryExecutionFactory.create(myQuery, onto);
			this.lastQueryResult = myQueryExecution.execSelect();
		}
		catch (QueryParseException e) {
			Logger.log("error during query parsing ! ("+ e.getLocalizedMessage() +").", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
		catch (QueryExecException e) {
			Logger.log("error during query execution ! ("+ e.getLocalizedMessage() +").", VerboseLevel.SERIOUS_ERROR);
			throw e;
		}
				
		return this.lastQueryResult;
		
	}

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#guess(java.lang.String, java.util.Vector, double)
	 */
	public Hashtable<Resource, Double> guess(String varName, Vector<PartialStatement> partialStatements, double threshold) throws UnmatchableException {

		Hashtable<Resource, Double> result = new Hashtable<Resource, Double>();
		Hashtable<Resource, Integer> nbMatches = new Hashtable<Resource, Integer>();
		
		Iterator<PartialStatement> stmts = partialStatements.iterator();
		
		String query = "";
		
		if (varName.startsWith("?")) varName = varName.substring(1);
		
		Logger.log("Trying to guess what \""+ varName + "\" could be...");
		
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
		
		Logger.log("\n  -> Results (threshold="+threshold+"):\n");
		
		while (objects.hasMoreElements())
		{
			Resource current = (Resource)objects.nextElement();
						
			result.put(current, result.get(current) / nbMatches.get(current));
			
			Logger.log("\t" + current.toString() + " -> " + result.get(current) + "\n");
			
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
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getResource(String)
	 */
	@Override
	public Resource getResource(String lex_resource) throws NotFoundException {
		
		lex_resource = Namespaces.format(lex_resource);
		
		//TODO : is it necessary to check the node exists? if it doesn't exist, the SPARQL query will answer an empty resultset.
		// This check is only useful to throw an exception...
		
		onto.enterCriticalSection(Lock.READ);
		Resource node = onto.getOntResource(lex_resource);
		onto.leaveCriticalSection();
		
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
		
		onto.enterCriticalSection(Lock.READ);
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
		finally {
			onto.leaveCriticalSection();
		}
			
		return resultModel;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getSuperclassesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
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
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getSubclassesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
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

	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getInstancesOf(com.hp.hpl.jena.ontology.OntClass, boolean)
	 */
	@Override
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
	
	
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#getClassesOf(com.hp.hpl.jena.ontology.Individual, boolean)
	 */
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
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#lookup(java.lang.String)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#clear(laas.openrobots.ontology.PartialStatement)
	 */
	@Override
	public void clear(PartialStatement partialStmt) {
		Logger.log("Clearing statements matching ["+ partialStmt + "]\n");
		
		Selector selector = new SimpleSelector(partialStmt.getSubject(), partialStmt.getPredicate(), partialStmt.getObject());
		
		onto.enterCriticalSection(Lock.READ);
		StmtIterator stmtsToRemove = onto.listStatements(selector);		
		onto.leaveCriticalSection();
		
		for (Statement s : stmtsToRemove.toList())
			remove(s);		
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#remove(com.hp.hpl.jena.rdf.model.Statement)
	 */
	@Override
	public void remove(Statement stmt) {
		Logger.log("Removing statement ["+ Namespaces.toLightString(stmt) + "]");
		
		onto.enterCriticalSection(Lock.WRITE);		
		onto.remove(stmt);	
		onto.leaveCriticalSection();
		
		//force the rebuilt of the lookup table at the next lookup.
		forceLookupTableUpdate = true;
		
		//notify the events subscribers.
		onModelChange();

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
		Logger.log("Saving ontology to " + path +"...", VerboseLevel.IMPORTANT);
		FileOutputStream file;
		try {
			file = new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			throw new OntologyServerException("Error while opening " + path + " to output the ontology. Check it's a valid filename and a writable location!");
		}
		onto.enterCriticalSection(Lock.READ);
		onto.write(file);
		onto.leaveCriticalSection();
		
		Logger.log("done.\n");
		
	}


	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#registerEventsHandlers(java.util.Set)
	 */
	public void registerEventsHandlers(Set<IEventsProvider> eventsProviders) {
		this.eventsProviders.addAll(eventsProviders);
		
	}

	
	@RPCMethod(
		category = "administration",
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
		
		Logger.log("Model changed!\n", VerboseLevel.DEBUG);
		
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
						Logger.log("Error while parsing a new watch pattern! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n", VerboseLevel.ERROR);
						return;
					}
						
					String resultQuery = "ASK { "+statement.asSparqlRow() +" }";
						
					try	{
						Query query = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
						watchersCache.put(w.getWatchPattern(), Pair.create(query, false));
						Logger.log("New watch expression added to cache: " + resultQuery + "\n");
					}
					catch (QueryParseException e) {
						Logger.log("Internal error during query parsing while trying to add an event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.\n", VerboseLevel.ERROR);
						return;
					}
					
					
				}
				
				onto.enterCriticalSection(Lock.READ);
				try	{
					Pair<Query, Boolean> currentQuery = watchersCache.get(w.getWatchPattern());
				
					if (QueryExecutionFactory.create(currentQuery.getLeft(), onto).execAsk()){
						
						Logger.log("Event triggered for pattern " + w.getWatchPattern() + "\n");
						
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
					Logger.log("Internal error during query execution while verifiying conditions for event handlers! ("+ e.getLocalizedMessage() +").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
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
				Logger.log("Common sense ontology initialized with "+ oroCommonSenseUri +".\n", VerboseLevel.IMPORTANT);
				
				if (oroRobotInstanceUri != null) 
					{
					robotInstancesModel = FileManager.get().loadModel(oroRobotInstanceUri);
					Logger.log("Robot-specific ontology loaded from " + oroRobotInstanceUri + ".\n", VerboseLevel.IMPORTANT);
					}
				
				if (oroScenarioUri != null) 
				{
				scenarioModel = FileManager.get().loadModel(oroScenarioUri);
				Logger.log("Scenario-specific ontology loaded from " + oroScenarioUri + ".\n", VerboseLevel.IMPORTANT);
				}
				
				
			} catch (NotFoundException nfe) {
				Logger.log("Could not find one of these files:\n\t- " + oroCommonSenseUri + ",\n\t- " + oroRobotInstanceUri + " or\n\t- " + oroScenarioUri + ".\nExiting.", VerboseLevel.FATAL_ERROR);
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
			
			Logger.cr();
			Logger.log("Ontology successfully loaded.\n", VerboseLevel.IMPORTANT);
			if (robotInstancesModel != null) Logger.log("\t- Robot-specific knowledge loaded and merged.\n");
			if (scenarioModel != null) Logger.log("\t- Scenario-specific knowledge loaded and merged.\n");
			Logger.log("\t- " + onto_model_reasonner_name + " initialized.\n");
			Logger.cr();
			
			
		} catch (ReasonerException re){
			re.printStackTrace();
			System.exit(1);
		} catch (JenaException je){
			je.printStackTrace();
			System.exit(1);
		}
		

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
