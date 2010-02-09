package laas.openrobots.ontology.modules;

import com.sun.xml.internal.fastinfoset.sax.Properties;

import laas.openrobots.ontology.backends.IOntologyBackend;
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
