/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import zcu.xutil.Objutil;

public class AccessProperty implements Accessor {
	public static Map<String, Accessor> build(Class clazz, Checker<Accessor> filter) {
		Map<String, Accessor> result = new HashMap<String, Accessor>();
		AccessProperty acp;
		for (Method m : clazz.getMethods()) {
			if (m.getDeclaringClass() == Object.class)
				continue;
			int index = Util.indexGetter(m);
			if (index > 0) {
				String name = Objutil.decapitalize(m.getName().substring(index));
				if ((acp = (AccessProperty) result.get(name)) == null)
					result.put(name, new AccessProperty(name, m.getReturnType(), m, null));
				else {
					Objutil.validate(acp.type == m.getReturnType(), "{} get set type difference.", m);
					acp.getter = m;
				}
				continue;
			}
			Class cls = Util.getSetterType(m);
			if (cls != null) {
				String name = Objutil.decapitalize(m.getName().substring(3));
				if ((acp = (AccessProperty) result.get(name)) == null)
					result.put(name, new AccessProperty(name, cls, null, m));
				else {
					Objutil.validate(acp.type == cls, "{} get set type difference.", m);
					acp.setter = m;
				}
			}
		}
		Iterator<Accessor> iter = result.values().iterator();
		while (iter.hasNext()) {
			acp = (AccessProperty) iter.next();
			if (acp.getter == null || (filter != null && filter.checks(acp)))
				iter.remove();
			else
				acp.setAccessible();
		}
		return result.isEmpty() ? Collections.<String, Accessor> emptyMap() : result;
	}

	private final String name;
	private final Class<?> type;
	private Method getter, setter;

	private AccessProperty(String n, Class<?> c, Method get, Method set) {
		this.name = n;
		this.type = c;
		this.getter = get;
		this.setter = set;
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public Class<?> getType() {
		return type;
	}
	@Override
	public boolean isWritable() {
		return setter != null;
	}
	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annoType) {
		return getter.getAnnotation(annoType);
	}
	@Override
	public int getModifiers() {
		return getter.getModifiers();
	}
	@Override
	public Object getValue(Object ctx) {
		return Util.call(ctx, getter, null);
	}
	@Override
	public void setValue(Object ctx, Object value) {
		Util.call(ctx, setter, new Object[] { value });
	}

	private void setAccessible() {
		getter.setAccessible(true);
		if (setter != null)
			setter.setAccessible(true);
	}
}
