﻿/*
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
import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * forword. 转发View. 根据不同的扩展名转发到不同的View.如 :
 * '.jsp'转发到RequestDispatcher.<br>
 * 
 */
public final class Forward extends HashMap<String, Object> implements View {
	private static final long serialVersionUID = 1L;
	private final String page;

	public Forward(String uri) {
		this.page = uri;
	}

	@Override
	public void handle(WebContext vc) throws ServletException, IOException {
		if (!vc.forward(page, this)) {
			HttpServletRequest request = vc.getRequest();
			for (Entry<String, Object> entry : entrySet())
				request.setAttribute(entry.getKey(), entry.getValue());
			RequestDispatcher disp = request.getRequestDispatcher(page);
			if (disp == null)
				throw new ServletException("none dispatcher for: " + page);
			if (!vc.getResponse().isCommitted() && request.getAttribute(Webutil.INCLUDE_REQUEST_URI) == null)
				disp.forward(request, vc.getResponse());
			else
				disp.include(request, vc.getResponse());
		}
	}

	public Forward add(String key, Object value) {
		put(key, value);
		return this;
	}
}
