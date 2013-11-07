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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import zcu.xutil.Objutil;

/**
 * 工厂数组
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class Xarray extends BeanReference implements Provider {
	private final Class arrType;
	private final Cache cache;

	public Xarray(Class compType, Object[] args) {
		BeanReference[] p = State.toParameters(args);
		int len = p.length;
		Provider[] providers = new Provider[len];
		while (--len >= 0)
			Objutil.validate((providers[len] = p[len].matches(compType, State.dummy)) != null,
					"can't cast to array.{}", p);
		this.arrType = Array.newInstance(compType, 0).getClass();
		this.cache = Cache.cache(providers);
	}

	Xarray(Class arrClass, Cache arr) {
		this.arrType = arrClass;
		this.cache = arr;
	}
	@Override
	public Class<?> getType() {
		return arrType;
	}
	@Override
	public Object instance() {
		Object result, arr[] = (Object[])cache.get();
		int len = arr.length;
		result = Array.newInstance(arrType.getComponentType(), len);
		while (--len >= 0)
			Array.set(result, len, arr[len]);
		return result;
	}

	@Override
	protected Provider matches(Class<?> toType, State state) {
		if (state.matched(getType(), toType))
			return this;
		if (!state.converRequired())
			return null;
		Class<?> compTo = toType.getComponentType();
		if (compTo == null) {
			if ((toType == Collection.class || toType == List.class || toType == Set.class)) {
				state.convertMatch();
				return new COL(toType, cache);
			}
		} else if (state.matched(arrType.getComponentType(), compTo))
			return new Xarray(toType, cache);
		return null;
	}

	@Override
	protected Provider get() {
		return this;
	}

	private static final class COL implements Provider {
		private final Class type;
		private final Cache cache;

		COL(Class c, Cache arr) {
			type = c;
			cache = arr;
		}
		@Override
		public Class<?> getType() {
			return type;
		}
		@Override
		public Object instance() {
			List<Object> ret = Arrays.asList((Object[])cache.get());
			return type == Set.class ? new HashSet<Object>(ret) : new ArrayList<Object>(ret);
		}
	}
}
