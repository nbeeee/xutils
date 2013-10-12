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

import zcu.xutil.utils.Util;

/**
 * 可调用的工厂引用. 用于IOC方法注入和实例初始化. 也有 {@link BeanReference} 的基本功能.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class RefCaller extends BeanReference implements Provider, Cloneable {

	/**
	 * 调用实例方法
	 *
	 * @param methodName
	 *            方法名
	 * @param args
	 *            方法参数,可以是{@link BeanReference} 或其他java 对象
	 * @return 返回 {@link RefCaller} 为继续操作.
	 */
	public abstract RefCaller call(String methodName, Object... args);

	/**
	 * 设置实例属性
	 *
	 * @param property
	 *            属性名
	 * @param object
	 *            属性值,可以是{@link BeanReference} 或其他java 对象.
	 * @return 返回 {@link RefCaller} 为继续操作.
	 */

	public final RefCaller set(String property, Object object) {
		return call(Util.nameOfSetter(property), object);
	}

	@Override
	protected final Provider get() {
		return this;
	}

	@Override
	public RefCaller clone() throws CloneNotSupportedException {
		return (RefCaller) super.clone();
	}
}
