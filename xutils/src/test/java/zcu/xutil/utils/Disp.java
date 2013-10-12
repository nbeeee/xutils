package zcu.xutil.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.Signal;
import zcu.xutil.DisposeManager;
import zcu.xutil.PathBuilder;
import zcu.xutil.Logger;
import zcu.xutil.utils.AbstractDispose;
import zcu.xutil.web.Stream;
import zcu.xutil.web.Webutil;


public class Disp extends AbstractDispose {
	private final static Logger logger = Logger.getLogger(Disp.class);
	String name;
	public static int disposeSize() {
		return DisposeManager.size();
	}
	public Disp() {
		name = "Disp";
	}

	public Disp(String _name) {
		name = _name;
	}
	public Disp(Object _name) {

	}
	public Disp(Class _name) {
	}
	protected void doDestroy() {
		Thread t = Thread.currentThread();
		logger.info("{} doDestroy, thread: {} , isDaemon: {} ", name, t.getName(), t.isDaemon());
	}

//	private static URL getClassLocationURL(final Class cls) {
//		if (cls == null)
//			throw new IllegalArgumentException("null input: cls");
//		URL result = null;
//		final String clsAsResource = cls.getName().replace('.', '/').concat(".class");
//		final ProtectionDomain pd = cls.getProtectionDomain();
//		// java.lang.Class contract does not specify
//		// if 'pd' can ever be null;
//		// it is not the case for Sun's implementations,
//		// but guard against null
//		// just in case:
//		if (pd != null) {
//			final CodeSource cs = pd.getCodeSource();
//			// 'cs' can be null depending on
//			// the classloader behavior:
//			if (cs != null)
//				result = cs.getLocation();
//
//			if (result != null) {
//				// Convert a code source location into
//				// a full class file location
//				// for some common cases:
//				if ("file".equals(result.getProtocol())) {
//					try {
//						if (result.toExternalForm().endsWith(".jar") || result.toExternalForm().endsWith(".zip"))
//							result = new URL("jar:".concat(result.toExternalForm()).concat("!/").concat(clsAsResource));
//						else if (new File(result.getFile()).isDirectory())
//							result = new URL(result, clsAsResource);
//					} catch (MalformedURLException ignore) {
//					}
//				}
//			}
//		}
//
//		if (result == null) {
//			// Try to find 'cls' definition as a resource;
//			// this is not
//			// document．d to be legal, but Sun's
//			// implementations seem to //allow this:
//			final ClassLoader clsLoader = cls.getClassLoader();
//			result = clsLoader != null ? clsLoader.getResource(clsAsResource) : ClassLoader
//					.getSystemResource(clsAsResource);
//		}
//		return result;
//	}

	static Object[] test(Object...objects){
		return objects;
	}
	public static String charsetOfContype(String contentType) {
		int i;
		if (contentType == null || (i = contentType.indexOf("charset=")) < 0)
			return null;
		i += 8;
		int end = contentType.indexOf(';', i);
		contentType = (end < 0 ? contentType.substring(i) : contentType.substring(i, end)).trim();
		return contentType.length() == 0 ? null : contentType;
	}

	public static void main(String[] args) {
		System.out.println(new File(".").getName());
		int ii= 255;
		byte by = (byte)ii;
		System.out.println(by);
		Pattern patter = Pattern.compile(".+");
		System.out.println("empry match: "+patter.matcher("").matches());
		System.out.println("1 match: "+patter.matcher("1").matches());
		System.out.println("m match: "+patter.matcher("小w3").matches());
		Pattern bp = Pattern.compile("(?-i)true|false");
		System.out.println("true or false: "+bp.matcher("False").matches());
		System.out.println("true or false: "+bp.matcher("false").matches());

		String ct = "text/plain; charset=GBK";
		System.out.println("/"+charsetOfContype(ct)+"/");
		//String ct=null;
		Pattern charsetPattern =Pattern.compile("charset=\\s*([a-z][_\\-0-9a-z]*)",Pattern.CASE_INSENSITIVE);
		 Matcher charsetMatcher = charsetPattern.matcher(ct);
		 if (charsetMatcher.find()){
			  String  encoding = charsetMatcher.group(1);
			 System.out.println("a:"+encoding+";");
		 }
		 else
			 System.out.println("NOT found");
		 String meta = "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />";
		 Pattern metaPattern =Pattern.compile("<meta\\s+([^>]*http-equiv=\"?content-type\"?[^>]*)>",Pattern.CASE_INSENSITIVE);
		 Matcher metaMatcher = metaPattern.matcher(meta);

		     if (metaMatcher.find())
		    	 System.out.println(metaMatcher.group(1));
		String range="bytes=100-200";
		//int [] r = ResumeAction.parseRange(range);

		//Pattern rangePattern =Pattern.compile("bytes=\\s*([0-9]*)\\-([0-9]*)",Pattern.CASE_INSENSITIVE);
		Pattern rangePattern =Pattern.compile("bytes=([0-9]+)?\\-([0-9]+)?",Pattern.CASE_INSENSITIVE);
		Matcher rMatcher = rangePattern.matcher(range);
	     if (rMatcher.find()){
	    	 int i= rMatcher.groupCount();
	    	 System.out.println("groups:"+i);
	    	 	System.out.println("group 0:"+rMatcher.group(0));
	    	 	System.out.println("group 1:"+rMatcher.group(1));
	    	 	System.out.println("group 2:"+rMatcher.group(2));
	     }

	     File file =new File("..");

	     try {
	    	System.out.println("parent: "+file.getParent());
	    	File file2=file.getCanonicalFile();
			System.out.println(file.getCanonicalPath());
			System.out.println(file.getPath());
			System.out.println(file2.getPath());
			System.out.println(file.getAbsolutePath());
			System.out.println(file2.getAbsolutePath());

			System.out.println(Arrays.toString(file.list()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String a=new String();
		String b="";
		String c="";
		System.out.println(a==b);
		System.out.println(b==c);
		System.out.println(int.class.equals(Integer.class));

		System.out.println(int.class.isInstance(null));
		Object[] objs=new String[]{"1","2"};
		Object[] ret=test(objs);
		System.out.println(ret==objs);
		ret=objs.clone();
		System.out.println(ret.getClass());
		//Arrays.
		String LOCATION;
		Class cls =org.slf4j.ILoggerFactory.class;

		try {
			LOCATION = URLDecoder.decode(cls.getProtectionDomain().getCodeSource()
					.getLocation().getFile(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOCATION = "";
		}
		System.out.println(LOCATION);
		ClassLoader clsLoader = cls.getClassLoader();
		final String clsAsResource = cls.getName().replace('.', '/').concat(".class");
		URL result = clsLoader != null ? clsLoader.getResource(clsAsResource) : ClassLoader.getSystemResource(clsAsResource);
		System.out.println(result);
		System.out.println(result.toExternalForm());
		Object d = 3.5;
		System.out.println(d.getClass());
		Disp disp = new Disp();
		disp = new Disp();
		disp = new Disp();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(!disp.isDestroyed())
			System.err.println("xxxxxxxxxxxxxxxxxxxxxxx");
		 sun.misc.Signal.raise(new Signal("INT"));
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
	}
}
