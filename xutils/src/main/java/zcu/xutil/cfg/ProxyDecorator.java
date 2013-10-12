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
package zcu.xutil.cfg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.ProxyHandler;

public final class ProxyDecorator implements Decorator {
	private final Provider base;
	private final Constructor constructor;
	private Interceptor[] interceptors;
	private String[] names;

	public ProxyDecorator(Class proxyInterface, Provider delegate, String[] interceptorNames) {
		base = delegate;
		names = interceptorNames;
		constructor = ProxyHandler.getProxyConstructor(proxyInterface == null ? delegate.getType() : proxyInterface);
	}

	@Override
	public void onStart(Context context) {
		if (base instanceof Decorator)
			((Decorator) base).onStart(context);
		int len = names == null ? 0 : names.length;
		ArrayList<Interceptor> list = new ArrayList<Interceptor>();
		for (int i = 0; i < len; i++)
			list.add((Interceptor) context.getBean(names[i]));
		if (list.isEmpty()) {
			for (Provider provider : context.getProviders(Interceptor.class)) {
				Interceptor aspect = (Interceptor) provider.instance();
				if (aspect.checks(base.getType()))
					list.add(aspect);
			}
		}
		interceptors = list.toArray(new Interceptor[list.size()]);
		names = null;
	}

	@Override
	public Class<?> getType() {
		return constructor.getDeclaringClass();
	}

	@Override
	public Object instance() {
		return Util.newInstance(constructor, new Object[]{new ProxyHandler(base.instance() , interceptors)});
	}
}