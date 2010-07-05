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

