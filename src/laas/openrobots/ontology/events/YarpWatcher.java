package laas.openrobots.ontology.events;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import yarp.Bottle;
import yarp.BufferedPortBottle;
import yarp.Network;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Statement;

public class YarpWatcher implements IWatcher {

	//private QueryExecution _watchExpression;
	
	private final static String _localTriggerPortName = "/oro/events_dispatcher";
	private String _watchExpression;
	static private BufferedPortBottle _localTriggerPort;
	private String _remoteTriggerPortName;
	
	//static initialization
	static {
		_localTriggerPort = new BufferedPortBottle();
		_localTriggerPort.open(_localTriggerPortName);
	}
	
	public YarpWatcher(String expressionToWatch, String portToTrigger) //, IOntologyBackend onto) throws IllegalStatementException
	{		
		_watchExpression = expressionToWatch;
		
		_remoteTriggerPortName = portToTrigger;
		
		System.out.println(" * New event registred. Trigger pattern is " + expressionToWatch);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.events.IWatcher#getWatchQuery()
	 */
	//public QueryExecution getWatchQuery() {
	public String getWatchQuery() {
		return _watchExpression;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.events.IWatcher#notifySubscriber()
	 */
	public void notifySubscriber() {
		
		Network.connect(_localTriggerPortName, _remoteTriggerPortName);
		
		Bottle notification = _localTriggerPort.prepare();
		notification.clear();
		
		//Pour l'instant, on ne renvoie rien d'intéressant dans la bouteille lors de la notification.
		notification.addString("trigger");
		
		_localTriggerPort.write();
		
		Network.disconnect(_localTriggerPortName, _remoteTriggerPortName);

	}
	
}
