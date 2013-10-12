package zcu.xutil.cfg;

import zcu.xutil.Objutil;

abstract class Cache {
	static final Object[] empty = {};
	volatile Object cache;

	final Object get() {
		if (cache == this)
			init();
		return cache;
	}

	abstract void init();

	static Cache cache(final Provider provider) {
		return new Cache() {
			Provider factory;
			{
				if (provider != null) {
					cache = this;
					factory = provider;
				}
			}
			@Override
			synchronized void init() {
				if (cache == this) {
					Provider f = Objutil.notNull(factory, "reentry.");
					factory = null;
					cache = f.instance();
				}
			}
		};
	}

	static Cache cache(final Provider[] providers) {
		return new Cache() {
			Provider[] factorys;
			{
				if (providers != null) {
					if (providers.length == 0)
						cache = empty;
					else {
						cache = this;
						factorys = providers;
					}
				}
			}
			@Override
			synchronized void init() {
				if (cache == this) {
					Provider[] arr = Objutil.notNull(factorys, "reentry.");
					factorys = null;
					int len = arr.length;
					Object[] array = new Object[len];
					while (--len >= 0)
						array[len] = arr[len].instance();
					cache = array;
				}
			}
		};
	}
}
