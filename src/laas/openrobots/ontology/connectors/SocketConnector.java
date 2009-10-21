package laas.openrobots.ontology.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

/** Implements a socket interface to {@code oro-server} RPC methods.<br/>
 * <br/>
 * The protocol is ASCII-based (ie, you can connect to the server with <em>telnet</em> to test everything).<br/>
 * <br/>
 * 
 * <h2>Requests</h2>
 * The general structure of a request to the server is:
 * <pre>
 * method_name
 * [parameter1]
 * [parameter2]
 * [...]
 * #end#
 * </pre>
 * 
 * <em>parameters</em> can be either:
 * <ul>
 * <li>strings (quotes are not necessary, and are removed if present),</li>
 * <li>integers (strings made only of numbers),</li>
 * <li>floats (strings made of numbers with a dot somewhere),</li>
 * <li>booleans (strings equal to <em>true</em> or <em>false</em>, case insensitive),</li>
 * <li>(ordered) lists or (unordered) sets of strings, with this structure: <em>[val1, val2, ..., valn]</em>. If strings contain commas, they must be (single or doubled) quoted.</li>
 * <li>map of (key, value) pairs of strings, with this structure: <em>{key1:val1, key2:val2, ...}</em>. If strings (be keys or values) contain commas, they must be (single or doubled) quoted.</li>
 * </ul>
 * Please note that collections of collections are not supported.<br/>
 * 
 * <br/>
 * 
 * <h2>Responses</h2>
 * The server response has this structure if the request succeeded:
 * <pre>
 * ok
 * [return_value]
 * #end#
 * </pre>
 * <br/>
 * And this structure in case of failure:
 * <pre>
 * error
 * [name of the exception, if available]
 * [human-readable error message - that *may* spend over several lines]
 * #end#
 * </pre>
 * 
 * <h2>Some examples</h2>
 * You can test this example by directly connecting to the server with a tool like <em>telnet</em>.<br/>
 * <br/>
 * 
 * <h3>Retrieving a human-friendly list of available methods on the server</h3>
 * <pre>
 * > help
 * </pre>
 * 
 * <h3>Retrieving a machine-friendly list of available methods on the server</h3>
 * <pre>
 * > listMethods
 * > #end#
 * </pre>
 * 
 * <h3>Adding facts to the knowledge base</h3>
 * <pre>
 * > add
 * > [human rdf:type Human, human rdfs:label "Ramses", myself sees Ramses]
 * > #end#
 * </pre>
 *
 * <h3>Retrieving facts</h3>
 * This command should return the list of humans the robot currently sees.
 * <pre>
 * > find
 * > [?humans rdf:type Human, myself sees ?humans]
 * > #end#
 * </pre>
 * 
 *
 * @since 0.6.0
 * @author slemaign
 *
 */
public class SocketConnector implements IConnector, Runnable {

	/** Maximum time (in seconds) the server should keep alive a socket when inactive.
	 * 
	 * This value may be configured by the option keep_alive_socket_duration in the server configuration file.
	 */
	int KEEP_ALIVE_SOCKET_DURATION;
	
	public static final String MESSAGE_TERMINATOR = "#end#";
	
	int port;
	ServerSocket server = null;
	
	HashMap<Pair<String, String>, Pair<Method, Object>> registredServices;
	private volatile boolean keepOn = true;
	
	/**
	 * Inner class that is forked at incoming connections.
	 * @author slemaign
	 *
	 */
	class ClientWorker implements Runnable {
		  private Socket client;
		  private String name;
		  
		  private Boolean keepOnThisWorker = true;
		  
		  ClientWorker(Socket client) {
		   this.client = client;
		   this.name = "ClientWorker " + client.toString();
		  }

		  public String getName(){
		  	return name;
		  }
		  
