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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import zcu.xutil.Objutil;
import zcu.xutil.msg.GroupService;
import zcu.xutil.utils.Util;

public final class Handler implements ThreadFactory {
	private static final int CORE_POOL_SIZE = 8;
	private static volatile int counter;
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
		return Util.newThread(r,  "MsgHandler-" + counter++, false);
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
			Objutil.dupChkPut(mp,serviceName, new GS(this, e.getValue()));
		}
		for (Remote obj : interfaceservice) {
			int size = mp.size();
			for (Class var = obj.getClass(); var != Object.class; var = var.getSuperclass()) {
				for (Class<?> itf : var.getInterfaces()) {
					if (Remote.class.isAssignableFrom(itf))
						Objutil.dupChkPut(mp,itf.getName(), new MS(this, itf, obj));
				}
			}
			Objutil.validate(size < mp.size(), "{}: not found interface extend Remote.", obj.getClass());
		}
		Objutil.validate(mp.size() > 0, "services is empty");
		executor.setCorePoolSize(CORE_POOL_SIZE);
		return mp;
	}

	private static final class MS extends ServiceObject {
		private final Map<String, Method> maps = new HashMap<String, Method>();
		private final Object service;

		MS(Handler h, Class iface, Object _service) {
			super(h);
			this.service = _service;
			for (Method m : iface.getMethods()) {
				Objutil.dupChkPut(maps,Util.signature(m.getName(),m.getParameterTypes()), m);
				m.setAccessible(true);
			}
		}

		@Override
		protected Object invoke(Event event) throws Throwable {
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

	private static final class GS extends ServiceObject {
		private final GroupService service;

		GS(Handler h, GroupService gs) {
			super(h);
			this.service = gs;
		}

		@Override
		protected Object invoke(Event event) throws Throwable {
			service.service(event.getValue(), event.parameters());
			return null;
		}
	}
}
