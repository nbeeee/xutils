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

import java.nio.charset.Charset;

import javax.sql.DataSource;

import zcu.xutil.Objutil;
import zcu.xutil.msg.SimpleBroker;
import zcu.xutil.utils.Base64;
import static zcu.xutil.Constants.*;

public class HttpBrokerFactory  {
	private static HttpBrokerFactory defaultFactory;
	private static volatile SimpleBroker instance;

	public static synchronized void setDefaultFactory(HttpBrokerFactory factory) {
		Objutil.validate(instance == null, "Broker initiated");
		defaultFactory = factory;
	}

	private static synchronized void initiate() {
		if (instance == null) {
			if (defaultFactory == null)
				instance = new HttpBrokerFactory().getObject();
			else {
				instance = defaultFactory.getObject();
				defaultFactory = null;
			}
		}
	}

	public static SimpleBroker instance() {
		if (instance == null)
			initiate();
		return instance;
	}

	String name = Objutil.systring(XUTILS_MSG_HTTP_ID, "httpmsg");
	String urls = Objutil.systring(XUTILS_MSG_HTTP_URLS);
	DataSource datasource;
	private String user = Objutil.systring(XUTILS_MSG_HTTP_USER, name);
	private String password = Objutil.systring(XUTILS_MSG_HTTP_PASSWORD);

	public final void setDataSource(DataSource ds) {
		this.datasource = ds;
	}

	public final void setUrls(String urlList) {
		this.urls = urlList;
	}

	public final void setName(String aName) {
		this.name = aName;
	}

	public final void setUser(String aUser) {
		this.user = aUser;
	}

	public final void setPassword(String pwd) {
		this.password = pwd;
	}

	protected String getCredentials() {
		return Objutil.isEmpty(password) ? null : "Basic " + Base64.encode((user + ":" + password).getBytes(Charset.forName("UTF-8")), false);
	}

	public SimpleBroker getObject() {
		return new HttpBrokerImpl(this);
	}
}
