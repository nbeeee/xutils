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
	public static final String extension = Objutil.systring(Constants.XUTILS_WEB_ACTION_EXTENSION, ".do");
	ServletContext servletCtx;
	Context context;
	Filter[] filters;
	Resolver[] resolvers;
	String[] resolverNames;

	@Override
	public void init(FilterConfig cfg) throws ServletException {
		context = Webutil.getAppContext(servletCtx = cfg.getServletContext());
		List<NProvider> res = context.getProviders(Filter.class);
		int len = res.size();
		filters = new Filter[len];
		while (--len >= 0)
			(filters[len] = (Filter) res.get(len).instance()).init(cfg);
		len = (res = context.getProviders(Resolver.class)).size();
		resolvers = new Resolver[len];
		resolverNames = new String[len];
		while (--len >= 0) {
			resolverNames[len] = res.get(len).getName();
			resolvers[len] = (Resolver) res.get(len).instance();
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, final FilterChain chain) throws IOException,
			ServletException {
		new Executor(chain).doFilter(req, resp);
	}

	@Override
	public void destroy() {
		// nothing
	}

	private final class Executor extends WebContext implements Invocation, FilterChain {
		private final FilterChain chain;
		private int cursor;
		private HttpServletRequest request;
		private HttpServletResponse response;
		private String name;
		private MethodInvocation invoc;

		Executor(FilterChain fc) {
			chain = fc;
		}

		@Override
		public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
			if (cursor < filters.length)
				filters[cursor++].doFilter(req, resp, this);
			else {
				request = (HttpServletRequest) req;
				response = (HttpServletResponse) resp;
				if (!forward(Webutil.getFilename(request), null))
					chain.doFilter(req, resp);
			}
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
			for (int i = resolverNames.length - 1; i >= 0; i--)
				if (view.endsWith(resolverNames[i])) {
					if (model == null)
						model = new HashMap<String, Object>();
					Objutil.dupChkPut(model, "request", request);
					Objutil.dupChkPut(model, "response", response);
					Objutil.dupChkPut(model, "application", servletCtx);
					resolvers[i].resolve(view, model, new Writer() {
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
								HttpServletResponse resp = getResponse();
								if (resp.getContentType() == null)
									resp.setContentType("text/html; charset=UTF-8");
								printer = resp.getWriter();
							}
							printer.write(cbuf, off, len);
						}
					});
					return true;
				}
			return false;
		}
	}
}
