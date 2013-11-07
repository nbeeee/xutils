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
import java.lang.reflect.Modifier;

import zcu.xutil.Objutil;
import zcu.xutil.utils.Function;
import zcu.xutil.utils.Util;

public abstract class Prototype extends RefCaller {
	public static Prototype create(Class<?> clazz, final Object[] args) {
		return new Prototype(clazz) {
			private final Constructor cstr;
			private Object obj;
			{
				State state = State.cstruMatch(getType(), args);
				(cstr = (Constructor) state.member).setAccessible(true);
				obj = Cache.cache(state.providers);
			}

			@Override
			protected Object getObject() {
				Object o = obj;
				if (o instanceof Cache)
					obj = o = ((Cache) o).get();
				return Util.newInstance(cstr, (Object[]) o);
			}
		};
	}

	public static Prototype create(Class product, Class factory, String factoryMethod, Object[] args) {
		Invoker f = new Invoker(factory, factoryMethod, args);
		Objutil.validate(Modifier.isStatic(f.method.getModifiers()), "{} not a static method", f.method);
		return create(product == null ? f.method.getReturnType() : product, null, f);
	}

	public static Prototype create(Class product, Provider provider, String factoryMethod, Object[] args) {
		Invoker f = new Invoker(provider.getType(), factoryMethod, args);
		return create(product == null ? f.method.getReturnType() : product, provider, f);
	}

	public static Prototype create(Class product, final Provider provider, final Function function) {
		return new Prototype(product) {
			private final Function func = Objutil.notNull(function, "function is null");
			private Object factory = Cache.cache(provider);

			@Override
			@SuppressWarnings("unchecked")
			protected Object getObject() {
				Object o = factory;
				if (o instanceof Cache)
					factory = o = ((Cache) o).get();
				return func.apply(o);
			}

			@Override
			protected boolean allowForceCast() {
				return true;
			}

			@Override
			protected Provider matches(Class<?> toType, State state) {
				Provider ret = super.matches(toType, state);
				if (ret == null && state.worstRequired()) {
					ret = this;
					state.worstMatch();
				}
				return ret;
			}
		};
	}

	private final Class<?> type;
	private Invoker link;

	Prototype(Class clazz) {
		Object o = Objutil.defaults(clazz);
		type = o == null ? clazz : o.getClass();
	}

	@Override
	public final RefCaller call(String methodName, Object... args) {
		link = link == null ? new Invoker(getType(), methodName, args) : link.link(getType(), methodName, args);
		return this;
	}

	@Override
	public final Class<?> getType() {
		return type;
	}

	@Override
	public final Object instance() {
		Object o = getObject();
		if (link != null)
			link.apply(o);
		return o;
	}
	protected abstract Object getObject();
}
