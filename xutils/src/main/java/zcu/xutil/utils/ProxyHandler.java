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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class ProxyHandler implements InvocationHandler {
	public static Constructor getProxyConstructor(Class clazz) {
		try {
			return Proxy.getProxyClass(clazz.getClassLoader(), getInterfaces(clazz)).getConstructor(
					InvocationHandler.class);
		} catch (NoSuchMethodException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static Class[] getInterfaces(Class clazz) {
		if (clazz.isInterface())
			return new Class[] { clazz };
		ArrayList<Class> list = new ArrayList<Class>();
		for (Class cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
			for (Class iface : cls.getInterfaces())
				if (!list.contains(iface))
					list.add(iface);
		}
		return list.toArray(new Class[list.size()]);
	}

	public static Object proxyObjectMethod(Object proxy, Method method, Object[] args) {
		if (method.getDeclaringClass() != Object.class)
			return null;
		String m = method.getName();
		if (m.equals("equals"))
			return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
		if (m.equals("hashCode"))
			return Integer.valueOf(System.identityHashCode(proxy));
		if (m.equals("toString")) {
			Class c = proxy.getClass();
			return Objutil.append(new StringBuilder(c.getName()), c.getInterfaces()).toString();
		}
		throw new IllegalArgumentException(m);
	}

	final Object target;
	final Interceptor[] intercepts;

	public ProxyHandler(Object aTarget, Interceptor... interceptors) {
		target = aTarget;
		intercepts = interceptors;
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		Object ret = proxyObjectMethod(proxy, method, args);
		return ret != null ? ret : new MethodInvocation() {
			private int cursor;

			@Override
			public Method getMethod() {
				return method;
			}

			@Override
			public Object[] getArguments() {
				return args;
			}

			@Override
			public Object proceed() throws Throwable {
				return cursor < intercepts.length ? intercepts[cursor++].invoke(this) : Util.invoke(target, method,
						args);
			}

			@Override
			public Object getThis() {
				return target;
			}
		}.proceed();
	}
}