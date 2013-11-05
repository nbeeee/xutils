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

	Map<String, ServiceObject> initiate(Object[] services) {
		Map<String, ServiceObject> mp = new HashMap<String, ServiceObject>();
		for (Object obj : services) {
			int size = mp.size();
			for (Class<?> itf : ProxyHandler.getInterfaces(obj.getClass())) {
				if(itf.isAnnotationPresent(GroupService.class))
					Objutil.validate(mp.put(itf.getName(), new Service(itf, obj)) == null, "duplicated name: {}", itf);
			}
			Objutil.validate(size < mp.size(), "{}: not found interface extend Remote.", obj);
		}
		Objutil.validate(mp.size() > 0, "services is empty");
		executor.setCorePoolSize(CORE_POOL_SIZE);
		return mp;
	}

	private final class Service implements ServiceObject {
		final Map<String, Method> maps = new HashMap<String, Method>();
		final Object service;

		Service(Class iface, Object _service) {
			this.service = _service;
			for (Method m : iface.getMethods()) {
				Objutil.validate(maps.put(Util.signature(m.getName(), m.getParameterTypes()), m) == null,
						"duplicated name: {}", m);
				m.setAccessible(true);
			}
		}

		public Object handle(final Event event) throws Throwable {
			if (event.syncall)
				return invoke(event);
			try {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							invoke(event);
						} catch (UnavailableException e) {
							eventDao.store(event);
							logger.warn("call {} fail. store it for later call.", e, event.getName());
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
		
		Object invoke(Event event) throws Throwable {
			Method method = maps.get(event.getValue());
			if (method == null)
				throw new IllegalMsgException("method not found. " + event);
			Object[] params = event.parameters();
			try {
				return method.invoke(service, params);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			} catch (Throwable e) {
				throw new IllegalMsgException("marshal exception: " + e);
			}
		}
	}
}
