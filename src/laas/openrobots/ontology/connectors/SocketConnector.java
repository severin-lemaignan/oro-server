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

import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

public class SocketConnector implements IConnector, Runnable {

	/** Maximum time (in millisecond) the server should keep alive a socket when inactive. 
	 */
	final int KEEP_ALIVE_SOCKET_DURATION = 5000;
	
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
					
					if (System.currentTimeMillis() - timeLastActivity > KEEP_ALIVE_SOCKET_DURATION) {
						System.err.println(" * Connection " + getName() + " has been closed because it was inactive since " + KEEP_ALIVE_SOCKET_DURATION + "ms. Please use the \"close\" method in your clients to close properly the socket.");
						keepOnThisWorker = false;
						break;
		    		}
				
					try{
			    		line = in.readLine(); //answers null if the stream doesn't contain a "\n"
			        } catch (IOException e) {
						System.err.println("Read failed on one of the openned socket.");
						keepOnThisWorker = false;
					}
		        
		    		if (line != null) {
			    		line = line.trim();
			    		
			    		if (line.equalsIgnoreCase("#end#")) {
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
	    		
	    		return result + "\n#end#";
	    	}
		  }
	    	
    	private <V> String listToString(List<V> list) {
    		String str = "[";
    		for (V v : list) {
    			str += v.toString() + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "]";
    		
    		return str;
    	}

    	private <V> String setToString(Set<V> list) {
    		String str = "[";
    		for (V v : list) {
    			str += v.toString() + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "]";
    		
    		return str;
    	}

    	private <K, V> String mapToString(Map<K, V> map) {
    		String str = "{";
    		for (Entry<K, V> es : map.entrySet()) {
    			str += es.getKey().toString() + ":" + es.getValue().toString() + ",";
    		}
    		
    		str = str.substring(0, str.length() - 1) + "}";
    		
    		return str;
    	}
			  

		  private Object deserialize(String val, Class<?> type) {
				//not typed because of Method::invoke requirements <- that's what I call a bad excuse
				
					if (type == String.class)
						return val;
							
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
					for (String s : val.split(","))
						if (!isValidMap || !s.contains(":"))
							isValidMap = false;
					
					assert(!(isValidMap && isValidSet));
					
					//if the string looks like a map and a map is indeed expected...
					if (isValidMap && Map.class.isAssignableFrom(type)){
						Map<String, String> result = new HashMap<String, String>();
						for (String s : val.split(","))
							result.put(s.trim().split(":")[0], s.trim().split(":")[1]);
						return result;
					}					
					//if the string looks like a set and a set of a list is indeed expected...
					else if (isValidSet && Set.class.isAssignableFrom(type)){
						Set<String> result = new HashSet<String>();
						for (String s : val.split(","))
							result.add(s.trim());
						return result;
					}					
					//if the string looks like a set and a list of a list is indeed expected...
					else if (isValidSet && List.class.isAssignableFrom(type)){
						List<String> result = new ArrayList<String>();
						for (String s : val.split(","))
							result.add(s.trim());
						return result;
					}
					
					else throw new IllegalArgumentException("Unable to deserialize the parameter of the query! (a " + type.getName() + " was expected, received \"" + val + "\")");
			}
	}
		
	public SocketConnector(
			Properties params,
			HashMap<Pair<String, String>, Pair<Method, Object>> registredServices) {
		
		port = Integer.parseInt(params.getProperty("port", "6969")); //defaulted to port 6969 if no port provided in the configuration file.
		
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
	    
	    
	    System.out.println(" * Server started on port " + port);
	    
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
