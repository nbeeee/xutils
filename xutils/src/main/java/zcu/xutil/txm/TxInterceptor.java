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
package zcu.xutil.txm;

import zcu.xutil.utils.Checker;
import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.MethodInvocation;
import zcu.xutil.utils.Matcher;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class TxInterceptor implements Interceptor {
	private TxManager txManager;
	private Checker<? super Class<?>> pointcut = Matcher.annoInherit(Transactional.class);
	@Override
	public boolean checks(Class type) {
		return pointcut.checks(type);
	}

	public void setPointcut(Checker<? super Class<?>> aPointcut) {
		this.pointcut = aPointcut;
	}

	public void setTxManager(TxManager manager) {
		if (txManager == null)
			txManager = manager;
		else if (txManager != manager)
			throw new IllegalArgumentException("txManager setted.");
	}
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		Transactional anno = mi.getMethod().getAnnotation(Transactional.class);
		if (anno == null)
			anno = Matcher.getAnnotation(mi.getMethod().getDeclaringClass(),Transactional.class);
		if (anno == null)
			return mi.proceed();
		if (txManager == null)
			txManager = JdbcTxManager.instance;
		return txManager.execute(new Call(mi, anno));
	}

	private static final class Call implements TxCall {
		private final MethodInvocation invoc;
		private final Transactional txanno;

		Call(MethodInvocation mi, Transactional anno) {
			invoc = mi;
			txanno = anno;
		}
		@Override
		public Object call() throws Throwable {
			return invoc.proceed();
		}
		@Override
		public int getIsolation() {
			return txanno.isolation();
		}
		@Override
		public Propagation getPropagation() {
			return txanno.value();
		}
		@Override
		public boolean checkRollback(Throwable t) {
			for(Class c : txanno.rollbackfor())
				if(c.isInstance(t))
					return true;
			return false;
		}
		@Override
		public int getTimeOut() {
			return txanno.timeoutSecs();
		}
	}
}
