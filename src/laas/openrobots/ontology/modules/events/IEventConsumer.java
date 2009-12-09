package laas.openrobots.ontology.modules.events;

import java.util.UUID;

/**
 * This interface is intended to be implemented by classes that are expected to
 * be notified when some event occurs, thus consuming the event.
 * @author slemaign
 *
 */
public interface IEventConsumer {

	void consumeEvent(UUID watcherID, OroEvent e);
}
