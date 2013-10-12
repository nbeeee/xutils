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
package zcu.xutil.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import zcu.xutil.Disposable;
import zcu.xutil.Logger;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;


/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class JMXManager implements Disposable {
	private final List<ObjectName> mbeans = new ArrayList<ObjectName>();
	private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	public void manage(Context context) {
		for (NProvider entry : context.listMe()) {
			Class<?> cls = entry.getType();
			MbeanResource anno = cls.getAnnotation(MbeanResource.class);
			if (anno != null)
				try {
					StringBuilder sb = new StringBuilder(64);
					if (anno.objectName().length() > 0)
						sb.append(anno.objectName()).append(",key=").append(entry.getName());
					else
						sb.append("xutils:name=").append(entry.getName());
					ObjectName name = new ObjectName(sb.toString());
					Object obj = entry.instance();
					registerMbean(obj instanceof DynamicMBean ? (DynamicMBean) obj : new SimpleDynamic(obj), name);
				} catch (JMException e) {
					Logger.LOG.info(toString(),e);
				}
		}
	}

	public void registerMbean(Object mbean, ObjectName objectName) throws JMException {
		ObjectInstance m = server.registerMBean(mbean, objectName);
		mbeans.add(m.getObjectName());
	}

	public MBeanServer getMBeanServer() {
		return server;
	}
	@Override
	public void destroy() {
		for (ObjectName objectName : mbeans) {
			try {
				server.unregisterMBean(objectName);
			} catch (JMException e) {
				Logger.LOG.info(toString(),e);
			}
		}
		mbeans.clear();
	}
}
