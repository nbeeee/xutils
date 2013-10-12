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

import java.lang.reflect.Method;

import zcu.xutil.utils.Function;
import zcu.xutil.utils.Util;

/**
 *
 * 方法调用
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class Invoker implements Function<Object, Object> {
	final Method method;
	private Object params;

	public Invoker(Class clazz, String methodName, Object[] args) {
		State state = State.methodMatch(clazz, methodName, args);
		(method = (Method) state.member).setAccessible(true);
		params = Cache.cache(state.providers);
	}
	@Override
	public Object apply(Object target) {
		Object o = params;
		if (o instanceof Cache)
			params = o = ((Cache) o).get();
		return Util.call(target, method, (Object[]) o);
	}

	Invoker link(Class clazz, String methodName, Object[] args) {
		return new Invoker(clazz, methodName, args) {
			@Override
			public Object apply(Object target) {
				Invoker.this.apply(target);
				return super.apply(target);
			}
		};
	}
}
