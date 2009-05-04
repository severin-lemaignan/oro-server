/**
 * 
 */
package laas.openrobots.ontology.events;

import java.util.Set;

/**
 * @author slemaign
 *
 */
public interface IEventsProvider {

	/**
	 * Returns the list of currently registred "watchers". The caller of {@link #getPendingWatchers()} (a method invoked when the ontology changes, for instance) is expected to iterate on the list and execute each of the <code>ASK</code> queries returned by {@link IWatcher#getWatchQuery()}. If the result of one of this query is <code>true</code>, the corresponding {@link IWatcher#notifySubscriber()} method is invoked.
	 * @return A list of currently registred event watchers.
	 */
	public abstract Set<IWatcher> getPendingWatchers();
	
}
