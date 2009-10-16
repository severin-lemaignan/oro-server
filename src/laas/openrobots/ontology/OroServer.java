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

package laas.openrobots.ontology;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.connectors.IConnector;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.events.IEventsProvider;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.memory.MemoryManager;

/**
 * {@code OroServer} is the application entry point. It initializes and starts the various services, connectors and background tasks, as set up in the <code>oro-server</code> configuration file.<br/> 
 * 
 * <p>
 * OroServer does mainly two things:
 * <ol>
 * <li>It registers and connects a set of services (presumably related to robotic cognition) to a socket interface,</li>
 * <li>It runs in the background several tasks related to robotic cognition.</li>
 * </ol>
 * </p>
 * 
 * <h2>Features</h2>
 * <p>
 * Most services are related to access and management of a cognitive <em>storage backend</em>. The main storage backend is an ontology, as implemented in the {@link OpenRobotsOntology} class. This class exposes a set of RPC services (and hence implement the {@link IServiceProvider} interface).<br/>
 * However, other services can be registred (like {@link #stats()} that returns statistics on the server itself).<br/>
 * </p>
 * 
 *  <p>
 *  Amongst the tasks that are run in background, we can list:
 *  <ul>
 *  	<li>The event manager (see {@link IEventsProvider})</li>
 *  	<li>The memory manager (see {@link MemoryManager})</li>
 *  </ul>
 *  Others are under developpement, including a "curiosity" module, a cognitive conflict detection and resolution module.
 *  </p>
 *  
 * <h2> Getting access to the server</h2>
 * <p>
 * The communication with the server relies on standard TCP/IP sockets. The ASCII protocole we use is documented here: {@link SocketConnector}.
 * </p>
 * 
 * <h2> Configuration </h2>
 * <p>
 * {@code OroServer} has a {@code main} function which expect a configuration file.<br/> 
 * For the server, the following options are currently available:
 * <ul>
 * <li><em>port = [port number over 4000]</em>: the port on which the server should starts and listen.</li>
 * </ul>
 * </p>
 * 
 * <i>See {@link OpenRobotsOntology#OpenRobotsOntology(Properties)} for others options, specific to the ontologies. Have a look at the config file itself for more details.</i>
 *
 * @author slemaign
 *
 */
public class OroServer implements IServiceProvider {


	/**
	 * The default configuration file (set to {@value}).
	 */
	public static final String DEFAULT_CONF = "etc/oro-server/oro.conf";
	public static final String VERSION = "0.6.1"; //version: major.minor.build (minor -> add/removal of feature, build -> bug correction)
	
	public static final Date SERVER_START_TIME = new Date();

	private volatile boolean keepOn = true;
	private volatile HashSet<IConnector> connectors;
	private volatile HashSet<IEventsProvider> eventsProviders;
	
	/**
	 * This map contains all the "services" offered by the ontology server. Each entry contain 1/the name of the service (unique) 2/a brief description of the service 3/a method to be called 4/the object on which the method should be called.
	 */
	private volatile HashMap<Pair<String,String>, Pair<Method, Object>> registredServices;
	
	private static OpenRobotsOntology oro = null;
	
	public class OnShuttingDown extends Thread { 
		public void run() { 
			System.out.println(" * Application interrupted. Shutting down..."); 

			keepOn = false; 
			try {
				for (IConnector c : connectors)	c.finalizeConnector();
			} catch (OntologyConnectorException e) {
				e.printStackTrace();
			} 
			
			System.out.println(" * Bye bye.");
		} 
	} 
	
	public void addNewServiceProviders(IServiceProvider provider)
	{
			Map<Pair<String,String>, Pair<Method, Object>> services = getDeclaredServices(provider);
			if (services != null)
				registredServices.putAll(services);
			
			// refresh connectors
			for (IConnector c : connectors)	c.refreshServiceList(registredServices);
	}
	
