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
package zcu.xutil.orm;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import zcu.xutil.Disposable;
import zcu.xutil.Objutil;
import zcu.xutil.web.Webutil;

public class OpenSessionFilter implements Filter {
	private HEntityManager entityManager;
	private String entityManagerName;
	private ServletContext servletContext;
	@Override
	public void doFilter(final ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		Disposable sessionCloser = getEntityManager().opensession();
		try {
			chain.doFilter(req, resp);
		} finally {
			sessionCloser.destroy();
		}
	}
	@Override
	public void init(FilterConfig config) throws ServletException {
		entityManagerName = Objutil.ifNull(config.getInitParameter("entityManagerName"), "entityManager");
		servletContext = config.getServletContext();
	}
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	private HEntityManager getEntityManager() {
		if (entityManager == null)
			entityManager = (HEntityManager) Webutil.getAppContext(servletContext).getBean(entityManagerName);
		return entityManager;
	}

	public void setEntityManager(HEntityManager em) {
		entityManager = em;
	}

}
