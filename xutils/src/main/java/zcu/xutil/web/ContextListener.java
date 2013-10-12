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
 * 用来配置 webapp {@link Context}. web.xml example:
 * 
 * <pre>
 * 
 * 	&lt;context-param&gt;
 * 		&lt;param-name&gt;xutils.web.config&lt;/param-name&gt;
 * 		&lt;param-value&gt;fileInWar.xml,path:fileInClassPath.xml,className&lt;/param-value&gt;
 * 	&lt;/context-param&gt;
 * 	&lt;listener&gt;
 * 		&lt;listener-class&gt;zcu.xutil.web.ContextListener&lt;/listener-class&gt;
 * 	&lt;/listener&gt;
 * 
 * </pre>
 * 
 * package.Config1.java example:
 * 
 * <pre>
 * 
 * public class Config1 extends zcu.xutil.cfg.CFG {
 * 
 * 	public void config(Binder b) {
 * 		sig(&quot;query&quot;, typ(Query.class, $(&quot;datasource&quot;)));
 * 		put(&quot;testaction&quot;, typ(TestAction.class, $(&quot;query&quot;)), null);
 * 		put(&quot;upload&quot;, typ(UploadAction.class), null);
 * 		put(&quot;resume&quot;, typ(ResumeAction.class, &quot;E:\\Light Music&quot;), null);
 * 
 * 	}
 * }
 * 
 * </pre>
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class ContextListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext sc = event.getServletContext();
		DefaultBinder b = new DefaultBinder("webapp", CFG.root());
		b.setEnv(ServletContext.class.getName(), sc);
		try {
			URL url = sc.getResource("/xutils-webapp.xml");
			if (url == null && (url = b.loader().getResource("xutils-webapp.xml")) == null)
				return;
			b.bind(url);
		} catch (MalformedURLException e) {
			throw new XutilRuntimeException(e);
		}
		Objutil.validate(sc.getAttribute(Webutil.XUTILS_WEBAPP_CONTEXT) == null, "webapp context present.");
		sc.setAttribute(Webutil.XUTILS_WEBAPP_CONTEXT, b.startup());
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
