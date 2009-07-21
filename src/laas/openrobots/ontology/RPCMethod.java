package laas.openrobots.ontology;

import java.lang.annotation.*;

/**
 * This annotation marks all the available methods exposed to remote clients.
 * To actually register your services by the server, you just need to annotate the relevant method with a @RPCMethod annotation and to call the {@link OroServer.addNewServiceProviders} method. This can be done at any time, even during execution. Connectors will be automaticcaly updated with the new services.
 * 
 *  The type of parameters for your service should be either no parameter of a vector of strings, and your method should always return a single string.
 * 
 * @author slemaign
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
