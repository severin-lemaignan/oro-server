package laas.openrobots.ontology.events;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.connectors.YarpConnector;
import laas.openrobots.ontology.events.IEventsProvider.TriggeringType;
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
	private TriggeringType _triggeringType;
	private boolean _isTriggered;
	
	//static initialization
	static {
		_localTriggerPort = new BufferedPortBottle();
		_localTriggerPort.open(_localTriggerPortName);
	}
	
	/** Creates a new event watcher with triggering type defaulted to {@code ON_TRUE_ONE_SHOT}
	 * 
	 * @param expressionToWatch a pattern (partial statement) defining an expression to watch. See {@link YarpConnector#subscribe(Bottle)}.
	 * @param portToTrigger a YARP port to notify on incoming events.  See {@link YarpConnector#subscribe(Bottle)}.
	 * @see IEventsProvider.TriggeringType
	 */
	public YarpWatcher(String expressionToWatch, String portToTrigger){
		this(expressionToWatch, IEventsProvider.TriggeringType.ON_TRUE_ONE_SHOT, portToTrigger);
	}
	
	/** Creates a new event watcher.
	 * 
	 * @param expressionToWatch a pattern (partial statement) defining an expression to watch. See {@link YarpConnector#subscribe(Bottle)}.
	 * @param portToTrigger a YARP port to notify on incoming events.  See {@link YarpConnector#subscribe(Bottle)}.
	 * @param triggerType the way you want the event to be triggered.
	 * @see IEventsProvider.TriggeringType
	 */
	public YarpWatcher(String expressionToWatch, TriggeringType triggerType, String portToTrigger){		
		_watchExpression = expressionToWatch;
		
		_triggeringType = triggerType;
		
		_remoteTriggerPortName = portToTrigger;
		
		System.out.println(" * New event registred. Trigger pattern is " + expressionToWatch);
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.events.IWatcher#getWatchQuery()
	 */
	//public QueryExecution getWatchQuery() {
	public String getWatchPattern() {
		return _watchExpression;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.events.IWatcher#notifySubscriber()
	 */
	public void notifySubscriber() {
		
		//System.out.println("Event occured! I'm getting notified!");
			
		Network.connect(_localTriggerPortName, _remoteTriggerPortName);
		
		Bottle notification = _localTriggerPort.prepare();
		notification.clear();
		
		//Pour l'instant, on ne renvoie rien d'intéressant dans la bouteille lors de la notification.
		notification.addString("trigger");
		
		_localTriggerPort.write();
		
		Network.disconnect(_localTriggerPortName, _remoteTriggerPortName);

	}

	@Override
	public TriggeringType getTriggeringType() {
		return _triggeringType;
	}
	
}
