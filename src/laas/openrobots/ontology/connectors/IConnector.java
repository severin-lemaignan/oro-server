/**
 * 
 */
package laas.openrobots.ontology.connectors;

import java.lang.reflect.Method;
import java.util.Map;

import laas.openrobots.ontology.exceptions.OntologyConnectorException;
import laas.openrobots.ontology.helpers.Pair;

/**
 * @author slemaign
 *
 */
public interface IConnector extends Runnable {
	
	public abstract void initializeConnector() throws OntologyConnectorException;
	
	public abstract void finalizeConnector() throws OntologyConnectorException;
	
	/**
	 * When called, should wait for one request, answer it and return.
	 * @throws MalformedYarpMessageException 
	 */
	public abstract void run();

	public abstract void refreshServiceList(Map<Pair<String, String>, Pair<Method, Object>> registredServices);

}
