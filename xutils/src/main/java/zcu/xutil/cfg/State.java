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
import java.lang.reflect.Method;

import zcu.xutil.Objutil;

/**
 * 
 * 参数匹配状态记录
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class State {
	public static final State dummy = new State(null, null);

	private static final BeanReference[] EMPTY_PARAMETERS = {};

	private short identical;
	private boolean convert;
	private boolean worst;
	private final State prev;
	final Object member;
	Provider[] providers;

	private State(Object memb, State previous) {
		member = memb;
		prev = previous;
	}

	boolean converRequired() {
		return prev == null || prev.convert || prev.worst;
	}

	void convertMatch() {
		convert = true;
	}

	boolean worstRequired() {
		return prev == null || prev.worst;
	}

	void worstMatch() {
		worst = true;
	}

	public boolean matched(Class<?> from, Class<?> to) {
		if (from == to) {
			identical++;
			return true;
		}
		Object o = Objutil.defaults(from);
		if (o != null)
			from = o.getClass();
		if ((o = Objutil.defaults(to)) != null)
			to = o.getClass();
		return to.isAssignableFrom(from);
	}

	private int getLevel() {
		return (worst ? 2 * Short.MAX_VALUE : convert ? Short.MAX_VALUE : 0) - identical;
	}

	private State select() {
		if (prev == null)
			return this;
		int level, current = getLevel();
		State ret = this, s = prev;
		do {
			if ((level = s.getLevel()) == current)
				ret = null;
			else if (level < current) {
				ret = s;
				current = level;
			}
		} while ((s = s.prev) != null);
		return ret;
	}

	static BeanReference[] toParameters(Object[] args) {
		int len;
		if (args == null || (len = args.length) == 0)
			return EMPTY_PARAMETERS;
		BeanReference[] p = new BeanReference[len];
		while (--len >= 0) {
			Object o = args[len];
			p[len] = o instanceof BeanReference ? (BeanReference) o : Instance.value(o);
		}
		return p;
	}

	static State cstruMatch(Class clazz, Object[] params) {
		final BeanReference[] args = toParameters(params);
		int i, len = args.length;
		State state = null;
		outer: for (Constructor<?> e : clazz.getConstructors()) {
			Class[] types = e.getParameterTypes();
			if (types.length == len) {
				State s = new State(e, state);
				if (len == 0)
					return s;
				Provider providers[] = new Provider[i = len];
				while (--i >= 0) {
					if ((providers[i] = args[i].matches(types[i], s)) == null)
						continue outer;
				}
				(state = s).providers = providers;
			}
		}
		if (state == null || (state = state.select()) == null)
			exception(clazz,"constructor",params);
		return state;
	}

	static State methodMatch(Class clazz, String methodName, Object[] params) {
		final BeanReference[] args = toParameters(params);
		int i, len = args.length;
		State state = null;
		outer: for (Method e : clazz.getMethods()) {
			if (!e.getName().equals(methodName) || e.isBridge())
				continue;
			Class[] types = e.getParameterTypes();
			if (types.length == len) {
				State s = new State(e, state);
				if (len == 0)
					return s;
				Provider providers[] = new Provider[i = len];
				while (--i >= 0) {
					if ((providers[i] = args[i].matches(types[i], s)) == null)
						continue outer;
				}
				(state = s).providers = providers;
			}
		}
		if (state == null || (state = state.select()) == null)
			exception(clazz,methodName,params);
		return state;
	}
	
	private static void exception(Class clazz,String method,Object[] params){
		throw new IllegalArgumentException( Objutil.append(
				new StringBuilder(clazz.getName()).append(" can't match: ").append(method), params).toString());
	}
}
