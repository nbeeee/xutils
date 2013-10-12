package zcu.xutil.utils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.RandomAccess;

public final class FixedList<E> extends AbstractList<E> implements RandomAccess, Serializable {
	private static final long serialVersionUID = -7087484530626174627L;
	private final E[] data;

	public FixedList(E[] array) {
		if (array == null)
			throw new NullPointerException();
		data = array;
	}

	@Override
	public int size() {
		return data.length;
	}

	@Override
	public Object[] toArray() {
		Object[] result = new Object[data.length];
		System.arraycopy(data, 0, result, 0, data.length);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		int size = data.length;
		if (a.length < size)
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		else if (a.length > size)
		  a[size] = null;
		System.arraycopy(data, 0, a, 0, size);
		return a;
	}

	@Override
	public E get(int index) {
		return data[index];
	}

	@Override
	public int indexOf(Object o) {
		if (o == null) {
			for (int i = 0; i < data.length; i++)
				if (data[i] == null)
					return i;
		} else {
			for (int i = 0; i < data.length; i++)
				if (o.equals(data[i]))
					return i;
		}
		return -1;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}
}
