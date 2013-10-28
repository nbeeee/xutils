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

import java.util.concurrent.RejectedExecutionException;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;

public abstract class ServiceObject {
	static final Logger logger = Logger.getLogger(ServiceObject.class);
	final Handler handler;

	protected ServiceObject(Handler h) {
		handler = h;
	}

	public final Object handle(final Event event) throws Throwable {
		if (event.syncall)
			return invoke(event);
		try {
			handler.executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						invoke(event);
					} catch (UnavailableException e) {
						handler.eventDao.store(event, true);
						logger.warn("{} unavailable. recall latter", e, event.getName());
					} catch (Throwable e) {
						event.discardLogger(e.toString());
					}
				}
			});
			return null;
		} catch (RejectedExecutionException e) {
			logger.info("TOO MANY TASK. ", e);
			throw new UnavailableException("TOO MANY TASK. "+Objutil.systring(Constants.XUTILS_LOCALHOST));
		}
	}

	protected abstract Object invoke(Event event) throws Throwable;
}