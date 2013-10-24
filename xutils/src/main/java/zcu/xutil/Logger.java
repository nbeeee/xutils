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
package zcu.xutil;

import java.util.logging.Level;

import static zcu.xutil.Constants.*;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public abstract class Logger {
	protected static final int DEBUG = 0, INFO = 1, WARN = 2, ERROR = 3;
	public static final Logger LOG = JULF.julf;

	public static Logger getLogger(Class clazz) {
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(String name) {
		if (JULF.FACTORY == null)
			JULF.init();
		return JULF.FACTORY.getLog(name);
	}

	public final void debug(String pattern) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, null, null);
	}

	public final void debug(String pattern, Object object) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, new Object[] { object }, null);
	}

	public final void debug(String pattern, Object object1, Object object2) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, new Object[] { object1, object2 }, null);
	}

	public final void debug(String pattern, Object... argArray) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, argArray, null);
	}

	public final void debug(String pattern, Throwable exception) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, null, exception);
	}

	public final void debug(String pattern, Throwable exception, Object object) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, new Object[] { object }, exception);
	}

	public final void debug(String pattern, Throwable exception, Object object1, Object object2) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, new Object[] { object1, object2 }, exception);
	}

	public final void debug(String pattern, Throwable exception, Object... argArray) {
		if (isLogable(DEBUG))
			log(DEBUG, pattern, argArray, exception);
	}

	public final void info(String pattern) {
		if (isLogable(INFO))
			log(INFO, pattern, null, null);
	}

	public final void info(String pattern, Object object) {
		if (isLogable(INFO))
			log(INFO, pattern, new Object[] { object }, null);
	}

	public final void info(String pattern, Object object1, Object object2) {
		if (isLogable(INFO))
			log(INFO, pattern, new Object[] { object1, object2 }, null);
	}

	public final void info(String pattern, Object... argArray) {
		if (isLogable(INFO))
			log(INFO, pattern, argArray, null);
	}

	public final void info(String pattern, Throwable exception) {
		if (isLogable(INFO))
			log(INFO, pattern, null, exception);
	}

	public final void info(String pattern, Throwable exception, Object object) {
		if (isLogable(INFO))
			log(INFO, pattern, new Object[] { object }, exception);
	}

	public final void info(String pattern, Throwable exception, Object object1, Object object2) {
		if (isLogable(INFO))
			log(INFO, pattern, new Object[] { object1, object2 }, exception);
	}

	public final void info(String pattern, Throwable exception, Object... argArray) {
		if (isLogable(INFO))
			log(INFO, pattern, argArray, exception);
	}

	public final void warn(String pattern) {
		if (isLogable(WARN))
			log(WARN, pattern, null, null);
	}

	public final void warn(String pattern, Object... argArray) {
		if (isLogable(WARN))
			log(WARN, pattern, argArray, null);
	}

	public final void warn(String pattern, Throwable exception) {
		if (isLogable(WARN))
			log(WARN, pattern, null, exception);
	}

	public final void warn(String pattern, Throwable exception, Object... argArray) {
		if (isLogable(WARN))
			log(WARN, pattern, argArray, exception);
	}

	public final void error(String pattern) {
		if (isLogable(ERROR))
			log(ERROR, pattern, null, null);
	}

	public final void error(String pattern, Object... argArray) {
		if (isLogable(ERROR))
			log(ERROR, pattern, argArray, null);
	}

	public final void error(String pattern, Throwable exception) {
		if (isLogable(ERROR))
			log(ERROR, pattern, null, exception);
	}

	public final void error(String pattern, Throwable exception, Object... argArray) {
		if (isLogable(ERROR))
			log(ERROR, pattern, argArray, exception);
	}

	public final boolean isDebugEnabled() {
		return isLogable(DEBUG);
	}

	public final boolean isInfoEnabled() {
		return isLogable(INFO);
	}

	public final boolean isWarnEnabled() {
		return isLogable(WARN);
	}

	public final boolean isErrorEnabled() {
		return isLogable(ERROR);
	}

	protected abstract boolean isLogable(int level);

	protected abstract void log(int level, String message, Object[] argArray, Throwable exception);

	private final static class JULF extends Logger implements LogFactory {
		static volatile LogFactory FACTORY;
		static final JULF julf = new JULF();

		static synchronized void init() {
			if (FACTORY == null) {
				String logcls = Objutil.systring(XUTILS_LOGCLASS,"zcu.xutil.misc.SLF4JLogFactory");
				try {
					FACTORY = (LogFactory) Objutil.contextLoader().loadClass(logcls).newInstance();
				} catch (Throwable e) {
					julf.info("class {} load fail. {}", logcls, e);
					FACTORY = julf;
				}
				julf.logger = FACTORY.getLog(Logger.class.getName());
				julf.info("Logging to: {}", FACTORY.getClass());
				Objutil.properties().remove(XUTILS_LOGCLASS);
			}
		}

		private Logger logger;
		@Override
		public Logger getLog(String name) {
			return new Julog(name);
		}

		@Override
		protected boolean isLogable(int level) {
			return logger == null || logger.isLogable(level);
		}

		@Override
		protected void log(int l, String m, Object[] argArray, Throwable t) {
			if (logger == null)
				Objutil.log(Logger.class, m, t, argArray);
			else
				logger.log(l, m, argArray, t);
		}
	}

	private static class Julog extends Logger {
		private static final Level[] levels = { Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE };

		private final java.util.logging.Logger logger;

		Julog(String name) {
			logger = java.util.logging.Logger.getLogger(name);
		}

		@Override
		protected boolean isLogable(int level) {
			return logger.isLoggable(levels[level]);
		}

		@Override
		protected void log(int level, String message, Object[] argArray, Throwable exception) {
			logger.logp(levels[level], null, null, Objutil.format(message, argArray), exception);
		}
	}
}
