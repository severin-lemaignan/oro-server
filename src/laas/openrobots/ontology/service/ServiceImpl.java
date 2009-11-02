package laas.openrobots.ontology.service;

import java.lang.reflect.Method;


public class ServiceImpl implements IService {

		private String name;
		private String category;
		private String desc;
		private Method method;
		private Object obj;
		
		public ServiceImpl(String name, String category, String desc, Method method,
				Object obj) {
			super();
			this.name = name;
			this.category = category;
			this.desc = desc;
			this.method = method;
			this.obj = obj;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public String getDesc() {
			return desc;
		}

		@Override
		public Method getMethod() {
			return method;
		}

		@Override
		public Object getObj() {
			return obj;
		}

		@Override
		public int compareTo(IService arg) {
			return method.toString().compareTo(arg.getMethod().toString());
		}
		
		
		
}

