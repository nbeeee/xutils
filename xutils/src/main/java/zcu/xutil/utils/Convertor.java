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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public class Convertor {
	private static final Map<String,Class> primitiveMap = new HashMap<String,Class>();
	private static final Map<Class, Function> functions = new HashMap<Class, Function>();

	static {
		primitiveMap.put("boolean", boolean.class);
		primitiveMap.put("byte", byte.class);
		primitiveMap.put("char", char.class);
		primitiveMap.put("short", short.class);
		primitiveMap.put("int", int.class);
		primitiveMap.put("long", long.class);
		primitiveMap.put("float", float.class);
		primitiveMap.put("double", double.class);
		primitiveMap.put("void", void.class);
		functions.put(Integer.class, new Function() {
			@Override
			public Object apply(Object value) {
				return intValue(value);
			}
		});
		functions.put(Long.class, new Function() {
			@Override
			public Object apply(Object value) {
				return longValue(value);
			}
		});
		functions.put(Byte.class, new Function() {
			@Override
			public Object apply(Object value) {
				return Byte.valueOf((byte) intValue(value));
			}
		});
		functions.put(Short.class, new Function() {
			@Override
			public Object apply(Object value) {
				return Short.valueOf((short) intValue(value));
			}
		});
		functions.put(Double.class, new Function() {
			@Override
			public Object apply(Object value) {
				return doubleValue(value);
			}
		});
		functions.put(Float.class, new Function() {
			@Override
			public Object apply(Object value) {
				return Float.valueOf((float) doubleValue(value));
			}
		});
		functions.put(Boolean.class, new Function() {
			@Override
			public Object apply(Object value) {
				if (value instanceof String)
					return Boolean.valueOf(((String) value).trim());
				throw exception(value, "boolean");
			}
		});
		functions.put(Character.class, new Function() {
			@Override
			public Object apply(Object value) {
				if (value instanceof String) {
					String s = (String) value;
					return s.length() > 0 ? s.charAt(0) : '\u0000';
				}
				if (value instanceof Number)
					return (char) ((Number) value).intValue();
				throw exception(value, "char");
			}
		});
		functions.put(BigDecimal.class, new Function() {
			@Override
			public Object apply(Object value) {
				if (value instanceof BigInteger)
					return new BigDecimal((BigInteger) value);
				if (value instanceof String) {
					String s = ((String) value).trim();
					return s.length() == 0 ? BigDecimal.ZERO : new BigDecimal(s);
				}
				return BigDecimal.valueOf(doubleValue(value));
			}
		});
		functions.put(BigInteger.class, new Function() {
			@Override
			public Object apply(Object value) {
				if (value instanceof BigDecimal)
					return ((BigDecimal) value).toBigInteger();
				if (value instanceof String) {
					String s = ((String) value).trim();
					return s.length() == 0 ? BigInteger.ZERO : new BigInteger(s);
				}
				return BigInteger.valueOf(longValue(value));
			}
		});
		functions.put(Class.class, new Function() {
			@Override
			public Object apply(Object value) {
				if (value instanceof String) {
					Object o = getPrimitive((String) value);
					return o != null ? o : Objutil.loadclass(Objutil.contextLoader(), (String) value);
				}
				throw exception(value, "class");
			}
		});
	}

	private Map<Class, Function> maps = functions;

	protected static RuntimeException exception(Object value, String toType) {
		return new XutilRuntimeException(value + " convert to " + toType, null);
	}

	protected Object customConvert(Object value, Class<?> toType) {
		if (toType == String.class) {
			if (value instanceof Class)
				return ((Class) value).getName();
			if (value instanceof char[])
				return String.valueOf((char[]) value);
			return value.toString();
		}
		throw exception(value, toType.getName());
	}

	public final <T> void addFunction(Class<T> type, Function<? extends T, ?> func) {
		if (maps == functions)
			maps = new HashMap<Class, Function>(functions);
		maps.put(type, func);
	}

	@SuppressWarnings("unchecked")
	public final Object convert(Object value, Class toType) {
		Object object = Objutil.defaults(toType);
		if (value == null)
			return object;
		if (object != null)
			toType = object.getClass();
		if (toType.isInstance(value))
			return value;
		Function f = maps.get(toType);
		if (f != null)
			return f.apply(value);
		Class compType = toType.getComponentType();
		if (compType != null) {
			if (compType == Character.TYPE && value instanceof String)
				return ((String) value).toCharArray();
			return arrayConvert(value, compType);
		}
		if (toType.isEnum() && value instanceof String)
			return Enum.valueOf(toType, (String) value);
		return customConvert(value, toType);
	}

	protected final Object arrayConvert(Object source, Class targetComponentType) {
		Object ret;
		if (source.getClass().isArray()) {
			int len = Array.getLength(source);
			ret = Array.newInstance(targetComponentType, len);
			while (--len >= 0)
				Array.set(ret, len, convert(Array.get(source, len), targetComponentType));
		} else {
			ret = Array.newInstance(targetComponentType, 1);
			Array.set(ret, 0, convert(source, targetComponentType));
		}
		return ret;
	}

	public static int intValue(Object value) {
		if (value instanceof String) {
			String s = ((String) value).trim();
			return s.length() == 0 ? 0 : Integer.parseInt(s);
		}
		if (value instanceof Number)
			return ((Number) value).intValue();
		if (value instanceof Character)
			return ((Character) value).charValue();
		throw exception(value, "int");
	}

	public static long longValue(Object value) {
		if (value instanceof String) {
			String s = ((String) value).trim();
			return s.length() == 0 ? 0 : Long.parseLong(s);
		}
		if (value instanceof Number)
			return ((Number) value).longValue();
		if (value instanceof Character)
			return ((Character) value).charValue();
		throw exception(value, "long");
	}

	public static double doubleValue(Object value) {
		if (value instanceof String) {
			String s = ((String) value).trim();
			return s.length() == 0 ? 0 : Double.parseDouble(s);
		}
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		if (value instanceof Character)
			return ((Character) value).charValue();
		throw exception(value, "double");

	}

	/**
	 * primitive class name to primitive class, return null if not primitive.
	 **/
	public static Class getPrimitive(String className) {
		return  primitiveMap.get(className);
	}
}
