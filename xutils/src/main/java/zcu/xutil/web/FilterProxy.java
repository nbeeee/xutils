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
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import zcu.xutil.Objutil;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.Provider;

public class FilterProxy implements Filter {
	private Filter[] filters;

	public void init(FilterConfig cfg) throws ServletException {
		List<String> targetNames = Objutil.split(cfg.getInitParameter("targetNames"), ',');
		int len = targetNames.size();
		Objutil.validate(len > 0, "InitParameter targetNames is empty");
		Context ctx = Webutil.getAppContext(cfg.getServletContext());
		filters = new Filter[len];
		for (int i = 0; i < len; i++) {
			String name = targetNames.get(i).trim();
			Provider p = Objutil.notNull(ctx.getProvider(name),"bean: {} not exist." , name);
			Objutil.validate(Filter.class.isAssignableFrom(p.getType()),"bena: {} is not Filter.",name);
			(filters[i] = (Filter) p.instance()).init(cfg);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		int len = filters.length;
		if (len == 0)
			chain.doFilter(request, response);
		else if (len == 1)
			filters[0].doFilter(request, response, chain);
		else
			new Chain(chain, filters).doFilter(request, response);
	}

	@Override
	public void destroy() {
		//nothing
	}

	private static final class Chain implements FilterChain {
		private final FilterChain chain;
		private final Filter[] filters;
		int cursor;

		Chain(FilterChain c, Filter[] f) {
			chain = c;
			filters = f;
		}

		@Override
		public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
			if (cursor < filters.length)
				filters[cursor++].doFilter(req, resp, this);
			else
				chain.doFilter(req, resp);
		}
	}
}
