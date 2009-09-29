package laas.openrobots.ontology.connectors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.RPCMethod;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

public class JsonConnector implements IConnector {

	private Server server;
	private Context root;

	public JsonConnector(Properties params, HashMap<Pair<String,String>, Pair<Method, Object>> listOfServices){
		
		server = new Server(Integer.parseInt(params.getProperty("json_port", "8080")));
		root = new Context(server,"/",Context.SESSIONS);
		root.addServlet(new ServletHolder(new OroHelloServlet()), "/");
		
		 JSONRPCServlet jsonServlet = new JSONRPCServlet();
		 ServletHolder _servlet = new ServletHolder(jsonServlet);
		 _servlet.setInitParameter("auto-session-bridge", "0");
		    
		root.addServlet(_servlet, "/oro");
		
		registerJsonServices(listOfServices);
	    
		
	}

	/**
	 * Registers the methods annotated with a RPCMethod annotation in the JSON server.
	 * Several limitations:
	 * <ul>
	 * 	<li>The RPC method name as specified in the annotation won't be used. The underlying Java method name is used instead.</li>
	 *  <li>The methods are identified after the Java class name (ie, to get the server stats, one must call the {@code OroServer.stats} JSON method). It means that it's not possible to have to different instances of the same class providing services in JSON.</li>
	 * </ul>
	 * @param list
	 */
	private void registerJsonServices(HashMap<Pair<String,String>, Pair<Method, Object>> list){

		Set<Object> objects = new HashSet<Object>();
		
		for (Pair<Method, Object> service : list.values()){
			if (!objects.contains(service.getRight()))
				objects.add(service.getRight());
		}
		
		for (Object o : objects) {
			//System.out.println(" * RPC services from " + o.getClass().getSimpleName() + " registred into the JSON server.");
			JSONRPCBridge.getGlobalBridge().registerObject(o.getClass().getSimpleName(), o, RPCMethod.class);
		}
		

	}
	
	@Override
	public void finalizeConnector() throws OntologyConnectorException {
		System.out.print(" * Stopping JSON server...");
		try {
			server.stop();
			System.out.println("done!");
		} catch (Exception e) {
			//TODO Handle that!
			throw new OntologyConnectorException("Exception while stopping the JSON server!");
		}
	

	}

	@Override
	public void initializeConnector() throws OntologyConnectorException {
		System.out.println(" * Starting JSON server...");
		 try {
			  server.start();
		} catch (Exception e) {
			throw new OntologyConnectorException("Exception while starting the JSON server (" + e.getLocalizedMessage() + ").");
		}

	}

	@Override
	public void refreshServiceList(
			Map<Pair<String, String>, Pair<Method, Object>> registredServices) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {

	}
	
	public static class OroHelloServlet extends HttpServlet
	  {
		private static final long serialVersionUID = 265835126832215207L;

		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		  {
		      response.setContentType("text/html");
		      response.setStatus(HttpServletResponse.SC_OK);
		      response.getWriter().println("<h1>ORO web access</h1>");
		      //response.getWriter().println("session="+request.getSession(true).getId() + "<br\\>");
		      response.getWriter().println("A JSON-RPC access is available at "+ request.getLocalAddr() + ":" + request.getLocalPort() + "/oro");
		  }
	  }

}
