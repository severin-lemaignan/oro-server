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
	
	
	/** Constants that defines the way an event is triggered.<br/>
	 * 
	 * <ul>
	 *  <li>{@code ON_TRUE}: the event is triggered each time the corresponding watch expression <em>becomes</em> true.</li>
	 *  <li>{@code ON_TRUE_ONE_SHOT}: the event is triggered the first time the corresponding watch expression <em>becomes</em> true. The watcher is then deleted.</li>
	 *  <li>{@code ON_FALSE}: the event is triggered each time the corresponding watch expression <em>becomes</em> false.</li>
	 *  <li>{@code ON_FALSE_ONE_SHOT}: the event is triggered the first time the corresponding watch expression <em>becomes</em> false. The watcher is then deleted.</li>
	 *  <li>{@code ON_TOGGLE}: the event is triggered each time the corresponding watch expression <em>becomes</em> true or false.</li>
	 * </ul>
	 * 
	 */
	static public enum TriggeringType {ON_TRUE, ON_TRUE_ONE_SHOT, ON_FALSE, ON_FALSE_ONE_SHOT, ON_TOGGLE};

	/**
	 * Returns the list of currently registred "watchers". The caller of {@link #getPendingWatchers()} (a method invoked when the ontology changes, for instance) is expected to iterate on the list and execute each of the <code>ASK</code> queries returned by {@link IWatcher#getWatchQuery()}. If the result of one of this query is <code>true</code>, the corresponding {@link IWatcher#notifySubscriber()} method is invoked.
	 * @return A list of currently registred event watchers.
	 */
	public abstract Set<IWatcher> getPendingWatchers();
	
}
