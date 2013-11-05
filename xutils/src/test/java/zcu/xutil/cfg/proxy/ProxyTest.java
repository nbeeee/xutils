package zcu.xutil.cfg.proxy;

import static org.junit.Assert.*;
import static zcu.xutil.cfg.CFG.*;
import static zcu.xutil.utils.Matcher.*;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.Empty;
import zcu.xutil.cfg.InterceptAll;
import zcu.xutil.cfg.Store;
import zcu.xutil.cfg.TestService;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.ProxyHandler;

public class ProxyTest {
	interface TestAnno extends TestService{
		
	}
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testMatcher() {
		assertTrue(subOf(ProxyTest.class).checks(ProxyTest.class));
		assertFalse(subOf(ProxyTest.class).checks(String.class));
		assertFalse(subOf(ProxyTest.class).not().checks(ProxyTest.class));
		assertTrue(subOf(ProxyTest.class).not().checks(String.class));
		assertFalse(subOf(ProxyTest.class).and(subOf(String.class)).checks(ProxyTest.class));
		assertTrue(subOf(Serializable.class).and(subOf(Comparable.class)).checks(String.class));
		assertTrue(subOf(ProxyTest.class).or(subOf(String.class)).checks(ProxyTest.class));
		assertTrue(subOf(ProxyTest.class).or(subOf(String.class)).checks(String.class));
		assertTrue(annoInherit(zcu.xutil.txm.Transactional.class).checks(TestAnno.class));
		Logger.LOG.info(subOf(ProxyTest.class).or(subOf(String.class)).toString());
	}

	@Test
	public void testProxy() {
//		List<Aspect> methodAspects = new ArrayList<Aspect>();
		InterceptorOne one=new InterceptorOne();
		InterceptorTwo two=new InterceptorTwo(one);
//		Aspect as = new SimpleAspect(subOf(TestService.class),one);
//		methodAspects.add(as);
//		as=new SimpleAspect(subOf(TestService.class),two);
//		methodAspects.add(as);
		Class proxyClass=Proxy.getProxyClass(TestServiceImpl.class.getClassLoader(),ProxyHandler.getInterfaces(TestServiceImpl.class));
		Logger.LOG.info("proxy method");
		for (Object obj:proxyClass.getDeclaredMethods())
			Logger.LOG.info(obj.toString());
		Logger.LOG.info("interface method");
		for (Object obj:TestService.class.getDeclaredMethods())
			Logger.LOG.info(obj.toString());

		ProxyHandler xpr =new ProxyHandler(TestServiceImpl.class);

		TestService service=(TestService)Util.newInstance( ProxyHandler.getProxyConstructor(TestService.class),new Object[]{ new ProxyHandler(new TestServiceImpl(),one,two)});
		for(Class clazz : service.getClass().getInterfaces()){
			System.out.println("interface name:" + clazz.getName());
		}
		String str = "ABC";
		String result =service.echo(str);
		assertEquals(str,result);
		assertEquals(1,one.count);
		assertEquals(2,two.count);
		result =service.no(str);
		assertEquals(str,result);
		assertEquals(1,one.count);
		assertEquals(4,two.count);

		xpr =new ProxyHandler(TestService.class);
		Class clazz=null;
		try {
			clazz = ProxyHandler.getProxyConstructor(InvocationHandler.class).getDeclaringClass();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.LOG.info(clazz.toString());
		Object obj =Util.newInstance( ProxyHandler.getProxyConstructor(TestService.class),new Object[]{ new ProxyHandler("dummy",new InterceptAll())});
		assertTrue(obj.equals(obj));
		Logger.LOG.info("equal : " +obj.equals(obj) + " toString:" + obj+" hash: "+obj.hashCode());

	}
	@Test
	public void testContextInherit() {
		Logger.LOG.info("testContextInherit begin");
		Context ctx1=CFG.build(null ,new Config(){
			public void config(Binder b) throws Exception {
				typ(Empty.class).uni(b,"testService",null,"InterceptAll,txIntercept");
				val(new InterceptAll()).uni(b,"InterceptAll");
				val(new TxInterceptor()).uni(b,"txIntercept");
			}
		}) ;

		Logger.LOG.info("ctx1:  {}",ctx1);
		Context ctx2=CFG.build(ctx1 ,new Config(){
			public void config(Binder b) throws Exception {
				typ(Store.class,"mystore",null).call("no", "start").call("echo", "echoStart").uni(b,"store",null,"txIntercept");
			}

		});
		Logger.LOG.info("ctx2:  {}",ctx2);

		TestService ts =(TestService)ctx2.getBean("store");
		TestService ts2=(TestService)ctx2.getBean("testService");
		Logger.LOG.info("testContextInherit =====================");
		assertEquals("echo inTx",ts.echo("echo"));
		assertEquals("no noTx",ts.no("no"));
		Logger.LOG.info("testContextInherit ----------------------------");
		assertEquals("abcdef",ts2.echo("abcdef"));
		assertEquals("hijklm",ts2.no("hijklm"));


		System.out.println("toString: " + ts2.toString()+ " hashCode: "+ts2.hashCode() + " equals: "+ts2.equals(""));
		ctx1.destroy();
		Logger.LOG.info("++++++++++++++++ testContextInherit end");
	}
}
