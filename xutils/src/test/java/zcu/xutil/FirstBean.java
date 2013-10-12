package zcu.xutil;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import sun.reflect.Reflection;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.cfg.State;
import zcu.xutil.utils.Function;
import zcu.xutil.utils.Util;
import zcu.xutil.web.Webutil;

public class FirstBean {
	static Map<String, String> map = new HashMap<String, String>();
	public static TMM tmm = new TMM();
	static String staticStr;
	private String firstString;
	private Integer firstInteger;
	private int firstInt;
	private byte[] firstbytes;
	private boolean secondBean;

	static void setStaticStr(String str) {
		staticStr = str;
	}

	static String getStaticStr() {
		return staticStr;
	}

	public boolean isClass() {
		return false;
	}

	public byte[] getFirstbytes() {
		return firstbytes;
	}

	public void setFirstbytes(byte[] firstbytes) {
		this.firstbytes = firstbytes;
	}

	public int getFirstInt() {
		return firstInt;
	}

	public void setFirstInt(int firstInt) {
		Logger.LOG.info("setFirstInt");
		this.firstInt = firstInt;
	}

	public Integer getFirstInteger() {
		return firstInteger;
	}

	public void setFirstInteger(Integer firstInteger) {
		Logger.LOG.info("setFirstInteger");
		this.firstInteger = firstInteger;
	}

	public String getFirstString() {
		Logger.LOG.info("getFirstString");
		return firstString;
	}

	public void setFirstString(String firstString) {
		Logger.LOG.info("setFirstString");
		this.firstString = firstString;
	}

	public boolean getSecondBean() {
		return secondBean;
	}

	public void setSecondBean(boolean secondBean) {
		this.secondBean = secondBean;
	}

	public String toString() {
		StringBuilder retValue = new StringBuilder("FirstBean [ ");
		retValue.append("firstString = ").append(this.firstString).append(" ][");
		retValue.append("firstInteger = ").append(this.firstInteger).append(" ][");
		retValue.append("firstInt = ").append(this.firstInt).append(" ][");
		retValue.append("firstbytes = ").append(Arrays.toString(this.firstbytes)).append(" ][");
		retValue.append("secondBean = ").append(this.secondBean).append(" ][");
		return retValue.toString();
	}

	static boolean ttt(Object... objects) throws Exception {
		for (Object object : objects) {
			System.out.println("xxxxxxxxx");
			System.out.println(object);
		}
		if (objects.length == 0)
			throw null;
		return true;
	}

	public static String capitalize(String name) {
		if (name == null || name.length() == 0)
			return name;
		return String.valueOf(Character.toTitleCase(name.charAt(0))).concat(name.substring(1));
	}
	public static String capitalize2(String name) {
		if (name == null || name.length() == 0)
			return name;
		char[] chars = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}

	interface Ecept {
		void ecept() throws XutilRuntimeException;

	}

	public static void main(String[] args) throws NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		Method mm = null;
		try {
			mm = Ecept.class.getMethod("ecept");
			System.out.println(" excepts : " + Objutil.append(new StringBuilder(), mm.getExceptionTypes()));
			Method m1 = FirstBean.class.getMethod("getFirstInteger");
			Method m2 = FirstBean.class.getMethod("getFirstInteger");
			System.out.println(" identical : " + (m1.getName() == m2.getName()));
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		long begin, end;
		begin = System.nanoTime();
		for (int i = 0; i < 10000; i++)
			Reflection.getCallerClass(1).getName();
		end = System.nanoTime();

		System.out.println("Reflection.getCallerClass: " + (end - begin));
		Class clazz = Reflection.getCallerClass(1);
		System.out.println(Reflection.getCallerClass(1));
		System.out.println("class name identical: " + (clazz.getName() == FirstBean.class.getName()));
		System.out.println("class name identical intern: " + (clazz.getName() == FirstBean.class.getName().intern()));
		begin = System.nanoTime();
		for (int i = 0; i < 10000; i++)
			new Throwable().getStackTrace()[0].getClassName();
		end = System.nanoTime();
		System.out.println("new Throwable(): " + (end - begin));
		System.out.println(new Throwable().getStackTrace()[0].getClassName());

		StackTraceElement[] ste;
		begin = System.nanoTime();
		for (int i = 0; i < 10000; i++)
			ste = Thread.currentThread().getStackTrace();
		end = System.nanoTime();

		System.out.println("StackTraceElement currentThread: " + (end - begin));
		Pattern.compile("(?-i)true|falseeeeeeeerrrrrrrrr");
		begin = System.nanoTime();
		for (int i = 0; i < 100000; i++)
			Pattern.compile("(?-i)true|falseeeeeeeerrrrrrrrr");
		end = System.nanoTime();

		System.out.println("Pattern.compile: " + (end - begin));

		begin = System.nanoTime();
		for (int i = 0; i < 10000; i++)
			ste = new Throwable().getStackTrace();
		end = System.nanoTime();
		System.out.println("StackTraceElement new Throwable(): " + (end - begin));
		java.util.Date date = new java.util.Date();
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.PRC).format(date);
		String ss;
		begin = System.nanoTime();

