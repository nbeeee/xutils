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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import zcu.xutil.Objutil;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class AccessorCache {

	public static AccessorCache property(int cacheSize, Checker<Accessor> filter) {
		return new AccessorCache(cacheSize, filter, null);
	}

	public static AccessorCache field(int cacheSize, Checker<Accessor> filter, boolean includeSuper) {
		return new AccessorCache(cacheSize, filter, includeSuper);
	}

	private final ConcurrentMap<Class, Map<String, Accessor>> cache;
	private final Checker<Accessor> filter;
	private final Boolean includeSuper;

	private AccessorCache(int cacheSize, Checker<Accessor> aFilter, Boolean incSuper) {
		this.cache = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.SECOND_CHANCE,
				cacheSize < 16 ? 16 : cacheSize);
		this.filter = aFilter;
		this.includeSuper = incSuper;
	}

	public Accessor getAccessor(Class clazz, String property) {
		return getAllAccessor(clazz).get(property);
	}

	public Accessor getAccessorIgnoreCase(Class clazz, String property) {
		for (Map.Entry<String, Accessor> entry : getAllAccessor(clazz).entrySet()) {
			if (entry.getKey().equalsIgnoreCase(property))
				return entry.getValue();
		}
		return null;
	}

	public Map<String, Accessor> getAllAccessor(Class clazz) {
		Map<String, Accessor> result = cache.get(clazz);
		if (result != null)
			return result;
		if (includeSuper == null)
			result=AccessProperty.build(clazz, filter);
		else
			result=AccessField.build(clazz, filter, includeSuper);
		return Objutil.ifNull(cache.putIfAbsent(clazz, result), result);
//		result = new HashMap<String, Accessor>();
//		if (includeSuper == null)
//			try {
//				for (PropertyDescriptor p : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
//					String name = p.getName();
//					Method getter = p.getReadMethod();
//					if (getter == null || "class".equals(name))
//						continue;
//					Accessor a = new Prop(name, p.getPropertyType(), getter, p.getWriteMethod());
//					if (filter == null || !filter.checks(a))
//						result.put(name, a);
//				}
//			} catch (IntrospectionException e) {
//				throw new XutilRuntimeException(clazz.getName(), e);
//			}
//		else
//			build(clazz, result);
//		if (result.isEmpty())
//			result = Collections.emptyMap();
//		return Objutil.defaultNull(cache.putIfAbsent(clazz, result), result);
	}

//	private void build(Class clazz, Map<String, Accessor> out) {
//		if (clazz == Object.class)
//			return;
//		if (includeSuper)
//			build(clazz.getSuperclass(), out);
//		for (Field fld : clazz.getDeclaredFields()) {
//			Accessor a = new Fld(fld);
//			if (filter == null || !filter.checks(a))
//				out.put(a.getName(), a);
//		}
//	}
//
//	private static final class Fld implements Accessor {
//		private final Field field;
//
//		Fld(Field _field) {
//			this.field = _field;
//			field.setAccessible(true);
//		}
//
//		public String getName() {
//			return field.getName();
//		}
//
//		public Class<?> getType() {
//			return field.getType();
//		}
//
//		public boolean isWritable() {
//			int mods = field.getModifiers();
//			return (mods & Modifier.STATIC) == 0 || (mods & Modifier.FINAL) == 0;
//		}
//
//		public <T extends Annotation> T getAnnotation(Class<T> annoType) {
//			return field.getAnnotation(annoType);
//		}
//
//		public int getModifiers() {
//			return field.getModifiers();
//		}
//
//		public Object getValue(Object ctx) {
//			try {
//				return field.get(ctx);
//			} catch (IllegalAccessException e) {
//				throw new XutilRuntimeException(getName(), e);
//			}
//		}
//
//		public void setValue(Object ctx, Object value) {
//			try {
//				field.set(ctx, value);
//			} catch (IllegalAccessException e) {
//				throw new XutilRuntimeException(getName(), e);
//			}
//		}
//	}
//
//	private static final class Prop implements Accessor {
//		private final String name;
//		private final Class<?> type;
//		private final Method getter, setter;
//
//		Prop(String pn, Class<?> c, Method get, Method set) {
//			this.name = pn;
//			this.type = c;
//			this.getter = get;
//			this.setter = set;
//			get.setAccessible(true);
//			if (set != null)
//				set.setAccessible(true);
//		}
//
//		public String getName() {
//			return name;
//		}
//
//		public Class<?> getType() {
//			return type;
//		}
//
//		public boolean isWritable() {
//			return setter != null;
//		}
//
//		public <T extends Annotation> T getAnnotation(Class<T> annoType) {
//			return getter.getAnnotation(annoType);
//		}
//
//		public int getModifiers() {
//			return getter.getModifiers();
//		}
//
//		public Object getValue(Object ctx) {
//			try {
//				return getter.invoke(ctx, (Object[]) Objutil.emptyTypes);
//			} catch (IllegalAccessException e) {
//				throw new XutilRuntimeException(name, e);
//			} catch (InvocationTargetException e) {
//				throw Objutil.rethrow(e.getCause());
//			}
//		}
//
//		public void setValue(Object ctx, Object value) {
//			try {
//				setter.invoke(ctx, value);
//			} catch (IllegalAccessException e) {
//				throw new XutilRuntimeException(name, e);
//			} catch (InvocationTargetException e) {
//				throw Objutil.rethrow(e.getCause());
//			}
//		}
//	}
}
