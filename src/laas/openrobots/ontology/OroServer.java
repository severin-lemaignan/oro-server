/*
 * Copyright (c) 2008-2011 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
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

package laas.openrobots.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.connectors.IConnector;
import laas.openrobots.ontology.connectors.SocketConnector;
import laas.openrobots.ontology.exceptions.EventRegistrationException;
import laas.openrobots.ontology.exceptions.InvalidModelException;
import laas.openrobots.ontology.exceptions.InvalidPluginException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.exceptions.OntologyServerException;
import laas.openrobots.ontology.exceptions.PluginNotFoundException;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.helpers.Logger.Colors;
import laas.openrobots.ontology.modules.IModule;
import laas.openrobots.ontology.modules.alterite.AlteriteModule;
import laas.openrobots.ontology.modules.base.BaseModule;
import laas.openrobots.ontology.modules.categorization.CategorizationModule;
import laas.openrobots.ontology.modules.events.EventModule;
import laas.openrobots.ontology.modules.events.IEventConsumer;
import laas.openrobots.ontology.modules.memory.MemoryManager;
import laas.openrobots.ontology.service.IService;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;
import laas.openrobots.ontology.service.ServiceImpl;

/**
 * {@code OroServer} is the application entry point. It initializes and starts 
 * the various services, connectors and background tasks, as set up in the 
 * <code>oro-server</code> configuration file.<br/> 
 * 
 * <p>
 * OroServer does mainly two things:
 * <ol>
 * <li>It registers and connects a set of services (presumably related to 
 * robotic cognition) to a socket interface,</li>
 * <li>It runs in the background several tasks related to robotic cognition.</li>
 * </ol>
 * </p>
 * 
 * <h2>Features</h2>
 * <p>
 * Most services are related to access and management of a cognitive <em>storage
 *  backend</em>. The main storage backend is an ontology, as implemented in the
 *   {@link OpenRobotsOntology} class. This class exposes a set of RPC services
 *    (and hence implement the {@link IServiceProvider} interface).<br/>
 * However, other services can be registred (like {@link #stats()} that returns 
 * statistics on the server itself).<br/>
 * </p>
 * 
 *  <p>
 *  Amongst the tasks that are run in background, we can list:
 *  <ul>
 *  	<li>The event manager (see {@link laas.openrobots.ontology.modules.events})</li>
 *  	<li>The memory manager (see {@link MemoryManager})</li>
 *  </ul>
 *  Others are under developpement, including a "curiosity" module, a cognitive
 *   conflict detection and resolution module.
 *  </p>
 *  
 * <h2> Getting access to the server</h2>
 * <p>
 * The communication with the server relies on standard TCP/IP sockets. The 
 * ASCII protocole we use is documented here: {@link SocketConnector}.
 * </p>
 * 
 * <h2> Configuration </h2>
 * <p>
 * {@code OroServer} has a {@code main} function which expect a configuration 
 * file.<br/> 
 * For the server, the following options are currently available:
 * <ul>
 * <li><em>port = [port number over 4000]</em>: the port on which the server 
 * should starts and listen.</li>
 * </ul>
 * </p>
 * 
 * <i>See {@link OpenRobotsOntology#OpenRobotsOntology(Properties)} for others 
 * options, specific to the ontologies. Have a look at the config file itself 
 * for more details.</i>
 *
 * @author slemaign
 *
 */
public class OroServer implements IServiceProvider {


	/**
	 * The default configuration file (set to {@value}).
	 */
	public static final String DEFAULT_CONF = "etc/oro-server/oro.conf";
	
	/**
	 * Defines the standard language used in particular for labels retrieval.<br/>
	 * This field can be set up in the configuration file with the {@code language} option.<br/>
	 *  Expected values are ISO 2 characters language codes.
	 */
	public static String DEFAULT_LANGUAGE = "en";
	
	public static boolean HAS_A_TTY;
	public static VerboseLevel VERBOSITY = VerboseLevel.INFO;
	public static boolean DEMO_MODE = false;
	public static Properties ServerParameters;
	
	public static boolean BLINGBLING;
	
	public static final String VERSION = "0.8.2"; //version: major.minor.build (minor -> add/removal of feature, build -> bug correction)
	
	public static final Date SERVER_START_TIME = new Date();

	private volatile boolean keepOn = true;
	private volatile HashSet<IConnector> connectors;
	private volatile HashSet<IModule> modules;
	
