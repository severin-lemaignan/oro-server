package laas.openrobots.ontology;

import java.lang.annotation.*;

import laas.openrobots.ontology.connectors.JsonSerializable;
import laas.openrobots.ontology.connectors.YarpSerializable;

/**
 * This annotation marks all the available methods exposed to remote clients.<br/>
 * To actually register your services by the server, you just need to annotate the relevant method with a @RPCMethod annotation and to call the {@link OroServer#addNewServiceProviders(IServiceProvider)} method. This can be done at any time, even during execution. Connectors will be automatically updated with the new services.<br/>
 * <br/>
 * A RPC method can take 0..n parameters (stuck to primitive type to ensure maximum compatibility with the various connectors.<br/>
 * The serialization of the return value is under the responsability of the connectors. We recommend to only use primitive types or collections (maps, sets, lists) of primitive type. You can also provide explicit serialization (see {@link JsonSerializable} or {@link YarpSerializable} for examples).
 * 
 * @author slemaign
 * @see laas.openrobots.ontology.backends.OpenRobotsOntology OpenRobotsOntology class for numerous example of RPC methods.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface RPCMethod {
	/**
	 * The name of the service. If this field is not specified, the name of the method on which the annotation is put will be use instead.
	 */
	public String rpc_name() default "";
	/**
	 * Contains a short description of the purpose of the service. 
	 */
	public String desc() default "";
}
