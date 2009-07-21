/**
 * 
 */
package laas.openrobots.ontology;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * This interface sole purpose is to signal that a class implement some service for the oro-server (ie, methods annotated with a RPCMethod annotation);
 * @author slemaign
 *
 */
public interface IServiceProvider {
	//public abstract Map<Pair<String,String>, Pair<Method, Object>> getDeclaredServices(Object o);

}
