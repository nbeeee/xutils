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

import java.io.File;
import java.net.URL;

import zcu.xutil.Constants;
import zcu.xutil.Objutil;
import static zcu.xutil.Objutil.*;

/**
 * 
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 * 
 */
public class CFG implements Config {
	private static volatile Context root;

	protected CFG() {
		// nothing
	}

	public static Context root() {
		if (root == null) {
			synchronized (CFG.class) {
				if (root == null) {
					String s = systring(Constants.XUTILS_CFG_ROOT, "").trim();
					root = build(null, isEmpty(s) ? new CFG() : (Config) newInstance(loadclass(contextLoader(), s)));
				}
			}
		}
		return root;
	}

	public static final Context build(Context parent, Config config) {
		DefaultBinder b = new DefaultBinder(config.toString(), parent, config.getClass().getClassLoader());
		try {
			config.config(b);
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
		return b.startup();
	}

	/**
	 * 创建原型可调用的工厂引用 {@link RefCaller}
	 * 
	 * @param product
	 *            输出产品类型.
	 * @param factory
	 *            工厂类。
	 * @param factoryMethod
	 *            工厂方法，静态方法。
	 * @param args
	 *            工厂方法参数,可以是{@link BeanReference} 或其他java 对象.
	 * 
	 * @return 可调用的工厂引用 {@link RefCaller}，调用作用于输出产品(而非工厂factory)。
	 */
	public static RefCaller ext(Class product, Class factory, String factoryMethod, Object... args) {
		return Prototype.create(product, factory, factoryMethod, args);
	}

	/**
	 * 创建原型可调用的工厂引用 {@link RefCaller}
	 * 
	 * @param type
	 *            类型,用于构造对象.
	 * @param objects
	 *            构造器参数,可以是{@link BeanReference} 或其他java 对象.
	 * 
	 * @return 可调用的工厂引用 {@link RefCaller}
	 */
	public static RefCaller typ(Class type, Object... objects) {
		return Prototype.create(type, objects);
	}

	public static RefCaller typ(Class type) {
		return Prototype.create(type, null);
	}

	/**
	 * 创建值型可调用的工厂引用 {@link RefCaller}
	 * 
	 * @param value
	 *            将在此对象调用方法和设置属性.
	 * 
	 * @return 可调用的工厂引用 {@link RefCaller}
	 */
	public static RefCaller val(Object value) {
		return Instance.value(value);
	}

	/**
	 * 创建数组参数
	 * 
	 * @param componentType
	 *            元素类型
	 * @param objects
	 *            数组元素,可以是{@link BeanReference} 或其他java 对象
	 * 
	 * @return 数组工厂引用 {@link Xarray}
	 */
	public static Xarray arr(Class componentType, Object... objects) {
		return new Xarray(componentType, objects);
	}

	@Override
	public void config(Binder b) throws Exception {
		File file = new File(systring(Constants.XUTILS_HOME), "xutils-root.xml");
		URL url = file.exists() ? toURL(file) : getClass().getClassLoader().getResource("xutils-root.xml");
		if (url != null)
			b.bind(url);
	}
}
