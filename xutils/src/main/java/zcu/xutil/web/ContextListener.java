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
package zcu.xutil.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.DefaultBinder;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class ContextListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext sc = event.getServletContext();
		URL url;
		try {
			if ((url = sc.getResource("/xutils-webapp.xml")) == null)
				 url = Objutil.contextLoader().getResource("xutils-webapp.xml");
		} catch (MalformedURLException e) {
			throw new XutilRuntimeException(e);
		}
		Context context = CFG.root();
		if(url != null){
			DefaultBinder b = new DefaultBinder("webapp",context);
			b.setEnv(ServletContext.class.getName(), sc);
			b.bind(url);
			context =  b.startup();
			Objutil.validate(sc.getAttribute(Webutil.XUTILS_WEBAPP_CONTEXT) == null, "webapp context present.");
			sc.setAttribute(Webutil.XUTILS_WEBAPP_CONTEXT, context);
		}
		if (sc.getMajorVersion() >= 3 && sc.getEffectiveMajorVersion() >=3){
			if(!context.getProviders(Action.class).isEmpty() || !context.getProviders(Resolver.class).isEmpty())
				sc.addFilter("dispatcher",Dispatcher.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		ServletContext servletContext = event.getServletContext();
		Object o = servletContext.getAttribute(Webutil.XUTILS_WEBAPP_CONTEXT);
		if (o instanceof Context) {
			servletContext.removeAttribute(Webutil.XUTILS_WEBAPP_CONTEXT);
			((Context) o).destroy();
		}
	}
}
