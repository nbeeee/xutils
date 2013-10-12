package zcu.xutil.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A hashset whose values can be garbage collected.
 */
public class WeakHashSet {

	public class Wref extends WeakReference<Object> {
		public int hashCode;

		public Wref(Object referent, ReferenceQueue<Object> queue) {
			super(referent, queue);
			this.hashCode = referent.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Wref))
				return false;
			Object referent = get();
			Object other = ((Wref) obj).get();
			if (referent == null)
				return other == null;
			return referent.equals(other);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}
	}

	private final ReferenceQueue<Object> referenceQueue;
	Wref[] values;
	int elementSize;
	int threshold;

	public WeakHashSet() {
		this(5);
	}

	public WeakHashSet(int size) {
		this(size, new ReferenceQueue<Object>());
	}

	private WeakHashSet(int size, ReferenceQueue<Object> queue) {
		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of
		// elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.values = new Wref[extraRoom];
		this.referenceQueue = queue;
	}

	/*
	 * Adds the given object to this set. If an object that is equals to the
	 * given object already exists, do nothing. Returns the existing object or
	 * the new object if not found.
	 */
	public Object add(Object obj) {
		cleanupGarbageCollectedValues();
		int index = (obj.hashCode() & 0x7FFFFFFF) % this.values.length;
		Wref currentValue;
		while ((currentValue = this.values[index]) != null) {
			Object referent;
			if (obj.equals(referent = currentValue.get())) {
				return referent;
			}
			index = (index + 1) % this.values.length;
		}
		this.values[index] = new Wref(obj, this.referenceQueue);

		// assumes the threshold is never equal to the size of the table
		if (++this.elementSize > this.threshold)
			rehash();

		return obj;
	}

	private void addValue(Wref value) {
		Object obj = value.get();
		if (obj == null)
			return;
		int valuesLength = values.length;
		int index = (value.hashCode & 0x7FFFFFFF) % valuesLength;
		Wref currentValue;
		while ((currentValue = values[index]) != null) {
			if (obj.equals(currentValue.get())) {
				return;
			}
			index = (index + 1) % valuesLength;
		}
		values[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++elementSize > threshold)
			rehash();
	}

	private void cleanupGarbageCollectedValues() {
		Wref toBeRemoved;
		while ((toBeRemoved = (Wref) this.referenceQueue.poll()) != null) {
			int hashCode = toBeRemoved.hashCode;
			int valuesLength = values.length;
			int index = (hashCode & 0x7FFFFFFF) % valuesLength;
			Wref currentValue;
			while ((currentValue = values[index]) != null) {
				if (currentValue == toBeRemoved) {
					// replace the value at index with the last value with the same hash
					int sameHash = index;
					int current;
					while ((currentValue = values[current = (sameHash + 1) % valuesLength]) != null
							&& currentValue.hashCode == hashCode)
						sameHash = current;
					values[index] = values[sameHash];
					values[sameHash] = null;
					elementSize--;
					break;
				}
				index = (index + 1) % valuesLength;
			}
		}
	}

	public boolean contains(Object obj) {
		return get(obj) != null;
	}

	/*
	 * Return the object that is in this set and that is equals to the given
	 * object. Return null if not found.
	 */
	public Object get(Object obj) {
		cleanupGarbageCollectedValues();
		int valuesLength = this.values.length;
		int index = (obj.hashCode() & 0x7FFFFFFF) % valuesLength;
		Wref currentValue;
		while ((currentValue = this.values[index]) != null) {
			Object referent;
			if (obj.equals(referent = currentValue.get())) {
				return referent;
			}
			index = (index + 1) % valuesLength;
		}
		return null;
	}

	private void rehash() {
		// double the number of expected elements
		WeakHashSet newHashSet = new WeakHashSet(elementSize * 2, referenceQueue);
		Wref currentValue;
		for (int i = 0, length = values.length; i < length; i++)
			if ((currentValue = values[i]) != null)
				newHashSet.addValue(currentValue);
		this.values = newHashSet.values;
		this.threshold = newHashSet.threshold;
		this.elementSize = newHashSet.elementSize;
	}

	/*
	 * Removes the object that is in this set and that is equals to the given
	 * object. Return the object that was in the set, or null if not found.
	 */
	public Object remove(Object obj) {
		cleanupGarbageCollectedValues();
		int valuesLength = this.values.length;
		int index = (obj.hashCode() & 0x7FFFFFFF) % valuesLength;
		Wref currentValue;
		while ((currentValue = this.values[index]) != null) {
			Object referent;
			if (obj.equals(referent = currentValue.get())) {
				this.elementSize--;
				this.values[index] = null;
				rehash();
				return referent;
			}
			index = (index + 1) % valuesLength;
		}
		return null;
	}

	public int size() {
		return this.elementSize;
	}
	//from HashMap
    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits. Note: Null keys always map to hash 0, thus index 0.
     */
    static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Returns index for hash code h.
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }
}
