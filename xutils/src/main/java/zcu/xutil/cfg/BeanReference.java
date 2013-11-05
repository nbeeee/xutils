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

import zcu.xutil.utils.Function;

/**
 * 工厂引用. 可作为配置时构造器和方法的参数,也可扩展为新的工厂.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class BeanReference {
	public final LifeCtrl put(Binder binder,String name, Class proxyIface, String interceptors) {
		return binder.put(false, name, get(), proxyIface, interceptors);
	}

	public final LifeCtrl put(Binder binder,String name) {
		return put(binder,name, null, null);
	}

	/**
	 * 将工厂以单例模式(cache)绑定 <br>
	 * 
	 * @see Binder#put(boolean, String, Provider, Class, String[])
	 */
	public final LifeCtrl uni(Binder binder,String name, Class proxyIface, String interceptors) {
		return binder.put(true, name, get(), proxyIface, interceptors);
	}

	public final LifeCtrl uni(Binder binder,String name) {
		return uni(binder,name, null,  null);
	}

	
	/**
	 * 将工厂引用扩展为新的工厂.通过工厂产品的方法创建一个新的工厂.
	 *
	 * @param product
	 *            新工厂的产品类型.如果为null则使用factoryMethod的返回类型
	 * @param factoryMethod
	 *            工厂方法
	 * @param args
	 *            工厂方法参数. 可以是{@link BeanReference} 或其他java 对象.
	 *
	 * @return RefCaller
	 */
	public final RefCaller ext(Class product, String factoryMethod, Object... args) {
		return Prototype.create(product, get(), factoryMethod, args);
	}

	public final RefCaller ext(String factoryMethod, Object... args) {
		return ext(null, factoryMethod, args);
	}

	public final RefCaller ext(Class product, Function function) {
		return Prototype.create(product, get(), function);
	}

	/**
	 * cast.参数类型转换.用于参数严格匹配
	 *
	 * @param toType
	 *            转换类型
	 */
	public final Object cast(Class toType) {
		Provider p = matches(toType, State.dummy);
		if (p != null)
			return	p instanceof BeanReference && p.getType()==toType ? p : new Cast(p, toType);
		if (allowForceCast())
			return new Cast(get(), toType);
		throw new ClassCastException(toString() + " can't cast to: " + toType.getName());
	}

	protected Provider matches(Class<?> toType, State state) {
		Provider provider = get();
		if (state.matched(provider.getType(), toType))
			return provider;
		if (toType != Provider.class || !state.converRequired())
			return null;
		state.convertMatch();
		return Instance.value(provider);
	}

	protected boolean allowForceCast() {
		return false;
	}

	protected abstract Provider get();

	@Override
	public String toString() {
		return getClass().getName() + '_' + get().getType().getName();
	}

	private static final class Cast extends BeanReference {
		private final Class type;
		private final Provider provider;

		Cast(Provider p, Class c) {
			type = c;
			provider = p;
		}

		@Override
		protected Provider matches(Class<?> toType, State state) {
			return state.matched(type,toType) ? provider : null;
		}

		@Override
		protected Provider get() {
			return provider;
		}

		@Override
		public String toString() {
			return provider.getType().getName() + " CAST " + type.getName();
		}
	}
}
