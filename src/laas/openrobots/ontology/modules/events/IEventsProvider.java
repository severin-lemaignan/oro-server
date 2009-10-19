package laas.openrobots.ontology.modules.events;

import java.util.Set;

/** oro-server introduces the concept of event. A client may register a pattern which is stored by the ontology server and evaluated every time the ontology changes. If the pattern match at least one statement, the event is triggered.<br/>
 * A class that implement IEventsProvider is expected to provide a set of {@link IWatcher}. Commonly, IEventsProvider are connectors (like {@link laas.openrobots.ontology.connectors.YarpConnector}) that offer RPC methods to subscribe (ie register) to some event (for instance {@link laas.openrobots.ontology.connectors.YarpConnector#subscribe(Bottle)}).<br/>
 * Several mode of triggering are supported, cf {@link TriggeringType}.<br/>
 * @see IWatcher Details on the watcher concept.
 * 
 * @author slemaign
 */
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
	 * Returns the list of currently registred "watchers". The caller of {@link #getPendingWatchers()} (a method invoked when the ontology changes, for instance) is expected to iterate on the list and execute each of the <code>ASK</code> queries returned by {@link IWatcher#getWatchPattern()}. If the result of one of this query is <code>true</code>, the corresponding {@link IWatcher#notifySubscriber()} method is invoked.
	 * @return A list of currently registred event watchers.
	 */
	public abstract Set<IWatcher> getPendingWatchers();
	
	/** Ask the underlying EventProvider to remove a watcher for its list of pending watchers (most probably because it has expired).
	 * @param watcher the watcher to remove.
	 */
	public abstract void removeWatcher(IWatcher watcher);
	
}
