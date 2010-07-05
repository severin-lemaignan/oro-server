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
import java.util.Set;

import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Namespaces;

import com.hp.hpl.jena.rdf.model.Resource;

public class OroEventNewInstances extends OroEventImpl {

	private Set<String> matchedId;
		
	public OroEventNewInstances(Set<Resource> matchedId) {
		super();
		
		this.matchedId = new HashSet<String>();
		
		for (Resource r : matchedId)
			this.matchedId.add(Namespaces.toLightString(r));

	}
	
	@Override
	public String getEventContext() {
		
		return Helpers.stringify(matchedId);
	}

	public String getMatchingId() {
		if (matchedId.size() != 1) return null;
		
		String res = "";
		
		for (String s : matchedId)
			res = s;
		
		return res;
	}
	
	public Set<String> getMatchingIds() {
		
		return matchedId;
	}

}