	public void runServer(String[] args) throws InterruptedException, OntologyConnectorException, OntologyServerException { 
   	
    	
    	String confFile;
    	Properties confParams;
    	
    	connectors = new HashSet<IConnector>();
    	eventsProviders = new HashSet<IEventsProvider>();
    	registredServices = new HashMap<Pair<String,String>, Pair<Method, Object>>();
    	
    	Helpers.printlnInBlue("--------------------------------------\n" +
    						"-                                    -\n" +			
    						"-          OroServer " + VERSION + "           -\n" +
    						"-                                    -\n" +
    						"-         " + (char)27 + "[0m(c)LAAS-CNRS 2009"+ (char)27 +"[34m          -\n" +
							"--------------------------------------");
    	
    	Runtime.getRuntime().addShutdownHook(new OnShuttingDown());
    	
    	
    	if (args.length < 1 || args.length > 1)
    		confFile = DEFAULT_CONF;
    	else
    		confFile = args[0];
    	
    	confParams = getConfiguration(confFile);
		System.out.println(" * Using configuration file " + confFile);
		
    	//System.err.close(); //remove YARP message, but remove Oro error messages as well!!

		/********************************************************************************
		 *               BACKENDS and SERVICES REGISTRATION                             *
		 ********************************************************************************/

		//add the services offered by the OroServer class
		addNewServiceProviders(this);
		
		//Open and load the ontology + register ontology services by the server. If the configuration file can not be found, it exits.
		oro = new OpenRobotsOntology(confParams);
		
		//System.out.println(oro.getResourceDetails("PurposefulAction").getJson());
		
		addNewServiceProviders(oro);
				
		// Check we have registred services and list them
		if (registredServices.size() == 0)
			throw new OntologyServerException("No service registred by the ontology server! I've no reason to continue, so I'm stopping now.");
			
		Helpers.printlnInPurple(" * Following services are registred:");
    	for (Pair<String,String> m : registredServices.keySet())
    	{
    		Helpers.printInPurple("\t- " + m.getLeft());
    		//if present, display the description as well
    		if (m.getRight() != "")
    			System.out.println(" -> " + m.getRight());
    		else
    			System.out.println("");
    	}

		
		/********************************************************************************
		 *                       CONNECTORS INITIALIZATION                              *
		 ********************************************************************************/
    	
    	// Currently, only one connector, the socket connector (others bridges like YARP or JSON are now out of the oro-server code base)
    	SocketConnector sc = new SocketConnector(confParams, registredServices);
		connectors.add(sc);

		for (IConnector c : connectors)	{
			try {
				c.initializeConnector();
			} catch (OntologyConnectorException e) {
				System.err.println("[ERROR] Couldn't initialize a connector: " + e.getLocalizedMessage());
				connectors.remove(c);
			}
		}
		
		if (connectors.size() == 0) {
			System.err.println("[FATAL ERROR] None of the connectors could be started! Killing myself now.");
			System.exit(1);
		}

		/********************************************************************************
		 *                               MAIN LOOP                                      *
		 ********************************************************************************/

		while(keepOn) {			
			
			Thread.sleep(10);
    	}
		
		//Finalization occurs in the shutdown hook, above.
	}
	
	
	public static void main(String[] args) throws OntologyConnectorException, InterruptedException, OntologyServerException {
		new OroServer().runServer(args);
	}
	
