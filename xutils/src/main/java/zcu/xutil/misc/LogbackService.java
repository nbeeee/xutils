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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogbackService implements LogService {
	private final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

	@Override
	public void log(String addr, ILoggingEvent event) {
		Logger remoteLogger = context.getLogger("loggerfor." + event.getLoggerName() + "." + addr);
		if (remoteLogger.isEnabledFor(event.getLevel())) {
			event.getLoggerContextVO().getPropertyMap().put("xaddr", addr);
			remoteLogger.callAppenders(event);// finally log the event as if was
		}
	}
}
