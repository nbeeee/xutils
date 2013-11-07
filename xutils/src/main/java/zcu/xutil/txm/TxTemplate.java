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

import java.sql.Connection;
import java.util.concurrent.Callable;

import zcu.xutil.Objutil;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class TxTemplate {
	final Class[] rollbackfor;
	final Propagation propagate;
	int timeout = 0;
	int isolation = Connection.TRANSACTION_NONE;
	private TxManager txManager;

	public TxTemplate(Class... rollbackFor) {
		this(Propagation.REQUIRED, rollbackFor);
	}

	public TxTemplate(Propagation propagation, Class... rollbackFor) {
		this.propagate = Objutil.notNull(propagation,"propagation");
		this.rollbackfor = Objutil.notNull(rollbackFor, "rollbackFor");
	}

	public void setTimeout(int timeoutSeconds) {
		this.timeout = timeoutSeconds;
	}

	public void setIsolation(int iIsolation) {
		this.isolation = iIsolation;
	}

	public void setTxManager(TxManager manager) {
		if (txManager == null)
			txManager = manager;
		else if (txManager != manager)
			throw new IllegalArgumentException("txManager setted.");
	}

	@SuppressWarnings("unchecked")
	public <T> T execute(final Callable<T> callable) throws Exception {
		if (txManager == null)
			txManager = JdbcTxManager.instance;
		try {
			return (T) txManager.execute(new TxCall() {
				@Override
				public Object call() throws Throwable {
					return callable.call();
				}
				@Override
				public int getIsolation() {
					return isolation;
				}
				@Override
				public Propagation getPropagation() {
					return propagate;
				}
				@Override
				public boolean checkRollback(Throwable t) {
					for (Class c : rollbackfor)
						if (c.isInstance(t))
							return true;
					return false;
				}
				@Override
				public int getTimeOut() {
					return timeout;
				}
			});
		} catch (Throwable th) {
			if (th instanceof Error)
				throw (Error) th;
			throw (Exception) th;
		}
	}
}
