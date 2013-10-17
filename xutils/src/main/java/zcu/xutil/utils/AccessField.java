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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zcu.xutil.XutilRuntimeException;

public class AccessField implements Accessor {

	public static Map<String, Accessor> build(Class clazz, Checker<Accessor> filter, boolean includeSuper) {
		Map<String, Accessor> result = new HashMap<String, Accessor>();
		build(clazz, filter, includeSuper, result);
		return result.isEmpty() ? Collections.<String, Accessor> emptyMap() : result;
	}

	private static void build(Class clazz, Checker<Accessor> filter, boolean includeSuper, Map<String, Accessor> out) {
		if (clazz == Object.class)
			return;
		if (includeSuper)
			build(clazz.getSuperclass(), filter, true, out);
		for (Field fld : clazz.getDeclaredFields()) {
			if ((fld.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0) {
				Accessor a = new AccessField(fld);
				if (filter == null || !filter.checks(a)) {
					out.put(a.getName(), a);
					fld.setAccessible(true);
				}
			}
		}
	}

	private final Field field;

	private AccessField(Field f) {
		this.field = f;
	}
	@Override
	public String getName() {
		return field.getName();
	}
	@Override
	public Class<?> getType() {
		return field.getType();
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annoType) {
		return field.getAnnotation(annoType);
	}
	@Override
	public int getModifiers() {
		return field.getModifiers();
	}
	@Override
	public Object getValue(Object ctx) {
		try {
			return field.get(ctx);
		} catch (IllegalAccessException e) {
			throw new XutilRuntimeException(getName(), e);
		}
	}
	@Override
	public void setValue(Object ctx, Object value) {
		try {
			field.set(ctx, value);
		} catch (IllegalAccessException e) {
			throw new XutilRuntimeException(getName(), e);
		}
	}
}
