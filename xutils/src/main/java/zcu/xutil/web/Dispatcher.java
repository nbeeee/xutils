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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.utils.MethodInvocation;
import static zcu.xutil.Objutil.*;
import static zcu.xutil.Constants.*;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 * 
 */
public final class Dispatcher implements Filter {
	static final String extension = notEmpty(systring(XUTILS_WEB_ACTION_EXTENSION, ".do"), XUTILS_WEB_ACTION_EXTENSION);
	static final String objectsKey = notEmpty(systring(XUTILS_WEB_OBJECTS_KEY, "xwo"), XUTILS_WEB_OBJECTS_KEY);

	private Resolver[] resolvers;
	private String[] resolverNames;
	Map<String, String> nameToPermission;
	ServletContext servletCtx;
	Context context;

	@Override
	public void init(FilterConfig cfg) throws ServletException {
		context = Webutil.getAppContext(servletCtx = cfg.getServletContext());
		List<NProvider> res = context.getProviders(Resolver.class);
		int len = res.size();
		resolvers = new Resolver[len];
		resolverNames = new String[len];
		while (--len >= 0) {
			resolverNames[len] = res.get(len).getName();
			resolvers[len] = (Resolver) res.get(len).instance();
		}
		len = (res = context.getProviders(Action.class)).size();
		nameToPermission = new HashMap<String, String>(len);
		while (--len >= 0) {
			String name = res.get(len).getName();
			int i = name.indexOf(':');
			dupChkPut(nameToPermission, name.substring(0, i), name.substring(i+1));
		}
		Logger.LOG.info("inited: resolver={}  actions={}",resolverNames,nameToPermission);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		String name = Webutil.getFilename((HttpServletRequest) req);
		if (nameToPermission.containsKey(name) || getResolver(name) != null)
			new Executor((HttpServletRequest) req, (HttpServletResponse) resp).forward(name, null);
		else
			chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {
		// nothing
	}

	Resolver getResolver(String name) {
		int i = resolverNames.length;
		while (--i >= 0) {
			if (name.endsWith(resolverNames[i]))
				return resolvers[i];
		}
		return null;
	}

	private final class Executor extends WebContext implements Invocation {
		private final HttpServletRequest request;
		private final HttpServletResponse response;
		private String name;
		private MethodInvocation invoc;

		Executor(HttpServletRequest req, HttpServletResponse resp) {
			request = req;
			response = resp;
		}

		@Override
		public HttpServletRequest getRequest() {
			return request;
		}

		@Override
		public HttpServletResponse getResponse() {
			return response;
		}

		@Override
		public ServletContext getServletContext() {
			return servletCtx;
		}

		@Override
		public Context getContext() {
			return context;
		}

		@Override
		public String getActionName() {
			return name;
		}

		@Override
		public Map<String, String> inject(Object obj) {
			return Webutil.inject(request, obj);
		}

		@Override
		Invocation setMethodInvocation(MethodInvocation mi) {
			invoc = mi;
			return this;
		}

		@Override
		boolean forward(String view, Map<String, Object> model) throws ServletException, IOException {
			if (nameToPermission.containsKey(view)) {
				Objutil.validate(!view.equals(name), "repeat action: {}", view);
				name = view;
				if (model != null) {
					for (Entry<String, Object> entry : model.entrySet())
						request.setAttribute(entry.getKey(), entry.getValue());
				}
				View v = ((Action) context.getBean(view)).execute(this);
				invoc = null;
				if (v != null)
					v.handle(this);
				return true;
			}
			Resolver resolver = getResolver(view);
			if (resolver == null)
				return false;
			if (model == null)
				model = new HashMap<String, Object>();
			Objutil.dupChkPut(model, objectsKey, this);
			resolver.resolve(view, model, new RespWriter(response));
			return true;
		}

		@Override
		public Object getAction() {
			return invoc.getThis();
		}

		@Override
		public String getPermission() {
			return nameToPermission.get(name);
		}

		@Override
		public View proceed() throws ServletException, IOException {
			try {
				return (View) invoc.proceed();
			} catch (ServletException e) {
				throw e;
			} catch (IOException e) {
				throw e;
			} catch (Throwable e) {
				throw Objutil.rethrow(e);
			}
		}
	}

	private static class RespWriter extends Writer {
		private HttpServletResponse response;
		private PrintWriter printer;

		RespWriter(HttpServletResponse resp) {
			response = resp;
		}

		@Override
		public void close() throws IOException {
			flush();
			response = null;
			printer = null;
		}

		@Override
		public void flush() throws IOException {
			if (printer != null)
				printer.flush();
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			if (printer == null) {
				while (len > 0 && cbuf[off] <= ' ') {
					off++;
					len--;
				}
				if (len <= 0)
					return;
				if (response == null)
					throw new IOException("closed");
				if (response.getContentType() == null)
					response.setContentType("text/html; charset=UTF-8");
				printer = response.getWriter();
			}
			printer.write(cbuf, off, len);
		}
	}
}
