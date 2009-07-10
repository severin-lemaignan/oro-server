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

package laas.openrobots.ontology.connectors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import yarp.Bottle;
import yarp.Network;
import yarp.Value;
import yarp.BufferedPortBottle;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.events.IEventsProvider;
import laas.openrobots.ontology.events.IWatcher;
import laas.openrobots.ontology.events.YarpWatcher;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.InconsistentOntologyException;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.exceptions.UnmatchableException;

/**
 * The {@link laas.openrobots.ontology.OroServer} class is the main entry point for the "outside world" to the ontology server (and if you're using <a href="http://eris.liralab.it/yarp/">YARP</a> as network abstraction layer, {@code OroServer} relies on {@code YarpConnector} to handle your queries and prepare the answers).<br/>
 * <br/>
 * All methods listed here and annotated with RPCMethod should be implemented by compliant YARP clients. For C++, <a href="https://www.laas.fr/~slemaign/wiki/doku.php?id=liboro">{@code liboro}</a> does the job.<br/>
 * The C++ code samples you'll find actaully rely on {@code liboro}.<br/>
 * 
 * The structure of messages handled by {@code YarpConnector} is based on YARP {@link yarp.Bottle}.
 * <ul>
 * <li>Expected queries format:</li>
 * <pre>
 * ([YARP port for answering] [name of the method] ([param1] [param2] [...]))
 * </pre>
 * Methods are those defined in {@link laas.openrobots.ontology.backends.IOntologyBackend}.</br>
 * Parameters are enclosed in a nested bottle (a list), and these parameters can themselves be lists.
 * Currently, these lists are always casted to vectors of string.
 * <li>Answers:</li>
 * <pre>
 * ([ok|error] [result|error msg])
 * </pre>
 * When the query succeed, results are returned as a list (a nested bottle) of objects.<br/>
 * In case of error, the "error msg" is actually made of two strings: the literal (Java) exception that was triggered and a possible informative error message.
 * </ul>
 * Each method details below how they must be invoked and what result they return.
 * 
 * @author Severin Lemaignan <severin.lemaignan@laas.fr>
 *
 */
public class YarpConnector implements IConnector, IEventsProvider {
	
	private IOntologyBackend oro;
	private Set<Method> ontologyMethods;
	
	private String yarpPort;
	private String lastReceiverPort = "";
	
	private BufferedPortBottle queryPort;	
	private BufferedPortBottle resultPort;
	
	private Bottle query;
	
	private HashSet<IWatcher> yarpWatchers;

	public YarpConnector(IOntologyBackend oro, Properties params) {
		
		System.loadLibrary("jyarp");
		
		this.oro = oro;
		
		yarpPort = "/" + params.getProperty("yarp_input_port", "oro"); //defaulted to "oro" if no "yarp_input_port" provided.

		
		
		//first we collect all the method available in this connector class
		Method[] rawOntologyMethods = YarpConnector.class.getMethods(); 
		ontologyMethods = new HashSet<Method>();
		
		//then we remove the ones which don't have a "RPCMethod" annotation, for obvious security reasons.
		for (Method m : rawOntologyMethods) {
			//TODO ...for some reason, the code below doesn't work:method annotation are not read by Java...
		//	if (m.getAnnotation(RPCMethod.class) != null) ontologyMethods.add(m);
			ontologyMethods.add(m);
		}
		
		query = new Bottle();
		
		yarpWatchers = new HashSet<IWatcher>();
	}
	
	@Override
	public void initializeConnector() throws OntologyConnectorException {
		
		
    	Network.init();
    	
    	System.out.println(" * Starting YARP server on port " + yarpPort);
    	
    	queryPort = new BufferedPortBottle();
    	resultPort = new BufferedPortBottle();
    	
    	if (!queryPort.open(yarpPort + "/in") || !resultPort.open(yarpPort + "/out")) {
    		System.err.println(" * No YARP server! abandon.");
    		System.exit(0);
    	}
		
	}
	
