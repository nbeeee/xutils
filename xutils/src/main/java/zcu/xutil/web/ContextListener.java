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
	static URL getConfig(ServletContext sc) {
		try {
			URL url = sc.getResource("/WEB-INF/xutils-webapp.xml");
			return url != null ? url : Objutil.contextLoader().getResource("xutils-webapp.xml");
		} catch (MalformedURLException e) {
			throw new XutilRuntimeException(e);
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext sc = event.getServletContext();
		URL url = getConfig(sc);
		if (url != null) {
			DefaultBinder b = new DefaultBinder("webapp", CFG.root());
			b.setEnv(ServletContext.class.getName(), sc);
			b.bind(url);
			Objutil.validate(sc.getAttribute(Webutil.XUTILS_WEBAPP_CONTEXT) == null, "webapp context present.");
			sc.setAttribute(Webutil.XUTILS_WEBAPP_CONTEXT, b.startup());
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
