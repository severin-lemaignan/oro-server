package laas.openrobots.ontology.service;

import java.lang.reflect.Method;

public interface IService {
	
	public String getName();

	public String getCategory();

	public String getDesc();

	public Method getMethod();

	public Object getObj();

}
