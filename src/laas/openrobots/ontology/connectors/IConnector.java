/**
 * 
 */
package laas.openrobots.ontology.connectors;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.MalformedYarpMessageException;
import laas.openrobots.ontology.exceptions.OntologyConnectorException;

/**
 * @author slemaign
 *
 */
public interface IConnector {
	
	public abstract IOntologyBackend getBackend();
	
	public abstract void initializeConnector() throws OntologyConnectorException;
	
	public abstract void finalizeConnector() throws OntologyConnectorException;
	
	/**
	 * When called, should wait for one request, answer it and return.
	 * @throws MalformedYarpMessageException 
	 */
	public abstract void run() throws OntologyConnectorException, MalformedYarpMessageException;

}
