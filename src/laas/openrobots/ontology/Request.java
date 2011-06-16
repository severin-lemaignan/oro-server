package laas.openrobots.ontology;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;

public class Request {

	private Method m;
	private Object o;
	private Object[] args;
	public BlockingQueue<String> result;
	private boolean invokationDone;
	
	public Request(Method m, Object o, Object[] args) {
		super();
		this.m = m;
		this.o = o;
		this.args = args;
		
		result = new LinkedBlockingQueue<String>();
		invokationDone = false;
	}
	
	public void execute() {
		
		String res = "ok\n";
		/** Now, do the invocation **/
		
		assert (m != null && o != null && args != null);
		
		try {
			if (m.getReturnType() == void.class)
			{
				if (m.getParameterTypes().length == 0) m.invoke(o); else m.invoke(o, args);
				invokationDone = true;
			}
			if (	m.getReturnType() == Double.class || 
					m.getReturnType() == double.class ||
					m.getReturnType() == Integer.class ||
					m.getReturnType() == int.class ||
					m.getReturnType() == Boolean.class ||
					m.getReturnType() == boolean.class ||
					m.getReturnType() == Float.class ||
					m.getReturnType() == float.class) 
			{
				res += (m.getParameterTypes().length == 0) ? m.invoke(o).toString() : m.invoke(o, args).toString();
				invokationDone = true;
			} 
			else if (m.getReturnType() == String.class) {
				//To be JSON-compliant, we need to double quote the strings
				res += Helpers.stringify((m.getParameterTypes().length == 0) ? m.invoke(o).toString() : m.invoke(o, args).toString());
				invokationDone = true;
			}
			else {
				
				List<Class<?>> rTypes = new ArrayList<Class<?>>();
				
				rTypes.add(m.getReturnType());
				
				rTypes.addAll(Arrays.asList(m.getReturnType().getInterfaces()));
				

    			for (Class<?> rType : rTypes ) {
    				if (rType == Serializable.class) {
    					res += (m.getParameterTypes().length == 0) ? m.invoke(o).toString() : m.invoke(o, args).toString();
    					invokationDone = true;
    					break;
    				}
    				//TODO : Lot of cleaning to do here
    				if (rType == Map.class) {
    					res += Helpers.stringify(((Map<?, ?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
    					invokationDone = true;
    					break;
    				}
    				
    				if (rType == Set.class) {
    					res += Helpers.stringify(((Set<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
    					invokationDone = true;
    					break;
    				}
    				
    				if (rType == List.class) {
    					res += Helpers.stringify(((List<?>)(((m.getParameterTypes().length == 0) ? m.invoke(o) : m.invoke(o, args)))));
    					invokationDone = true;
    					break;
    				}

    			}
			}
			
			if (!invokationDone) {
				Logger.log("Error while executing the request: no way to " +
						"serialize the return value of method '"+ m.getName() + 
						"' (return type is " + m.getReturnType().getName() + 
						").\nPlease contact the maintainer :-)\n", 
						VerboseLevel.SERIOUS_ERROR);
				res= "error\n" +
					 "OntologyServerException\n" +
					 "No way to serialize return value of method '" + m.getName() + 
					 "' (return type is " + m.getReturnType().getName() + ").";	
			}
			

			
		} catch (IllegalArgumentException e) {
			Logger.log("Error while executing the request '" + m.getName() + 
					"': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + 
					"\n", VerboseLevel.ERROR);
			res = "error\n" +
					e.getClass().getName() + "\n" +
					e.getLocalizedMessage().replace("\"", "'");
			
		} catch (ClassCastException e) {
			Logger.log("Error while executing the request '" + m.getName() + 
					"': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + 
					"\n", VerboseLevel.ERROR);
			res = "error\n" +
			e.getClass().getName() + "\n" +
			e.getLocalizedMessage().replace("\"", "'");
			
		} catch (IllegalAccessException e) {
			Logger.log("Error while executing the request '" + m.getName() + 
					"': " + e.getClass().getName() + " -> " + e.getLocalizedMessage() + 
					"\n", VerboseLevel.ERROR);
			res = "error\n" +
			e.getClass().getName() + "\n" +
			e.getLocalizedMessage().replace("\"", "'");	
			
		} catch (InvocationTargetException e) {
			Logger.log("Error while executing the request '" + m.getName() + 
					"': " + e.getCause().getClass().getName() + " -> " + 
					e.getCause().getLocalizedMessage() + "\n", VerboseLevel.ERROR);
			
			res = "error\n" + 
					e.getCause().getClass().getName() + "\n";
			
			String cause = e.getCause().getLocalizedMessage();					
			if (cause != null) res += cause;
		
		}
		
		result.add(res);
	}
	
	
}
