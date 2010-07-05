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

package laas.openrobots.ontology.modules;

import laas.openrobots.ontology.service.IServiceProvider;

/**
 * Modules must implement this interface to be loaded at runtime.
 * 
 * At instanciation time, ORO-server will look for a constructor that takes a
 * {@linkplain IOntologyBackend} as first parameter and a {@linkplain Properties}
 * object as second parameter. The module will be instanciated with the current
 * ontology backend and the server parameters coming from the configuration file.
 * 
 * If this constructor doesn't exist, ORO-server will look in this order for:
 * <ul>
 * <li>Constructor(IOntologyBackend)</li>
 * <li>Constructor(Properties)</li>
 * <li>Constructor()</li>
 * </ul>
 * 
 * The name and version of the module must be specified in the module manifest.
 * TODO:Complete the doc!
 * 
 * @author Séverin Lemaignan <severin.lemaignan@laas.fr>
 *
 */
public interface IModule {
	
	/**
	 * A null return is expected if the module doesn't provide any RPC service 
	 * to register.
	 * 
	 * @return
	 */
	public IServiceProvider getServiceProvider();
}
