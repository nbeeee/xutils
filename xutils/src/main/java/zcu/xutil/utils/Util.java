package zcu.xutil.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

public final class Util implements Iterator {
	public static final String FILE_ENCODING = Objutil.systring("file.encoding", "GBK");
	public static final ThreadGroup XUTIL_GROUP = new ThreadGroup("xutils") {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Logger.LOG.error("uncaught exception in {} (thread group={} )", e, t.getName(), this);
			super.uncaughtException(t, e);
		}
	};
	private static final Map<String, Format> formatCache = lruMap(11, null);

	public static String format(String pattern, Object dateOrNumber) {
		boolean date = dateOrNumber instanceof Date;
		if (!date && !(dateOrNumber instanceof Number))
			throw new UnsupportedOperationException("neither Date nor Number.");
		Format format;
		synchronized (formatCache) {
			if ((format = formatCache.get(pattern)) == null)
				formatCache.put(pattern, format = date ? new SimpleDateFormat(pattern) : new DecimalFormat(pattern));
		}
		synchronized (format) {
			return format.format(dateOrNumber);
		}
	}

	public static Class getSetterType(Method m) {
		String s = m.getName();
		if (s.startsWith("set") && s.length() > 3 && m.getReturnType() == Void.TYPE) {
			Class[] types = m.getParameterTypes();
			if (types.length == 1)
				return types[0];
		}
		return null;
	}

	// public static int getParamsLen(Method m) {
	// return getParamTypes(m).length;
	// }

	public static boolean isSetter(Method m) {
		return getSetterType(m) != null;
	}

	public static String nameOfSetter(String property) {
		char[] chars = new char[property.length() + 3];
		property.getChars(0, property.length(), chars, 3);
		chars[0] = 's';
		chars[1] = 'e';
		chars[2] = 't';
		chars[3] = Character.toTitleCase(chars[3]);
		return new String(chars);
	}

	public static int indexGetter(Method m) {
		String s = m.getName();
		if (s.startsWith("get")) {
			if (s.length() > 3 && m.getReturnType() != Void.TYPE && m.getParameterTypes().length == 0)
				return 3;
		} else if (s.startsWith("is") && s.length() > 2 && m.getReturnType() == Boolean.TYPE
				&& m.getParameterTypes().length == 0)
			return 2;
		return -1;
	}

	public static String signature(String method, Class... paramTypes) {
		final int params;
		if ((params = paramTypes.length) == 0)
			return method;
		StringBuilder sb = new StringBuilder(method);
		int len, i = 0;
		do {
			String n = paramTypes[i].getName();
			char last = n.charAt((len = n.length()) - 1);
			sb.append(n.charAt(0)).append((char) (len + 30)).append(last == ';' ? n.charAt(len - 2) : last);
		} while (++i < params);
		return sb.toString();
	}

	public static Object newInstance(Constructor constructor, Object[] params) {
		try {
			return constructor.newInstance(params);
		} catch (InvocationTargetException e) {
			throw Objutil.rethrow(e.getCause());
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
	}

	public static Object call(Object target, Method m, Object[] params) {
		try {
			return m.invoke(target, params);
		} catch (InvocationTargetException e) {
			throw Objutil.rethrow(e.getCause());
		} catch (IllegalAccessException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static Object invoke(Object target, Method m, Object[] params) throws Throwable {
		try {
			return m.invoke(target, params);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		} catch (IllegalAccessException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static void closeQuietly(Connection conn) {
		if (conn != null)
			try {
				conn.close();
			} catch (Throwable e) { // do nothing
			}
	}

	public static void testConnection(Connection conn, String testQuery) {
		try {
			if (testQuery == null) {
				if (conn.isValid(6))
					return;
				throw new IllegalStateException("test invalid connection.");
			}
			PreparedStatement stmt = conn.prepareStatement(testQuery);
			try {
				stmt.executeQuery().close();
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static StringBuilder readAndClose(Reader r, StringBuilder sb) {
		try {
			int i;
			char[] b = new char[1024];
			while ((i = r.read(b)) > 0)
				sb.append(b, 0, i);
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		} finally {
			Objutil.closeQuietly(r);
		}
		return sb;
	}

	/**
	 * Transfer.
	 * 
	 * @param r
	 *            the r
	 * @param w
	 *            the w
	 * @param len
	 *            len >=0 表示转送长度; <br>
	 *            len < 0 转送到EOF, -len>1024时,值表示缓存大小.
	 * @return 长度
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static int transfer(Reader r, Writer w, int len) throws IOException {
		int c = 0, i;
		if (len < 0) {
			for (char[] b = new char[Math.max(-len, 1024)]; (i = r.read(b)) > 0; c += i)
				w.write(b, 0, i);
		} else if (len > 0) {
			i = len < 2048 ? Math.min(len, 1024) : (len < 64 * 1024 ? 2048 : (len < 1024 * 1024 ? 4096 : 8192));
			for (char[] b = new char[i]; len > c && (i = r.read(b, 0, Math.min(len - c, b.length))) > 0; c += i)
				w.write(b, 0, i);
		}
		return c;
	}

	/**
	 * Transfer.
	 * 
	 * @param in
	 *            the in
	 * @param out
	 *            the out
	 * @param len
	 *            len >=0 表示转送长度; <br>
	 *            len < 0 转送到EOF, -len>1024时,值表示缓存大小.
	 * @return 长度
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static int transfer(InputStream in, OutputStream out, int len) throws IOException {
		int c = 0, i;
		if (len < 0) {
			for (byte[] b = new byte[Math.max(-len, 1024)]; (i = in.read(b)) > 0; c += i)
				out.write(b, 0, i);
		} else if (len > 0) {
			i = len < 2048 ? Math.min(len, 1024) : (len < 64 * 1024 ? 2048 : (len < 1024 * 1024 ? 4096 : 8192));
			for (byte[] b = new byte[i]; len > c && (i = in.read(b, 0, Math.min(len - c, b.length))) > 0; c += i)
				out.write(b, 0, i);
		}
		return c;
	}

	@SuppressWarnings("serial")
	public static <K, V> Map<K, V> lruMap(final int maximun, final EvictListener<K, V> evict) {
		return new LinkedHashMap<K, V>(maximun > 64 ? (maximun >> 1) : maximun, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Entry<K, V> eldest) {
				if (size() <= maximun)
					return false;
				if (evict != null)
					evict.onEvict(eldest.getKey(), eldest.getValue());
				return true;
			}
		};
	}

	/**
	 * Returns the path of one File relative to another.
	 * 
	 * @param target
	 *            the target directory
	 * @param base
	 *            the base directory
	 * @return target's path relative to the base directory
	 * 
	 * */
	public static File relativize(File target, File base) {
		char sep = File.separatorChar;
		List<String> baseComps, targetComps;
		try {
			baseComps = Objutil.split(base.getCanonicalPath(), sep);
			targetComps = Objutil.split(target.getCanonicalPath(), sep);
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		}
		// skip common components
		int len = Math.min(targetComps.size(), baseComps.size()), idx = 0;
		while (idx < len && targetComps.get(idx).equals(baseComps.get(idx)))
			idx++;
		StringBuilder sb = new StringBuilder();
		// backtrack to base directory
		for (len = baseComps.size() - idx; len > 0; len--)
			sb.append("..").append(sep);
		for (len = targetComps.size(); idx < len; idx++)
			sb.append(targetComps.get(idx)).append(sep);
		// remove final path separator
		String s = target.getPath();
		if (!s.endsWith("/") && !s.endsWith("\\") && sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return new File(sb.toString());
	}

	public static Thread newThread(Runnable target, String name, boolean daemon) {
		Thread t = new Thread(XUTIL_GROUP, target, name);
		if (t.isDaemon() != daemon)
			t.setDaemon(daemon);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

	/**
	 * 
	 * 这是一个单线程调度服务，只能用于不频繁而且非常轻量级的任务。
	 * 
	 */
	public static ScheduledExecutorService getScheduler() {
		return Timer.executor;
	}

	/**
	 * 
	 * 快速但不太精确(0.5秒)的时间。
	 * 
	 */
	public static long now() {
		// 确保后调用返回值大于等于前调用返回值
		int i;
		while ((i = Timer.vlock.get()) != 0) {
			long now = System.currentTimeMillis();
			if (Timer.vlock.compareAndSet(i, i + 1))
				return now;
		}
		return Timer.currentMillis;
	}

	private static final class Timer extends ScheduledThreadPoolExecutor implements Runnable, ThreadFactory {
		static volatile long currentMillis = System.currentTimeMillis();
		static final AtomicInteger vlock = new AtomicInteger();
		static final ScheduledExecutorService executor = Executors.unconfigurableScheduledExecutorService(new Timer());

		private Timer() {
			super(1);
			setThreadFactory(this);
			scheduleWithFixedDelay(this, 250, 250, TimeUnit.MILLISECONDS);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			int i;
			do {
				i = vlock.get();
				currentMillis = System.currentTimeMillis();
			} while (!vlock.compareAndSet(i, 0));
			// super.afterExecute(r, t);
		}

		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			// super.beforeExecute(t, r);
			if (r != this)
				vlock.set(1);
		}

		@Override
		public void run() {
			// nothing
		}

		@Override
		public Thread newThread(Runnable target) {
			return Util.newThread(target, "Timer", true);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> concat(Iterator<? extends T> a, Iterator<? extends T> b) {
		if (a == null)
			return b == null ? Collections.EMPTY_LIST.iterator() : b;
		if (b == null)
			return (Iterator) a;
		return new Util(a, b);
	}

	private Iterator one;
	private final Iterator two;

	private Util(Iterator a, Iterator b) {
		one = a;
		two = b;
	}

	@Override
	public boolean hasNext() {
		if (one != null) {
			if (one.hasNext())
				return true;
			one = null;
		}
		return two.hasNext();
	}

	@Override
	public Object next() {
		return one == null ? two.next() : one.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
