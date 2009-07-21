/**
 * 
 */
package laas.openrobots.ontology.connectors;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import laas.openrobots.ontology.Pair;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

/**
 * @author slemaign
 *
 */
public interface IConnector {
	
	public abstract void initializeConnector() throws OntologyConnectorException;
	
	public abstract void finalizeConnector() throws OntologyConnectorException;
	
	/**
	 * When called, should wait for one request, answer it and return.
	 * @throws MalformedYarpMessageException 
	 */
	public abstract void run() throws OntologyConnectorException, MalformedYarpMessageException;

	public abstract void refreshServiceList(Map<Pair<String, String>, Pair<Method, Object>> registredServices);

}