		  public void run() {

		    BufferedReader in = null;
		    PrintWriter out = null;
		    
		    long timeLastActivity = System.currentTimeMillis();
		    
		    try{
		      in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		      out = new PrintWriter(client.getOutputStream(), true);
		    } catch (IOException e) {
		      System.err.println("in or out failed");
		      System.exit(-1);
		    }

		    while(keepOn && keepOnThisWorker){
		    	    	 
		    	ArrayList<String> request = new ArrayList<String>();
		    	String line = null;
		    	while (true) {
		    		
		    		try {
						Thread.sleep(20);
					} catch (InterruptedException e) {}
					
					if (System.currentTimeMillis() - timeLastActivity > (KEEP_ALIVE_SOCKET_DURATION * 1000)) {
						System.err.println(" * Connection " + getName() + " has been closed because it was inactive since " + KEEP_ALIVE_SOCKET_DURATION + " sec. Please use the \"close\" method in your clients to close properly the socket.");
						keepOnThisWorker = false;
						break;
		    		}
				
					try{
			    		line = in.readLine(); //answers null if the stream doesn't contain a "\n"
			        } catch (IOException e) {
						System.err.println("Read failed on one of the opened socket.");
						keepOnThisWorker = false;
						break;
					}
		        
		    		if (line != null) {
			    		line = line.trim();
			    		
			    		if (line.equalsIgnoreCase(MESSAGE_TERMINATOR)) {
			    			timeLastActivity = System.currentTimeMillis();
			    			break;
			    		}
			    		
			    		if (line.equalsIgnoreCase("help")) { //Special case for the command "help": we don't require to enter the message terminaison string.
			    			request.add(line);
			    			timeLastActivity = System.currentTimeMillis();
			    			break;
			    		}
			    		
			    		
			    		else request.add(line);
		    		}
		    	}
		    	
		        //Send data back to client
		        if (request.size() != 0) {
		        	String res = handleRequest(request);
		        
		        	out.println(res);
		        }

		    }
		  }
		  
