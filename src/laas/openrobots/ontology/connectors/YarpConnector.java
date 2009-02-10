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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import yarp.Bottle;
import yarp.Value;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import laas.openrobots.ontology.IOntologyServer;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.OpenRobotsOntology;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.UnmatchableException;

/**
 * The {@link OroServer} class is the main entry point for the "outside world" to the ontology server (and if you're using <a href="http://eris.liralab.it/yarp/">YARP</a> as network abstraction layer, {@code OroServer} relies on {@code YarpConnector} to handle your queries and prepare the answers).<br/>
 * <br/>
 * All methods listed here should be implemented by compliant YARP clients. For C++, <a href="https://www.laas.fr/~slemaign/wiki/doku.php?id=liboro">{@code liboro}</a> does the job.<br/>
 * The C++ code samples you'll find actaully rely on {@code liboro}.<br/>
 * 
 * The structure of messages handled by {@code YarpConnector} is based on YARP {@link yarp.Bottle}.
 * <ul>
 * <li>Expected queries format:</li>
 * <pre>
 * ([YARP port for answering] [name of the method] ([param1] [param2] [...]))
 * </pre>
 * Parameters are enclosed in a nested bottle (a list), and these parameters can themselves be lists.
 * Currently, these list are always casted to vectors of string.
 * <li>Answers:</li>
 * <pre>
 * ([ok|error] [result|error msg])
 * </pre>
 * Result is a list (a nested bottle) of objects.
 * </ul>
 * 
 * @author Severin Lemaignan <severin.lemaignan@laas.fr>
 *
 */
public class YarpConnector {

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

	private IOntologyServer oro;

	public YarpConnector(IOntologyServer oro) {
		this.oro = oro;
	}

	public YarpConnector(String conf) {
		oro = new OpenRobotsOntology(conf);
	}

	/**
	 * Adds a new statement to the ontology.
	 * YARP interface to {@link laas.openrobots.ontology.OpenRobotsOntology#add(String)}. Please follow the link for details.<br/>
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
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws IllegalStatementException 
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.OpenRobotsOntology#add(String)
	 */
	public Bottle add(Bottle args) throws IllegalStatementException, MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		checkValidArgs(bottleToArray(args), 1);
		
