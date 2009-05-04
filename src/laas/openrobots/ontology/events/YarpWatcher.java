package laas.openrobots.ontology.events;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import yarp.Bottle;
import yarp.BufferedPortBottle;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Statement;

public class YarpWatcher implements IWatcher {

	//private QueryExecution _watchExpression;
	private String _watchExpression;
	private BufferedPortBottle _triggerPort;
	private String _triggerPortName;
	
	public YarpWatcher(String expressionToWatch, String portToTrigger) //, IOntologyBackend onto) throws IllegalStatementException
	{		
//		Statement statement;
//		
//		try {
//			statement = onto.createStatement(expressionToWatch);
//		} catch (IllegalStatementException e) {
//			System.err.println("[ERROR] Error while parsing the expression to watch for the event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.");
//			throw e;
//		}
//			
//		String resultQuery = "ASK { <" + statement.getSubject().getURI() +"> <" + statement.getPredicate().getURI() + "> <" + statement.getObject().toString() + "> }";
//		
//		try	{
//			Query query = QueryFactory.create(resultQuery, Syntax.syntaxSPARQL);
//			_watchExpression = QueryExecutionFactory.create(query, onto.getModel());
//		}
//		catch (QueryParseException e) {
//			System.err.println("[ERROR] internal error during query parsing while trying to add an event hook! ("+ e.getLocalizedMessage() +").\nCheck the syntax of your statement.");
//			throw e;
//		}
		_watchExpression = expressionToWatch;
		
		_triggerPort = new BufferedPortBottle();
		_triggerPortName = portToTrigger;
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
		_triggerPort.open(_triggerPortName);
		
		Bottle notification = _triggerPort.prepare();
		notification.clear();
		
		//Pour l'instant, on ne renvoie rien d'intéressant dans la bouteille lors de la notification.
		notification.addInt(0);
		
		_triggerPort.write();
		
		_triggerPort.close();

	}
	
}
