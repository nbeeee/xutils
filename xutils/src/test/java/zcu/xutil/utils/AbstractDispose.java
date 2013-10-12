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
package zcu.xutil.utils;

import java.nio.ByteBuffer;

import zcu.xutil.Disposable;
import zcu.xutil.DisposeManager;
import zcu.xutil.Logger;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class AbstractDispose implements Disposable {
	private volatile boolean destroyed;

	protected AbstractDispose() {
		DisposeManager.register(this);
	}

	public final void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		doDestroy();
	}

	public final void validDestroyed() {
		if (destroyed)
			throw new IllegalStateException(getClass().getName());
	}

	public final boolean isDestroyed() {
		return destroyed;
	}

	protected abstract void doDestroy();

	@Override
	protected void finalize() throws Throwable {
		if (!destroyed) {
			Logger.LOG.warn("{} !!!!!!!!! destroy by finalize !!!!!!!!!", getClass().getName());
			destroy();
		}
	}
}