	@Override
	public void run() throws MalformedYarpMessageException {
		//System.out.println("Waiting for a new request...");
	    
	    query = queryPort.read(false); //non blocking read.
	    //System.out.println("Incoming bottle " + query.toString());
	    
	    
	       	    
	    if (query!=null) {
	    	Bottle result = resultPort.prepare();
	    	result.clear();
	    	
	    	String receiverPort = query.get(0).toString();
	    	if (!receiverPort.startsWith("/"))
	    		throw new MalformedYarpMessageException("Your YARP message should start with the YARP port (prefixed with /) on which you want to get the result of your query.");
	    	
    	    if (!lastReceiverPort.equals(receiverPort)){ //not the same receiver ! disconnect the old one an connect to the new one.
    	    	System.out.println(" * Changing client to " + receiverPort);
    	    	Network.disconnect(yarpPort + "/out", lastReceiverPort);
    	    	Network.connect(yarpPort + "/out", receiverPort);
    	    }
    	    
	    	String queryName = query.get(1).toString();
	    	
	    	Bottle yarpArgs = query.pop().asList();
	    	
	    	boolean methodFound = false;

	    	if (queryName.equalsIgnoreCase("close")){
	    		System.out.println(" * Closing communication with " + lastReceiverPort);
	    		lastReceiverPort = "";
	    		Network.disconnect(yarpPort + "/out", lastReceiverPort);	    		
	    	}
	    	else
	    	{

	    		/******* YARP connector methods ********/
	    	    for (Method m : ontologyMethods){
	    	    	//if (m.isAccessible() && m.getName().equalsIgnoreCase(queryName))
	    	    	if (m.getName().equalsIgnoreCase(queryName))
	    	    	{
	    	    		methodFound = true;
	    	    		
	    	    		try {
	    	    			
	    	    			result.addString("ok");
	    	    			Value rawValue = new Value();
	    	    			rawValue.fromString("(" + ((Bottle)m.invoke(this, yarpArgs)).toString() + ")");
	    	    			result.add(rawValue);
	    	    			
	    	    			
						} catch (IllegalArgumentException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getClass().getName());
							result.addString(e.getLocalizedMessage().replace("\"", "'"));
							
						} catch (IllegalAccessException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getClass().getName());
							result.addString(e.getLocalizedMessage().replace("\"", "'"));	
							
						} catch (InvocationTargetException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getCause().getClass().getName() + " -> " + e.getCause().getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getCause().getClass().getName());
							result.addString(e.getCause().getLocalizedMessage().replace("\"", "'"));							
						}
	    	    	}
	    	    }
	    	    
	    	    if (!methodFound){
					System.err.println("ERROR while executing the request: method \""+queryName + "\" not implemented by the ontology server.");
					result.clear();
					result.addString("error");
					result.addString("");
					result.addString("Method " + queryName + " not implemented by the ontology server.");						
	    	    }
	    	    
	    	    
	    	    //...Send the answer...
	    	    //System.out.println("Answering to " + receiverPort + " that " + result);
	    	    resultPort.write();
	    	    
