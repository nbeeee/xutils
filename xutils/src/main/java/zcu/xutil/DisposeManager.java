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
package zcu.xutil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a> *
 */
public final class DisposeManager implements Runnable, Comparator<Entry> {
	private static volatile Runnable terminator;
	private static volatile Thread shutdownHook;

	static {
		Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(new DisposeManager(), "shutdown"));
		try {
			install("INT");
			install("TERM");
		} catch (Throwable e) {
			Objutil.log(DisposeManager.class, "can't install sun.misc.SignalHandler.", e);
		}
	}

	@SuppressWarnings("restriction")
	private static void install(final String signal) {
		new sun.misc.SignalHandler() {
			private final sun.misc.SignalHandler old = sun.misc.Signal.handle(new sun.misc.Signal(signal), this);

			@Override
			public void handle(sun.misc.Signal sig) {
				runTerminator();
				if (old != SIG_DFL && old != SIG_IGN)
					old.handle(sig);
			}
		};
	}

	public static void setTerminateHandler(Runnable r) {
		terminator = r;
	}

	static void runTerminator() {
		Runnable r = terminator;
		if (r != null) {
			terminator = null;
			try {
				Thread worker = new Thread(r, "terminate");
				worker.start();
				worker.join(3000);
			} catch (Throwable e) { // nothing
			}
		}
	}

	public static void removeShutdownHook() {
		Thread t = shutdownHook;
		if (t != null) {
			shutdownHook = null;
			Runtime.getRuntime().removeShutdownHook(t);
		}
	}

	public static void register(Disposable target) {
		register(target, "destroy");
	}

	public static void register(Object target, String destroyMethod) {
		new Item(target, destroyMethod);
	}

	public static void register(Object target, String destroyMethod, int order) {
		new Item(target, destroyMethod, order);
	}

	@Override
	public void run() {
		runTerminator();
		Entry[] entrys = Item.disposes.entrySet().toArray(new Entry[0]);
		Arrays.sort(entrys, this);
		for (int i = entrys.length - 1; i >= 0; i--) {
			Object o = entrys[i].getKey();
			if (Item.disposes.remove(o) == null)
				continue;
			Item item = ((Item) entrys[i].getValue());
			Objutil.log(DisposeManager.class, "{} destroyed in order: {}", null, o, item.order);
			destroyCall(o, item.method);
		}
	}

	@Override
	public int compare(Entry o1, Entry o2) {
		int l = ((Item) o1.getValue()).order, r = ((Item) o2.getValue()).order;
		return l > r ? 1 : (l < r ? -1 : 0);
	}

	public static void destroyCall(Object o, String method) {
		Method m;
		try {
			(m = o.getClass().getMethod(method, (Class[]) null)).setAccessible(true);
			m.invoke(o, (Object[]) null);
		} catch (Throwable e) {
			Objutil.log(DisposeManager.class, "destroy exception.", e);
		}
	}

	private static class Item {
		private static volatile int counter;
		static final Map<Object, Item> disposes = Collections.synchronizedMap(new WeakHashMap<Object, Item>());
		final String method;
		final int order;

		Item(Object o, String s) {
			this(o, s, counter++);
		}

		Item(Object o, String s, int i) {
			method = s;
			order = i;
			disposes.put(o, this);
		}
	}
}