	/**
	 * This map contains all the "services" offered by the ontology server. Each
	 * entry contain 1/the name of the service (unique) 2/an instance of a 
	 * service model storing an instancied object on which the service should 
	 * be called.
	 */
	private volatile HashMap<String, IService> registredServices;
	
	/** 
	 * This set holds the types that should not be exposed to oro-server clients
	 *  but are required by some RPC methods (for instance to retrieve the
	 *  context when the query was received).
	 *  
	 *  TODO: This is more like a workaround...
	 */
	public static final Set<Class> discardedTypeFromServiceArgs;
	
	static {
		discardedTypeFromServiceArgs = new HashSet<Class>();
		discardedTypeFromServiceArgs.add(IEventConsumer.class);
	}
	
	private static OpenRobotsOntology oro = null;
	
	/**
	 * This thread-safe queue holds incoming requests.
	 */
	private BlockingQueue<Request> incomingRequests;
	
	private static AlteriteModule AlteriteModule = null;
	
	public class OnShuttingDown extends Thread { 
		public void run() { 
			Logger.log("Application interrupted. Shutting down...\n", VerboseLevel.WARNING); 

			keepOn = false; 
			try {
				for (IConnector c : connectors)	c.finalizeConnector();
			} catch (OntologyConnectorException e) {
				e.printStackTrace();
			} 
			
			Logger.log("Bye bye.\n", VerboseLevel.IMPORTANT);
		} 
	} 
	
	public OroServer(String confFile) {
		
    	connectors = new HashSet<IConnector>();
    	modules = new HashSet<IModule>();
    	registredServices = new HashMap<String, IService>();
    	incomingRequests = new LinkedBlockingQueue<Request>();
    	
    	//Check if the application is connected to a console. We don't want to
    	//color outputs in a logfile for instance.
    	HAS_A_TTY = System.console() == null ? false : true;
   	
    	Logger.log("Loading configuration file " + confFile + "...", 
				VerboseLevel.IMPORTANT);

    	//Load the configuration file. If it can not be found, exits.
    	ServerParameters = getConfiguration(confFile);
    	
    	Logger.log("done.\n", false);    	
    	Logger.cr();
   	
    	
	}
	
	public void addNewServiceProviders(IServiceProvider provider)
	{
		if (provider != null) {
			
			Map<String, IService> services = getDeclaredServices(provider);
			if (services != null)
				registredServices.putAll(services);
			
			// refresh connectors
			for (IConnector c : connectors)	c.refreshServiceList(registredServices);
		}
	}
	
	public void pushRequest(Request r) {
		incomingRequests.add(r);
	}
	