	    	    lastReceiverPort = receiverPort;
		    }
	    }
		
	}

	@Override
	public void finalizeConnector() throws OntologyConnectorException {
		
		System.out.print(" * Closing YARP...");
		
		if (!queryPort.isClosed()) queryPort.close();
		if (!resultPort.isClosed()) resultPort.close();
		Network.fini();
		
		System.out.println(" done!");
		
		
	}

	/**
	 * Adds one or several new statements to the ontology.
	 * YARP interface to {@link laas.openrobots.ontology.backends.OpenRobotsOntology#add(String)} (syntax details are provided on the linked page).<br/>
	 * 
	 * YARP C++ code snippet:
	 *  
	 * <pre>
	 * #include &quot;liboro.h&quot;
	 * 
	 * using namespace std;
	 * using namespace openrobots;
	 * int main(void) {
	 * 
	 * 		Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 
	 * 		oro.add("gorilla rdf:type Monkey");
	 * 		oro.add("gorilla age 12^^xsd:int");
	 * 		oro.add("gorilla weight 75.2");
	 * 
	 * 		// You can as well send a set of statement. The transport will be optimized (all the statements are sent in one time).
	 * 		vector<string> stmts;
	 * 
	 * 		stmts.push_back("gorilla rdf:type Monkey");
	 * 		stmts.push_back("gorilla age 12^^xsd:int");
	 * 		stmts.push_back("gorilla weight 75.2");
	 * 
	 * 		oro.add(stmts);
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws IllegalStatementException 
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#add(String)
	 */
	@RPCMethod public Bottle add(Bottle args) throws IllegalStatementException, MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		//checkValidArgs(bottleToArray(args), 1);
		
		while (args.size() != 0)
		{
			oro.add(args.pop().asString().c_str());
		}
		

		
		result.addString("true");
		return result;
	}

	/**
	 * Removes one or several statements from the ontology. Does nothing if the statements don't exist.
	 * YARP interface to {@link laas.openrobots.ontology.backends.OpenRobotsOntology#remove(Statement)}.<br/>

	 * 
	 */
	@RPCMethod public Bottle remove(Bottle args) throws MalformedYarpMessageException, IllegalStatementException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		//checkValidArgs(bottleToArray(args), 1);
		
		/*Value arg = args.pop();
		
		if (arg.isList()) {
			for (Value v : bottleToArray(args.pop().asList()))
			{
					oro.remove(v.asString().c_str());
			}
		} else	oro.remove(args.pop().asString().c_str());
		*/
		
		while (args.size() != 0)
		{
			oro.remove(args.pop().asString().c_str());
		}
		
		result.addString("true");
		return result;
	}
	
	/**
	 * Adds one or several new statements to the ontology.
	 * YARP interface to {@link laas.openrobots.ontology.backends.OpenRobotsOntology#add(String)} (syntax details are provided on the linked page).<br/>
	 * 
	 * YARP C++ code snippet:
	 *  
	 * <pre>
	 * #include &quot;liboro.h&quot;
	 * 
	 * using namespace std;
	 * using namespace openrobots;
	 * int main(void) {
	 * 
	 * 		Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 
	 * 		oro.add("gorilla rdf:type Monkey");
	 * 		oro.add("gorilla age 12^^xsd:int");
	 * 		oro.add("gorilla weight 75.2");
	 * 
	 * 		// You can as well send a set of statement. The transport will be optimized (all the statements are sent in one time).
	 * 		vector<string> stmts;
	 * 
	 * 		stmts.push_back("gorilla rdf:type Monkey");
	 * 		stmts.push_back("gorilla age 12^^xsd:int");
	 * 		stmts.push_back("gorilla weight 75.2");
	 * 
	 * 		oro.add(stmts);
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws IllegalStatementException 
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#add(String)
	 */
	@RPCMethod public Bottle checkConsistency(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		checkValidArgs(bottleToArray(args), 0);
		
		try {
			oro.checkConsistency();					
			result.addString("true");			
		} catch (InconsistentOntologyException e) {
			result.addString("false");
		}
		
		return result;
	}
	
	/**
	 * Tries to identify a resource given a set of partially defined statements plus restrictions about this resource.
	 * YARP interface to {@link laas.openrobots.ontology.backends.OpenRobotsOntology#find(String, Vector, Vector)}. Please follow the link for details.<br/>
	 * 
	 * <b>Structure of YARP bottles:</b>
	 * <ul>
	 *   <li>Argument:
	 *   <ul>
	 *   	<li>name of the unbound variable to be looked for (a sub-bottle containing only one string)</li>
	 *   	<li>one or several partial statements defining this variable (a sub-bottle containing 1..n strings)</li>
	 *      <li>one or several so-called \"filters\" (cf example below and the link over) (a sub-bottle containing 1..n strings)</li>
	 *   </ul></li>
	 *   <li>Result:
	 *   <ul>
	 *      <li>status of the query ("ok" if no error, else "error")</li>
	 *   	<li>a list of instances foud to match the partial statements and the filters (list <=> sub-bottle)</li>
	 *   	<li> or, if status=error, 2 strings: the exception name and the exception message.</li>
	 *   </ul></li>
	 * </ul>
	 * 
	 * <b>C++ code snippet using liboro:</b>
	 *  
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 		vector&lt;Concept&gt; result;
	 * 		vector&lt;string&gt; partial_stmts;
	 * 		vector&lt;string&gt; filters;
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		partial_stmts.push_back("?mysterious rdf:type oro:Monkey");
	 * 		partial_stmts.push_back("?mysterious oro:weight ?value");
	 * 
	 * 		filters.push_back("?value >= 50");
	 * 
	 * 		onto->find(&quot;mysterious&quot;, partial_stmts, filters, result);
	 * 
	 * 		//display the result
	 * 		copy(result.begin(), result.end(), ostream_iterator<Concept>(cout, "\n"));
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#find(String, Vector, Vector)
	 */
	@RPCMethod public Bottle filtredFind(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 3);

		Vector<String> filters = new Vector<String>();
		Vector<PartialStatement> partialStmts = new Vector<PartialStatement>();

		//this method uses a "list" structure for YARP bottle: the "args" argument is a bottle made of bottles. 
		String varName = args.get(0).asList().get(0).asString().c_str(); //miam ! oh la jolie ligne !
				
		Value[] rawStmts = bottleToArray(args.get(1).asList());
		
		Value[] rawFilters = bottleToArray(args.get(2).asList());
		

		for (Value v : rawFilters) {
			filters.add(v.asString().c_str());
		}

		for (Value v : rawStmts) {
			try {
				partialStmts.add(oro.createPartialStatement(v.asString()
						.c_str()));
			} catch (IllegalStatementException e) {

				System.err.println("[ERROR] " + e.getLocalizedMessage());
				return result;
			}
		}

		Vector<Resource> results = new Vector<Resource>();

		results = oro.find(varName, partialStmts, filters);

		Iterator<Resource> resultsIt = results.iterator();

		while (resultsIt.hasNext()) {
			Resource res = resultsIt.next();
			result.addString(Namespaces.toLightString(res));
		}

		return result;
	}

	/**
	 * Tries to identify a resource given a set of partially defined statements about this resource.
	 * YARP interface to {@link laas.openrobots.ontology.backends.OpenRobotsOntology#find(String, Vector)}. Please follow the link for details.<br/>
	 * 
	 * <b>Structure of YARP bottles:</b>
	 * <ul>
	 *   <li>Argument:
	 *   <ul>
	 *   	<li>name of the unbound variable to be looked for (attention: a sub-bottle containing only one string)</li>
	 *   	<li>one or several partial statements defining this variable (a sub-bottle containing 1..n strings)</li>
	 *   </ul></li>
	 *   <li>Result:
	 *   <ul>
	 *      <li>status of the query ("ok" if no error, else "error")</li>
	 *   	<li>a list of instances foud to match the partial statements (list <=> sub-bottle)</li>
	 *   	<li> or, if status=error, 2 strings: the exception name and the exception message.</li>
	 *   </ul></li>
	 * </ul>
	 * 
	 * <b>C++ code snippet using liboro:</b>
	 * 
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 		vector&lt;Concept&gt; result;
	 * 		vector&lt;string&gt; partial_stmts;
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		partial_stmts.push_back("?mysterious oro:eats oro:banana_tree");
	 * 		partial_stmts.push_back("?mysterious oro:isFemale true^^xsd:boolean");
	 * 
	 * 		onto->find(&quot;mysterious&quot;, partial_stmts, result);
	 * 
	 * 		//display the result
	 * 		copy(result.begin(), result.end(), ostream_iterator<Concept>(cout, "\n"));
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#find(String, Vector)
	 */
	@RPCMethod public Bottle find(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 2);

		Vector<PartialStatement> partialStmts = new Vector<PartialStatement>();

		//this method uses a "list" structure for YARP bottle: the "args" argument is a bottle made of bottles. 
		String varName = args.get(0).asList().get(0).asString().c_str(); //miam ! oh la jolie ligne !
		
		Bottle stmtsBottle = args.get(1).asList();
		
		if (stmtsBottle == null) throw new MalformedYarpMessageException("No partial statements provided to execute the \"find\" request!");
		
		Value[] rawStmts = bottleToArray(stmtsBottle);
		

		for (Value v : rawStmts) {
			try {
				partialStmts.add(oro.createPartialStatement(v.asString()
						.c_str()));
			} catch (IllegalStatementException e) {

				System.err.println("[ERROR] " + e.getLocalizedMessage());
				return result;
			}
		}

		Vector<Resource> results = new Vector<Resource>();

		results = oro.find(varName, partialStmts);

		Iterator<Resource> resultsIt = results.iterator();

		while (resultsIt.hasNext()) {
			Resource res = resultsIt.next();
			result.addString(Namespaces.toLightString(res));
		}

		return result;
	}

	/**
	 * Same as {@link #getInfos(Bottle)} but returns the set of asserted and
	 * inferred statements in a human-friendly way.<br/>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see #getInfos(Bottle)
	 */
	@RPCMethod public Bottle getHumanReadableInfos(Bottle args) throws MalformedYarpMessageException {

		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 1);

		Model infos = oro.getInfos(args.pop().asString().c_str());

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

			result.addString(stmt.getPredicate().getLocalName()
					+ " OF " + stmt.getSubject().getLocalName() + " IS "
					+ objString);
		}
		return result;
	}

	/**
	 * Returns the set of asserted and inferred statements whose the given node
	 * is part of.<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.backends.OpenRobotsOntology#getInfos(String)}.
	 * Please follow the link for details.<br/>
	 * 
	 * YARP C++ code snippet:
	 * 
	 * <pre>
	 * #include &quot;liboro.h&quot;
	 * 
	 * using namespace std;
	 * using namespace openrobots;
	 * int main(void) {
	 * 		vector&lt;string&gt; result;
	 * 		Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		oro.getInfos(&quot;monkey&quot;, result);
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * Structure of the resulting YARP bottle (when no error):
	 * <ul>
	 * 	<li>The response status ("ok"),</li>
	 *  <li>A list (bottle) of statements on the resource given as argument.</li>
	 * </ul>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#getInfos(String)
	 */
	@RPCMethod public Bottle getInfos(Bottle args) throws MalformedYarpMessageException {

		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 1);

		Model infos = oro.getInfos(args.pop().asString().c_str());

		StmtIterator stmts = infos.listStatements();

		while (stmts.hasNext()) {
			result.addString(Namespaces.toLightString(stmts.nextStatement()));
		}
		return result;
	}

	/**
	 * Tries to approximately identify an individual given a set of known
	 * statements about this resource.<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.backends.OpenRobotsOntology#guess(String, Vector, double)}
	 * . Please follow the link for details.<br/>
	 * 
	 * YARP C++ code snippet:
	 * 
	 * <pre>
	 * #include &quot;liboro.h&quot;
	 * 
	 * using namespace std;
	 * using namespace openrobots;
	 * int main(void) {
	 * 		vector&lt;string&gt; result;
	 * 		vector&lt;string&gt; partial_stmts;
	 * 
	 * 		Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 
	 * 		partial_stmts.push_back(&quot;?mysterious age \&quot;40\&quot;&circ;&circ;xsd:int&quot;);
	 * 		partial_stmts.push_back(&quot;?mysterious weight \&quot;60\&quot;&circ;&circ;xsd:double&quot;);
	 * 
	 * 		oro.guess(&quot;mysterious&quot;, 0.8, partial_stmts, result);
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#guess(String, Vector,
	 *      double)
	 */
	@RPCMethod public Bottle guess(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 3);

		Vector<PartialStatement> partialStmts = new Vector<PartialStatement>();

		Value[] rawStmts = bottleToArray(args.pop().asList());
		Double threshold = args.pop().asDouble();
		String varName = args.pop().asString().c_str();

		for (Value v : rawStmts) {
			try {
				partialStmts.add(oro.createPartialStatement(v.asString()
						.c_str()));
			} catch (IllegalStatementException e) {

				System.err.println("[ERROR] " + e.getLocalizedMessage());
				return result;
			}
		}

		Hashtable<Resource, Double> results = new Hashtable<Resource, Double>();

		try {
			results = oro.guess(varName, partialStmts, threshold);
		} catch (UnmatchableException e) {
			System.err.println("[ERROR] " + e.getLocalizedMessage());
			return result;
		}

		Enumeration<Resource> resultsEnum = results.keys();

		while (resultsEnum.hasMoreElements()) {
			Resource res = resultsEnum.nextElement();
			result.addString(Namespaces.toLightString(res)
					+ " (match quality: " + results.get(res).toString() + ")");
		}

		return result;
	}

	/**
	 * Performs a SPARQL query on the OpenRobots ontology.<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.backends.OpenRobotsOntology#query(String)}. Please
	 * follow the link for details.<br/>
	 * This method can only have one variable to select. See
	 * {@link #queryAsXML(Bottle)} to select several variables.<br/>
	 * 
	 * <b>Structure of YARP bottles:</b>
	 * <ul>
	 *   <li>Argument:
	 *   <ul>
	 *   	<li>name of the variable to be returned (string)</li>
	 *   	<li>a valid SPARQL query (string)</li>
	 *   </ul></li>
	 *   <li>Result:
	 *   <ul>
	 *      <li>status of the query ("ok" if no error, else "error")</li>
	 *   	<li>a list of instances matching this query (list <=> sub-bottle)</li>
	 *   	<li> or, if status=error, 2 strings: the exception name and the exception message.</li>
	 *   </ul></li>
	 * </ul>
	 * 
	 * <b>C++ code snippet using liboro:</b>
	 * 
	 * <pre>
	 * #include &quot;oro.h&quot;
	 * #include &quot;yarp_connector.h&quot;
	 * 
	 * using namespace std;
	 * using namespace oro;
	 * int main(void) {
	 * 		vector&lt;string&gt; result;
	 * 
	 * 		YarpConnector connector(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 		Ontology* onto = Ontology::createWithConnector(connector);
	 * 
	 * 		oro->query(&quot;instances&quot;, &quot;SELECT ?instances \n WHERE { \n ?instances rdf:type owl:Thing}\n&quot;, result);
	 * 
	 * 		//display the result
	 * 		copy(result.begin(), result.end(), ostream_iterator<string>(cout, "\n"));
	 * 
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#query(String)
	 */
	@RPCMethod public Bottle query(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		// checkValidArgs(bottleToArray(args),
		// OpenRobotsOntology.class.getDeclaredMethod("query", String.class));
		checkValidArgs(bottleToArray(args), 2);

		ResultSet results = oro.query(args.pop().asString().c_str());

		String key = args.pop().asString().c_str();

		while (results.hasNext()) {
			RDFNode node = results.nextSolution().getResource(key);
			if (node != null && !node.isAnon()) //node == null means that the current query solution contains no resource named after the given key.
				result.addString(Namespaces.toLightString(node));
		}
		return result;
	}
	
	/**
	 * Serialize to in-memory ontology model to a RDF/XML file.<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.backends.OpenRobotsOntology#save(String)}. Please
	 * follow the link for details.<br/>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#save(String)
	 */
	@RPCMethod public Bottle save(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 1);

		oro.save(args.pop().asString().c_str());

		return result;
	}

	/**
	 * Returns the complete XML-encoded SPARQL result (works as well with
	 * several selected variables).<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.backends.OpenRobotsOntology#queryAsXML(String)}.
	 * Please follow the link for details.<br/>
	 * <br/>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see #query(Bottle)
	 * @see laas.openrobots.ontology.backends.OpenRobotsOntology#query(String)
	 */
	@RPCMethod public Bottle queryAsXML(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 1);

		result.addString(oro.queryAsXML(args.pop().asString()
				.c_str()));
		return result;
	}

	
	/** Register a new event watcher in the ontology.<br/>
	 * When you register an event watcher, you provide a pattern (actually, a {@link laas.openrobots.ontology.PartialStatement}). This pattern is matched against the ontology each time the model change, and when some statements match the pattern, the port given as argument is triggered.
	 * @param args The expected arguments bottle has this prototype: [string, string]. The first string is the (YARP) port to trigger, the second one is a partial statement representing the pattern to watch.
	 * @throws IllegalStatementException
	 * @throws MalformedYarpMessageException
	 * @see laas.openrobots.ontology.events.IEventsProvider
	 */
	@RPCMethod public Bottle subscribe(Bottle args) throws IllegalStatementException, MalformedYarpMessageException {
				
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		checkValidArgs(bottleToArray(args), 3);
			
		String watchExpression = new String(args.get(0).asString().c_str());
		if (watchExpression.indexOf("?") == -1) throw new MalformedYarpMessageException("The first argument in an event subscription bottle must be a valid pattern (ie a valid PartialStatement.");
		
		String triggerType = new String(args.get(1).asString().c_str());
		
		TriggeringType triggeringType = null;
		
		//iterate over the various type of trigger type and compare them to their string representation as carried by the YARP bottle.
		for (TriggeringType trigger : TriggeringType.values())
		{
			if (triggerType.equalsIgnoreCase(trigger.toString()))
			{
				triggeringType = trigger;
				break;
			}
		}
		
		if (triggeringType == null) throw new MalformedYarpMessageException("The second argument in an event subscription bottle must be a valid trigger type (got "+ triggerType +").");

	
		String triggerPort = new String(args.get(2).asString().c_str());
		if (triggerPort.indexOf("/") == -1) throw new MalformedYarpMessageException("The third argument in an event subscription bottle must be a valid YARP port.");
			
		yarpWatchers.add(new YarpWatcher(watchExpression, triggeringType, triggerPort));
		
		result.addString("true");
		return result;
	}
	
	
	/**
	 * Returns some stats on the server.
	 * 
	 * <b>Structure of YARP bottles:</b>
	 * <ul>
	 *   <li>Argument: <i>none</i></li>
	 *   
	 *   <li>Result:
	 * 		<ul>
	 *  		<li>the hostname where the server runs (string)</li>
	 *  		<li>server uptime (string)</li>
	 *  		<li>the current amount of classes in the ontology (string)</li>
	 *  		<li>the current amount of instances in the ontology (string)</li>
	 *  		<li>the current amount of client connected to the server (string)</li>
	 * 		</ul></li>
	 * </ul>
	 */
	
	@RPCMethod public Bottle stats(Bottle args) {
		
		Bottle result = Bottle.getNullBottle();
		result.clear();
		
		Map<String, String> stats = OroServer.getStats();
		
		result.addString(stats.get("version"));
		result.addString(stats.get("hostname"));
		result.addString(stats.get("uptime"));
		result.addString(stats.get("nb_classes"));
		result.addString(stats.get("nb_instances"));
		result.addString(stats.get("nb_clients"));
		return result;
		
	}
	
	/**
	 * A simple test to check if the YARP connector is working.<br/>
	 * It can be called with a string as argument and it will return the string appended to another one.
	 *  
	 * @throws MalformedYarpMessageException
	 */
	@RPCMethod public Bottle test(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();
		
		checkValidArgs(bottleToArray(args), 1);
		
		result.addString("This is successful. Test = " + args.pop().asString().c_str());
		return result;
	}

	@Override
	public IOntologyBackend getBackend() {
		return oro;
	}

	static private Value[] bottleToArray(final Bottle bottle) {
		Value[] result = new Value[bottle.size()];

		for (int i = 0; i < bottle.size(); i++)
			result[i] = bottle.get(i);
		// System.out.println(" -> Arg " + i + ": " + result.get(i).toString());

		return result;
	}
	
	/**
	 * Check we have the right number of arguments and no lists with nested
	 * sub-list.
	 * 
	 * @param args
	 *            the list of arguments to check
	 * @param expectedArgsNumber
	 *            the expected number of arguments
	 * @return true is ok.
	 * @throws MalformedYarpMessageException
	 */
	static private boolean checkValidArgs(final Value[] args,
			int expectedArgsNumber) throws MalformedYarpMessageException
	// TODO finish the implementation of a better type checking
	// static private boolean checkValidArgs(Value[] args, int
	// expectedArgsNumber, Method method)
	{

		// Class[] expectedType = method.getParameterTypes();

		for (int i = 0; i < args.length; i++) {
			// if (!(args[i].isString() && expectedType[i] == String.class))
			// return false;
			// if (!(args[i].isDouble() && expectedType[i] == Double.class))
			// return false;
			// if (!(args[i].isInt() && expectedType[i] == Integer.class))
			// return false;

			if (args[i].isList()) // if one of the arg is a nested list, check
									// there's not sub list in this list ( <=>
									// only one level of list is allowed)
			{
				// if (expectedType[i])
				for (Value subArg : bottleToArray(args[i].asList())) {
					if (subArg.isList())
						throw new MalformedYarpMessageException(
								"Lists in lists are not permitted in method parameters.");
				}

			}

		}

		if (args.length != expectedArgsNumber)
			throw new MalformedYarpMessageException(
					"Wrong number of parameters (" + expectedArgsNumber
							+ " were expected, found " + args.length + ").");

		return true;
	}

	@Override
	public Set<IWatcher> getPendingWatchers() {
		return yarpWatchers;
	}

	@Override
	public void removeWatcher(IWatcher watcher) {
		yarpWatchers.remove(watcher);
		
	}


}
