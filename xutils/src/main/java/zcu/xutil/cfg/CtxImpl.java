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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import zcu.xutil.DisposeManager;
import zcu.xutil.Objutil;

/**
 * 
 * 容器实现
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

final class CtxImpl implements Context {
	private static final String anonymous = "@";
	private static final AtomicInteger counter = new AtomicInteger(-0x0fffffff);

	private final String ctxName;
	private final Context parent;
	private final Map<String, Bean> beanmap;
	private volatile List<Bean> beanlist;
	private volatile Reference<Map<Class, List<NProvider>>> cacheRef;
	private List<Object> closeObjects;

	CtxImpl(String name, Context father) {
		this.ctxName = name;
		this.parent = father;
		this.beanlist = new ArrayList<Bean>();
		this.beanmap = new HashMap<String, Bean>();
	}

	@Override
	public String toString() {
		return ctxName + " beans: " + beanmap.size() + ", parent:" + parent;
	}

	@Override
	public synchronized void destroy() {
		if (beanlist == null)
			return;
		beanlist = null;
		beanmap.clear();
		cacheRef.clear();
		List list = closeObjects;
		closeObjects = Collections.emptyList();
		if (list != null) {
			int i = list.size();
			while (--i > 0) {
				String m = (String) list.get(i);
				DisposeManager.destroyCall(list.get(--i), m);
			}
			list.clear();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (beanlist != null)
			destroy();
	}

	@Override
	public Object getBean(String name) throws NoneBeanException {
		Provider p = getProvider(name);
		if (p == null)
			throw new NoneBeanException(name);
		return p.instance();
	}

	@Override
	public Provider getProvider(String name) {
		if (beanlist == null)
			throw new IllegalStateException("context destroyed.");
		Provider p = beanmap.get(name);
		return p == null && parent != null ? (parent.getProvider(name)) : p;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<NProvider> listMe() {
		return (Collection) Collections.unmodifiableList(beanlist);
	}

	@Override
	public synchronized List<NProvider> getProviders(Class<?> type) {
		List<NProvider> ret, parents;
		Map<Class, List<NProvider>> cache = cacheRef.get();
		if (cache == null)
			cacheRef = new SoftReference<Map<Class, List<NProvider>>>(cache = new HashMap<Class, List<NProvider>>());
		else if ((ret = cache.get(type)) != null)
			return ret;
		final int len = (parents = parent == null ? Collections.<NProvider> emptyList() : parent.getProviders(type))
				.size();
		ret = new ArrayList<NProvider>();
		for (int i = 0; i < len; i++) {
			NProvider p = parents.get(i);
			if (!beanmap.containsKey(p.getName()))
				ret.add(p);
		}
		boolean same = ret.size() == len;
		for (NProvider p : beanlist) {
			if (type.isAssignableFrom(p.getType()))
				ret.add(p);
		}
		cache.put(type, ret = (same && len == ret.size()) ? parents : Collections.unmodifiableList(ret));
		return ret;
	}

	void initValid() {
		if (cacheRef == null || beanlist == null)
			throw new IllegalStateException("destroyed or not started.");
	}

	synchronized void addDestroy(Object o, String method) {
		if (closeObjects == null)
			closeObjects = new ArrayList<Object>();
		closeObjects.add(o);
		closeObjects.add(method);
	}

	synchronized void startup() {
		Objutil.validate(cacheRef == null, "context started.");
		List<Bean> list = beanlist;
		beanlist = Collections.emptyList();
		final int len = list.size();
		List<Bean> ret = new ArrayList<Bean>(len);
		for (int i = 0; i < len; i++) {
			Bean bean = list.get(i);
			if (bean.getName().startsWith(anonymous))
				beanmap.remove(bean.getName());
			else
				ret.add(bean);
		}
		if (!ret.isEmpty())
			beanlist = Arrays.asList(ret.toArray(new Bean[ret.size()]));
		Map<Class, List<NProvider>> cache = new HashMap<Class, List<NProvider>>();
		cacheRef = new SoftReference<Map<Class, List<NProvider>>>(cache);
		Initor r;
		for (int i = 0; i < len; i++) {
			if ((r = list.get(i).initor) != null)
				r.initDecorator();
		}
		for (int i = 0; i < len; i++) {
			if ((r = list.get(i).initor) != null)
				r.eagerInstance();
		}
		list.clear();
		DisposeManager.register(this);
	}

	LifeCtrl put(boolean cache, String name, Provider p) {
		if (Objutil.isEmpty(name))
			name = anonymous + counter.getAndDecrement();
		Bean bean = !cache || p instanceof Instance || p instanceof Only ? new Comp(name, p, this) : new Only(name, p,
				this);
		Objutil.validate(beanmap.put(name, bean) == null,"duplicated name: {}",name);
		beanlist.add(bean);
		return bean.initor;
	}

	private final class Initor implements LifeCtrl {
		private final Bean bean;
		private boolean eager;
		private boolean entry;
		private volatile boolean inited;
		private Provider provider;
		private String destroy;

		Initor(Provider p, Bean b) {
			provider = p;
			bean = b;
		}

		Object firstCallByBean() {
			initDecorator();
			Provider temp = Objutil.notNull(provider, "{} reentry.", bean);
			provider = null;
			Object ret = temp.instance();
			if (destroy != null && (bean instanceof Only || temp instanceof Instance))
				addDestroy(ret, destroy);
			return ret;
		}

		void initDecorator() {
			if (!inited)
				synchronized (CtxImpl.this) {
					if (!inited) {
						if (provider instanceof Decorator) {
							initValid();
							Objutil.validate(!entry, "{} reentry.", bean);
							entry = true;
							((Decorator) provider).onStart(CtxImpl.this);
							entry = false;
						}
						inited = true;
					}
				}
		}

		void eagerInstance() {
			if (eager)
				bean.instance();
		}

		@Override
		public LifeCtrl eager(boolean eagerInstance) {
			eager = eagerInstance;
			return this;
		}

		@Override
		public LifeCtrl die(String destroyMethod) {
			destroy = destroyMethod;
			return this;
		}

		@Override
		public String name() {
			return bean.getName();
		}
	}

	private static abstract class Bean extends BeanReference implements NProvider {
		private final String name;
		private final Class type;
		volatile Initor initor;

		Bean(String id, Provider p, CtxImpl ctx) {
			name = id;
			type = p.getType();
			initor = ctx.new Initor(p, this);
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public final Class<?> getType() {
			return type;
		}

		@Override
		protected final boolean allowForceCast() {
			return true;
		}

		@Override
		public final String toString() {
			return "bean:" + name + " " + getClass().getName() + " " + getType();
		}

	}

	private static final class Only extends Bean {
		private volatile Object cache;

		Only(String id, Provider p, CtxImpl ctx) {
			super(id, p, ctx);
		}

		@Override
		public Object instance() {
			if (initor != null)
				synchronized (this) {
					if (initor != null) {
						cache = initor.firstCallByBean();
						initor = null;
					}
				}
			return cache;
		}

		@Override
		protected final Provider get() {
			return this;
		}
	}

	private static final class Comp extends Bean {
		private final Provider base;

		Comp(String id, Provider p, CtxImpl ctx) {
			super(id, p, ctx);
			base = p;
		}

		@Override
		public Object instance() {
			if (initor != null)
				synchronized (this) {
					if (initor != null) {
						Object ret = initor.firstCallByBean();
						initor = null;
						return ret;
					}
				}
			return base.instance();
		}

		@Override
		protected Provider get() {
			return (base instanceof Bean) ? base : this;
		}

		@Override
		protected Provider matches(Class<?> toType, State state) {
			if (base instanceof BeanReference)
				return ((BeanReference) base).matches(toType, state);
			return super.matches(toType, state);
		}
	}
}
