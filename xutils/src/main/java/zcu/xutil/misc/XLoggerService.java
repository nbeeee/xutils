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

import java.util.Iterator;

import org.slf4j.LoggerFactory;

import zcu.xutil.Constants;
import zcu.xutil.Objutil;
import zcu.xutil.msg.GroupService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class XLoggerService implements GroupService {
	private final LoggerContext context;

	public XLoggerService() {
		this.context = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		context.reset();
		configurator.setContext(context);
		try {
			configurator.doConfigure(Objutil.systring(Constants.XUTILS_MSG_LOGBACK_CONFIG,"loggerService.xml"));
		} catch (JoranException e) {
			throw new IllegalArgumentException(e);
		}
		StatusPrinter.printIfErrorsOccured(context);
		for(Logger l : context.getLoggerList()){
			Iterator<Appender<ILoggingEvent>> iter = l.iteratorForAppenders();
			while(iter.hasNext())
				if(iter.next() instanceof RemoteAppender)
					throw new IllegalArgumentException("not allow appender: ".concat(RemoteAppender.class.getName()));
		}
	}
	@Override
	public void service(String value, Object... params) {
		ILoggingEvent event = (ILoggingEvent) params[0];
		Logger remoteLogger = context.getLogger(event.getLoggerName());
		// apply the logger-level filter
		if (remoteLogger.isEnabledFor(event.getLevel())) {
			event.getLoggerContextVO().getPropertyMap().put("xaddr", value);
			remoteLogger.callAppenders(event);// finally log the event as if was
		}
	}
}
