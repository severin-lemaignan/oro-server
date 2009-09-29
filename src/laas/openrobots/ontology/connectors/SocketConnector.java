package laas.openrobots.ontology.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

public class SocketConnector implements IConnector, Runnable {

	int port = 4444;
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
		  
		  ClientWorker(Socket client) {
		   this.client = client;
		  }

		  
		  public void run(){
		    String request;
		    BufferedReader in = null;
		    PrintWriter out = null;
		    try{
		      in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		      out = new PrintWriter(client.getOutputStream(), true);
		    } catch (IOException e) {
		      System.err.println("in or out failed");
		      System.exit(-1);
		    }

		    while(keepOn){
		      try{
		        request = in.readLine().trim();
		        //Send data back to client
		        
		        String res = handleRequest(request);
		        
		         out.println(res);
		       } catch (IOException e) {
		         System.out.println("Read failed");
		         System.exit(-1);
		       }
		    }
		  }
		  
		  public String handleRequest(String request) {
			  
			  boolean methodFound = false;
			  String[] tokens = request.split("\\s");
			  
			  System.out.println(" * Got a request: ");
			  for (String s : tokens)
			         System.out.println(s);


	    	if (tokens[0].equalsIgnoreCase("close")){
	    		System.out.println(" * Closing communication");
	    		//TODO : Fermer le socket
	    		return "Closing now!";
	    	}
	    	else
	    	{
	    		/******* Externally registred methods ********/
	    		for (Pair<String,String> name : registredServices.keySet()){
	    			
	    			Method m = registredServices.get(name).getLeft();
	    			Object o = registredServices.get(name).getRight();
	    			
	    	    	if (name.getLeft().equalsIgnoreCase(tokens[0]) &&
	    	    			(
    	    				(tokens.length == 1) ? 
    	    						m.getParameterTypes().length == 0 :
    	    						(m.getParameterTypes().length == tokens.length - 1)
    	    				)
    	    			)
	    	    	{
	    	    		methodFound = true;
	    	    		
	    	    		return "Method " + tokens[0] + " found. Youpi.";
	    	    		
	    	    	}
	    		}
	    		return "Method not found. Bouhouh.";
	    	}
			  
			  
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
	        Thread t = new Thread(w);
	        t.start();
	      } catch (IOException e) {
	        System.err.println("Accept failed on port " + port);
	        System.exit(-1);
	      }
	    }

	}

}
