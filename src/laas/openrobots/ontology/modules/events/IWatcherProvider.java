package laas.openrobots.ontology.modules.events;

import java.util.Set;

/** Interface to be implemented by classes that provides events to register.
 * 
 * @see laas.openrobots.ontology.modules.events General description of events in oro-server
 * @author slemaign
 */
public interface IWatcherProvider {
	
	
	/** Constants that defines the way an event is triggered.
	 * 
	 * <p>
	 * <ul>
	 *  <li>{@code ON_TRUE}: the event is triggered each time the corresponding watch expression <em>becomes</em> true.</li>
	 *  <li>{@code ON_TRUE_ONE_SHOT}: the event is triggered the first time the corresponding watch expression <em>becomes</em> true. The watcher is then deleted.</li>
	 *  <li>{@code ON_FALSE}: the event is triggered each time the corresponding watch expression <em>becomes</em> false.</li>
	 *  <li>{@code ON_FALSE_ONE_SHOT}: the event is triggered the first time the corresponding watch expression <em>becomes</em> false. The watcher is then deleted.</li>
	 *  <li>{@code ON_TOGGLE}: the event is triggered each time the corresponding watch expression <em>becomes</em> true or false.</li>
	 * </ul>
	 * </p>
	 * 
	 * @see laas.openrobots.ontology.modules.events General description of events in oro-server
	 */
	static public enum TriggeringType {ON_TRUE, ON_TRUE_ONE_SHOT, ON_FALSE, ON_FALSE_ONE_SHOT, ON_TOGGLE};

	/**
	 * Returns the list of currently registred "watchers". The caller of {@link #getPendingWatchers()} (a method invoked when the ontology changes, for instance) is expected to iterate on the list and execute each of the <code>ASK</code> queries returned by {@link IWatcher#getWatchPattern()}. If the result of one of this query is <code>true</code>, the corresponding {@link IWatcher#notifySubscriber()} method is invoked.
	 * @return A list of currently registred event watchers.
	 */
	public abstract Set<IWatcher> getPendingWatchers();
	
	/** Ask the underlying EventProvider to remove a watcher for its list of pending watchers (most probably because it has expired).
	 * @param watcher the watcher to remove.
	 */
	public abstract void removeWatcher(IWatcher watcher);
	
}
