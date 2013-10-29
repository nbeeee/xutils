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
package zcu.xutil.msg.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.msg.GroupService;
import zcu.xutil.utils.ProxyHandler;
import zcu.xutil.utils.Util;

public final class Handler implements ThreadFactory {
	static final Logger logger = Logger.getLogger(Handler.class);
	private static final int CORE_POOL_SIZE = 8;
	private static final AtomicInteger counter = new AtomicInteger();
	final ThreadPoolExecutor executor;
	final EventDao eventDao;

	Handler(int maxPoolSize, EventDao eventdao) {
		if (maxPoolSize < CORE_POOL_SIZE)
			maxPoolSize = CORE_POOL_SIZE;
		this.eventDao = eventdao;
		this.executor = new ThreadPoolExecutor(1, maxPoolSize, 20, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
				this);
	}

	@Override
	public Thread newThread(Runnable r) {
		return Util.newThread(r, "MsgHandler-" + counter.getAndIncrement(), false);
	}

	void shutdown() {
		try {
			executor.shutdown();
			executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (Throwable e) {
			// ignore
		}
	}

	Map<String, ServiceObject> initiate(Map<String, ? extends GroupService> groupServices, Remote[] interfaceservice) {
		Map<String, ServiceObject> mp = new HashMap<String, ServiceObject>();
		for (Map.Entry<String, ? extends GroupService> e : groupServices.entrySet()) {
			String serviceName = e.getKey().intern();
			Objutil.validate(mp.put(serviceName, new Group(e.getValue())) == null, "duplicated name: {}", serviceName);
		}
		for (Remote obj : interfaceservice) {
			int size = mp.size();
			for (Class itf : ProxyHandler.getInterfaces(obj.getClass())) {
				if (Remote.class.isAssignableFrom(itf))
					Objutil.validate(mp.put(itf.getName(), new Iface(itf, obj)) == null, "duplicated name: {}", itf);
			}
			Objutil.validate(size < mp.size(), "{}: not found interface extend Remote.", obj);
		}
		Objutil.validate(mp.size() > 0, "services is empty");
		executor.setCorePoolSize(CORE_POOL_SIZE);
		return mp;
	}

	private final class Iface implements ServiceObject {
		final Map<String, Method> maps = new HashMap<String, Method>();
		final Object service;

		Iface(Class iface, Object _service) {
			this.service = _service;
			for (Method m : iface.getMethods()) {
				Objutil.validate(maps.put(Util.signature(m.getName(), m.getParameterTypes()), m) == null,
						"duplicated name: {}", m);
				m.setAccessible(true);
			}
		}

		public Object handle(final Event event) throws Throwable {
			final Method method = maps.get(event.getValue());
			if (method == null)
				throw new IllegalMsgException("method not found. " + event);
			final Object[] params = event.parameters();
			if (event.syncall)
				try {
					return method.invoke(service, params);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				} catch (Throwable e) {
					throw new IllegalMsgException("marshal exception: " + e);
				}
			try {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							method.invoke(service, params);
						} catch (InvocationTargetException e) {
							Throwable cause = e.getCause();
							if (cause instanceof UnavailableException) {
								eventDao.store(event);
								logger.warn("{} unavailable.", e, event.getName());
							} else
								eventDao.discardLogger(event, cause);
						} catch (Throwable e) {
							eventDao.discardLogger(event, e);
						}
					}
				});
			} catch (RejectedExecutionException e) {
				logger.info("TOO MANY TASK. ", e);
				throw new UnavailableException("TOO MANY TASK.");
			}
			return Objutil.defaults(method.getReturnType());
		}
	}

	private final class Group implements ServiceObject {
		final GroupService service;

		Group(GroupService gs) {
			this.service = gs;
		}

		public Object handle(final Event event) {
			if (event.syncall)
				service.service(event.getValue(), event.parameters());
			else
				try {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								service.service(event.getValue(), event.parameters());
							} catch (UnavailableException e) {
								eventDao.store(event);
								logger.warn("{} unavailable.", e, event.getName());
							} catch (Throwable e) {
								eventDao.discardLogger(event, e);
							}
						}
					});
				} catch (RejectedExecutionException e) {
					logger.info("TOO MANY TASK. ", e);
					throw new UnavailableException("TOO MANY TASK.");
				}
			return null;
		}
	}
}
