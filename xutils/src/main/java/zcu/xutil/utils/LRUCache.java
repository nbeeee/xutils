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
package zcu.xutil.utils;

import java.util.Map;

public final class LRUCache<K, V> {
	private final Map<K, V> map;

	public LRUCache(int maximun, EvictListener<K, V> evict) {
		map = Util.lruMap(maximun, evict);
	}

	public synchronized int size() {
		return map.size();
	}

	public synchronized V get(Object key) {
		return map.get(key);
	}

	public synchronized V remove(Object key) {
		return map.remove(key);
	}

	public synchronized V put(K key, V value) {
		if (value == null)
			throw new IllegalArgumentException("null value");
		return map.put(key, value);
	}

	public synchronized void clear() {
		map.clear();
	}

	public synchronized boolean remove(Object key, Object value) {
		if (value == null || !value.equals(map.get(key)))
			return false;
		map.remove(key);
		return true;
	}

	public synchronized V putIfAbsent(K key, V value) {
		if (value == null)
			throw new IllegalArgumentException("null value");
		V ret = map.get(key);
		if (ret == null)
			map.put(key, value);
		return ret;
	}
}
