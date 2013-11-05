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
package zcu.xutil.msg.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.Objutil;
import zcu.xutil.msg.Server;

public class ServiceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private transient Map<String, String> permissions;
	private transient BrokerImpl broker;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		List<String> list = Objutil.split(getInitParameter("xutils.msg.service"), ',');
		int i = list.size();
		if (i > 0) {
			permissions = new HashMap<String, String>();
			while (--i >= 0) {
				String s = list.get(i).trim().intern();
				permissions.put(s, s);
			}
		}
		broker =(BrokerImpl) BrokerFactory.instance();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.write("<html>\n<head>\n<title>节点和服务 </title>\n</head>\n<body>\n");
		out.write("<h3>允许对外的HTTP服务 </h3>\n");
		out.write(permissions == null ? "允许所有服务" : permissions.keySet().toString());
		out.write("\n<h3>正在访问节点 </h3><br>\n");
		out.write(broker.toString());
		out.write("\n<h3>所有节点 </h3><br>\n");
		int i = 0;
		
		for (Object member : broker.getMembers()) {
			if (i > 0) {
				out.write(i % 8 == 0 ? "<br>" : "&nbsp;&nbsp;&nbsp;");
				i++;
			}
			out.write(member.toString());
		}
		out.write("\n<h3>所有服务器 </h3>\n");
		for (Server s : broker)
			out.append(s.toString()).append("<br>");
		out.write("\n</body>\n</html>\n");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		boolean test = req.getParameter("Xtest")!=null;
		Event event = new Event();
		event.readFrom(new DataInputStream(req.getInputStream()));
		OutputStream out = resp.getOutputStream();
		if (permissions != null) {
			String s = permissions.get(event.getName());
			if (s == null) {
				out.write(Event.marshall(new UnavailableException("not permit: " + event.getName())));
				return;
			}
			event.setName(s);
		}
		out.write(broker.proxy(event,test));
	}
}