		oro.add(args.pop().asString().c_str());
		

		
		result.addString("true");
		return result;
	}

	/**
	 * Adds a set of statements to the ontology.
	 * Like {@link #add(Bottle)} but for sets of statements.<br/>
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
	 *      Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 
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
	 * @throws MalformedYarpMessageException 
	 * @throws IllegalStatementException 
	 * 
	 */
	public Bottle addMultiple(Bottle args) throws MalformedYarpMessageException, IllegalStatementException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		checkValidArgs(bottleToArray(args), 1);
		
		for (Value v : bottleToArray(args.pop().asList()))
		{
				oro.add(v.asString().c_str());
		}
		
		result.addString("true");
		return result;
	}
	
	/**
	 * 
	 * 
	 */
	public Bottle removeMultiple(Bottle args) throws MalformedYarpMessageException, IllegalStatementException {
		Bottle result = Bottle.getNullBottle();
		
		result.clear();
		
		checkValidArgs(bottleToArray(args), 1);
		
		for (Value v : bottleToArray(args.pop().asList()))
		{
				oro.add(v.asString().c_str());
		}
		
		result.addString("true");
		return result;
	}

	/**
	 * Tries to identify a resource given a set of partially defined statements plus restrictions about this resource.
	 * YARP interface to {@link laas.openrobots.ontology.OpenRobotsOntology#find(String, Vector, Vector)}. Please follow the link for details.<br/>
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
	 * 		vector&lt;string&gt; filters;
	 * 
	 * 		Oro oro(&quot;myDevice&quot;, &quot;oro&quot;);
	 * 
	 * 		partial_stmts.push_back("?mysterious rdf:type oro:Monkey");
	 * 		partial_stmts.push_back("?mysterious oro:weight ?value");
	 * 
	 * 		filters.push_back("?value >= 50");
	 * 
	 * 		oro.filtredFind(&quot;mysterious&quot;, partial_stmts, filters, result);
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.OpenRobotsOntology#find(String, Vector, Vector)
	 */
	public Bottle filtredFind(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 3);

		Vector<String> filters = new Vector<String>();
		Vector<PartialStatement> partialStmts = new Vector<PartialStatement>();

		Value[] rawFilters = bottleToArray(args.pop().asList());
		Value[] rawStmts = bottleToArray(args.pop().asList());
		String varName = args.pop().asString().c_str();

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
	 * YARP interface to {@link laas.openrobots.ontology.OpenRobotsOntology#find(String, Vector)}. Please follow the link for details.<br/>
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
	 * 		partial_stmts.push_back("?mysterious oro:eats oro:banana_tree");
	 * 		partial_stmts.push_back("?mysterious oro:isFemale true^^xsd:boolean");
	 * 
	 * 		oro.find(&quot;mysterious&quot;, partial_stmts, result);
	 * 		return 0;
	 * }
	 * </pre>
	 * @throws MalformedYarpMessageException 
	 * 
	 * @see laas.openrobots.ontology.OpenRobotsOntology#find(String, Vector)
	 */
	public Bottle find(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 2);

		Vector<PartialStatement> partialStmts = new Vector<PartialStatement>();

		Value[] rawStmts = bottleToArray(args.pop().asList());
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
	public Bottle getHumanReadableInfos(Bottle args) throws MalformedYarpMessageException {

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
	 * {@link laas.openrobots.ontology.OpenRobotsOntology#getInfos(String)}.
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
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.OpenRobotsOntology#getInfos(String)
	 */
	public Bottle getInfos(Bottle args) throws MalformedYarpMessageException {

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
	 * {@link laas.openrobots.ontology.OpenRobotsOntology#guess(String, Vector, double)}
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
	 * @see laas.openrobots.ontology.OpenRobotsOntology#guess(String, Vector,
	 *      double)
	 */
	public Bottle guess(Bottle args) throws MalformedYarpMessageException {
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
	 * {@link laas.openrobots.ontology.OpenRobotsOntology#query(String)}. Please
	 * follow the link for details.<br/>
	 * This method can only have one variable to select. See
	 * {@link #queryAsXML(Bottle)} to select several variables.<br/>
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
	 * 		oro.query(&quot;instances&quot;, &quot;SELECT ?instances \n WHERE { \n ?instances rdf:type owl:Thing}\n&quot;, result);
	 * 		return 0;
	 * }
	 * </pre>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see laas.openrobots.ontology.OpenRobotsOntology#query(String)
	 */
	public Bottle query(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		// checkValidArgs(bottleToArray(args),
		// OpenRobotsOntology.class.getDeclaredMethod("query", String.class));
		checkValidArgs(bottleToArray(args), 2);

		ResultSet results = oro.query(args.pop().asString().c_str());

		String key = args.pop().asString().c_str();

		while (results.hasNext()) {
			result.addString(Namespaces.toLightString(results.nextSolution().getResource(key)));
		}
		return result;
	}

	/**
	 * Returns the complete XML-encoded SPARQL result (works as well with
	 * several selected variables).<br/>
	 * YARP interface to
	 * {@link laas.openrobots.ontology.OpenRobotsOntology#queryAsXML(String)}.
	 * Please follow the link for details.<br/>
	 * <br/>
	 * 
	 * @throws MalformedYarpMessageException
	 * 
	 * @see #query(Bottle)
	 * @see laas.openrobots.ontology.OpenRobotsOntology#query(String)
	 */
	public Bottle queryAsXML(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();

		checkValidArgs(bottleToArray(args), 1);

		result.addString(oro.queryAsXML(args.pop().asString()
				.c_str()));
		return result;
	}

	/**
	 * A simple test to check if the YARP connector is working.<br/>
	 * It can be called with a string as argument and it will return the string appended to another one.
	 *  
	 * @throws MalformedYarpMessageException
	 */
	public Bottle test(Bottle args) throws MalformedYarpMessageException {
		Bottle result = Bottle.getNullBottle();
		result.clear();
		
		checkValidArgs(bottleToArray(args), 1);
		
		result.addString("This is successful. Test = " + args.pop().asString().c_str());
		return result;
	}

}
