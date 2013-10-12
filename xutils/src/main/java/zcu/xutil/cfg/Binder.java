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
package zcu.xutil.cfg;

import java.net.URL;

/**
 * The Interface Binder.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface Binder{
	/**
	 * 邦定工厂<br>
	 *
	 * @param cache
	 *            是否单例cache<br>
	 * @param name
	 *            工厂名.空串为匿名临时工厂,以'@'开头的为为临时工厂,临时工厂用于DI.
	 * @param provider
	 *            被邦定的工厂
	 * @param proxyIface
	 *            代理接口, null 代理所有接口。
	 * @param interceptors
	 *            拦截器名, empty 代理自动匹配拦截器.<br>
	 *            proxyIface==null && interceptors==null 无拦截器(不代理).
	 *
	 */
	LifeCtrl put(boolean cache, String name, Provider provider, Class<?> proxyIface, String[] interceptors);

	/**
	 *
	 * @see Binder#put(boolean, String, Provider, Class, String[])
	 */
	LifeCtrl put(boolean cache, String name, Provider provider);

	/**
	 * 从Binder中获得工厂引用.先从容器中查找，如果没有，再查{@link Binder#getEnv(String)}环境<br>
	 *
	 * @param name
	 *            被引用对象的名字.
	 *
	 * @return 工厂引用 {@link BeanReference}
	 *
	 * @throws {@link NoneBeanException}如果当前容器和环境属性都不存在名为name的对象
	 */

	BeanReference ref(String name);

	/**
	 *
	 * 判断Bean名字是否存在于容器中.
	 *
	 */
	boolean exist(String beanName);

	/**
	 * 绑定xml配置文件<br>
	 *
	 * @param url
	 *            配置文件路径.
	 * @param loader
	 *            配置文件类加载器.
	 *
	 */
	void bind(URL url);

	/**
	 *
	 * 获得现行Binder环境 .
	 *
	 */

	Object getEnv(String name);
	/**
	 *
	 * 设置现行Binder环境 .
	 *
	 */
	void setEnv(String name,Object value);
	
	ClassLoader loader();
}
