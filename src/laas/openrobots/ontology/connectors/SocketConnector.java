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

package laas.openrobots.ontology.connectors;


import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
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

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.Request;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Pair;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.events.EventModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.events.IWatcher;
import laas.openrobots.ontology.modules.events.OroEvent;
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
 * return values depend on the type of event. Details are provided on the {@link IWatcher.EventType}
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
	public static final String DEFAULT_KEEP_ALIVE_SOCKET_DURATION = "6000";
	
	int KEEP_ALIVE_SOCKET_DURATION;
	
	public static final String DEFAULT_PORT = "6969";
	public static final String MESSAGE_TERMINATOR = "#end#";

	private Charset charset = Charset.forName("UTF-8");
	
	private int port;
	private ServerSocketChannel server = null;
		
	/**
	 * serviceIndex holds a map of service name to keys in registredServices.
	 * It allows a fast retrieval of the list of service that matches a name.
	 * 
	 * serviceIndex also store, for each service, the number of exposed arguments.
	 */
	private HashMap<String, Set<Pair<IService, Integer>>> serviceIndex;
	
	private volatile boolean keepOn = true;

	private OroServer mainThread;
	
	/**
	 * Inner class that is forked at incoming connections.
	 * @author slemaign
	 *
	 */
	public class ClientWorker implements Runnable, IEventConsumer {
		  private SocketChannel client;
		 
		  private ByteBuffer buffer = ByteBuffer.allocate(4096);
		  private String remainsOfMyBuffer = "";
		  
		  private Selector selector;
		  private SelectionKey key;
		  
		  private String name;
		  
		  private boolean keepOnThisWorker = true;
		  
		  volatile private boolean incomingEvent = false;
		  
		  private List<Pair<UUID, OroEvent>> eventsQueue;
		  
		  public ClientWorker(SocketChannel client) {
			  
			  if (client == null) return; // Useful when uni-testing functions in this class
			  
			  try {
				  selector = Selector.open();
			  } catch (IOException e1) {
				  Logger.log("SocketConnector error: impossible to get a selector" +
				 		"for a socket! Better to quit now :-/\n", VerboseLevel.FATAL_ERROR);
				  Logger.log("Exception: " + e1.getLocalizedMessage() + "\n", VerboseLevel.DEBUG);
				  System.exit(1);
			  }

			  
			  this.client = client;
			  
			  //Set the socket in non-blocking mode
			  try {
				  client.configureBlocking(false);
			  } catch (IOException e) {
				  Logger.log("SocketConnector error: impossible to set the socket " +
						   "in non-blocking mode! I kill it now.\n", VerboseLevel.SERIOUS_ERROR);
				  Logger.log("Exception: " + e.getLocalizedMessage() + "\n", VerboseLevel.DEBUG);
				  return;
			  }
				
			  try {
				key = client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
			} catch (ClosedChannelException e) {
				 Logger.log("SocketConnector error: the socket has been closed" +
				 		"before any operation! I kill it now.\n", VerboseLevel.SERIOUS_ERROR);
				 Logger.log("Exception: " + e.getLocalizedMessage() + "\n", VerboseLevel.DEBUG);
				 return;
			}

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

			List<String> req = null;
		    long timeLastActivity = System.currentTimeMillis();
		    
		    while(keepOn && keepOnThisWorker){
		    	    	 	    		
	    		try {
					Thread.sleep(20);
				} catch (InterruptedException e) {}
				
				if (System.currentTimeMillis() - timeLastActivity > (KEEP_ALIVE_SOCKET_DURATION * 1000)) {
					Logger.log("Connection " + getName() + " has been closed because it was inactive since " + KEEP_ALIVE_SOCKET_DURATION + " sec. Please use the \"close\" method in your clients to close properly the socket.\n", VerboseLevel.WARNING);
					keepOnThisWorker = false;
					break;
	    		}

				long timeStartParsingReq = System.currentTimeMillis();
			
				if (remainsOfMyBuffer.length() > 500) {
								Logger.log("Incoming socket buffer filling up! Current length: " +  remainsOfMyBuffer.length() + "\n", VerboseLevel.WARNING);
				}
				req = parseBuffer(null); //First, see if we have a pending request from previous socket reads.
				if (req == null) {
			
					try{
						req = null;
						buffer.clear();
						int count = client.read( buffer );
						
						if (count > 0) { 
							buffer.flip();					
							req = parseBuffer(buffer);
							
							if (req != null && req.size() == 0) {
								Logger.log("Got an empty query! (only #end#) Discarding it.\n", VerboseLevel.ERROR);
								req = null;
							}
						}
			    		
			        } catch (IOException e) {
			        	Logger.log("Read failed on one of the opened socket (" + getName() + "). Killing it.\n", VerboseLevel.SERIOUS_ERROR);
						keepOnThisWorker = false;
						break;
					}
				}
	        
				if (System.currentTimeMillis() - timeStartParsingReq > 50) { // If we take more than 50ms to parse the socket stream, smthg is wrong somewhere!
		        	Logger.log("oro-server took " + (System.currentTimeMillis() - timeStartParsingReq) + "ms to parse the socket stream! Too much!\n", VerboseLevel.WARNING);
				}

	    		if (req != null) 
	    		{
		    		timeLastActivity = System.currentTimeMillis();
			    		
		    		//Execute the request
		    		String res = handleRequest(req);
		        
		        	Logger.log("<< Send response: " + res + "\n", VerboseLevel.DEBUG);
		        	
		        	//Send data back to client	
		        	try {
						client.write(charset.encode(res + "\n"));
					} catch (IOException e) {
						 Logger.log("SocketConnector error: impossible to" +
							 		"write to a socket! I kill it now.\n", VerboseLevel.SERIOUS_ERROR);
						 Logger.log("Exception: " + e.getLocalizedMessage() + "\n", VerboseLevel.DEBUG);
						 return;
					}

	    		}
	    		
				
				if (incomingEvent) {
					incomingEvent = false;
					
					List<Pair<UUID, OroEvent>> tmpEvtsQueue = new ArrayList<Pair<UUID,OroEvent>>(eventsQueue);
					
					Iterator<Pair<UUID, OroEvent>> it = tmpEvtsQueue.iterator();
					
					while(it.hasNext()) {
						Pair<UUID, OroEvent> evt = it.next();
						OroEvent e = evt.getRight();
						try {
							String evtMsg = "event\n" +
											evt.getLeft() + "\n" +
											(!e.getEventContext().equals("") ?
													e.getEventContext() + "\n":
													"\n") +
											MESSAGE_TERMINATOR + "\n";
												
							client.write(charset.encode(evtMsg));
						} catch (IOException e1) {
							 Logger.log("SocketConnector error: impossible to" +
								 		"write to a socket! I kill it now.\n", VerboseLevel.SERIOUS_ERROR);
							 Logger.log("Exception: " + e1.getLocalizedMessage() + "\n", VerboseLevel.DEBUG);
							 return;
						}
						
						eventsQueue.remove(evt);
						
						Logger.log("Event " + evt.getLeft() + " notified.\n", VerboseLevel.INFO);
					}
					
				}
		    }
		  }
		  
		  public List<String> parseBuffer(ByteBuffer buffer) {
			
			  String req = remainsOfMyBuffer; 
			  
			  if (buffer != null)
				  req += charset.decode(buffer).toString().replaceAll("\r\n|\r", "\n");
			  	  
			  List<String> res = new ArrayList<String>();
			  
		      //Special case for the command "help": we don't require to enter 
		      //the message terminaison string.
			  if (req.startsWith("help")) {
				  res.add("help");
				  remainsOfMyBuffer = req.substring(5); //len("help\n") = 5
				  return res;
			  }
			  
			  int i = req.indexOf(MESSAGE_TERMINATOR);

			  if (i >= 0) {
				  String rawReq = req.substring(0, i);
				  res = Arrays.asList(rawReq.split("\n"));
				  
				  if (req.length() > i)
					  remainsOfMyBuffer = req.substring(i + MESSAGE_TERMINATOR.length()).replaceAll("\n$", "");
				  
				  return res;
			  }
			  
			  remainsOfMyBuffer = req; // waiting for more input to complete the request
			  
			  return null;
		}

		public String handleRequest(List<String> raw_query) {
			  
			  boolean methodFound = false;
			  boolean hasRightArgsNb = false;
			  boolean invokationDone = false;
			  
			  Method m = null;
			  Object o = null;
			  Object[] args = null;
			  
			  String result = "error\n\n";
			  String queryName = raw_query.get(0);
			  
			  // Log the incoming request
			  String formatted = "";
			  formatted = "[" + Logger.GetTimestamp() + " thread " + this.hashCode() + "] >> Got incoming request: " + queryName + "(";
	  
			  if (raw_query.size() > 1) {
				  for (int i = 1; i < raw_query.size() ; i++) {
					  String arg = raw_query.get(i);
				  	  if (arg.startsWith("[") || arg.startsWith("{")) //assume a set or a map
				  		formatted += arg + ",";
				  	  else
				  		formatted += "\"" + arg + "\",";
				  }
				  formatted = formatted.substring(0, formatted.length() -1); //remove trailing comma
			  }
			  Logger.log(formatted + ")\n", VerboseLevel.VERBOSE, false);
			  // --end logging
			  
	    	if (queryName.equalsIgnoreCase("close")){
	    		Logger.log("Closing communication with client " + getName() + ".\n");
	    		
	    		keepOnThisWorker = false;
	    		
	    		return "Closing now!";
	    	}
	    	else
	    	{
	    		
	    		/* Check we know a service with the required name */
	    		if (!serviceIndex.containsKey(queryName.toLowerCase())) {
	    			Logger.log("Error while executing the request: method \""+ 
	    					queryName + "\" not implemented.\n", VerboseLevel.ERROR);
 					result = "error\n" +
 							"NotImplementedException\n" +
 							"Method \""+ queryName + "\" not implemented.";
 					return result + "\n" + MESSAGE_TERMINATOR;
	    		}
	    		
	    		/******* Iterate on registered methods ********/
	    		for (Pair<IService, Integer> service : serviceIndex.get(queryName.toLowerCase())){
	    			
	    			if (service.getRight().intValue() == (raw_query.size() - 1)) //Do we have the right amount of arguments?
	    	    	{
	    				hasRightArgsNb = true;
    	    			
    	    			m = service.getLeft().getMethod();
    	    			o = service.getLeft().getObj();
	    	    			
    	    			result = "";
    	    			
    	    			args = new Object[m.getParameterTypes().length];
  	    			
    	    			int i = 0;
    	    			int shiftSpecialCases = 0;
    	    			
    	    			//Now, check we have the expected types
    	    			methodFound = true;
    	    			for (Class<?> param : m.getParameterTypes()) {
	    	    				
	    					//TODO: This is hackish.
    	    				if (OroServer.discardedTypeFromServiceArgs.contains(param)) {
    	    					shiftSpecialCases ++;
    	    					
    	    					if (param.equals(IEventConsumer.class))
    	    							args[i] = this;
    	    					
    	    				}
    	    				else {
    	    					try {
	    	    					Object ob = Helpers.deserialize(raw_query.get(i + 1), param);
	    	    					Logger.log("Parameter: " + ob.toString() + "\n", VerboseLevel.DEBUG);
	    	    					args[i + shiftSpecialCases] = ob; 
		    	    				i++;
    	    					}
    	    					catch (IllegalArgumentException iae) {
    	    						methodFound = false;
    	    						break;
    	    					}
		    					catch (OntologyServerException e) { //This exception occurs when a unicode string couldn't be unescaped
									Logger.log(e.getLocalizedMessage(), VerboseLevel.ERROR);
									result = "error\n" +
											"OntologyServerException\n" +
											e.getLocalizedMessage();
								}
    	    				}
    	    				
    	    			}
    	    			
    	    			if (methodFound)
    	    				break;
	    	    	}
	    			
	    		} //End of for-loop on the service that match the query name
	    		
	    		if (!hasRightArgsNb) {
    				String msg = "Error while executing the request: wrong number " +
    						"of parameters provided for " +
    						"method \""+ queryName + "\" (" + (raw_query.size() - 1) + 
    						" were provided).";
					Logger.log(msg + "\n", VerboseLevel.ERROR);
 					result = "error\n" +
 							"NotImplementedException\n" +
 							msg;
 					return result + "\n" + MESSAGE_TERMINATOR;
    			}
    			
    			if (!methodFound) {
    				String msg = "Error while executing the request: no method " +
    						"prototype for '" + queryName + "' match the given " +
							"arguments.";
					Logger.log(msg + "\n", VerboseLevel.ERROR);
 					result = "error\n" +
 							"NotImplementedException\n" +
 							msg;
 					return result + "\n" + MESSAGE_TERMINATOR;
    			}
	    	    		
    			Request r = new Request(m,o,args);
    			
	    		mainThread.pushRequest(r);
    			
    			try {
					result = r.result.take(); //Blocks until a result is available
				} catch (InterruptedException e) {
					String msg = "Internal error while executing the request: thread " +
					"interrupted. Please report this bug to openrobots@laas.fr";
					Logger.log(msg + "\n", VerboseLevel.SERIOUS_ERROR);
					result = "error\n" +
						"InterruptedException\n" +
						msg;
					return result + "\n" + MESSAGE_TERMINATOR;
				} 
   		
	    		return result + "\n" + MESSAGE_TERMINATOR;
	    	}
		  }
	    
	}
		
		public SocketConnector(
				Properties params,
				HashMap<String, IService> registredServices,
				OroServer server) {
			
			if (params != null) {
				port = Integer.parseInt(params.getProperty("port", DEFAULT_PORT)); //defaulted to port DEFAULT_PORT if no port provided in the configuration file.
				KEEP_ALIVE_SOCKET_DURATION = Integer.parseInt(params.getProperty("keep_alive_socket_duration", "60")); //defaulted to 1 min if no duration is provided in the configuration file.
			}
			else {
				port = Integer.parseInt(DEFAULT_PORT);
				KEEP_ALIVE_SOCKET_DURATION = Integer.parseInt(DEFAULT_KEEP_ALIVE_SOCKET_DURATION);
			}
				  
		// Fills the serviceIndex map.
		
		serviceIndex = new HashMap<String, Set<Pair<IService,Integer>>>();
		
		if (registredServices != null) refreshServiceList(registredServices);
		
		this.mainThread = server;

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
			server = ServerSocketChannel.open();
			server.socket().bind(new InetSocketAddress(port)); 
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
	    	  if (!server.socket().isClosed()) {
	    		  	Logger.log("Accept failed on port " + port + "\n", VerboseLevel.FATAL_ERROR);
	    	  		System.exit(1);
	    	  }
	      }
	    }

	}

	@Override
	public void refreshServiceList(
			Map<String, IService> registredServices) {
		
		for (String key : registredServices.keySet()){
			
			Method m = registredServices.get(key).getMethod();
			String name = registredServices.get(key).getName().toLowerCase();
			
			if (!serviceIndex.containsKey(name))
				serviceIndex.put(name, new HashSet<Pair<IService,Integer>>());
			
			Pair<IService, Integer> entry = new Pair<IService, Integer>(
					registredServices.get(key), 
					OroServer.nbExposedParameters(m));
			
			serviceIndex.get(name).add(entry);
			
		}
	}
	
	@Override
	public void clearServiceList() {
		serviceIndex.clear();
	}
	

}