		  public String handleRequest(ArrayList<String> request) {
			  
			  boolean methodFound = false;
			  
			  String result = "error\n\n";
			  String queryName = request.get(0);
			  
	    	if (queryName.equalsIgnoreCase("close")){
	    		System.out.println(" * Closing communication with client " + getName());
	    		
	    		keepOnThisWorker = false;
	    		
	    		return "Closing now!";
	    	}
	    	else
	    	{
	    		/******* Iterate on registred methods ********/
	    		for (Pair<String,String> name : registredServices.keySet()){
	    			
	    			Method m = registredServices.get(name).getLeft();
	    			Object o = registredServices.get(name).getRight();

	    			if (name.getLeft().equalsIgnoreCase(queryName) &&
	    	    			(
    	    				(request.size() == 1) ? 
    	    						m.getParameterTypes().length == 0 :
    	    						(m.getParameterTypes().length == request.size() - 1)
    	    				)
    	    			)
	    	    	{
	    	    		methodFound = true;
	    	    		boolean invokationDone = false;
	    	    		
	    	    		try {
	    	    			
	    	    			
	    	    			result = "ok\n";
	    	    			
	    	    			Object[] args = new Object[m.getParameterTypes().length];
	    	    			
	    	    			int i = 0;	    	    			
	    	    			if (request.size() > 1) {
		    	    			for (Class<?> param : m.getParameterTypes()) {
		    	    				args[i] = deserialize(request.get(i + 1), param);
		    	    				i++;
		    	    			}
	    	    			}
	    	    			
	    	    			if (m.getReturnType() == void.class)
	    	    			{
	    	    				if (m.getParameterTypes().length == 0) m.invoke(o); else m.invoke(o, args);
	    	    				invokationDone = true;
	    	    			}
	    	    			if (m.getReturnType() == String.class || m.getReturnType() == Double.class || m.getReturnType() == Integer.class || m.getReturnType() == Boolean.class || m.getReturnType() == Float.class) {
	    	    				result += (m.getParameterTypes().length == 0) ? m.invoke(o).toString() : m.invoke(o, args).toString();
	    	    				invokationDone = true;
	    	    			} else {
	    	    				
	    	    				List<Class<?>> rTypes = new ArrayList<Class<?>>();
	    	    				
	    	    				rTypes.add(m.getReturnType());
	    	    				
	    	    				rTypes.addAll(Arrays.asList(m.getReturnType().getInterfaces()));
	    	    				

		    	    			for (Class<?> rType : rTypes ) {
		    	    				if (rType == Serializable.class) {
		    	    					result += (m.getParameterTypes().length == 0) ? m.invoke(o).toString() : m.invoke(o, args).toString();
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == Map.class) {
		    	    					result += mapToString(((Map<?, ?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == Set.class) {
		    	    					result += setToString(((Set<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == List.class) {
		    	    					result += listToString(((List<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
	
		    	    			}
	    	    			}
	    	    			
	    	    			if (!invokationDone) {
	    						System.err.println("ERROR while executing the request: no way to serialize the return value of method '"+ queryName + "' (return type is " + m.getReturnType().getName() + ").");
	    						result ="error\n" +
	    								"\n" +
	    								"No way to serialize return value of method '" + queryName + "' (return type is " + m.getReturnType().getName() + ").";	
	    	    			}
	    	    			
	    	    			
						} catch (IllegalArgumentException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result = "error\n" +
									e.getClass().getName() + "\n" +
									e.getLocalizedMessage().replace("\"", "'");
							
						} catch (ClassCastException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result = "error\n" +
							e.getClass().getName() + "\n" +
							e.getLocalizedMessage().replace("\"", "'");
							
						} catch (IllegalAccessException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result = "error\n" +
							e.getClass().getName() + "\n" +
							e.getLocalizedMessage().replace("\"", "'");	
							
						} catch (InvocationTargetException e) {
							System.err.println("ERROR while executing the request '" + queryName + "': " + e.getCause().getClass().getName() + " -> " + e.getCause().getLocalizedMessage());
							result = "error\n" + 
									e.getCause().getClass().getName() + "\n" +
									e.getCause().getLocalizedMessage().replace("\"", "'");							
						
						}
						
						break;
	    	    		
	    	    	}
	    		}
	    		if (!methodFound){
					System.err.println("ERROR while executing the request: method \""+ queryName + "\" (with " + (request.size() - 1) + " parameters) not implemented by the ontology server.");
					result = "error\n" +
							"NotImplementedException\n" +
							"Method " + queryName + " (with " + (request.size() - 1) + " parameters) not implemented by the ontology server.";						
	    	    }
	    		
	    		return result + "\n" + MESSAGE_TERMINATOR;
	    	}
		  }
	    	
    	private <V> String listToString(List<V> list) {
    		String str = "[";
    		for (V v : list) {
    			//TODO: insert double quote only if it's not parsable to a boolean or a number.
    			str += protectValue(v.toString()) + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "]";
    		
    		return str;
    	}

    	private <V> String setToString(Set<V> list) {
    		String str = "[";
    		for (V v : list) {
    			str += protectValue(v.toString()) + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "]";
    		
    		return str;
    	}

    	private <K, V> String mapToString(Map<K, V> map) {
    		String str = "{";
    		for (Entry<K, V> es : map.entrySet()) {
    			str += protectValue(es.getKey().toString()) + ":" + protectValue(es.getValue().toString()) + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "}";
    		
    		return str;
    	}
			  
    	/** Remove leading and trailing quotes and whitespace if needed. 
    	 * 
    	 * @param value
    	 * @return
    	 */
    	private String cleanValue(String value) {
    		String res = value.trim();
    		if ((res.startsWith("\"") && res.endsWith("\"")) || (res.startsWith("'") && res.endsWith("'")))
    			res = res.substring(1, res.length() - 1);
    		
    		return res;
    	}
    	
    	/** Protect a string by escaping the quotes and surrounding the string with quotes.
    	 * 
    	 * @param value
    	 * @return
    	 */
    	private String protectValue(String value) {
    		String res = value.replaceAll("\"", "\\\"");
    		    		
    		return "\"" + res + "\"";
    	}
    	
		  private Object deserialize(String val, Class<?> type) {
				//not typed because of Method::invoke requirements <- that's what I call a bad excuse
				
					if (type == String.class)
						return cleanValue(val);
							
					if (type == Integer.class)
						return Integer.parseInt(val);
					
					if (type == Double.class)
						return Double.parseDouble(val);
					
					if (type == Boolean.class)
						return Boolean.parseBoolean(val);
					
					//assumes it's a list or map
						
					boolean isValidMap = true;
					boolean isValidSet = true;
					
					//First, inspect the string to determine the type.
					//If it starts and ends with {}, it's a map
					//If it starts and ends with [], it's a set
					if ( !val.substring(0, 1).equals("{") || !val.substring(val.length() - 1, val.length()).equals("}"))
							isValidMap = false;
					
					if ( !val.substring(0, 1).equals("[") || !val.substring(val.length() - 1, val.length()).equals("]"))
							isValidSet = false;
					
					val = val.substring(1, val.length() - 1); //remove the [] or {}
					
					//checks that every elements of the map is made of tokens separated by :
					for (String s : Helpers.tokenize(val, ','))
						if (!isValidMap || !s.contains(":"))
							isValidMap = false;
					
					assert(!(isValidMap && isValidSet));
					
					//if the string looks like a map and a map is indeed expected...
					if (isValidMap && Map.class.isAssignableFrom(type)){
						Map<String, String> result = new HashMap<String, String>();
						
						for (String s : Helpers.tokenize(val, ','))
							result.put(	cleanValue(s.trim().split(":", 2)[0]), 
										cleanValue(s.trim().split(":", 2)[1]));
						
						return result;
					}					
					//if the string looks like a set and a set of a list is indeed expected...
					else if (isValidSet && Set.class.isAssignableFrom(type)){
						Set<String> result = new HashSet<String>();
						for (String s : Helpers.tokenize(val, ','))
							result.add(cleanValue(s));
						return result;
					}					
					//if the string looks like a set and a list of a list is indeed expected...
					else if (isValidSet && List.class.isAssignableFrom(type)){
						List<String> result = new ArrayList<String>();
						for (String s : Helpers.tokenize(val, ','))
							result.add(cleanValue(s));
						return result;
					}
					
					else throw new IllegalArgumentException("Unable to deserialize the parameter of the query! (a " + type.getName() + " was expected, received \"" + val + "\")");
			}
	}
		
	public SocketConnector(
			Properties params,
			HashMap<Pair<String, String>, Pair<Method, Object>> registredServices) {
		
		port = Integer.parseInt(params.getProperty("port", "6969")); //defaulted to port 6969 if no port provided in the configuration file.
		KEEP_ALIVE_SOCKET_DURATION = Integer.parseInt(params.getProperty("keep_alive_socket_duration", "60")); //defaulted to 1 min if no duration is provided in the configuration file.
		
		this.registredServices = registredServices;
	}

	@Override
	public void finalizeConnector() throws OntologyConnectorException {
		//Objects created in run method are finalized when 
		//program terminates and thread exits
		keepOn = false;
		
		if (server != null) {
			try{
			        server.close();
			} catch (IOException e) {
			        throw new OntologyConnectorException("[ERROR] Could not close the socket server!");
			}
		}

	}

	@Override
	public void initializeConnector() throws OntologyConnectorException {
		Thread t = new Thread(this, "Socket connector");
        t.start();
	}

	@Override
	public void refreshServiceList(
			Map<Pair<String, String>, Pair<Method, Object>> registredServices) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// 
		try{
	      server = new ServerSocket(port); 
	    } catch (IOException e) {
	      System.err.println("[ERROR] Error while creating the server: could not listen on port " + port + ". Port busy?");
	      System.exit(-1);
	    }
	    
	    
	    Helpers.printlnInGreen(" * Server started on port " + port);
	    
	    while(keepOn){
	      ClientWorker w;
	      try{
	        w = new ClientWorker(server.accept());
	        Thread t = new Thread(w, w.getName());
	        t.start();
	      } catch (IOException e) {
	        System.err.println("Accept failed on port " + port);
	        System.exit(-1);
	      }
	    }

	}

}
