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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import zcu.xutil.Constants;
import zcu.xutil.Disposable;
import zcu.xutil.DisposeManager;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.SimpleBroker;
import zcu.xutil.msg.impl.Event;
import zcu.xutil.msg.impl.EventDao;
import zcu.xutil.msg.impl.MSGException;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.ProxyHandler;

final class HttpBrokerImpl implements SimpleBroker, BrokerAgent, Disposable {
	private static final boolean testmode = Boolean.parseBoolean(Objutil.systring(Constants.XUTILS_MSG_TEST));
	static final Logger logger = Logger.getLogger(HttpBrokerImpl.class);
	private final URL[] urls;
	private final String credentials;
	private int current;
	final EventDao eventDao;
	volatile boolean destroyed, available = true;

	HttpBrokerImpl(HttpBrokerFactory hbf) {
		List<String> l = Objutil.split(hbf.urls, ',');
		int len = l.size();
		this.urls = new URL[len];
		try {
			while (--len >= 0)
				urls[len] = new URL(l.get(len));
		} catch (MalformedURLException e) {
			throw new XutilRuntimeException(e);
		}
		this.credentials = hbf.getCredentials();
		this.eventDao = new EventDao(hbf.name, hbf.datasource, this);
		DisposeManager.register(this); // destory before EventDao.
		eventDao.start();
	}

	private synchronized HttpURLConnection openConnection(int timeout) {
		HttpURLConnection conn;
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[current];
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setUseCaches(false);
				conn.setDoOutput(true);
				conn.setConnectTimeout(3000);
				// conn.setDoInput(true);
				// conn.setAllowUserInteraction(true);
				// conn.setRequestMethod("POST");
				// conn.setRequestProperty("Connection", "close");
				// conn.setRequestProperty("Content-Type",
				// "application/octet-stream");
				if (credentials != null)
					conn.setRequestProperty("Authorization", credentials);
				if(testmode)
					conn.setRequestProperty("Xtest", "T");
				conn.setReadTimeout(timeout > 0 ? timeout : 30000);
				conn.connect();
				return conn;
			} catch (IOException e) {
				if (++current >= urls.length)
					current = 0;
				logger.info("connect fail. {}", e, url);
			}
		}
		available = false;
		throw new MSGException("can't open connection.");
	}

	private synchronized void unavailable() {
		if (++current >= urls.length)
			current = 0;
		available = false;
	}

	@Override
	public ServiceObject getLocalService(String canonicalName) {
		return null;
	}

	@Override
	public Object sendToRemote(Event event, int timeoutMillis) throws Throwable {
		Object ret;
		HttpURLConnection conn = openConnection(timeoutMillis);
		try {
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			try {
				event.writeTo(out);
			} finally {
				Objutil.closeQuietly(out);
			}
			int code = conn.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				if (code == HttpURLConnection.HTTP_UNAVAILABLE)
					unavailable();
				else
					available = true;
				throw new MSGException("http status: " + code);
			}
			available = true;
			InputStream in = conn.getInputStream();
			try {
				ret = Event.unmarshall(in);
			} finally {
				Objutil.closeQuietly(in);
			}

		} catch (IOException e) {
			throw new MSGException(e.toString());
		} finally {
			conn.disconnect();
		}
		if (ret instanceof Throwable)
			throw (Throwable) ret;
		return ret;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		eventDao.destroy();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Class<T> iface) {
		GroupService gs = Objutil.notNull(iface.getAnnotation(GroupService.class), "not a groupservice", iface);
		final boolean syncmode = !gs.asyncall();
		final boolean sendprefer = gs.sendprefer();
		final int timeoutMillis = gs.timeoutMillis();
		final int expireMinutes = gs.expireMinutes();
		final String cname = iface.getName().intern();
		InvocationHandler h = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
				Object ret = ProxyHandler.proxyObjectMethod(proxy, m, args);
				if (ret != null)
					return ret;
				Event event = new Event(cname, Util.signature(m.getName(), m.getParameterTypes()), args);
				if (destroyed)
					throw new IllegalStateException("Broker destroyed.");
				if (event.syncall = syncmode)
					return sendToRemote(event, timeoutMillis);
				ret = Objutil.defaults(m.getReturnType());
				if (expireMinutes > 0)
					event.setExpire(new java.util.Date(Util.now() + expireMinutes * 60000L));
				try {
					if (sendprefer && available)
						sendToRemote(event, timeoutMillis);
					return ret;
				} catch (IllegalMsgException e) {
					eventDao.discardLogger(event, e);
					return ret;
				} catch (Throwable e) {
					logger.debug("prefer send fail. store event: {}", e, event);
				}
				eventDao.store(event);
				return ret;
			}
		};
		return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, h);
	}
}
