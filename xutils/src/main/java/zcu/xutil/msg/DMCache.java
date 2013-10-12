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
package zcu.xutil.msg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgroups.Address;

import zcu.xutil.Objutil;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.utils.EvictListener;
import zcu.xutil.utils.Function;
import zcu.xutil.utils.LRUCache;
import zcu.xutil.utils.Util;

/**
 * Distributed MultiKey Cache. 可以按多个键值查询的分布式cache。<br>
 * 当一个节点更新或删除时可通知其他节点将缓存元素逐出。分布式cache主键必须序列化<br>
 * 应用场景：按主键 userid 和别名　username　查询　User
 *
 * <pre>
 *
 * DMCache&lt;Long, User&gt; primaryCache = new DMCache&lt;Long, User&gt;(16, &quot;x.b.user&quot;, 60);
 * Alias&lt;String, User&gt; cacheByUserName = primaryCache.createAlias(new Function&lt;User, String&gt;() {
 * 	public String apply(User v) {
 * 		return v.getName();
 * 	}
 * });
 * User u = cacheByUserName.get(&quot;xiaozaichu&quot;);
 * if (u == null) {
 * 	u = new User(12345, &quot;xiaozaichu&quot;);
 * 	primaryCache.put(12345, u, false);
 * }
 * User u2 = cacheByUserName.get(&quot;xiaozaichu&quot;);
 * User u3 = primaryCache.get(12345);
 * </pre>
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public final class DMCache<K, V> {
	private final int timeoutMillis;
	private final LRUCache<K, Node<V>> cache;
	private final Listener listener;
	private volatile Alias<?, V>[] array;
	private List<Alias> childs;

	/**
	 * @param cacheSize
	 *            缓存大小，超过时最不常用的元素将被逐出。
	 * @param uniqueID
	 *            分布式缓存的标识，建议用缓存对象的类名。为空时无分布式效果。
	 * @param timeoutSeconds
	 *            0 不做超时检查。
	 */
	public DMCache(int cacheSize, String uniqueID, int timeoutSeconds) {
		this.timeoutMillis = timeoutSeconds <= 0 || timeoutSeconds > Integer.MAX_VALUE / 1000 ? 0
				: timeoutSeconds * 1000;
		this.listener = new Listener(uniqueID);
		this.cache = new LRUCache<K, Node<V>>(cacheSize, listener);
		this.childs = new ArrayList<Alias>();
	}

	public V get(Object primaryKey) {
		Node<V> node = cache.get(primaryKey);
		if (node == null)
			return null;
		if (timeoutMillis == 0 || node.untilMills >= Util.now())
			return node.value;
		if (cache.remove(primaryKey, node))
			removeAlias(node);
		return null;
	}

	public V putIfAbsent(K primaryKey, V value) {
		Alias<?, V>[] alias = getAlias();
		Node<V> node = new Node<V>(value, alias, timeoutMillis);
		Object[] keys = node.aliasKeys;
		node = cache.putIfAbsent(primaryKey, node);
		if (node != null)
			return node.value;
		for (int i = alias.length - 1; i >= 0; i--)
			alias[i].aliasPrimaryMap.put(keys[i], primaryKey);
		return null;
	}

	public V put(K primaryKey, V value, boolean updateNotify) {
		Alias<?, V>[] alias = getAlias();
		Node<V> node = new Node<V>(value, alias, timeoutMillis);
		Object[] keys = node.aliasKeys;
		node = cache.put(primaryKey, node);
		for (int i = alias.length - 1; i >= 0; i--) {
			alias[i].aliasPrimaryMap.put(keys[i], primaryKey);
			if (node != null && !keys[i].equals(node.aliasKeys[i]))
				alias[i].aliasPrimaryMap.remove(node.aliasKeys[i]);
		}
		if (updateNotify)
			listener.multicast(primaryKey);
		return node == null ? null : node.value;
	}

	public void remove(Object primaryKey, boolean removeNotify) {
		removeAlias(cache.remove(primaryKey));
		if (removeNotify)
			listener.multicast(primaryKey);
	}

	public void clear(boolean clearNotify) {
		cache.clear();
		for (Alias a : getAlias())
			a.aliasPrimaryMap.clear();
		if (clearNotify)
			listener.multicast(null);
	}

	public synchronized <T> Alias<T, V> createAlias(Function<V, T> aliasKeyGetter) {
		Alias<T, V> ret = new Alias<T, V>(this, aliasKeyGetter);
		Objutil.notNull(childs, "DMCache has been used.").add(ret);
		return ret;
	}

	public synchronized void setBroker(Broker broker) {
		Objutil.validate(array == null, "started.");
		listener.broker = broker;
	}

	@SuppressWarnings("unchecked")
	private Alias<?, V>[] getAlias() {
		if (array == null)
			synchronized (this) {
				if (array == null) {
					listener.start();
					array = childs.toArray(new Alias[childs.size()]);
					childs = null;
				}
			}
		return array;
	}

	void removeAlias(Node<V> node) {
		if (node == null)
			return;
		Alias<?, V> alias[] = getAlias();
		Object keys[] = node.aliasKeys;
		for (int i = keys.length - 1; i >= 0; i--)
			alias[i].aliasPrimaryMap.remove(keys[i]);
	}

	public static final class Alias<A, E> {
		private final DMCache<?, E> base;
		private final Function<E, A> func;
		final Map<Object, Object> aliasPrimaryMap;

		Alias(DMCache<?, E> dmc, Function<E, A> f) {
			this.base = dmc;
			this.func = f;
			this.aliasPrimaryMap = Collections.synchronizedMap(new HashMap<Object, Object>());
		}

		public E get(A aliasKey) {
			Object primaryKey = aliasPrimaryMap.get(aliasKey);
			if (primaryKey == null)
				return null;
			E e = base.get(primaryKey);
			if (e == null)
				aliasPrimaryMap.remove(aliasKey);
			return e;
		}

		public int size() {
			return aliasPrimaryMap.size();
		}

		A getAliasKey(E v) {
			return Objutil.notNull(func.apply(v), v);
		}
	}

	private final class Listener implements MsgListener, EvictListener<K, Node<V>> {
		private final String eventValue;
		Broker broker;

		Listener(String uniqueID) {
			this.eventValue = uniqueID == null ? "" : uniqueID;
		}

		void start() {
			if (eventValue.length() > 0) {
				if (broker == null)
					broker = BrokerFactory.instance();
				Objutil.validate(broker.addListener(this), "exist cache: {}", eventValue);
			}
		}

		void multicast(Object object) {
			if (eventValue.length() > 0) {
				if (broker == null)
					broker = BrokerFactory.instance();
				broker.sendToAll(false, "msg.dmcache", eventValue, object == null ? null : new Object[] { object });
			}
		}
		@Override
		public void onEvent(Address from, String name, String value, List<Object> params) {
			if ("msg.dmcache".equals(name) && eventValue.equals(value)) {
				if (params.isEmpty())
					clear(false);
				else
					remove(params.get(0), false);
			}
		}
		@Override
		public void onEvict(K key, Node<V> node) {
			removeAlias(node);
		}

		@Override
		public int hashCode() {
			return eventValue.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DMCache.Listener)
				return eventValue.equals(((DMCache.Listener) obj).eventValue);
			return false;
		}
	}

	private final static class Node<V> {
		final long untilMills;
		final V value;
		final Object[] aliasKeys;

		Node(V v, Alias<?, V>[] alias, int timeout) {
			this.value = Objutil.notNull(v, "value is null.");
			int i = alias.length;
			this.aliasKeys = i == 0 ? alias : new Object[i];
			while (--i >= 0)
				aliasKeys[i] = alias[i].getAliasKey(value);
			this.untilMills = timeout <= 0 ? 0 : Util.now() + timeout;
		}
	}
}
