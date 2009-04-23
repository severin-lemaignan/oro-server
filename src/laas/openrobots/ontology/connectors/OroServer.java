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

import laas.openrobots.ontology.OpenRobotsOntology;
import yarp.Bottle;
import yarp.BufferedPortBottle;
import yarp.Network;
import yarp.Value;

/**
 * {@code OroServer} is the door of the ontology for network client.</br>
 * It relies on other classes to handle specific network protocols. Currently, only <a href="http://eris.liralab.it/yarp/">YARP</a> is implemented, through {@link YarpConnector}. Others (ROS...) may follow.<br/>
 * <br/>
 * <ul>
 * <li>The main definition of the server interface is here {@link laas.openrobots.ontology.IOntologyServer}.</li>
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
 * <i>See {@link laas.openrobots.ontology.OpenRobotsOntology#OpenRobotsOntology(String)} for others options, specific to the ontologies. Have a look at the config file itself for more details.</i>
 *
 */
public class OroServer {


	public static final String DEFAULT_CONF = "oro.conf";
	static YarpConnector oro2yarp;
	
    public static void main(String[] args) {
    	
    	
    	String confFile;
    	String yarpPort;
    	String lastReceiverPort = "";
    	
    	if (args.length < 1 || args.length > 1)
    		confFile = DEFAULT_CONF;
    	else
    		confFile = args[0];
    	
    	//System.err.close(); //remove YARP message, but remove Oro error messages as well!!
    	
		System.out.println("*** OroServer ***");
		System.out.println(" * Using configuration file " + confFile);
		
		//Open and load the ontology. If the configuration file can not be found, it exits.
		OpenRobotsOntology oro = new OpenRobotsOntology(confFile);
		
		//if (oro.getParameters().getProperty("yarp", "") != "enabled") System.out.println("YARP bindings should not be enabled but...well...I like them, so I start them anyway. Sorry.");
		
		Method[] ontologyMethods = YarpConnector.class.getDeclaredMethods();
		ArrayList<Value> queryArgs = new ArrayList<Value>();
		
    	System.loadLibrary("jyarp");
    	Network.init();
    	
    	yarpPort = "/" + oro.getParameters().getProperty("yarp_input_port", "oro"); //defaulted to "oro" if no "yarp_input_port" provided.
    	
    	System.out.println(" * Starting YARP server on port " + yarpPort);
    	
    	BufferedPortBottle queryPort = new BufferedPortBottle();
    	queryPort.open(yarpPort + "/in");
    	
    	
    	BufferedPortBottle resultPort = new BufferedPortBottle();
    	resultPort.open(yarpPort + "/out");
    	
    	Bottle query = new Bottle();
    	
		
		oro2yarp = new YarpConnector(oro);
		

		while(true) {
			
			//System.out.println("Waiting for a new request...");
    	    
    	    query = queryPort.read();
    	    //System.out.println("Incoming bottle " + query.toString());
    	    
    	    
    	       	    
    	    if (query!=null) {
    	    	Bottle result = resultPort.prepare();
    	    	result.clear();
    	    	
    	    	String receiverPort = query.get(0).toString();
    	    	if (!receiverPort.startsWith("/"))
    	    		receiverPort = "/" + receiverPort;
    	    	
        	    if (!lastReceiverPort.equals(receiverPort)){ //not the same receiver ! disconnect the old one an connect to the new one.
        	    	System.out.println(" * Changing client to " + receiverPort);
        	    	Network.disconnect(yarpPort + "/out", lastReceiverPort);
        	    	Network.connect(yarpPort + "/out", receiverPort);
        	    }
        	    
    	    	String queryName = query.get(1).toString();
    	    	
    	    	Bottle yarpArgs = query.pop().asList();
    	    	
    	    	boolean methodFound = false;
    	    	
        	    for (Method m : ontologyMethods){
        	    	//if (m.isAccessible() && m.getName().equalsIgnoreCase(queryName))
        	    	if (m.getName().equalsIgnoreCase(queryName))
        	    	{
        	    		methodFound = true;
        	    		
        	    		try {
        	    			
        	    			result.addString("ok");
        	    			result.append((Bottle)m.invoke(oro2yarp, yarpArgs));        	    			
        	    			
						} catch (IllegalArgumentException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.fromString("error \"" + e.getClass().getName() + ": " + e.getLocalizedMessage() + "\"");
							
						} catch (IllegalAccessException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getClass().getName() + " -> " + e.getLocalizedMessage());
							result.clear();
							result.fromString("error \"" + e.getClass().getName() + ": " + e.getLocalizedMessage() + "\"");
							
						} catch (InvocationTargetException e) {
							System.err.println("ERROR while executing the request \"" + queryName + "\": " + e.getCause().getClass().getName() + " -> " + e.getCause().getLocalizedMessage());
							result.clear();
							result.fromString("error \"" + e.getCause().getClass().getName() + ": " + e.getCause().getLocalizedMessage() + "\"");							
						}
        	    	}
        	    }
        	    
        	    if (!methodFound){
					System.err.println("ERROR while executing the request: method \""+queryName + "\" not implemented by the ontology server.");
					result.clear();
					result.fromString("error \"method " + queryName + " not implemented by the ontology server.\"");							
        	    }
        	    
        	    //System.out.println("sending bottle: " + result);
        	    
        	    //...Send the answer...
        	    resultPort.write();
        	    
        	    lastReceiverPort = receiverPort;
        	    	
    	    }

    	    //Time.delay(0.25);
    	}
    	//Network.fini(); // never reached

	}

}