	public void runServer() throws InterruptedException, OntologyServerException { 
    	
    	if (! (VERBOSITY == VerboseLevel.SILENT)) {
	    	if (HAS_A_TTY && BLINGBLING) System.out.print((char)27 + "[6m"); 
    		Logger.colorPrint(Colors.BLUE, 
	    						"+------------------------------------+\n" +
	    						"|                                    |\n" +			
	    						"|          OroServer " + 
	    										VERSION + "          |\n" +
	    						"|                                    |\n" +
	    						"|       ");
	    	System.out.print("(c)LAAS-CNRS 2008-2011");
	    	Logger.colorPrintLn(Colors.BLUE, "       |\n" +
								"+------------------------------------+");
	    	if (HAS_A_TTY && BLINGBLING) System.out.print((char)27 + "[25m");
    	}
    	
    	Runtime.getRuntime().addShutdownHook(new OnShuttingDown());
    	
    	
    	serverInitialization(ServerParameters);
		
/*******************************************************************************
*                    SOCKET CONNECTOR INITIALIZATION                           *
*******************************************************************************/
    	
    	// Currently, only one connector, the socket connector 
    	// (others bridges like YARP or JSON are now out of the oro-server code base)
    	SocketConnector sc = new SocketConnector(ServerParameters, registredServices, this);
		connectors.add(sc);

		for (IConnector c : connectors)	{
			try {
				c.initializeConnector();
			} catch (OntologyConnectorException e) {
				Logger.log("Couldn't initialize a connector: " + 
						e.getLocalizedMessage() + 
						". Ignoring it.\n", 
						VerboseLevel.SERIOUS_ERROR);
				connectors.remove(c);
			}
		}
		
		if (connectors.size() == 0) {
			Logger.log("None of the connectors could be started! Killing myself" +
					" now.\n", VerboseLevel.FATAL_ERROR);
			System.exit(1);
		}

/*******************************************************************************
*                                MAIN LOOP                                     *
*******************************************************************************/

		while(keepOn) {			
			
			// Wait 10ms for a request to leave some time to other processes
			Request r = incomingRequests.poll(10, TimeUnit.MILLISECONDS);
			
			/**
			 * Executes the request, and stores the result internally as r.result
			 * 
			 * The thread that pushed the request is probably waiting for r.result
			 * to contain something...
			 */
			if (r != null) r.execute();
			
			// Step the main models and all modules.
			oro.step();
			for (IModule m : modules) m.step();

    	}
		
		//Finalization occurs in the shutdown hook, above.
	}
	
	
	//this method is public only because of unittesting. If someone has a proposal...
   	public void serverInitialization(Properties serverParameters) throws OntologyServerException { 
/*******************************************************************************
 *                   BACKENDS and SERVICES REGISTRATION                        *
 ******************************************************************************/

		//add the services offered by the OroServer class
		addNewServiceProviders(this);
		
		//Open and load the ontology + register ontology services by the server. 
		oro = new OpenRobotsOntology(serverParameters);
		
		addNewServiceProviders(oro);

		/********************* SERVICES REGISTRATION **************************/
		
		// Base modules
		IServiceProvider baseModule = new BaseModule(oro);
		addNewServiceProviders(baseModule);
		
		IServiceProvider eventModule = new EventModule(oro);
		addNewServiceProviders(eventModule);
		
		IServiceProvider diffModule = new CategorizationModule(oro);
		addNewServiceProviders(diffModule);
		
		try {
			AlteriteModule = new AlteriteModule(oro);
			modules.add(AlteriteModule);
			addNewServiceProviders(AlteriteModule);
		} catch (EventRegistrationException e1) {
		} catch (InvalidModelException e1) {
		}

		
		// External plugins
		
		PluginLoader pl = new PluginLoader(oro, serverParameters);		
		
		String pluginsPath = serverParameters.getProperty("plugins_path");
		
		if (pluginsPath != null) {
			
			File f = new File(pluginsPath);
			
			Logger.log("Looking fo external plugins...\n");
			
			if (!f.exists())
			{
				Logger.log("Plugin path " + f + " doesn't exist! Continuing\n", VerboseLevel.ERROR);
			}
			else {
				FilenameFilter filter = new FilenameFilter() {
		            	public boolean accept(File f, String s) {return s.endsWith("jar") ? true : false; }
					};
	         
				if (f.listFiles(filter).length == 0)
				{
					Logger.log("No plugin found. Continuing.\n");
				} 
				else {
					for (File jarFile : f.listFiles(filter)) {
						Logger.log(jarFile.toString() + "\n", VerboseLevel.DEBUG);
						IModule module;
						try {
							module = pl.loadJAR("file://" + jarFile.getAbsolutePath());
							modules.add(module);
							addNewServiceProviders(module.getServiceProvider());
						}
						catch (PluginNotFoundException e) {
							Logger.log("Error while loading the plugin: " + e.getMessage() + ". Skipping it.\n", VerboseLevel.SERIOUS_ERROR);
						}
						catch (InvalidPluginException e) {
							Logger.log("Error while loading the plugin: " + e.getMessage() + ". Skipping it.\n", VerboseLevel.SERIOUS_ERROR);
						}
					}
				}
			}
				
		}
					
		// Check we have registered services and list them
		if (registredServices.size() == 0)
			throw new OntologyServerException("No service registred by the " +
					"ontology server! I've no reason to continue, so I'm " +
					"stopping now.");
		
		if (HAS_A_TTY) {
		Logger.log("Following services are registred:\n", VerboseLevel.EMPHASIZE);
		Logger.log(niceMethodsList(), false);
		}
		
		Logger.cr();
   	}
   	
	public static void main(String[] args) throws 
									OntologyConnectorException, 
									InterruptedException, 
									OntologyServerException {
		
		String confFile;
		
    	if (args.length < 1 || args.length > 1)
    		confFile = DEFAULT_CONF;
    	else
    		confFile = args[0];
    	
		new OroServer(confFile).runServer();
	}
	