	/**
	 * Returns several statistics on the server.
	 * 
	 * Returned values:
	 * <ul>
	 *  <li>the server version</li>
	 *  <li>the hostname where the server runs</li>
	 *  <li>server uptime</li>
	 *  <li>the current amount of classes in the ontology</li>
	 *  <li>the current amount of instances in the ontology</li>
	 *  <li>the current amount of client connected to the server</li>
	 * </ul>
	 * 
	 * @return a map containing the statistics (pairs name/value)
	 */
	@RPCMethod(
			desc = "returns some statistics on the server"
	)
	public static Map<String, String> stats() {
		Map<String, String> stats = new HashMap<String, String>();
		
		
		SimpleDateFormat formatNew = new SimpleDateFormat("HHHH 'hour(s)' mm 'minute(s)' ss 'second(s)'");
		formatNew.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		
		stats.put("version", VERSION);
		
		try {
			stats.put("host", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			stats.put("host", "unknow");
		}
		
		stats.put("uptime", formatNew.format((new Date()).getTime() - OroServer.SERVER_START_TIME.getTime()));
		
		// Nb of classes
		//stats.put("nb_classes", String.valueOf(oro.getModel().listClasses().toSet().size()));
		stats.put("nb_classes", "not available");
		
		//Nb of instances
		//stats.put("nb_instances", String.valueOf(oro.getModel().listIndividuals().toSet().size()));
		stats.put("nb_instances", "not available");
		
		//Nb of clients
		stats.put("nb_clients", "not available");
		
		return stats;
	}

	@RPCMethod(
			desc = "returns a human-friendly list of available methods with their signatures and short descriptions."
	)
	public String help() {
		
		
		String help = (char)27 + "[34mHello!" + (char)27 + "[32m You are running oro-server v." + VERSION + (char)27 + "[0m\n\nYou'll find below the list of available remote services in the knowledge base:\n";
		
		Iterator<Entry<Pair<String, String>, Pair<Method, Object>>> it = registredServices.entrySet().iterator();
	    while (it.hasNext()) {
	    	
	    	Entry<Pair<String, String>, Pair<Method, Object>> pairs = it.next();
	    	
	    	
	    	//build the list of expected parameters
	    	String params = "(";
	    	for (Class<?> param : pairs.getValue().getLeft().getParameterTypes())
	    		params += param.getSimpleName() + ", ";
	    	
	    	if (!params.equals("("))
	    		params = params.substring(0, params.length() - 2);
	    	
	    	params += ")";
	    	
	        help += "\t- " + (char)27 + "[35m" + pairs.getKey().getLeft() + params + (char)27 + "[0m: " + pairs.getKey().getRight() + "\n";
	    }
		
	    help += "\nTo execute a command, you must enter its name (case insensitive), followed by one parameter per line. To finish, type " + SocketConnector.MESSAGE_TERMINATOR + " and 'return'. The server should answer.\n";
		return help;
	}
	
	@RPCMethod(
			desc = "returns the list of available methods with their signatures and short descriptions as a map."
	)
	public Map<String, String> listMethods() {
		
		
		Map<String, String> help = new HashMap<String, String>();
		
		Iterator<Entry<Pair<String, String>, Pair<Method, Object>>> it = registredServices.entrySet().iterator();
	    while (it.hasNext()) {
	    	
	    	Entry<Pair<String, String>, Pair<Method, Object>> pairs = it.next();
	    	
	    	
	    	//build the list of expected parameters
	    	String params = "(";
	    	for (Class<?> param : pairs.getValue().getLeft().getParameterTypes())
	    		params += param.getSimpleName() + ", ";
	    	
	    	if (!params.equals("("))
	    		params = params.substring(0, params.length() - 2);
	    	
	    	params += ")";
	    	
	        help.put(pairs.getKey().getLeft() + params, pairs.getKey().getRight());
	    }
		
		return help;
	}
	
	@RPCMethod(
			desc = "returns a raw list of available methods."
	)
	public Set<String> listSimpleMethods() {
		
		
		Set<String> help = new HashSet<String>();
		
		Iterator<Entry<Pair<String, String>, Pair<Method, Object>>> it = registredServices.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<Pair<String, String>, Pair<Method, Object>> pairs = it.next();
	    	//build the list of expected parameters 	
	        help.add(pairs.getKey().getLeft());
	    }
		return help;
	}

	private Map<Pair<String, String>, Pair<Method, Object>> getDeclaredServices(Object o) {
		
		HashMap<Pair<String, String>, Pair<Method, Object>> registredServices = new HashMap<Pair<String, String>, Pair<Method, Object>>();
		
		for (Method m : o.getClass().getMethods()) {
			RPCMethod a = m.getAnnotation(RPCMethod.class);
			if (a != null) {
				String name = a.rpc_name().equalsIgnoreCase("") ? m.getName() : a.rpc_name();
				
				registredServices.put(new Pair<String, String>(name, a.desc()), new Pair<Method, Object>(m, o));
			}
		}
		return registredServices;
	}
	
	/**
	 * Read a configuration file and return to corresponding "Properties" object.
	 * The configuration file contains the path to the ontology to be loaded and several options regarding the server configuration.
	 * @param configFileURI The path and filename of the configuration file.
	 * @return A Java.util.Properties instance containing the application configuration.
	 */
	private Properties getConfiguration(String configFileURI){
		/****************************
		 *  Parsing of config file  *
		 ****************************/
		Properties parameters = new Properties();
        try
		{
        	FileInputStream fstream = new FileInputStream(configFileURI);
        	parameters.load(fstream);
			fstream.close();
			
			if (!parameters.containsKey("oro_common_sense"))
			{
				System.err.println("No common sense ontology specified in the configuration file (\"" + configFileURI + "\"). Add smthg like oro_common_sense=commonsense.owl");
	        	System.exit(1);
			}
		}
        catch (FileNotFoundException fnfe)
        {
        	System.err.println("No config file. Check \"" + configFileURI + "\" exists.");
        	System.exit(1);
        }
        catch (Exception e)
		{
			System.err.println("Config file input error. Check config file syntax.");
			System.exit(1);
		}
        
        return parameters;
	}

}
