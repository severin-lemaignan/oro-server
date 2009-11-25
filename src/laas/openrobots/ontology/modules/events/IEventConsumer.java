package laas.openrobots.ontology.modules.events;

/**
 * This interface is intended to be implemented by classes that are expected to
 * be notified when some event occurs, thus consumming the event.
 * @author slemaign
 *
 */
public interface IEventConsumer {

	void consumeEvent(OroEvent e);
}
