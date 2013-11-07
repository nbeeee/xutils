package zcu.xutil.cfg;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import zcu.xutil.XutilRuntimeException;
import zcu.xutil.utils.Convertor;

public final class Instance extends RefCaller {
	private final static Instance NIL = new Instance(null);
	private final static Convertor convertor = new Convertor() {

		@Override
		protected Object customConvert(Object value, Class<?> toType) {
			if (value instanceof String){
				if (toType == File.class)
					return new File((String) value);
				if(toType == URL.class)
					try {
						return new URL((String) value);
					} catch (MalformedURLException e) {
						throw new XutilRuntimeException(e);
					}
			}
			throw exception(value, toType.getName());
		}
	};

	public static Instance value(Object val) {
		return val == null ? NIL : new Instance(val);
	}

	private final Object value;
	private volatile Invoker link;

	private Instance(Object aValue) {
		this.value = aValue;
	}

	@Override
	public RefCaller call(String methodName, Object... args) {
		link = link == null ? new Invoker(getType(), methodName, args) : link.link(getType(), methodName, args);
		return this;
	}
	@Override
	public Class<?> getType() {
		return value == null ? Void.class : value.getClass();
	}
	@Override
	public Object instance() {
		if (link != null)
			synchronized (this) {
				if (link != null) {
					link.apply(value);
					link = null;
				}
			}
		return value;
	}

	@Override
	protected Provider matches(Class<?> toType, State state) {
		if (value == null)
			return toType.isPrimitive() ? null : this;
		if (state.matched(getType(), toType))
			return this;
		if (!state.converRequired())
			return null;
		if (value instanceof Number || value instanceof String)
			try {
				Object o = convertor.convert(value, toType);
				state.convertMatch();
				return new Instance(o);
			} catch (RuntimeException e) {// can't convert
			}
		return null;
	}

	@Override
	public RefCaller clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
