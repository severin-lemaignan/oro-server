package laas.openrobots.ontology.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import laas.openrobots.ontology.OroServer;

/**
 * This annotation marks all the available methods exposed to remote clients.<br/>
 * To actually register your services by the server, you just need to annotate 
 * the relevant method with a @RPCMethod annotation and to call the 
 * {@link OroServer#addNewServiceProviders(IServiceProvider)} method. This can 
 * be done at any time, even during execution. Connectors will be automatically 
 * updated with the new services.<br/>
 * <br/>
 * 
 * A RPC method can take 0..n parameters, limited to primitive type or collection
 * of primitive type to ensure maximum compatibility with the various connectors.<br/>
 * One exception exist: if one of the parameter expect a {@link laas.openrobots.ontology.modules.events.IEventConsumer},
 * the connector is expected to provide it as a mean for the 
 * event manager to notify the clients (only if events are implemented by this 
 * connector).
 * <br/>
 * 
 * The serialization of the return value is under the responsability of the 
 * connectors. We recommend to only use primitive types or collections (maps, 
 * sets, lists) of primitive type. For unknown type, expect their 
 * {@link Object#toString()} method to be called for serialization.
 * 
 * @author slemaign
 * @see laas.openrobots.ontology.modules.base.BaseModule BaseModule class for numerous example of RPC methods.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface RPCMethod {
	/**
	 * The category of the service. Used to sort the RPC methods by groups.
	 * Optional.
	 */
	public String category() default "base";
	/**
	 * Contains a short description of the purpose of the service. 
	 */
	public String desc() default "";
}