	@RPCMethod(
			category = "administration",
			desc = "Reload the base ontologies, discarding all inserted of " +
					"removed statements, in every models" 
	)
	public void reset() throws OntologyServerException {
		
		Logger.log("RESETTING ORO-server!\n",VerboseLevel.IMPORTANT);
		
		for (IConnector c : connectors)
			c.clearServiceList();
		
		registredServices.clear();
		
		AlteriteModule.close();
		AlteriteModule = null;
		
		oro.close();
		oro = null;
		
		//Hint the system to garbage collect.
		System.gc();
		
		serverInitialization(ServerParameters);
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
			category = "administration",
			desc = "returns some statistics on the server"
	)
	public Map<String, String> stats() {
		Map<String, String> stats = new HashMap<String, String>();
		
		
		SimpleDateFormat formatNew = new SimpleDateFormat("HH:mm:ss");
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

		//Amount of pending requests in the server.
		stats.put("pending_requests", String.valueOf(incomingRequests.size()));
		
		return stats;
	}
	
	public long size() {
		return oro.size();
	}

	@RPCMethod(
			desc = "returns a human-friendly list of available methods with " +
					"their signatures and short descriptions."
	)
	public String help() {
		
		
		String help = (char)27 + "[34mHello!" + (char)27 + "[32m You are running" +
			" oro-server v." + VERSION + (char)27 + "[0m\n\nYou'll find below " +
			"the list of available remote services in the knowledge base:\n";
		
		help += niceMethodsList();
		
	    help += "\nTo execute a command, you must enter its name (case " +
	    		"insensitive), followed by one parameter per line. To finish, type " + 
	    		SocketConnector.MESSAGE_TERMINATOR + " and 'return'. " +
	    		"The server should answer.\n";
		return help;
	}
	
	private String niceMethodsList(){

		String result = "";
		
		SortedMap<String, SortedSet<IService>> services = getServicesByCategory();
		
		for (String cat : services.keySet())
	    {
			result += "\n\t" + (char)27 + "[35m[" + 
    		cat.toUpperCase() + "]"+
    		(char)27 + "[39m\n";
	    	
			for (IService s : services.get(cat)) {
							
				String params = formatParameters(s.getMethod());
		    	
		    	result += "\t- " + (char)27 + "[31m" + 
		        		s.getName() + params + 
		        		(char)27 + "[39m: " + 
		        		s.getDesc() + "\n";
			}
	    }
		
		return result;
	}
	
	@RPCMethod(
			category = "administration",
			desc = "returns the list of available methods with their signatures " +
					"and short descriptions as a map."
	)
	public Map<String, String> listMethods() {
		
		
		Map<String, String> help = new HashMap<String, String>();
		
		for (String m : registredServices.keySet())
	    {
	        help.put(m, registredServices.get(m).getDesc());
	    }
		
		return help;
	}
	
	@RPCMethod(
			category = "administration",
			desc = "returns a raw list of available methods."
	)
	public Set<String> listSimpleMethods() {
		
		Set<String> help = new HashSet<String>();
		
		for (String m : registredServices.keySet())
	    {
	        help.add(m);
	    }
		return help;
	}
	
	@RPCMethod(
			category = "administration",
			desc = "returns a list of available methods in HTML format for " +
					"inclusion in documentation."
	)
	public String makeHtmlDoc() {
		
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

	    SortedMap<String, SortedSet<IService>> services = getServicesByCategory();
	    
		String help = "<h2>List of available methods</h2>\n";
		help += "<i>(Last updated on " + sdf.format(cal.getTime()) + ")</i>\n";
		help += "<ul class=\"RpcMethodsList\">\n";
		
		for (String cat : services.keySet())
	    {
			help += "\t<li>" + cat + "\n\t<ul>\n"; 
    			    	
			for (IService s : services.get(cat)) {
		
		    	String params = formatParameters(s.getMethod());
		    		        
		    	//build the list of expected parameters 	
		        help += "\t<li>{@linkplain " + 
		        		s.getObj().getClass().getName() + "#" + 
		        		s.getMethod().getName() + params + " <em>" + 
		        		s.getName() + "</em><i>" + params + "</i>}: " + 
		        		s.getDesc() + "</li>\n";
		    }
			
			help += "</ul>\n</li>\n";
			
	    }
	    help += "</ul>\n";
		return help;
	}
	
