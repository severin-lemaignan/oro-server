package laas.openrobots.ontology.modules.events;

import java.util.Set;

import laas.openrobots.ontology.modules.events.IWatcherProvider.TriggeringType;

/** Interface to patterns that may trigger events.
 * 
 * <p>
 * A class which implements IWatcher is expected to represent an "event trigger
 * pattern" for the ontology. A watcher has a <i>{@linkplain EventType event type}
 * </i> and a <i>watch pattern</i>. See {@link EventType} for details regarding
 * how the pattern should look like according to the event type.
 * </p>
 * 
 * <p>
 * When the event is triggered, its {@link #notifySubscriber(OroEvent)} method 
 * is called and expected to warn the event subscribers that the event they were
 * watching occured.
 * </p>
 * 
 * <p>
 * The way the trigger is actually fired depends on the triggering mode, as 
 * returned by {@link #getTriggeringType()}. Supported trigger mode are defined 
 * in {@link TriggeringType}.
 * </p>
 * 
 * @see laas.openrobots.ontology.modules.events General description of events in oro-server
 * @author slemaign
 *
 */
public interface IWatcher {

	/** Constants that defines the type of event the event module can handle.
	 * 
	 * <p>
	 * When a watcher is registered into an event source, the source can check
	 * what kind of event are expected by calling the {@link IWatcher#getPatternType()}
	 * method. The interpretation of the watch pattern (returned by 
	 * {@link IWatcher#getWatchPattern()}) depends of the type of event, as follow:
	 * 
	 * <ul>
	 *  <li>{@code FACT_CHECKING}: the watch pattern must be a 
	 *  {@linkplain laas.openrobots.ontology.PartialStatement partial statement}.
	 *  If, when evaluated, it returns true (ie, at least one asserted or 
	 *  inferred statement match the pattern), the event is fired.</li>
	 *  <li>{@code NEW_INSTANCE}: the event is triggered when a new instance of
	 *  the class returned by the watch pattern is added.</li>
	 * </ul>
	 * </p>
	 * 
	 * @see laas.openrobots.ontology.modules.events General description of events in oro-server
	 */
	static public enum EventType {FACT_CHECKING, NEW_INSTANCE};
	
	public Set<String> getWatchPattern();
	
	public EventType getPatternType();
	
	public TriggeringType getTriggeringType();
	
	public void notifySubscriber(OroEvent e);

}