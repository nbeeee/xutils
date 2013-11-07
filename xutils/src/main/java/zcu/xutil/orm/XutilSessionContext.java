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
package zcu.xutil.orm;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.context.CurrentSessionContext;
import org.hibernate.engine.SessionFactoryImplementor;

import zcu.xutil.Disposable;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.txm.MResource;
import zcu.xutil.txm.MResourceFactory;
import zcu.xutil.txm.TxManager;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.ProxyHandler;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public final class XutilSessionContext implements CurrentSessionContext, MResourceFactory {
	private static final long serialVersionUID = 1L;
	private static volatile XutilSessionContext[] factorys = {};
	static final Constructor constructor = ProxyHandler.getProxyConstructor(Session.class);

	private static synchronized void addFactory(XutilSessionContext sc) {
		ArrayList<XutilSessionContext> list = new ArrayList<XutilSessionContext>();
		for (XutilSessionContext x : factorys) {
			if (!x.sessionFactory.isClosed())
				list.add(x);
		}
		list.add(sc);
		factorys = list.toArray(new XutilSessionContext[list.size()]);
	}

	static XutilSessionContext getSessionContext(SessionFactory sessionFactory) {
		for (XutilSessionContext sc : factorys) {
			if (sc.sessionFactory.equals(sessionFactory))
				return sc;
		}
		throw new IllegalArgumentException("not a managed sessionFactory: " + sessionFactory);
	}

	private static final class MyLocal extends ThreadLocal<Reference<SRes>> implements Disposable {
		MyLocal() {// visible
		}

		@Override
		protected Reference<SRes> initialValue() {
			return Objutil.nullRefence();
		}
		@Override
		public void destroy() {
			// nonthing
		}
	}

	final transient MyLocal srestlocal = new MyLocal();
	final SessionFactory sessionFactory;
	private final transient TxManager txManager;

	public XutilSessionContext(SessionFactoryImplementor factory) {
		SessionFactoryBean sfb = SessionFactoryBean.configTimeHolder.get();
		this.txManager = sfb.txManager;
		this.sessionFactory = factory;
		addFactory(this);
	}

	Disposable opensession() {
		if (TxManager.existActivTx())
			return srestlocal;
		SRes sres = srestlocal.get().get();
		if (sres != null)
			return srestlocal;
		return new SRes(false);
	}
	@Override
	public Session currentSession() {
		try {
			Session s = (Session) txManager.getConnection(this);
			if (s != null)
				return s;
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
		SRes sres = srestlocal.get().get();
		if (sres == null)
			throw new HibernateException("current session not exist.");
		return sres.getHandle();
	}
	@Override
	public int getCommitOrder() {
		return -1;
	}
	@Override
	public MResource newResource(TxInfo txinfo) throws Exception {
		SRes sres = srestlocal.get().get();
		if (sres != null)
			srestlocal.set(Objutil.<SRes> nullRefence());
		else
			sres = new SRes(true);
		sres.beginTransaction(txinfo);
		return sres;
	}

	private final class SRes implements MResource, Disposable {
		private final SWref swref;
		private Session realSession;
		private Transaction current;

		SRes(boolean createInTrx) {
			swref = new SWref(this, createInTrx);
			if (!createInTrx)
				srestlocal.set(swref);
		}

		private void flush() {
			if (!FlushMode.isManualFlushMode(realSession.getFlushMode()))
				realSession.flush();
		}
		@Override
		public void destroy() {
			swref.clear();
			if (realSession != null) {
				realSession.close();
				realSession = null;
			}
		}

		Session getRealSession() {
			if (realSession == null)
				realSession = sessionFactory.openSession();
			return realSession;
		}

		void beginTransaction(TxInfo info) {
			if (realSession == null)
				realSession = sessionFactory.openSession();
			else {
				flush();
				realSession.disconnect();
			}
			int timeout = info.getTimeout();
			if (timeout <= 0)
				current = realSession.beginTransaction();
			else {
				current = realSession.getTransaction();
				current.setTimeout(timeout);
				current.begin();
			}
		}
		@Override
		public MResourceFactory getFactory() {
			return XutilSessionContext.this;
		}
		@Override
		public Session getHandle() {
			if (swref.handle == null)
				swref.handle = (Session)Util.newInstance(constructor,new Object[]{swref});
			return swref.handle;
		}
		@Override
		public void afterCompletion() {
			current = null;
			if (swref.createInTrx)
				destroy();
			else
				srestlocal.set(swref);
		}
		@Override
		public void beforeCompletion() {
			flush();
		}
		@Override
		public void beforeSuspend() {
			flush();
		}
		@Override
		public void commit() throws Exception {
			current.commit();
		}
		@Override
		public void rollback() throws Exception {
			current.rollback();
		}

		@Override
		protected void finalize() throws Throwable {
			if (realSession != null) {
				realSession.close();
				Logger.LOG.warn("!!!!!! open session not close. !!!!!!");
			}
		}
	}

	private static final class SWref extends WeakReference<SRes> implements InvocationHandler {
		final boolean createInTrx;
		Session handle;

		SWref(SRes sres, boolean inTrx) {
			super(sres);
			createInTrx = inTrx;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Object ret = ProxyHandler.proxyObjectMethod(proxy, method, args);
			if (ret != null)
				return ret;
			String name = method.getName();
			if (name.equals("close"))
				return null;
			if (name.equals("beginTransaction") || name.equals("getTransaction"))
				throw new IllegalStateException("invalid method in session proxy: ".concat(name));
			return Util.invoke(Objutil.notNull(get(), "underly session closed.").getRealSession(), method, args);
		}
	}
}
