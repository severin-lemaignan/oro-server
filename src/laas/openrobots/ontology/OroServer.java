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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import laas.openrobots.ontology.backends.OpenRobotsOntology;
import laas.openrobots.ontology.connectors.IConnector;
import laas.openrobots.ontology.connectors.YarpConnector;
import laas.openrobots.ontology.events.IEventsProvider;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

/**
 * {@code OroServer} is the door of the ontology for network client.</br>
 * It relies on other classes to handle specific network protocols. Currently, only <a href="http://eris.liralab.it/yarp/">YARP</a> is implemented, through {@link YarpConnector}. Others (ROS...) may follow.<br/>
 * <br/>
 * <ul>
 * <li>The main definition of the server interface is here {@link laas.openrobots.ontology.backends.IOntologyBackend}.</li>
 * <li>If you need details on the YARP API, {@linkplain YarpConnector jump here}.</li>
 * </ul>
 * <br/>
 * {@code OroServer} has a {@code main} function which expect a configuration file.<br/> 
 * For the server, the following options are currently available:
 * <ul>
 * <li><em>yarp = [enabled|disabled]</em>: <em>enabled</em> activates YARP bindings.</li>
 * </ul>
 * YARP specific options:
 * <ul>
 * <li><em>yarp_input_port = PORT (default:oro)</em>: set the name of the YARP port where queries can be sent to the ontology server.</li> 
 * </ul>
 * <i>See {@link laas.openrobots.ontology.backends.OpenRobotsOntology#OpenRobotsOntology(String)} for others options, specific to the ontologies. Have a look at the config file itself for more details.</i>
 *
 */
/**
 * @author slemaign
 *
 */
public class OroServer {


	public static final String DEFAULT_CONF = "oro.conf";
	public static final String VERSION = "0.4.2"; //version: major.minor.build (minor -> add/removal of feature, build -> bug correction)
	
	public static final Date SERVER_START_TIME = new Date();

	private volatile boolean keepOn = true;
	private volatile HashSet<IConnector> connectors;
	private volatile HashSet<IEventsProvider> eventsProviders;
	
	private static OpenRobotsOntology oro = null;
	
	public class OnShuttingDown extends Thread { 
		public void run() { 
			System.out.println(" * Control-C caught. Shutting down..."); 

			keepOn = false; 
			try {
				for (IConnector c : connectors)	c.finalizeConnector();
				//Thread.sleep(2000); 
			} catch (OntologyConnectorException e) {
				e.printStackTrace();
			} 
			
			System.out.println(" * Bye bye.");
		} 
	} 
	
	public void runServer(String[] args) throws InterruptedException, OntologyConnectorException, MalformedYarpMessageException { 
   	
    	
    	String confFile;
    	
    	connectors = new HashSet<IConnector>();
    	eventsProviders = new HashSet<IEventsProvider>();
    	
    	
    	Runtime.getRuntime().addShutdownHook(new OnShuttingDown());
    	
    	
    	if (args.length < 1 || args.length > 1)
    		confFile = DEFAULT_CONF;
    	else
    		confFile = args[0];
    	
    	//System.err.close(); //remove YARP message, but remove Oro error messages as well!!
    	
		System.out.println("*** OroServer " + VERSION + " ***");
		System.out.println(" * Using configuration file " + confFile);
		
		//Open and load the ontology. If the configuration file can not be found, it exits.
		oro = new OpenRobotsOntology(confFile);
		
		//if (oro.getParameters().getProperty("yarp", "") != "enabled") System.out.println("YARP bindings should not be enabled but...well...I like them, so I start them anyway. Sorry.");
		YarpConnector yc = new YarpConnector(oro, oro.getParameters());
		
		eventsProviders.add(yc);
		
		oro.registerEventsHandlers(eventsProviders);
    			
		connectors.add(yc);
		
		for (IConnector c : connectors)	c.initializeConnector();
		

		while(keepOn) {			
        	    	
			//!!! TODO !!! Ouhouh! use threads before adding new backends!
			for (IConnector c : connectors) c.run();

			
			Thread.sleep(10);
    	}
		
		//Finalization occurs in the shutdown hook, above.
	}
	
	
	public static void main(String[] args) throws OntologyConnectorException, InterruptedException, MalformedYarpMessageException {
		new OroServer().runServer(args);
	}
	
	
	/**
	 * Returns several statistics on the server.
	 * 
	 * Returned values:
	 * <ul>
	 *  <li>the hostname where the server runs</li>
	 *  <li>server uptime</li>
	 *  <li>the current amount of classes in the ontology</li>
	 *  <li>the current amount of instances in the ontology</li>
	 *  <li>the current amount of client connected to the server</li>
	 * </ul>
	 * 
	 * @return a map containing the statistics (pairs name/value)
	 */
	public static Map<String, String> getStats() {
		Map<String, String> stats = new HashMap<String, String>();
		
		SimpleDateFormat formatNew = new SimpleDateFormat("HHHH 'hour(s)' mm 'minute(s)' ss 'second(s)'");
		formatNew.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		
		try {
			stats.put("hostname", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			stats.put("hostname", "unknow");
		}
		
		stats.put("uptime", formatNew.format((new Date()).getTime() - OroServer.SERVER_START_TIME.getTime()));
		
		//stats.put("nb_classes", String.valueOf(oro.getModel().listClasses().toSet().size()));
		stats.put("nb_classes", "not available");
		//stats.put("nb_instances", String.valueOf(oro.getModel().listIndividuals().toSet().size()));
		stats.put("nb_instances", "not available");
		stats.put("nb_clients", "not available");
		
		return stats;
	}

}
