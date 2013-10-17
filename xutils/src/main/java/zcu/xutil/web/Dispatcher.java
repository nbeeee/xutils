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

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 * 
 */
public final class Dispatcher implements Filter {
	private Resolver[] resolvers;
	private String[] resolverNames;
	Map<String, String[]> namesMap;
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
		namesMap = new HashMap<String, String[]>(len);
		while (--len >= 0) {
			String bean = res.get(len).getName();
			int i = bean.indexOf('/');
			String key = notEmpty(i < 0 ? bean : bean.substring(0, i), "invalid action beanName.");
			String perm = i < 0 ? null : bean.substring(i+1);
			validate(namesMap.put(key, new String[]{bean,perm})==null, "duplicated name: {}",bean);
		}
		Logger.LOG.info("inited: resolver={}  actions={}", resolverNames, namesMap);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		String name = Webutil.getFilename((HttpServletRequest) req);
		if (namesMap.containsKey(name) || getResolver(name) != null)
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
			if (namesMap.containsKey(view)) {
				Objutil.validate(!view.equals(name), "repeat action: {}", view);
				name = view;
				if (model != null) {
					for (Entry<String, Object> entry : model.entrySet())
						request.setAttribute(entry.getKey(), entry.getValue());
				}
				View v = ((Action) context.getBean(namesMap.get(view)[0])).execute(this);
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
			validate(model.put("xwbo", this) == null, "reserved key 'xwbo'");
			resolver.resolve(view, model, new RespWriter(response));
			return true;
		}

		@Override
		public Object getAction() {
			return invoc.getThis();
		}

		@Override
		public String getPermission() {
			return namesMap.get(name)[1];
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
