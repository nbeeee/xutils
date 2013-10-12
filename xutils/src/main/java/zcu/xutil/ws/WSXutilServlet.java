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
package zcu.xutil.ws;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.Objutil;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.web.Webutil;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public class WSXutilServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private transient Context context;
	private transient WSServletDelegate delegate;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext sc = getServletContext();
		context =  Webutil.getAppContext(sc);
		String pattern = getInitParameter("urlPattern");
		if (pattern == null)
			pattern = "/{}.ws";
		ServletAdapterList l = new ServletAdapterList(sc);
		for (NProvider entry : context.getProviders(WSEndpoint.class)) {
			WSEndpoint endpoint = (WSEndpoint) entry.instance();
			l.createAdapter(entry.getName(), Objutil.format(pattern, entry.getName()), endpoint);
		}
		delegate = new WSServletDelegate(l, sc);
		sc.log(getServletName() + " inited. class: " + getClass().getName());
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		delegate.doPost(request, response, getServletContext());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		delegate.doGet(request, response, getServletContext());
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		delegate.doPut(request, response, getServletContext());
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		delegate.doDelete(request, response, getServletContext());
	}
}
