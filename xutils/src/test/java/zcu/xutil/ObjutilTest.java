package zcu.xutil;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.txm.Propagation;
import zcu.xutil.utils.Convertor;
import zcu.xutil.utils.Function;
import zcu.xutil.utils.Util;
import zcu.xutil.web.Resolver;

public class ObjutilTest {
	int i;
	long l;
	short s;
	double d;
	float f;
	char c;
	byte b;
	boolean bool;

	@Before
	public void setUp() throws Exception {
	}

	public static File locateDir(final Class cls) {
		ProtectionDomain pd = cls.getProtectionDomain();
		URL url;
		if (pd != null) {
			CodeSource cs = pd.getCodeSource();
			url = cs == null ? null : cs.getLocation();
			if (url != null && url.getProtocol().equals("file")) {
				File f = new File(URI.create(url.toExternalForm()));
				if (f.isDirectory())
					return f;
				String p = f.getPath();
				if (p.endsWith(".jar") || p.endsWith(".zip"))
					return f.getParentFile();
			}
		}
		String res = cls.getName().replace('.', '/').concat(".class");
		ClassLoader cl = cls.getClassLoader();
		url = cl != null ? cl.getResource(res) : ClassLoader.getSystemResource(res);
		String s = url.toExternalForm();
		int si = s.indexOf("!/"); // jarUrl separator
		if (si < 0)
			return new File(URI.create(s.substring(0, s.length() - res.length())));
		return new File(URI.create(s.substring("jar:".length(), si))).getParentFile();
	}

	@After
	public void tearDown() throws Exception {
	}

