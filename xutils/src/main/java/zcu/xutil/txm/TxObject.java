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

import static javax.transaction.Status.*;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import zcu.xutil.Logger;
import zcu.xutil.utils.Util;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class TxObject implements MResourceFactory.TxInfo {
	static final Logger logger = Logger.getLogger(TxObject.class);

	public static String nameOfStatus(int status) {
		switch (status) {
		case Status.STATUS_ACTIVE:
			return "ACTIVE";
		case Status.STATUS_COMMITTED:
			return "COMMITTED";
		case Status.STATUS_COMMITTING:
			return "COMMITTING";
		case Status.STATUS_MARKED_ROLLBACK:
			return "MARKED_ROLLBACK";
		case Status.STATUS_NO_TRANSACTION:
			return "NO_TRANSACTION";
		case Status.STATUS_PREPARED:
			return "PREPARED";
		case Status.STATUS_PREPARING:
			return "PREPARING";
		case Status.STATUS_ROLLEDBACK:
			return "ROLLEDBACK";
		case Status.STATUS_ROLLING_BACK:
			return "ROLLING_BACK";
		default:
			return "UNKNOWN";
		}
	}

	final List<MResource> mresources = new ArrayList<MResource>(4);
	private final List<Synchronization> syncs = new ArrayList<Synchronization>(4);
	private final long createMillis;
	private final int timeoutSecs;
	private final int isolation;

	protected TxObject(int timeoutSeconds, int iIsolation) {
		this.isolation = iIsolation;
		this.timeoutSecs = timeoutSeconds;
		this.createMillis = Util.now();
	}

	protected abstract TxManager getManager();

	protected abstract void resume();

	protected abstract void suspend();

	protected abstract void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SystemException;

	protected abstract void rollback() throws SystemException;

	final void complete(boolean end, Throwable t, boolean rollback) {
		try {
			if (end) {
				if (rollback || getStatus() == STATUS_MARKED_ROLLBACK)
					rollback();
				else
					commit();
			} else if (rollback)
				setRollbackOnly();
		} catch (Exception e) {
			if (t != null) {
				TxException te = new TxException(e.toString());
				te.initCause(t);
				throw te;
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new TxException(e);
		}
	}
	@Override
	public final int getTimeout() {
		if (timeoutSecs > 0) {
			int i = (int) ((Util.now() - createMillis) / 1000L);
			if ((i = timeoutSecs - i) > 0)
				return i;
			throw new TxException("transaction timeout.");
		}
		return 0;
	}
	@Override
	public final int getIsolation() {
		return isolation;
	}

	final void syncRegister(Synchronization sync) {
		int status = getStatus();
		if (status != STATUS_ACTIVE)
			throw new IllegalStateException(nameOfStatus(status));
		syncs.add(sync);
	}

	final MResource getResource(MResourceFactory factory) throws Exception {
		MResource resource;
		int i = mresources.size();
		while(--i >= 0){
			if ((resource = mresources.get(i)).getFactory().equals(factory))
				return resource;
		}
		if ((i = getStatus()) != STATUS_ACTIVE)
			throw new IllegalStateException(nameOfStatus(i));
		i = mresources.size();
		while(--i >= 0){
			if (factory.getCommitOrder() >= mresources.get(i).getFactory().getCommitOrder())
				break;
		}
		mresources.add(i+1, resource = factory.newResource(this));
		return resource;
	}

	public final void beforeCompletion() {
		for (int i = 0; i < syncs.size(); i++)
			syncs.get(i).beforeCompletion();
		for (int i = 0; i < mresources.size(); i++)
			mresources.get(i).beforeCompletion();
	}

	protected final void beforeSuspend() {
		for (int i = 0; i < mresources.size(); i++)
			mresources.get(i).beforeSuspend();
	}

	public final void afterCompletion(int status) {
		for (int i = 0; i < mresources.size(); i++)
			try {
				mresources.get(i).afterCompletion();
			} catch (Throwable e) {
				logger.warn("afterCompletion", e);
			}
		mresources.clear();
		for (int i = 0; i < syncs.size(); i++)
			try {
				syncs.get(i).afterCompletion(status);
			} catch (Throwable e) {
				logger.warn("afterCompletion", e);
			}
		syncs.clear();
		int millis = (int) (Util.now() - createMillis);
		if (millis > 500) {
			if (millis > 5000)
				logger.warn("transaction millis: {}", millis);
			else
				logger.info("transaction millis: {}", millis);
		}
	}
}
