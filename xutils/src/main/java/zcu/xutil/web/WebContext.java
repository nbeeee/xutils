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
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.cfg.Context;
import zcu.xutil.utils.MethodInvocation;

public abstract class WebContext {
	public abstract HttpServletRequest getRequest();
	public abstract HttpServletResponse getResponse();
	public abstract ServletContext getServletContext();
	abstract Context getContext();
	abstract Invocation setInterceptorInvoc(MethodInvocation mi);
	abstract boolean forward(String view, Map<String, Object> model) throws ServletException, IOException;
}
