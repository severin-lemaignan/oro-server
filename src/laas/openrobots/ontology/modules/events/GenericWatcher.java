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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GenericWatcher implements IWatcher {

	protected EventType eventType;
	protected List<String> eventPattern;
	protected IWatcher.TriggeringType triggeringType;
	
	protected Set<IEventConsumer> clients;
	
	protected UUID watcherId; 
	
	public GenericWatcher(EventType eventType, 
						IWatcher.TriggeringType triggeringType,
						List<String> eventPattern,  
						IEventConsumer client) {
		super();
		this.eventType = eventType;
		this.eventPattern = eventPattern;
		this.triggeringType = triggeringType;
		
		this.clients = new HashSet<IEventConsumer>();
		this.clients.add(client);
		
		this.watcherId = UUID.randomUUID();
	}

	@Override
	public EventType getPatternType() {
		return eventType;
	}

	@Override
	public List<String> getWatchPattern() {
		return eventPattern;
	}
	
	@Override
	public IWatcher.TriggeringType getTriggeringType() {
		return triggeringType;
	}
	
	@Override
	public UUID getId() {
		return watcherId;
	}

	@Override
	public void notifySubscribers(OroEvent e) {
		for (IEventConsumer client : clients) {
			client.consumeEvent(watcherId, e);
		}

	}
	
	public boolean equals(IWatcher gw){
		boolean test = 	gw.getPatternType() == this.eventType &&
				gw.getTriggeringType() == this.triggeringType &&
				gw.getWatchPattern().equals(this.eventPattern);
		return test;
	}

	@Override
	public void addSubscriber(IEventConsumer e) {
		clients.add(e);
		
	}

}