	/**
	 * Return a map that associate categories of RPC services to the alphabetically
	 * sorted list of the services.
	 * 
	 * The categories themselves are alphabetically sorted.
	 * 
	 * @return
	 */
	private SortedMap<String, SortedSet<IService>> getServicesByCategory(){
	
		SortedMap<String, SortedSet<IService>> services = 
									new TreeMap<String, SortedSet<IService>>();
		
		for (String m : registredServices.keySet()) {
			
			String cat = registredServices.get(m).getCategory();
			
			if (!services.containsKey(cat)) {
				services.put(cat, new TreeSet<IService>());
			}
			
			services.get(cat).add(registredServices.get(m));
		}
		
		return services;
	}
	
	/**
	 * Formats in a human-readable way the parameters of a method.
	 * 
	 * This method skips all the parameters whose type belongs to
	 * {@link #discardedTypeFromServiceArgs}
	 * 
	 * @param m The method
	 * @return the in-parenthesis list of the method's parameters
	 */
	public static String formatParameters(Method m) {
		//build the list of expected parameters
    	String params = "(";
    	for (Class<?> param : m.getParameterTypes())
    		if (!discardedTypeFromServiceArgs.contains(param))
    			params += param.getSimpleName() + ", ";
    	
    	if (!params.equals("("))
    		params = params.substring(0, params.length() - 2);
    	
    	params += ")";
    	
    	return params;
	}
	
	public static int nbExposedParameters(Method m) {
		//build the list of expected parameters
    	int i = 0;
    	for (Class<?> param : m.getParameterTypes())
    		if (!discardedTypeFromServiceArgs.contains(param))
    			i++;
    	  	
    	return i;
	}
	
	private Map<String, IService> getDeclaredServices(Object o) {
		
		HashMap<String, IService> registredServices = new HashMap<String, IService>();
		
		for (Method m : o.getClass().getMethods()) {
			RPCMethod a = m.getAnnotation(RPCMethod.class);
			if (a != null) {				
				IService service = new ServiceImpl(
										m.getName(), 
										a.category(), 
										a.desc(), 
										m, 
										o);
				
				String name = m.getName()+formatParameters(m);
				registredServices.put(name, service);
			}
		}
		return registredServices;
	}
	
	/**
	 * Read a configuration file and return to corresponding "Properties" object.
	 * The configuration file contains the path to the ontology to be loaded and 
	 * several options regarding the server configuration.
	 * @param configFileURI The path and filename of the configuration file.
	 * @return A Java.util.Properties instance containing the application 
	 * configuration.
	 */
	public static Properties getConfiguration(String configFileURI){
		/****************************
		 *  Parsing of config file  *
		 ****************************/
		Properties parameters = new Properties();
        try
		{
        	FileInputStream fstream = new FileInputStream(configFileURI);
        	parameters.load(fstream);
			fstream.close();
			
			// Retrieve, if available, the level of verbosity.
			try {
				String level = parameters.getProperty("verbosity", "info").toUpperCase();
				if (level.equals("DEMO")) {
					DEMO_MODE = true;
					level = "SERIOUS_ERROR"; //in demo mode, only display serious errors
				}
				VERBOSITY = VerboseLevel.valueOf(level);
			} catch (IllegalArgumentException iae) {
				VERBOSITY = VerboseLevel.INFO;
				Logger.log("Invalid value for the verbosity level. Switch back " +
						"to default verbosity.", VerboseLevel.SERIOUS_ERROR);
			}
			

			Boolean log_with_timestamp = Boolean.parseBoolean(parameters.getProperty("display_timestamp", "true"));
			
			Logger.display_timestamp = log_with_timestamp;

			
			if (!parameters.containsKey("oro_common_sense"))
			{
				Logger.log("No common sense ontology specified in the " +
						"configuration file (\"" + configFileURI + "\"). Add " +
						"smthg like oro_common_sense=commonsense.owl\n", 
						VerboseLevel.FATAL_ERROR);
	        	System.exit(1);
			}
			
			// Retrieve, if available, the default language for label retrieval.
			DEFAULT_LANGUAGE = parameters.getProperty("language", "en");
			
			BLINGBLING = Boolean.parseBoolean(parameters.getProperty("blingbling", "false"));
			
		}
        catch (FileNotFoundException fnfe)
        {
        	Logger.log("No config file. Check \"" + configFileURI + "\" exists.\n", 
        			VerboseLevel.FATAL_ERROR);
        	System.exit(1);
        }
        catch (Exception e)
		{
        	Logger.log("Config file input error. Check config file syntax.\n", 
        			VerboseLevel.FATAL_ERROR);
			System.exit(1);
		}
        
        return parameters;
	}

}
