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

import javax.sql.DataSource;

import zcu.xutil.Objutil;
import zcu.xutil.msg.Broker;
import static zcu.xutil.Constants.*;

public class BrokerFactory {
	// singleton_name="yeepaytcp"
	private static final String CONFIG = "TCP(singleton_name=xutils:oob_thread_pool_enabled=false;enable_diagnostics=false):MPING:MERGE2:FD_SOCK:FD(timeout=10000;max_tries=3):"
			+ "VERIFY_SUSPECT:pbcast.NAKACK2(use_mcast_xmit=false):UNICAST2:pbcast.STABLE:pbcast.GMS:pbcast.FLUSH";

	private static BrokerFactory defaultFactory;

	private static volatile Broker instance;

	public static synchronized void setDefaultFactory(BrokerFactory factory) {
		Objutil.validate(instance == null, "Broker initiated");
		defaultFactory = factory;
	}

	private static synchronized void initiate() {
		if (instance == null)
			try {
				if (defaultFactory == null)
					instance = new BrokerFactory().getObject();
				else {
					instance = defaultFactory.getObject();
					defaultFactory = null;
				}
			} catch (Exception e) {
				throw Objutil.rethrow(e);
			}
	}

	public static Broker instance() {
		if (instance == null)
			initiate();
		return instance;
	}

	String clusterName = Objutil.systring(XUTILS_MSG_CHANNEL_ID, "groupmsg");
	int serverStamp = Objutil.systring(XUTILS_MSG_SERVERSTAMP,0);
	int clientStamp = Objutil.systring(XUTILS_MSG_CLIENTSTAMP,0);
	int maxPoolSize = Objutil.systring(XUTILS_MSG_MAXPOOLSIZE, 100);
	String config = Objutil.systring(XUTILS_MSG_CONFIG, CONFIG);
	String nodeName = Objutil.systring(XUTILS_MSG_NODENAME);
	DataSource datasource;

	public final void setDataSource(DataSource ds) {
		this.datasource = ds;
	}

	public final void setClusterName(String name) {
		this.clusterName = name;
	}

	public final void setServerStamp(int serverstamp) {
		this.serverStamp = serverstamp;
	}

	public final void setClientStamp(int clientstamp) {
		this.clientStamp = clientstamp;
	}

	public final void setMaxPoolSize(int maxpool) {
		this.maxPoolSize = maxpool;
	}

	public final void setConfig(String cfg) {
		this.config = cfg;
	}

	public final void setNodeName(String nodename) {
		this.nodeName = nodename;
	}

	public Broker getObject(){
		return new BrokerImpl(this);
	}
}
