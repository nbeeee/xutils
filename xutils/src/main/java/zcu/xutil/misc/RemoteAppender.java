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

import zcu.xutil.Constants;
import zcu.xutil.Objutil;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.msg.impl.HttpBrokerFactory;
import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public final class RemoteAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
	private volatile boolean sendprefer = true;
	private volatile boolean httpmode;
	private volatile GroupService service;
	private boolean includeCallerData;
	private final LoggingEventPreSerializationTransformer pst = new LoggingEventPreSerializationTransformer();
	private final String address = Objutil.systring(Constants.XUTILS_LOCALHOST);

	@Override
	protected void append(ILoggingEvent event) {
	    if (event == null)
	        return;
	    if(includeCallerData)
	    	event.getCallerData();
	    getService().service(address, pst.transform(event));
	}

	private GroupService getService() {
		if (service == null)
			synchronized (this) {
				if (service == null)
					service = ((httpmode ? HttpBrokerFactory.instance() : BrokerFactory.instance())).create(XLoggerService.class.getName(), sendprefer,0);
			}
		return service;
	}

	public void setSendprefer(boolean b) {
		this.sendprefer = b;
	}

	public void setHttpmode(boolean b) {
		this.httpmode = b;
	}

	public void setIncludeCallerData(boolean b) {
		this.includeCallerData = b;
	}
}
