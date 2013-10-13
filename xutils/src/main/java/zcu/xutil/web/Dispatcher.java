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

import zcu.xutil.Constants;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.cfg.Provider;
import zcu.xutil.utils.MethodInvocation;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 * 
 */
public final class Dispatcher implements Filter {
	public static final String extension;
	public static final String objectsKey;
	static {
		String s = Objutil.systring(Constants.XUTILS_WEB_ACTION_EXTENSION);
		extension = Objutil.isEmpty(s) ? ".do" : s;
		objectsKey = Objutil.isEmpty(s = Objutil.systring(Constants.XUTILS_WEB_OBJECTS_KEY)) ? "xwo" : s;
	}

	private Resolver[] resolvers;
	private String[] resolverNames;
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
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		String name = Webutil.getFilename((HttpServletRequest) req);
		if (name.endsWith(extension) || getResolver(name) != null)
			new Executor((HttpServletRequest) req, (HttpServletResponse) resp).forward(name, null);
		else
			chain.doFilter(req, resp);
	}

	Resolver getResolver(String name) {
		for (int i = resolverNames.length - 1; i >= 0; i--)
			if (name.endsWith(resolverNames[i]))
				return resolvers[i];
		return null;
	}

	@Override
	public void destroy() {
		// nothing
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
		public Object getAction() {
			return invoc.getThis();
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

		@Override
		public String getActionName() {
			return name;
		}

		@Override
		public Map<String, String> inject(Object obj) {
			return Webutil.inject(request, obj);
		}

		@Override
		Context getContext() {
			return context;
		}

		@Override
		Invocation setInterceptorInvoc(MethodInvocation mi) {
			invoc = mi;
			return this;
		}

		@Override
		boolean forward(String view, Map<String, Object> model) throws ServletException, IOException {
			if (view.endsWith(extension)) {
				view = view.substring(0, view.length() - extension.length());
				Provider provider = getContext().getProvider(view);
				if (provider == null || !Action.class.isAssignableFrom(provider.getType()))
					return false;
				Objutil.validate(!view.equals(name), "repeat action: {}", view);
				name = view;
				if (model != null) {
					for (Entry<String, Object> entry : model.entrySet())
						request.setAttribute(entry.getKey(), entry.getValue());
				}
				View v = ((Action) provider.instance()).execute(this);
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
			resolver.resolve(view, model, getWriter(response));
			return true;
		}
	}

	static Writer getWriter(final HttpServletResponse resp) {
		return new Writer() {
			PrintWriter printer;

			@Override
			public void close() throws IOException {
				// nothing
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
					if (resp.getContentType() == null)
						resp.setContentType("text/html; charset=UTF-8");
					printer = resp.getWriter();
				}
				printer.write(cbuf, off, len);
			}
		};
	}
}
