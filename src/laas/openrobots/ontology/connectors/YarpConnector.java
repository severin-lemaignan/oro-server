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
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.Map.Entry;

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
import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.RPCMethod;
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
	private Map<Pair<String,String>, Pair<Method, Object>> registredServices;
	
	private String yarpPort;
	private String lastReceiverPort = "";
	
	private BufferedPortBottle queryPort;	
	private BufferedPortBottle resultPort;
	
	private Bottle query;
	
	private HashSet<IWatcher> yarpWatchers;

	public YarpConnector(IOntologyBackend oro, Properties params, Map<Pair<String,String>,Pair<Method,Object>> registredServices) {
		
		System.loadLibrary("jyarp");
		
		this.oro = oro;
		
		yarpPort = "/" + params.getProperty("yarp_input_port", "oro"); //defaulted to "oro" if no "yarp_input_port" provided.

		this.registredServices = registredServices;
		
		
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
    		throw new OntologyConnectorException("Exception while starting the YARP connector: no YARP server!");
    	}
		
	}
	
	@Override
	public void run() {
		//System.out.println("Waiting for a new request...");
	    
	    query = queryPort.read(false); //non blocking read.
	    //System.out.println("Incoming bottle " + query.toString());
	    
	    
	       	    
	    if (query!=null) {
	    	Bottle result = resultPort.prepare();
	    	result.clear();
	    	
	    	String receiverPort = query.get(0).toString();
	    	if (!receiverPort.startsWith("/"))
	    		//throw new MalformedYarpMessageException("Your YARP message should start with the YARP port (prefixed with /) on which you want to get the result of your query.");
	    		System.exit(-1);
	    	
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
	    		/******* Externally registred methods ********/
	    		for (Pair<String,String> name : registredServices.keySet()){
	    			
	    			Method m = registredServices.get(name).getLeft();
	    			Object o = registredServices.get(name).getRight();
	    			
	    	    	if (	name.getLeft().equalsIgnoreCase(queryName) && 
	    	    				(
	    	    				(yarpArgs == null) ? 
	    	    						m.getParameterTypes().length ==0 :
	    	    						(m.getParameterTypes().length == yarpArgs.size())
	    	    				)
	    	    		)
	    	    	{
	    	    		methodFound = true;
	    	    		boolean invokationDone = false;
	    	    		
	    	    		try {
	    	    			
	    	    			
	    	    			result.addString("ok");
	    	    			
	    	    			Value rawValue = new Value();
	    	    			
	    	    			if((yarpArgs == null && m.getParameterTypes().length !=0))
	    	    				throw new IllegalArgumentException("Wrong number of arguments for method " + queryName + " :" + m.getParameterTypes().length + " expected, " + yarpArgs.size() + " provided.");
	    	    			
	    	    			Object[] args = new Object[m.getParameterTypes().length];
	    	    			
	    	    			int i = 0;
	    	    			
	    	    			if (yarpArgs != null) {
		    	    			for (Class<?> param : m.getParameterTypes()) {

		    	    				args[i] = deserialize(yarpArgs.get(i), param);
		    	    				i++;
		    	    			}
	    	    			}
	    	    			
	    	    			if (m.getReturnType() == void.class)
	    	    			{
	    	    				if (yarpArgs == null) m.invoke(o); else m.invoke(o, args);
	    	    				invokationDone = true;
	    	    			}
	    	    			if (m.getReturnType() == String.class || m.getReturnType() == Double.class || m.getReturnType() == Integer.class || m.getReturnType() == Boolean.class || m.getReturnType() == Float.class) {
	    	    				rawValue.fromString("(" + ((yarpArgs == null) ? m.invoke(o) : m.invoke(o, args)) + ")");
	    	    				invokationDone = true;
	    	    			} else {
	    	    				
	    	    				List<Class<?>> rTypes = new ArrayList<Class<?>>();
	    	    				
	    	    				rTypes.add(m.getReturnType());
	    	    				
	    	    				rTypes.addAll(Arrays.asList(m.getReturnType().getInterfaces()));
	    	    				

		    	    			for (Class<?> rType : rTypes ) {
		    	    				if (rType == YarpSerializable.class) {
		    	    					rawValue = (((YarpSerializable)(((yarpArgs == null) ? m.invoke(o) : m.invoke(o, args)))).toYarp());
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == Map.class) {
		    	    					rawValue = mapToValue(((Map<?, ?>)(((yarpArgs == null) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == Set.class) {
		    	    					rawValue = setToValue(((Set<?>)(((yarpArgs == null) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == List.class) {
		    	    					rawValue = listToValue(((List<?>)(((yarpArgs == null) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
	
		    	    			}
	    	    			}
	    	    			
	    	    			if (!invokationDone) {
	    						System.err.println("ERROR while executing the request: no way to serialize for YARP return value of method '"+queryName + "' (return type is " + m.getReturnType().getName() + ").");
	    						result.clear();
	    						result.addString("error");
	    						result.addString("");
	    						result.addString("No way to serialize for YARP return value of method '"+queryName + "' (return type is " + m.getReturnType().getName() + ").");	
	    	    			}
	    	    			else {
		    	    			result.add(rawValue);
	    	    			}
	    	    			
	    	    			
						} catch (IllegalArgumentException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getClass().getName());
							result.addString(e.getLocalizedMessage().replace("\"", "'"));
							
						} catch (ClassCastException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getClass().getName());
							result.addString(e.getLocalizedMessage().replace("\"", "'"));
							
						} catch (IllegalAccessException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getClass().getName());
							result.addString(e.getLocalizedMessage().replace("\"", "'"));	
							
						} catch (InvocationTargetException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getCause().getClass().getName() + " -> " + e.getCause().getLocalizedMessage());
							result.clear();
							result.addString("error");
							result.addString(e.getCause().getClass().getName());
							result.addString(e.getCause().getLocalizedMessage().replace("\"", "'"));							
						
						}
						
						break;
	    	    	}
	    	    }
	    		

	    	    
	    	    if (!methodFound){
					System.err.println("ERROR while executing the request: method \""+queryName + "\" not implemented by the ontology server.");
					result.clear();
					result.addString("error");
					result.addString("NotImplementedException");
					result.addString("Method " + queryName + " not implemented by the ontology server.");						
	    	    }
	    	    
	    	    
	    	    //...Send the answer...
	    	    //System.out.println("Answering to " + receiverPort + " that " + result);
	    	    resultPort.write();
	    	    
	    	    lastReceiverPort = receiverPort;
		    }
	    }
		
	}

	private <V> Value listToValue(List<V> list) {
		String str = "(";
		for (V v : list) {
			str += "\"" + v.toString() + "\" ";
		}
		
		str = str.substring(0, str.length() - 1) + ")";
		
		return Value.makeList(str);
	}

	private <V> Value setToValue(Set<V> set) {
		String str = "(";
		for (V v : set) {
			str += "\"" + v.toString() + "\" ";
		}
		
		str = str.substring(0, str.length() - 1) + ")";
		
		return Value.makeList(str);
	}

	private <K, V> Value mapToValue(Map<K, V> map) {
		String str = "(";
		for (Entry<K, V> es : map.entrySet()) {
			str += "(\"" + es.getKey().toString() + "\" \"" + es.getValue().toString() + "\")"+ " ";
		}
		
		str = str.substring(0, str.length() - 1) + ")";
		
		return Value.makeList(str);
	}
	
	private Object deserialize(Value val, Class<?> type) {
	//not typed because of Method::invoke requirements <- that's what I call a bad excuse
	
		if (val.isString() && type == String.class)
			return val.asString().c_str();
				
		if (val.isInt() && type == Integer.class)
			return val.asInt();
		
		if (val.isDouble() && type == Double.class)
			return val.asDouble();
		
		if (val.isList()) {
			
			boolean isValidMap = true;
			boolean isValidSet = true;
			
			//First, inspect the bottle to determine the type.
			for (int i = 0; i < val.asList().size(); i++)
			{
				if (!isValidMap || !val.asList().get(i).isList() || val.asList().get(i).asList().size() != 2 || !val.asList().get(i).asList().get(0).isString() || !val.asList().get(i).asList().get(1).isString())
					isValidMap = false;
					
				if (!isValidSet || !val.asList().get(i).isString())
					isValidSet = false;
			}
			
			assert(!(isValidMap && isValidSet));
			
			//if the bottle looks like a map and a map is indeed expected...
			if (isValidMap && Map.class.isAssignableFrom(type)){
				Map<String, String> result = new HashMap<String, String>();
				for (int i = 0; i < val.asList().size(); i++)
				{
					result.put(val.asList().get(i).asList().get(0).asString().c_str(), val.asList().get(i).asList().get(1).asString().c_str());
				}
				return result;
			}
			
			//if the bottle looks like a set and a set of a list is indeed expected...
			else if (isValidSet && Set.class.isAssignableFrom(type)){
				Set<String> result = new HashSet<String>();
				for (int i = 0; i < val.asList().size(); i++)
				{
					result.add(val.asList().get(i).asString().c_str());
				}
				return result;
			}
			
			//if the bottle looks like a set and a list of a list is indeed expected...
			else if (isValidSet && List.class.isAssignableFrom(type)){
				List<String> result = new ArrayList<String>();
				for (int i = 0; i < val.asList().size(); i++)
				{
					result.add(val.asList().get(i).asString().c_str());
				}
				return result;
			}
			
			else throw new IllegalArgumentException("Error while deserializing the parameter of the query! Incompatible collections (a " + type.getName() + " was expected)");

		}		
		else throw new IllegalArgumentException("Unable to deserialize the parameter of the query! (a " + type.getName() + " was expected)");
	}

	@Override
	public void finalizeConnector() throws OntologyConnectorException {
		
		System.out.print(" * Closing YARP...");
		
		if (!queryPort.isClosed()) queryPort.close();
		if (!resultPort.isClosed()) resultPort.close();
		Network.fini();
		
		System.out.println(" done!");
		
		
	}

	@Override
	public Set<IWatcher> getPendingWatchers() {
		return yarpWatchers;
	}

	@Override
	public void removeWatcher(IWatcher watcher) {
		yarpWatchers.remove(watcher);
		
	}

	@Override
	public void refreshServiceList(
			Map<Pair<String, String>, Pair<Method, Object>> registredServices) {
		this.registredServices = registredServices;
		
	}


}
