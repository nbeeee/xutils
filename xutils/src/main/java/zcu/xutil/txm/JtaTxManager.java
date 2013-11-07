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

import static javax.transaction.Status.STATUS_NO_TRANSACTION;

import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import zcu.xutil.Objutil;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class JtaTxManager extends TxManager {
	private static final AtomicReference<JtaTxManager> jtaref = new AtomicReference<JtaTxManager>();

	static final TxObject getExternalTxObject() {
		JtaTxManager jta = jtaref.get();
		if (jta == null)
			return null;
		try {
			UserTransaction ut = jta.getUserTransaction();
			if (ut.getStatus() == STATUS_NO_TRANSACTION)
				return null;
			return jta.new JTA(0, 0);
		} catch (RollbackException e) {
			throw new TxException(e);
		} catch (SystemException e) {
			throw new TxException(e);
		}
	}

	private final TransactionManager transactionManager;
	private final UserTransaction userTransaction;
	final TransactionSynchronizationRegistry tsyncRegister;

	public JtaTxManager(UserTransaction ut) {
		this(ut, null, null);
	}

	public JtaTxManager(UserTransaction ut, TransactionSynchronizationRegistry tsr) {
		this(ut, tsr, null);
	}

	public JtaTxManager(UserTransaction ut, TransactionSynchronizationRegistry tsr, TransactionManager tm) {
		if (tm == null && ut instanceof TransactionManager)
			tm = (TransactionManager) ut;
		if (ut == null && tm instanceof UserTransaction)
			ut = (UserTransaction) tm;
		userTransaction = Objutil.notNull(ut, "UserTransaction");
		transactionManager = tm;
		tsyncRegister = tsr;
		Objutil.validate(jtaref.compareAndSet(null, this), "JtaTxManager existed");
	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	public TransactionManager getTransactionManager() {
		return Objutil.notNull(transactionManager, "TransactionManager not set");
	}

	@Override
	protected TxObject newTxObject(int timeoutSecs, int isolation) {
		try {
			UserTransaction ut = getUserTransaction();
			if (timeoutSecs > 0)
				ut.setTransactionTimeout(timeoutSecs);
			ut.begin();
			return new JTA(timeoutSecs, isolation);
		} catch (RollbackException e) {
			throw new TxException(e);
		} catch (NotSupportedException e) {
			throw new TxException(e);
		} catch (SystemException e) {
			throw new TxException(e);
		}
	}

	protected final class JTA extends TxObject implements Synchronization {
		private Transaction suspend;

		protected JTA(int timeoutSecs, int isolation) throws RollbackException, SystemException {
			super(timeoutSecs, isolation);
			if (tsyncRegister == null)
				getTransactionManager().getTransaction().registerSynchronization(this);
			else
				tsyncRegister.registerInterposedSynchronization(this);
		}

		@Override
		protected void resume() {
			try {
				getTransactionManager().resume(suspend);
			} catch (InvalidTransactionException e) {
				throw new TxException(e);
			} catch (SystemException e) {
				throw new TxException(e);
			} finally {
				suspend = null;
			}
		}

		@Override
		protected void suspend() {
			beforeSuspend();
			try {
				suspend = getTransactionManager().suspend();
			} catch (SystemException e) {
				throw new TxException(e);
			}
		}

		@Override
		protected void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
				SystemException {
			getUserTransaction().commit();
		}

		@Override
		protected void rollback() throws SystemException {
			getUserTransaction().rollback();
		}
		@Override
		public void setRollbackOnly() {
			Objutil.validate(suspend == null, "suspended.");
			try {
				getUserTransaction().setRollbackOnly();
			} catch (SystemException e) {
				throw new TxException(e);
			}
		}
		@Override
		public int getStatus() {
			try {
				return suspend == null ? getUserTransaction().getStatus() : STATUS_NO_TRANSACTION;
			} catch (SystemException e) {
				throw new TxException(e);
			}
		}

		@Override
		protected TxManager getManager() {
			return JtaTxManager.this;
		}
	}
}
