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
package zcu.xutil.misc;

import org.slf4j.spi.LocationAwareLogger;

import zcu.xutil.LogFactory;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class SLF4JLogFactory implements LogFactory {
	private final org.slf4j.ILoggerFactory slffactory = org.slf4j.LoggerFactory.getILoggerFactory();

	@Override
	public Logger getLog(String name) {
		return new Log(slffactory.getLogger(name));
	}

	private static class Log extends Logger {
		private static final String FQCN = Logger.class.getName();
		private final org.slf4j.Logger logger;
		private final LocationAwareLogger lalog;

		Log(org.slf4j.Logger log) {
			this.logger = log;
			this.lalog = log instanceof LocationAwareLogger ? (LocationAwareLogger) log : null;
		}

		@Override
		protected final boolean isLogable(int level) {
			if (level == DEBUG)
				return logger.isDebugEnabled();
			if (level == INFO)
				return logger.isInfoEnabled();
			if (level == WARN)
				return logger.isWarnEnabled();
			if (level == ERROR)
				return logger.isErrorEnabled();
			return false;
		}

		@Override
		protected void log(int level, String message, Object[] argArray, Throwable exception) {
			if (lalog == null) {
				message = Objutil.format(message, argArray);
				if (level == DEBUG)
					logger.debug(message, exception);
				else if (level == INFO)
					logger.info(message, exception);
				else if (level == WARN)
					logger.warn(message, exception);
				else if (level == ERROR)
					logger.error(message, exception);
			} else {
				if (!lalog.isTraceEnabled()) {
					message = Objutil.format(message, argArray);
					argArray = null;
				}
				if (level == DEBUG)
					lalog.log(null, FQCN, LocationAwareLogger.DEBUG_INT, message, argArray, exception);
				else if (level == INFO)
					lalog.log(null, FQCN, LocationAwareLogger.INFO_INT, message, argArray, exception);
				else if (level == WARN)
					lalog.log(null, FQCN, LocationAwareLogger.WARN_INT, message, argArray, exception);
				else if (level == ERROR)
					lalog.log(null, FQCN, LocationAwareLogger.ERROR_INT, message, argArray, exception);

			}
		}
	}
}