	public static <T> Iterator<T> concat(final Iterator<? extends Iterator<? extends T>> inputs) {
		Objutil.validate(inputs != null, "inputs is null");
		return new Iterator<T>() {
			@SuppressWarnings("unchecked")
			Iterator<? extends T> current = Collections.EMPTY_LIST.iterator();
			Iterator<? extends T> removeFrom;

			public boolean hasNext() {
				while (!current.hasNext() && inputs.hasNext())
					current = inputs.next();
				return current.hasNext();
			}

			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				removeFrom = current;
				return current.next();
			}

			public void remove() {
				Objutil.validate(removeFrom != null, "no calls to next() since last call to remove()");
				removeFrom.remove();
				removeFrom = null;
			}
		};
	}

	@Test
	public void testDefaultNull() {
		assertEquals("xzc", Objutil.ifNull(null, "xzc"));
		assertEquals("xxxxx", Objutil.ifNull("xxxxx", "xzc"));
		Logger.LOG.info("}{}---{}--\\{}---{}--{", -1 & 0xff, -128 & 0xff, 3, 4);
		Logger.LOG.info("}{}---{}--{}-----{}{", 1, 2, 3, 4);
		Logger.LOG.info("{{}---{}--{}{}---\\{}--{}", 1, 2, 3, 4, 5);
		Logger.LOG.info("\\{}---{}--{}{}---\\{}--\\{}", 1, 2, 3, 4, 5);
		Logger.LOG.info("\\{}---{}--{}{}---\\{}--\\\\{}", 1, 2, 3, 4, 5);
		Logger.LOG.info("{}", 1, 2, 3, 4, 5);
		Logger.LOG.info("{}{}{}", 1, 2, 3, 4, 5);
		String ss = Objutil.systring("xutils.home");
		ss = ss +" xutils.home[  xutils.home]  "+ss+" "+ss;
		assertEquals(ss, Objutil.placeholder("${xutils.home} ${xutils.home[}  ${xutils.home]}  ${xutils.home[]} ${xutils.home[---]}",(Replace)Objutil.properties()));
		ss ="xutils.h xutils.h[  xutils.h]   ---";
		assertEquals(ss, Objutil.placeholder("${xutils.h} ${xutils.h[}  ${xutils.h]}  ${xutils.h[]} ${xutils.h[---]}",(Replace)Objutil.properties()));
	}

	@Test
	public void testEqual() {
		assertTrue(Objutil.equal(null, null));
		assertTrue(!Objutil.equal(1, null));
		assertTrue(Objutil.equal("abc", "abc"));
		assertTrue(equalsIgnoreCase("123aAmMzZ89", "123AaMmZz89"));
	}

	@Test
	public void testCloseQuietly() {
		try {
			Objutil.closeQuietly(null);
			Objutil.closeQuietly(new StringWriter());
		} catch (Exception e) {
			fail("closeQuietly");
		}
	}

	@Test
	public void testPrimitiveDefault() {
		assertNull(Objutil.defaults(Object.class));
		assertNull(Objutil.defaults(Long.class));
		assertEquals(0L, Objutil.defaults(Long.TYPE));
		assertEquals('\u0000', Objutil.defaults(Character.TYPE));
		assertEquals(Objutil.defaults(Integer.TYPE), i);
		assertEquals(Objutil.defaults(Long.TYPE), l);
		assertEquals(Objutil.defaults(Double.TYPE), d);
		assertEquals(Objutil.defaults(Short.TYPE), s);
		assertEquals(Objutil.defaults(Float.TYPE), f);
		assertEquals(Objutil.defaults(Byte.TYPE), b);
		assertEquals(Objutil.defaults(Character.TYPE), c);
		assertEquals(Objutil.defaults(Boolean.TYPE), bool);

	}

	@Test
	public void testConvertor() {
		Convertor UTILS = new Convertor();
		String data = "!@#412309aAzZqwtuMkLLFDS";
		String str = "1234";
		char[] chars = str.toCharArray();
		byte[] bytes = new byte[] { '1', '2', '3', '4' };
		Integer j = (Integer) UTILS.convert(str, Integer.class);
		assertEquals(1234, j.intValue());
		String ss = (String) UTILS.convert(j, String.class);
		assertEquals(str, ss);
		char[] cs = (char[]) UTILS.convert(str, char[].class);
		assertTrue(Arrays.equals(chars, cs));
		assertEquals(str, UTILS.convert(cs, String.class));
		byte[] bs = (byte[]) UTILS.convert(cs, byte[].class);
		assertTrue(Arrays.equals(bytes, bs));
		assertFalse(((Boolean) UTILS.convert(null, Boolean.TYPE)).booleanValue());
		double dd = 13.456;
		String s3 = (String) UTILS.convert(dd, String.class);
		assertEquals("13.456", s3);
		assertEquals(dd, UTILS.convert(s3, Double.TYPE));
		assertEquals('1', UTILS.convert(s3, Character.TYPE));
		assertEquals("1", UTILS.convert('1', String.class));
		assertEquals(Propagation.NEVER, UTILS.convert("NEVER", Propagation.class));
		for (Class c : new Class[] { void.class, boolean.class, byte.class, char.class, short.class, int.class,
				float.class, double.class, long.class }) {
			assertEquals(c, UTILS.convert(c.getName(), Class.class));
		}
		try {
			for (String ff : new String[] { "test\\", "test/zcu", "\\temp" }) {
				File target = new File(ff);
				String cpath = target.getCanonicalPath();
				File base = new File(Objutil.systring("xutils.home"));
				assertEquals(cpath,new File(base, Util.relativize(target,base).getPath()).getCanonicalPath());
				base =  new File(".");
				assertEquals(cpath,new File(base, Util.relativize(target,base).getPath()).getCanonicalPath());
				base =  new File("\\temp");
				Logger.LOG.info(Util.relativize(target,base).getPath());
				assertEquals(cpath,new File(base, Util.relativize(target,base).getPath()).getCanonicalPath());
				Logger.LOG.info("pppp:{}",new File(".").toURI().relativize(new File("D:/myproject").toURI()).getPath());
				Logger.LOG.info("pppp:{}",Util.relativize(new File("D:/myproject"),new File(".")).getPath());
			}
		} catch (IOException ex) {
			throw new XutilRuntimeException(ex);
		}
	}

	@Test
	public void testLauncher() {
		// HashMap env = new HashMap();
		// env.put("key1", "FF");
		// String s1 = "$(abc$(de$(key1)fgh$(key2)ijk$(file.encoding)";
		// assertEquals("$(abc$(deFFfgh$(key2)ijk"+Objutil.fileEncoding,
		// Objutil.eval(s1, env));
		// String s2 = "$(abc$(de$(key1)fgh$(XZQ#ZYX)ijk$(XZQ#<;:)";
		// assertEquals("$(abc$(deFFfgh123ijkabc", Objutil.eval(s2, env));
		String[] Empty = new String[] {};
		assertArrayEquals(Empty, Objutil.split("", ',').toArray(Empty));
		assertArrayEquals(Empty, Objutil.split(",", ',').toArray(Empty));
		assertArrayEquals(Empty, Objutil.split(null, ',').toArray(Empty));
		assertArrayEquals(new String[] { "ab", "cd" }, Objutil.split(",ab,,cd,", ',').toArray(Empty));
		assertArrayEquals(new String[] { "1", "2", "3", "4", "5" }, Objutil.split("1,2,3,4,5", ',').toArray(Empty));
		try {
			assertEquals(new File("./target").getCanonicalPath(), locateDir(getClass()).getParentFile().getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testToBytes() {
		assertEquals(9876543210L, bytesToLong(longToBytes(9876543210L), 0));
		assertEquals(987654321, bytesToInt(intToBytes(987654321), 0));
		assertEquals(32100, bytesToShort(shortToBytes((short) 32100), 0));
	}

	@Test
	public void testRethrow() {
		try {
			try {
				exception();
			} catch (IOException e) {
				throw Objutil.rethrow(e);
			} finally {
				number = 111111;
			}
		} catch (Throwable e) {
			assertEquals(111111, number);
		}
	}

	int number = 1;

	// @Test
	public void testFuncRethrow() {
		try {
			execFunc(this);
		} catch (IOException e) {
			Logger.LOG.info("unwrapped: ", e);
		}
	}

	static void execFunc(ObjutilTest obj) throws IOException {
		Function f = new Function<ObjutilTest, Object>() {
			public Object apply(ObjutilTest o) {
				try {
					o.exception();
					return null;
				} catch (IOException e) {
					Logger.LOG.info("origianl: ", e);
					throw new XutilRuntimeException(e);
				}
			}
		};
		try {
			f.apply(obj);
		} catch (XutilRuntimeException e) {
			Logger.LOG.info("WrappedException: ", e);
			// Logger.LOG.info("XutilRuntimeException: ",new
			// XutilRuntimeException("no cause"));
			throw e.rethrowIf(IOException.class);
		}
	}

	void exception() throws IOException {
		throw new IOException("io");
	}

	public static byte[] longToBytes(long l) {
		byte b[] = new byte[8];
		ByteBuffer.wrap(b).putLong(l);
		return b;
	}

	public static long bytesToLong(byte[] b, int offset) {
		return ByteBuffer.wrap(b, offset, 8).getLong();
	}

	public static byte[] intToBytes(int i) {
		byte b[] = new byte[4];
		ByteBuffer.wrap(b).putInt(i);
		return b;
	}

	public static int bytesToInt(byte[] b, int offset) {
		return ByteBuffer.wrap(b, offset, 4).getInt();
	}

	public static byte[] shortToBytes(short s) {
		byte b[] = new byte[2];
		ByteBuffer.wrap(b).putShort(s);
		return b;
	}

	public static short bytesToShort(byte[] b, int offset) {
		return ByteBuffer.wrap(b, offset, 2).getShort();
	}

	public static boolean equalsIgnoreCase(String s1, String s2) {
		if (s1 == s2)
			return true;
		if (s1 == null || s2 == null)
			return false;
		int len = s1.length();
		if (len != s2.length())
			return false;
		for (int i = 0; i < len; i++) {
			char c = s1.charAt(i);
			int d = c - s2.charAt(i);
			if (d != 0 && (d != 'a' - 'A' || c < 'a' || c > 'z') && (d != 'A' - 'a' || c < 'A' || c > 'Z'))
				return false;
		}
		return true;
	}

	public static void main(String[] args) {
		char chars[] = { '小', '1', '%', '$', '_', '[', '}', '-' };
		for (char ch : chars)
			Logger.LOG.info("{}  D:{} L:{} P:{} S:{}", ch, Character.isDigit(ch), Character.isLetter(ch), Character
					.isJavaIdentifierPart(ch), Character.isJavaIdentifierStart(ch));

		Sizeof.testCaller();
		Objutil.loadProps(Objutil.toURL(new File("xutils.properties")), new Properties()).list(System.out);
		try {
			Logger.LOG.info("EMPTY PATH: {}",new File("").getPath());
			Logger.LOG.info("Relative: {}", Util.relativize(new File("test\\"), new File(Objutil
					.systring("xutils.home"))));
			Logger.LOG.info("Relative: {}", Util.relativize(new File("test/zcu"), new File(".")));
			Logger.LOG.info("Relative: {}", Util.relativize(new File("d:\\temp"), new File(".")));
			Logger.LOG.info("Relative: {}", Util.relativize(new File("c:\\temp"), new File(".")));
			File f1, f2;
			URL url = new File("test").toURI().toURL();
			URL url3 = new URL(url, "a.b");
			URL url1 = (f1 = new File("test//a.b")).toURI().toURL();
			URL url2 = (f2 = new File("test\\A.b")).toURI().toURL();
			Logger.LOG.info("f1={} f2={}  a  {}  {} {}", f1.getPath(), f2.getPath(), f1.getAbsolutePath(), f2
					.getCanonicalPath(), url1.getPath());
			Logger.LOG.info("url1={}  url2={} url3={} eaual={} {} {}", url1, url2, url3, url1.equals(url3), url1
					.sameFile(url2), f1.equals(f2.getCanonicalFile()));
		} catch (IOException ex) {
			throw new XutilRuntimeException(ex);
		}
		for (Method m : Sizeof.class.getMethods()) {
			Logger.LOG.info("{} property: {}", m.getName(), Util.indexGetter(m));
		}
		File file = new File("/temp/paramters.ser");
		Logger.LOG.info("parent file:{}}", new File(".", "a/b.txt").getParent());
		FileOutputStream out = null;
		Object[] params = new Object[] { new Date(), "12345" };
		try {
			file.createNewFile();
			out = new FileOutputStream(file);
			ObjectOutputStream oo = new ObjectOutputStream(out);
			oo.writeObject(params);
			oo.flush();
		} catch (IOException ex) {
			throw new XutilRuntimeException(ex);
		} finally {
			Objutil.closeQuietly(out);
		}
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			ObjectInputStream oi = new ObjectInputStream(in);
			Object obj = oi.readObject();
			Logger.LOG.info("{}", obj);
			if (obj instanceof Object[]) {
				Logger.LOG.info("{} {} {} ", (Object[]) obj);
			}
		} catch (Exception ex) {
			throw new XutilRuntimeException(ex);
		} finally {
			Objutil.closeQuietly(in);
		}

		Logger.LOG.info(Integer.toString(Integer.MAX_VALUE, Character.MAX_RADIX));
		Logger.LOG.info("1-min={} min-1={} max-min={} min-max={}", 1 - Integer.MIN_VALUE, Integer.MIN_VALUE - 1,
				Integer.MAX_VALUE - Integer.MIN_VALUE, Integer.MIN_VALUE - Integer.MAX_VALUE);
		Logger log = Logger.getLogger("global");
		log.info("global");
	}

}
