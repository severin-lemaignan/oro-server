package laas.openrobots.ontology.modules.events;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.tests.EventsTest;

/**
 * This abstract implementation of a {@link IWatcherProvider} can be commonly
 * subclassed with an overloaded constructor in charge of adding {@link IWatcher}s
 * to the protected {@code watchers} set.
 * 
 * @see EventsTest.NewInstanceEventTester An example of such use in the unit-tests.
 * @author slemaign
 *
 */
public abstract class GenericWatcherProvider implements IWatcherProvider {

	protected Set<IWatcher> watchers;
	
	public GenericWatcherProvider() {
		watchers = new HashSet<IWatcher>();
	}
	
	@Override
	public Set<IWatcher> getPendingWatchers() {			
		return watchers;
	}

	@Override
	public void removeWatcher(IWatcher watcher) {
		watchers.remove(watcher);
	}

}
