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

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_COMMITTING;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_ROLLING_BACK;
import static javax.transaction.Status.STATUS_UNKNOWN;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

public class JdbcTxManager extends TxManager {
	public static final JdbcTxManager instance = new JdbcTxManager();

	private JdbcTxManager() { // nothing
	}

	@Override
	protected TxObject newTxObject(int timeoutSecs, int isolation) {
		return new JDBC(timeoutSecs, isolation);
	}

	private static final class JDBC extends TxObject {
		private int status;

		JDBC(int timeoutSecs, int iIsolation) {
			super(timeoutSecs, iIsolation);
			this.status = STATUS_ACTIVE;
		}

		@Override
		protected void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
				SystemException {
			try {
				beforeCompletion();
			} catch (RuntimeException e) {
				rollback();
				throw e;
			}
			if (status == STATUS_MARKED_ROLLBACK) {
				rollback();
				throw new RollbackException("MARKED_ROLLBACK");
			}
			if (status != STATUS_ACTIVE)
				throw new IllegalStateException(nameOfStatus(status));
			try {
				status = STATUS_COMMITTING;
				List<Exception> errors = new ArrayList<Exception>();
				for (int i = 0; i < mresources.size(); i++)
					try {
						mresources.get(i).commit();
					} catch (Exception e) {
						errors.add(e);
					}
				if (errors.isEmpty())
					status = STATUS_COMMITTED;
				else {
					status = STATUS_UNKNOWN;
					for (Exception e : errors)
						logger.warn("doCommit", e);
					throw new HeuristicMixedException(errors.toString());
				}
			} finally {
				afterCompletion(status);
			}
		}

		@Override
		protected void rollback() throws SystemException {
			if (status == STATUS_ROLLEDBACK || status == STATUS_COMMITTED || status == STATUS_UNKNOWN
					|| status == STATUS_NO_TRANSACTION)
				return;
			try {
				status = STATUS_ROLLING_BACK;
				List<Exception> errors = new ArrayList<Exception>();
				for (int i = 0; i < mresources.size(); i++) {
					try {
						mresources.get(i).rollback();
					} catch (Exception e) {
						errors.add(e);
					}
				}
				if (errors.isEmpty())
					status = STATUS_ROLLEDBACK;
				else {
					status = STATUS_UNKNOWN;
					for (Exception e : errors)
						logger.warn("doRollback", e);
					throw new SystemException("resources partly rollback fail.");
				}
			} finally {
				afterCompletion(status);
			}
		}

		@Override
		protected void resume() {
			// nothing
		}

		@Override
		protected void suspend() {
			beforeSuspend();
		}
		@Override
		public void setRollbackOnly() {
			if (status == STATUS_ACTIVE)
				status = STATUS_MARKED_ROLLBACK;
			else if (status != STATUS_MARKED_ROLLBACK && status != STATUS_ROLLING_BACK)
				throw new IllegalStateException(nameOfStatus(status));
		}
		@Override
		public int getStatus() {
			return status;
		}

		@Override
		protected TxManager getManager() {
			return instance;
		}
	}
}
