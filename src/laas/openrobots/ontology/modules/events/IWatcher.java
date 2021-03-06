/*
 * Copyright (c) 2008-2010 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package laas.openrobots.ontology.modules.events;

import java.util.List;
import java.util.UUID;


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
 * watching occurred.
 * </p>
 * 
 * <p>
 * The way the trigger is actually fired depends on the triggering mode, as 
 * returned by {@link #getTriggeringType()}. Supported trigger mode are defined 
 * in {@link IWatcher.TriggeringType}.
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
	 * <p>
	 * 
	 * <h2>{@code FACT_CHECKING}</h2>
	 * 
	 * The watch pattern must be a {@linkplain laas.openrobots.ontology.PartialStatement partial statement}.
	 * If, when evaluated, it returns true (ie, at least one asserted or inferred 
	 * statement match the pattern), the event is fired.
	 * 
	 * <h2>{@code NEW_INSTANCE}</h2>
	 * 
	 * The event pattern for this type of event is first a variable and then a 
	 * set of {@linkplain laas.openrobots.ontology.PartialStatement partial statement}. 
	 * The event is triggered when a new statement matches this set.
	 * 
	 * The server return the list of instances bound to the variable.
	 *  
	 * <h3>Example</h3>
	 * 
	 * Registration:
	 * <pre>
	 * > registerEvent
	 * > NEW_INSTANCE
	 * > ON_TRUE
	 * > b
	 * > [?a desires ?b, ?a rdf:type Human]
	 * </pre>
	 * 
	 * Add some facts:
	 * <pre>
	 * > add
	 * > [ramses rdf:type Human, pyramidInauguration rdf:type StaticSituation, ramses desires pyramidInauguration]
	 * </pre>
	 * 
	 * This would fire the event and return:
	 * <pre>
	 * > event
	 * > [pyramidInauguration]
	 * </pre>
	 * 
	 * <h2>{@code NEW_CLASS_INSTANCE}</h2>
	 * 
	 * The event is triggered when a new instance of the class returned by the 
	 * watch pattern is added.
	 * 
	 * When the event is fired, the server send to the client the list of the new
	 * instances.
	 * 
	 * This kind of event is a special, optimized version of {@code NEW_INSTANCE}
	 * for class instances.
	 * 
	 *  
	 * @see laas.openrobots.ontology.modules.events General description of events in oro-server
	 */
	static public enum EventType {FACT_CHECKING, NEW_CLASS_INSTANCE, NEW_INSTANCE};
	
	/** Constants that defines the way an event is triggered.
	 * 
	 * <p>
	 * <ul>
	 *  <li>{@code ON_TRUE}: the event is triggered each time the corresponding 
	 *  watch expression <em>becomes</em> true.</li>
	 *  <li>{@code ON_TRUE_ONE_SHOT}: the event is triggered the first time the 
	 *  corresponding watch expression <em>becomes</em> true. The watcher is 
	 *  then deleted.</li>
	 *  <li>{@code ON_FALSE}: the event is triggered each time the corresponding
	 *   watch expression <em>becomes</em> false.</li>
	 *  <li>{@code ON_FALSE_ONE_SHOT}: the event is triggered the first time the
	 *   corresponding watch expression <em>becomes</em> false. The watcher is 
	 *   then deleted.</li>
	 *  <li>{@code ON_TOGGLE}: the event is triggered each time the corresponding
	 *   watch expression <em>becomes</em> true or false.</li>
	 * </ul>
	 * </p>
	 * 
	 * @see laas.openrobots.ontology.modules.events General description of 
	 * events in oro-server
	 */
	static public enum TriggeringType {
		ON_TRUE, 
		ON_TRUE_ONE_SHOT, 
		ON_FALSE, 
		ON_FALSE_ONE_SHOT, 
		ON_TOGGLE}

	public List<String> getWatchPattern();
	
	public EventType getPatternType();
	
	public IWatcher.TriggeringType getTriggeringType();
	
	public void notifySubscribers(OroEvent e);

	public void addSubscriber(IEventConsumer e);
	/**
	 * Returns a unique (at least for this instance of the server) identifier
	 * for the current event watcher.
	 * 
	 * @return A unique ID associated to this event watcher.
	 */
	public UUID getId();
	
	public boolean equals(IWatcher gw);

}
