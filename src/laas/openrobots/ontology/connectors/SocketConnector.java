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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Pair;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.EventModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
import laas.openrobots.ontology.modules.events.IWatcher.EventType;
import laas.openrobots.ontology.service.IService;

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
 * <h2>Events</h2>
 * If an event has been registered (cf {@link EventModule#registerEvent(String, String, Set, IEventConsumer)}),
 * the server may send the following type of message when the event it triggered.
 * <pre>
 * event
 * [event_id]
 * [event_return_value]
 * #end#
 * </pre>
 * The {@code event_id} will match the id returned at event registration. The 
 * return values depend on the type of event. Details are provided on the {@link EventType}
 * page.
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
 * <h3>Registering an event</h3>
 * The example below registers an trigger upon new instances of humans.
 * <pre>
 * > registerEvent
 * > new_instance	//event type
 * > on_true		//triggering type
 * > [Human]
 * > #end#
 * </pre>
 * 
 * The server answer ok, with a unique ID bound to this so-called "event watcher"
 * <pre>
 * > ok
 * > 4565-4587995-112355-21446
 * > #end#
 * </pre>
 * 
 * If a new human appears in the ontology, the server send this kind of notification
 * <pre>
 * > event
 * > 4565-4587995-112355-21446
 * > [ramses]
 * > #end#
 * </pre>
 * 
 * Please refer to {@link IWatcher.EventType} page for the list of event types and
 * {@link IWatcher.TriggeringType} for the list of ways the trigger an 
 * event.
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
	
	public static final String DEFAULT_PORT = "6969";
	public static final String MESSAGE_TERMINATOR = "#end#";
	
	int port;
	ServerSocket server = null;
	
	HashMap<String, IService> registredServices;
	private volatile boolean keepOn = true;
	
	/**
	 * Inner class that is forked at incoming connections.
	 * @author slemaign
	 *
	 */
	public class ClientWorker implements Runnable, IEventConsumer {
		  private Socket client;
		  private String name;
		  
		  private boolean keepOnThisWorker = true;
		  
		  volatile private boolean incomingEvent = false;
		  
		  private List<Pair<UUID, OroEvent>> eventsQueue;
		  
		  ClientWorker(Socket client) {
		   this.client = client;
		   this.name = "ClientWorker " + client.toString();
		   
		   eventsQueue = new ArrayList<Pair<UUID, OroEvent>>();
		  }

		  public String getName(){
		  	return name;
		  }
		  
		  public void consumeEvent(UUID watcherId, OroEvent e) {
			  
			  incomingEvent = true;
			  synchronized (this) {
				  eventsQueue.add(new Pair<UUID, OroEvent>(watcherId, e));				
			}
		  }
		  
		  public void run() {

		    BufferedReader in = null;
		    PrintWriter out = null;
		    
		    long timeLastActivity = System.currentTimeMillis();
		    
		    try{
		      in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		      out = new PrintWriter(client.getOutputStream(), true);
		    } catch (IOException e) {
		      Logger.log("SocketConnector fatal error: in or out failed", VerboseLevel.FATAL_ERROR);
		      System.exit(1);
		    }

		    while(keepOn && keepOnThisWorker){
		    	    	 
		    	ArrayList<String> request = new ArrayList<String>();
		    	String line = null;
		    	while (true) {
		    		
		    		try {
						Thread.sleep(20);
					} catch (InterruptedException e) {}
					
					if (System.currentTimeMillis() - timeLastActivity > (KEEP_ALIVE_SOCKET_DURATION * 1000)) {
						Logger.log("Connection " + getName() + " has been closed because it was inactive since " + KEEP_ALIVE_SOCKET_DURATION + " sec. Please use the \"close\" method in your clients to close properly the socket.\n", VerboseLevel.WARNING);
						keepOnThisWorker = false;
						break;
		    		}
					
					if (incomingEvent) {
						incomingEvent = false;
						
						Iterator<Pair<UUID, OroEvent>> it = eventsQueue.iterator();
						
						while(it.hasNext()) {
							Pair<UUID, OroEvent> evt = it.next();
							OroEvent e = evt.getRight();
							out.println("event");
							out.println(evt.getLeft());
							if (e.getEventContext() != "")
								out.println(e.getEventContext());
							out.println(MESSAGE_TERMINATOR);
							
							eventsQueue.remove(evt);
							
							Logger.log("Event " + evt.getLeft() + " notified.\n", VerboseLevel.VERBOSE);
						}
						
					}
				
					try{
			    		line = in.readLine(); //answers null if the stream doesn't contain a "\n"
			        } catch (IOException e) {
			        	Logger.log("Read failed on one of the opened socket (" + getName() + "). Killing it.\n", VerboseLevel.SERIOUS_ERROR);
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
		        
		        	Logger.log("<< Send response: " + res + "\n", VerboseLevel.DEBUG);
		        	
		        	out.println(res);
		        }

		    }
		  }
		  
		  public String handleRequest(ArrayList<String> request) {
			  
			  boolean methodFound = false;
			  
			  String result = "error\n\n";
			  String queryName = request.get(0);
			  
			  Logger.log(">> Got incoming request: " + queryName + "\n", VerboseLevel.DEBUG);
			  
	    	if (queryName.equalsIgnoreCase("close")){
	    		Logger.log("Closing communication with client " + getName() + ".\n");
	    		
	    		keepOnThisWorker = false;
	    		
	    		return "Closing now!";
	    	}
	    	else
	    	{
	    		/******* Iterate on registred methods ********/
	    		for (String key : registredServices.keySet()){
	    			
	    			Method m = registredServices.get(key).getMethod();

	    			if (registredServices.get(key).getName().equalsIgnoreCase(queryName))
	    	    	{
	    	    		methodFound = true;
	    	    		boolean invokationDone = false;
	    	    		
	    	    		try {
	    	    			
	    	    			Object o = registredServices.get(key).getObj();
	    	    			
	    	    			result = "ok\n";
	    	    			
	    	    			Object[] args = new Object[m.getParameterTypes().length];
	    	    			
    	    			
	    	    			if (	request.size() == 1 && 
	    	    					!(m.getParameterTypes().length == 0))
	    	    			{
	    	    				Logger.log("Error while executing the request: method \""+ queryName + "\" expects no parameters.\n", VerboseLevel.ERROR);
	    	 					result = "error\n" +
	    	 							"NotImplementedException\n" +
	    	 							"Method " + queryName + " expects no parameters."; 
	    	 					return result + "\n" + MESSAGE_TERMINATOR;
	    	    			}

	    	    			int i = 0;
	    	    			int shiftSpecialCases = 0;
	    	    			
	    	    			if (request.size() > 1) {
		    	    			for (Class<?> param : m.getParameterTypes()) {
		    	    				
		    	    				try {
			    	    				if (param.equals(IEventConsumer.class))	{
			    	    					args[i] = this;
			    	    					shiftSpecialCases ++;
			    	    				}
			    	    				else {
			    	    					args[i + shiftSpecialCases] = deserialize(request.get(i + 1), param);
				    	    				i++;
			    	    				}
		    	    				} catch (IndexOutOfBoundsException e)
		    	    				{
		    	    					Logger.log("Error while executing the request: missing parameters for method \""+ queryName + "\".\n", VerboseLevel.ERROR);
			    	 					result = "error\n" +
			    	 							"NotImplementedException\n" +
			    	 							"missing parameters for method " + queryName + ".";
			    	 					return result + "\n" + MESSAGE_TERMINATOR;
		    	    				}
		    	    			}
		    	    			
		    	    			if (i != request.size() - 1) {
		    	    				Logger.log("Error while executing the request: too many parameters provided for " +
		    	    						"method \""+ queryName + "\" (" + i + " were expected).\n", VerboseLevel.ERROR);
		    	 					result = "error\n" +
		    	 							"NotImplementedException\n" +
		    	 							"Too many parameters provided for " +
		    	    						"method \""+ queryName + "\" (" + i + " were expected).";
		    	 					return result + "\n" + MESSAGE_TERMINATOR;
		    	    			}
	    	    			}
	    	    			
	    	    			if (m.getReturnType() == void.class)
	    	    			{
	    	    				if (m.getParameterTypes().length == 0) m.invoke(o); else m.invoke(o, args);
	    	    				invokationDone = true;
	    	    			}
	    	    			if (	m.getReturnType() == String.class ||
	    	    					m.getReturnType() == Double.class || 
	    	    					m.getReturnType() == double.class ||
	    	    					m.getReturnType() == Integer.class ||
	    	    					m.getReturnType() == int.class ||
	    	    					m.getReturnType() == Boolean.class ||
	    	    					m.getReturnType() == boolean.class ||
	    	    					m.getReturnType() == Float.class ||
	    	    					m.getReturnType() == float.class) 
	    	    			{
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
		    	    				//TODO : Lot of cleaning to do here
		    	    				if (rType == Map.class) {
		    	    					result += Helpers.stringify(((Map<?, ?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == Set.class) {
		    	    					result += Helpers.stringify(((Set<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
		    	    				
		    	    				if (rType == List.class) {
		    	    					result += Helpers.stringify(((List<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
		    	    					invokationDone = true;
		    	    					break;
		    	    				}
	
		    	    			}
	    	    			}
	    	    			
	    	    			if (!invokationDone) {
	    	    				Logger.log("Error while executing the request: no way to serialize the return value of method '"+ queryName + "' (return type is " + m.getReturnType().getName() + ").\nPlease contact the maintainer :-)\n", VerboseLevel.SERIOUS_ERROR);
	    						result ="error\n" +
	    								"\n" +
	    								"No way to serialize return value of method '" + queryName + "' (return type is " + m.getReturnType().getName() + ").";	
	    	    			}
	    	    			
	    	    			
						} catch (IllegalArgumentException e) {
							Logger.log("Error while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + "\n", VerboseLevel.ERROR);
							result = "error\n" +
									e.getClass().getName() + "\n" +
									e.getLocalizedMessage().replace("\"", "'");
							
						} catch (ClassCastException e) {
							Logger.log("Error while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + "\n", VerboseLevel.ERROR);
							result = "error\n" +
							e.getClass().getName() + "\n" +
							e.getLocalizedMessage().replace("\"", "'");
							
						} catch (IllegalAccessException e) {
							Logger.log("Error while executing the request '" + queryName + "': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + "\n", VerboseLevel.ERROR);
							result = "error\n" +
							e.getClass().getName() + "\n" +
							e.getLocalizedMessage().replace("\"", "'");	
							
						} catch (InvocationTargetException e) {
							Logger.log("Error while executing the request '" + queryName + "': " + e.getCause().getClass().getName() + " -> " + e.getCause().getLocalizedMessage() + "\n", VerboseLevel.ERROR);
							result = "error\n" + 
									e.getCause().getClass().getName() + "\n" +
									e.getCause().getLocalizedMessage().replace("\"", "'");							
						
						}
						
						break;
	    	    		
	    	    	}
	    		}
	    		if (!methodFound){
	    			Logger.log("Error while executing the request: method \""+ queryName + "not implemented by the ontology server.\n", VerboseLevel.ERROR);
					result = "error\n" +
							"NotImplementedException\n" +
							"Method " + queryName + " not implemented by the ontology server.";						
	    	    }
	    		
	    		return result + "\n" + MESSAGE_TERMINATOR;
	    	}
		  }
	    
		
    	
		  private Object deserialize(String val, Class<?> type) {
				//not typed because of Method::invoke requirements <- that's what I call a bad excuse
				
					if (type == String.class)
						return Helpers.cleanValue(val);
							
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
							result.put(	Helpers.cleanValue(s.trim().split(":", 2)[0]), 
									Helpers.cleanValue(s.trim().split(":", 2)[1]));
						
						return result;
					}					
					//if the string looks like a set and a set of a list is indeed expected...
					else if (isValidSet && Set.class.isAssignableFrom(type)){
						Set<String> result = new HashSet<String>();
						for (String s : Helpers.tokenize(val, ','))
							result.add(Helpers.cleanValue(s));
						return result;
					}					
					//if the string looks like a set and a list of a list is indeed expected...
					else if (isValidSet && List.class.isAssignableFrom(type)){
						List<String> result = new ArrayList<String>();
						for (String s : Helpers.tokenize(val, ','))
							result.add(Helpers.cleanValue(s));
						return result;
					}
					
					else throw new IllegalArgumentException("Unable to deserialize the parameter of the query! (a " + type.getName() + " was expected, received \"" + val + "\")");
			}
	}
		
	public SocketConnector(
			Properties params,
			HashMap<String, IService> registredServices) {
		
		port = Integer.parseInt(params.getProperty("port", DEFAULT_PORT)); //defaulted to port DEFAULT_PORT if no port provided in the configuration file.
		KEEP_ALIVE_SOCKET_DURATION = Integer.parseInt(params.getProperty("keep_alive_socket_duration", "60")); //defaulted to 1 min if no duration is provided in the configuration file.
		
		this.registredServices = registredServices;
	}

	@Override
	public void finalizeConnector() throws OntologyConnectorException {
		//Objects created in run method are finalized when 
		//program terminates and thread exits
	
		if (server != null) {
			try{
			        server.close();
			} catch (IOException e) {
			        throw new OntologyConnectorException("Could not close the socket server!");
			}
		}
		
		keepOn = false;

	}

	@Override
	public void initializeConnector() throws OntologyConnectorException {
		Thread t = new Thread(this, "Socket connector");
        t.start();
	}


	@Override
	public void run() {
		// 
		try{
	      server = new ServerSocket(port); 
	    } catch (IOException e) {
	    	Logger.log("Error while creating the server: could not listen on port " + port + ". Port busy?\n", VerboseLevel.FATAL_ERROR);
	    	System.exit(-1);
	    }
	    
	    
	    Logger.log("Server started on port " + port + "\n", VerboseLevel.IMPORTANT);
	    
	    while(keepOn){
	      ClientWorker w;
	      try{
    	    w = new ClientWorker(server.accept());
	        Thread t = new Thread(w, w.getName());
	        t.start();
	      } catch (IOException e) {
	    	  if (!server.isClosed()) {
	    		  	Logger.log("Accept failed on port " + port + "\n", VerboseLevel.FATAL_ERROR);
	    	  		System.exit(1);
	    	  }
	      }
	    }

	}

	@Override
	public void refreshServiceList(
			Map<String, IService> registredServices) {
		// TODO Auto-generated method stub
		
	}

}
