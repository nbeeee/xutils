package zcu.xutil.cfg.proxy;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import zcu.xutil.Disposable;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.TestService;




public class TestServiceImpl implements TestService,Disposable {
	public String echo(String str) {
		return str;
	}

	public String no(String str) {
		return str;
	}

	public void destroy() {
		System.out.println("TestServiceImpl  dispose");

	}
	public static void main(String[] args) {
		Class<?> proxyClass0 = Proxy.getProxyClass(Objutil.contextLoader(), TestService.class,java.io.Closeable.class);
		Class<?> proxyClass1 = Proxy.getProxyClass(Objutil.contextLoader(), TestService.class,java.io.Closeable.class);
		Class<?> proxyClass2 = Proxy.getProxyClass(Objutil.contextLoader(), java.io.Closeable.class,TestService.class);
		System.out.println("==" + (proxyClass1==proxyClass0)+ " eq:" +proxyClass1.equals(proxyClass0));
		System.out.println("==" + (proxyClass1==proxyClass2)+ " eq:" +proxyClass1.equals(proxyClass2));
		System.out.println(Arrays.toString(proxyClass1.getInterfaces()));
		System.out.println(Arrays.toString(proxyClass2.getInterfaces()));
	}
}
