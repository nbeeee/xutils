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
import javax.transaction.Synchronization;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class TxManager {
	private static final ThreadLocal<TxObject> currentTx = new ThreadLocal<TxObject>();

	private static TxObject getCurrent() {
		TxObject current = currentTx.get();
		if (current == null) {
			if ((current = JtaTxManager.getExternalTxObject()) == null)
				return null;
			currentTx.set(current);
		} else if (current.getStatus() == STATUS_NO_TRANSACTION) {
			currentTx.set(null);
			return null;
		}
		return current;
	}

	public static boolean existActivTx() {
		return getCurrent() != null;
	}

	public static void registerSync(Synchronization sync) {
		TxObject current = getCurrent();
		if (current == null)
			throw new TxException("transaction not exist.");
		current.syncRegister(sync);
	}

	public final Object getConnection(MResourceFactory factory) throws Exception {
		TxObject current = getCurrent();
		if (current == null)
			return null;
		if (current.getManager() != this)
			throw new TxException("TxManager is not me.");
		return current.getResource(factory).getHandle();
	}

	public final Object execute(TxCall call) throws Throwable {
		TxObject suspend = null, current = getCurrent();
		if (current != null && current.getManager() != this)
			throw new TxException("TxManager is not me.");
		boolean boundary = false;
		switch (call.getPropagation()) {
		case MANDATORY:
			if (current == null)
				throw new TxException("none transaction for MANDATORY.");
			break;
		case REQUIRED:
			if (current == null) {
				currentTx.set(current = newTxObject(call.getTimeOut(), call.getIsolation()));
				boundary = true;
			}
			break;
		case REQUIRES_NEW:
			if (current != null)
				(suspend = current).suspend();
			try {
				currentTx.set(current = newTxObject(call.getTimeOut(), call.getIsolation()));
				boundary = true;
			} catch(Throwable e) {//RuntimeException or Error
				if (suspend != null)
					suspend.resume();
				throw e;
			}
			break;
		case NOT_SUPPORTED:
			if (current != null) {
				(suspend = current).suspend();
				currentTx.set(current = null);
			}
			break;
		case NEVER:
			if (current != null)
				throw new TxException("existing transaction for NEVER.");
			break;
		case SUPPORTS:
		}
		try {
			Object ret = call.call();
			if (current != null){
				TxObject tmp = current;
				current = null; //避免在 catch 中再次调用 complete
				tmp.complete(boundary, null, false);
			}
			return ret;
		} catch (Throwable t) {
			if (current != null)
				current.complete(boundary, t, t instanceof RuntimeException || t instanceof Error
						|| call.checkRollback(t));
			throw t;
		} finally {
			resume(suspend, boundary);
		}
	}

	private static void resume(TxObject suspend, boolean end) {
		if (suspend != null) {
			currentTx.set(suspend);
			suspend.resume();
		} else if (end)
			currentTx.set(null);
	}

	protected abstract TxObject newTxObject(int timeoutSecs, int isolation);
}