		for (int i = 0; i < 100000; i++) {
			ss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.PRC).format(date);
		}

		end = System.nanoTime();
		System.out.println("date format: " + (end - begin));

		Util.format("yyyy-MM-dd HH:mm:ss", date);
		begin = System.nanoTime();
		for (int i = 0; i < 100000; i++) {
			ss = Util.format("yyyy-MM-dd HH:mm:ss", date);
		}
		end = System.nanoTime();
		System.out.println("Util date format: " + (end - begin));
		long ll = Util.now();
		begin = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			ll = Util.now();
		}
		end = System.nanoTime();
		System.out.println("Util.now(): " + (end - begin));

		begin = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			ll = System.currentTimeMillis();
		}
		end = System.nanoTime();
		System.out.println("System.currentTimeMillis(): " + (end - begin));

		Class c = FirstBean.class;
		begin = System.nanoTime();
		for (int i = 0; i < 100000; i++)
			c.isArray();
		end = System.nanoTime();
		System.out.println("isArray:" + (end - begin));
		Object oo = args;
		begin = System.nanoTime();
		for (int i = 0; i < 100000; i++)
			c.getComponentType();
		end = System.nanoTime();
		System.out.println("getComptype:" + (end - begin));

		char ch = 'a';
		boolean b;
		begin = System.nanoTime();
		for (int i = 0; i < 1000000; i++)
			b = (ch - 97 | 122 - ch) >= 0;
		end = System.nanoTime();
		System.out.println("or: " + (end - begin));
//		begin = System.nanoTime();
//		for (int i = 0; i < 1000000; i++)
//			b = ch >= 97 && ch <= 122;
//		end = System.nanoTime();
//		System.out.println("if: " + (end - begin));
//		c = FirstBean.class;
//		begin = System.nanoTime();
//
//		for (int i = 0; i < 10000000; i++)
//			oo=Objutil.defaults(c);
//		end = System.nanoTime();
//		System.out.println("primitiveDefault: " + (end - begin));
//
//		begin = System.nanoTime();
//		for (int i = 0; i < 10000000; i++)
//			b=c.isPrimitive();
//		end = System.nanoTime();
//		System.out.println("isPrimitive: " + (end - begin));
//		String s="isPrimitive: ";
//		begin = System.nanoTime();
//		for (int i = 0; i < 10000000; i++)
//			s=s.intern();
//		end = System.nanoTime();
//		System.out.println("intern: " + (end - begin));
//
//		int k= mm.getParameterTypes().length;
//		begin = System.nanoTime();
//		for (int i = 0; i < 10000000; i++)
//			k = mm.getParameterTypes().length;
//		end = System.nanoTime();
//		System.out.println("method getParameterTypes: " + (end - begin));
//
//		k= Util.getParamsLen(mm);
//		begin = System.nanoTime();
//		for (int i = 0; i < 10000000; i++)
//			k = Util.getParamsLen(mm);
//		end = System.nanoTime();
//		System.out.println("Methods getParamsLen: " + (end - begin));
//
//		s = "abcdefgh";
//
//		begin = System.currentTimeMillis();
//		for (int i = 0; i < 1000000; i++)
//			capitalize(s);
//		end = System.currentTimeMillis();
//		System.out.println("concat " + (end - begin));
//
//		begin = System.currentTimeMillis();
//		for (int i = 0; i < 1000000; i++)
//			capitalize2(s);
//		end = System.currentTimeMillis();
//		System.out.println("toarray " + (end - begin));

		// TT tt = new TT();
		// tt.start();
		// for (int i = 0; i < 10; i++) {
		// //map.put("i= " + i, "v= " + i);
		// tmm.add();
		// tt.j=i;
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		//
		// try {
		// Thread.sleep(4000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	static class TMM implements Function {
		int size;

		public void add() {
			size++;
		}

		public int size() {
			return size;
		}

		public Integer apply(String from) {
			// TODO Auto-generated method stub
			return null;
		}

		public int apply(Integer from) {
			// TODO Auto-generated method stub
			return 0;
		}

		public Object apply(Object from) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	static class TT extends Thread {
		int j = 0;

		public void run() {
			StringBuilder sb = new StringBuilder();
			int k = j;
			sb.append("size= ").append(tmm.size).append(" j=").append(k).append("\n");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			k = j;
			sb.append("size= ").append(tmm.size).append(" j=").append(k).append("\n");
			for (int i = 0; i < 20; i++) {
				sb.append("size= " + tmm.size + " j=" + j + "\n");

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println(sb.toString());
		}
	}
}
