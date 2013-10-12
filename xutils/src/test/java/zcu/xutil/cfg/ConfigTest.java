package zcu.xutil.cfg;

import static org.junit.Assert.*;
import static zcu.xutil.cfg.CFG.*;
import static zcu.xutil.utils.Matcher.*;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;




import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.FirstBean;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.cfg.RefCaller;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.CtxAwareTest.TestFactory;
import zcu.xutil.txm.Transactional;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.utils.Disp;
import zcu.xutil.utils.Util;




public class ConfigTest {
	private static final Logger logger = Logger.getLogger(ConfigTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	//@Test
	public void testStartup() {
	}
	@Test
	public void testXarray() {
		String [] str= {"abc","def","abc"};
		String[] arr =(String[]) CFG.arr(String.class,(Object[])str).matches(str.getClass(),State.dummy).instance();
		assertArrayEquals(str, arr);
		List list =(List) CFG.arr(String.class,(Object[])str).matches(List.class,State.dummy).instance();
		assertArrayEquals(str, list.toArray(new String[]{}));
		String [] ss= {"1","2","3"};
		int[] ii = (int[])CFG.arr(int.class,(Object[])ss).instance();
		assertArrayEquals(new int[]{1,2,3}, ii);
	}
	@Test
	public void testCollection() {
		//System.gc();
		logger.info("-----testCollection------"+Store.logo);
		final Book blueBook = new Book("blueBook",4.0d,56);
		final Book redBook=new Book("redBook",3.58, 100);
		logger.info("build start !!!");
		Context ctx=CFG.build(null ,new Config(){
			public void config(Binder b) throws Exception {
				b.setEnv("InterceptAll.0", "InterceptAll:");
				typ(Book.class,"number",1,2).put(b,"numb");
				typ(Book.class,"redBook",3.58d,100).set("str",b.ref("InterceptAll.0")).put(b,"redBook");
				val(new Book("bookbook",4.0d,56)).uni(b,"bookbook");
				typ(Store.class, "mystore",arr(Book.class,b.ref("redBook"),blueBook)).uni(b,"store");
				typ(Store.class, "mystore","list",new ArrayList()).uni(b,"storelist");
				typ(Store.class, "mystore","list",arr(Book.class,blueBook)).uni(b,"storelist2");
				typ(Store.class, "mystore",arr(Book.class,b.ref("redBook"))).uni(b,"setset");
				typ(Store.class,"mystore",arr(Book.class,blueBook)).uni(b,"mapmap");
				typ(Store.class, "mystore",arr(Book.class)).uni(b,"storevar");
				typ(FirstBean.class)
						.call("setFirstString", "string")
						.call("setFirstInteger", 12345)
						.call("setFirstInt", 1).uni(b,"firstbean");
				b.ref("firstbean").ext("getFirstString").uni(b,"factory");
			}
		});
		logger.info("build over !!!   "+ctx);
		Book redBook1=(Book)ctx.getBean("redBook");
		Book redBook2=(Book)ctx.getBean("redBook");
		assertEquals(redBook,redBook2);
		assertTrue(redBook1!=redBook2);
		assertEquals("InterceptAll:",redBook2.str);
		logger.info("redbook   "+redBook1);
		logger.info("redbook   "+redBook1);
		Book bookBook1=(Book)ctx.getBean("bookbook");
		Book bookBook2=(Book)ctx.getBean("bookbook");
		assertSame(bookBook1,bookBook2);
		Book num=(Book)ctx.getBean("numb");
		assertEquals(1.0,num.price,0.00000001);
		Store storevar=(Store)ctx.getBean("storevar");
		assertNotNull(storevar.books);
		assertEquals(0,storevar.books.length);
		logger.info("storevar   "+storevar);

		Store store=(Store)ctx.getBean("store");
		assertArrayEquals(new Book[]{redBook1,blueBook}, store.books);
		assertSame(blueBook, store.books[1]);
		logger.info("store   "+store);


		Store storelist=(Store)ctx.getBean("storelist");
		assertEquals(0,storelist.booklist.size());
		logger.info("storelist   "+storelist);

		Store storelist2=(Store)ctx.getBean("storelist2");
		assertEquals(1,storelist2.booklist.size());
		logger.info("storelist2   "+storelist2);

		Store storeset=(Store)ctx.getBean("mapmap");
		assertTrue(storeset.books.length==1);
		logger.info("storeset   "+storeset);

		storeset=(Store)ctx.getBean("setset");
		assertTrue(storeset.books.length==1);
		logger.info("storeset2   "+storeset);

		logger.info(ctx.getBean("firstbean").toString());
		Provider pv= ctx.getProvider("factory");
		logger.info(pv.getType().toString() + "   "+ pv.instance());
		assertEquals(String.class,pv.getType());
		assertEquals("string",ctx.getBean("factory"));
		ctx.destroy();
		logger.info("-----testCollection end------");
	}

	@Test
	public void testInterceptor() {
		System.out.println("-----testInterceptor------");
		//final SmartDataSource sds =new SmartDataSource(null);
		int begin=Disp.disposeSize();
		logger.info("build start !!!");
		Context ctx=CFG.build(null ,new Config(){
			public void config(Binder b) throws Exception {
				typ(Store.class).call("no","start").call("echo", "echoStart").uni(b,"store",null,"txIntercept");
				typ(Empty.class).uni(b,"testService",null,"txIntercept","InterceptAll:");
				val(new TxInterceptor()).uni(b,"txIntercept");
				val(new InterceptAll()).uni(b,"InterceptAll:");
			}
		});
		logger.info("build over !!!");
		for(NProvider name : ctx.getProviders(Object.class)){
			logger.info("provider: " + name);
		}

		TestService ts =(TestService)ctx.getBean("store");
		TestService ts2=(TestService)ctx.getBean("testService");

		assertEquals("echo inTx",ts.echo("echo"));
		assertEquals("no noTx",ts.no("no"));

		assertEquals("abcdef",ts2.echo("abcdef"));
		assertEquals("hijklm",ts2.no("hijklm"));
		System.out.println("weakRef size="+Disp.disposeSize());
		ctx.destroy();
	}

	@Test
	public void testMutliThread() {
		System.out.println("-----testMutliThread------");
		ContextThread thread= new ContextThread();
		thread.start();
		while(thread.ctx==null){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		TestService ts =(TestService)thread.ctx.getBean("store");
		TestService ts2=(TestService)thread.ctx.getBean("testService");

		assertEquals("echo inTx",ts.echo("echo"));
		assertEquals("no noTx",ts.no("no"));

		assertEquals("abcdef",ts2.echo("abcdef"));
		assertEquals("hijklm",ts2.no("hijklm"));
		System.out.println("weakRef size="+Disp.disposeSize());
		thread.exit=true;
		while(thread.ctx!=null){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("weakRef size="+Disp.disposeSize());
	}
	@Test
	public void testFactory() {
		System.out.println("--------------testFactory: ");
		Context ctx = CFG.build(null ,new Config() {
			public void config(Binder b) throws Exception {
				typ(TestFactory.class).uni(b,"factory");
				typ(CtxAwareTest.class,arr(String.class,b.ref("factory").ext("instance"))).uni(b,"oneArr");
				typ(CtxAwareTest.class,arr(String.class,"-----",b.ref("factory").ext(Object.class,"instance").cast(String.class))).uni(b,"twoArr");
				typ(CtxAwareTest.class,"-----").uni(b,"threeArr");
				typ(Book.class, "number", 1, 2).put(b,"numb");
				typ(Empty.class, val(null).cast(String.class)).put(b,"NUL");
				typ(Store.class, "ssssss",arr(Book.class, new Book("bkn", 11.1, 4))).call("start").put(b,"store");
				typ(Empty.class, b.ref("store").cast(TestService.class)).uni(b,"empty");
				typ(CtxAwareTest.class).set("factory",b.ref("numb")).put(b,"nine");
				typ(CtxAwareTest.class).set("factory", b.ref("factory")).put(b,"ten");
				
				typ(TestFactory.class).ext("getObject").uni(b,"12");
			}
			public void onStart(Context ctx){
				ctx.getBean("empty");
			}
		});
		CtxAwareTest nine = (CtxAwareTest) ctx.getBean("nine");
		System.out.println("factory: " + nine.provider);
		assertTrue(!(nine.provider instanceof TestFactory));
		CtxAwareTest ten = (CtxAwareTest) ctx.getBean("ten");
		System.out.println("factory: " + ten.provider);
		assertTrue(ten.provider instanceof TestFactory);
		List l1=ctx.getProviders(CtxAwareTest.class);
		List l2=ctx.getProviders(CtxAwareTest.class);
		assertTrue(l1==l2);
		List l3=ctx.getProviders(CtxAwareTest.class);
		assertTrue(l1==l3);
		System.out.println("getProviders list : " + l3);
		ctx.getBean("oneArr");
		ctx.getBean("twoArr");
		ctx.getBean("threeArr");
		assertEquals("factory getObject", ctx.getBean("12"));
		ctx.destroy();
	}
	@Test
	public void testBaseProvider() throws CloneNotSupportedException {
		System.out.println("-------------testBaseProvider: ");
		RefCaller baseType = CFG.typ(CtxAwareTest.class).set("beanName", "xiaozaichu");
		RefCaller field1 = baseType.set("field", "111");
		RefCaller field2 = baseType.clone().set("field", "222");
		CtxAwareTest f1 = (CtxAwareTest) field1.get().instance();
		assertEquals("xiaozaichu", f1.beanName);
		assertEquals("111", f1.field);
		CtxAwareTest f2 = (CtxAwareTest) field2.get().instance();
		assertEquals("xiaozaichu", f2.beanName);
		assertEquals("222", f2.field);
		CtxAwareTest tmp=f1;
		f1 = (CtxAwareTest) field1.get().instance();
		assertTrue(f1!=tmp);
		assertEquals("xiaozaichu", f1.beanName);
		assertEquals("111", f1.field);
		f2 = (CtxAwareTest) field2.get().instance();
		assertEquals("xiaozaichu", f2.beanName);
		assertEquals("222", f2.field);

		RefCaller instance1 = CFG.val(new CtxAwareTest()).set("beanName", "xiaozaichu");
		RefCaller instance2 =instance1.set("field", "111");
		assertTrue(instance1==instance2);
		f1 = (CtxAwareTest) instance1.get().instance();
		assertEquals("xiaozaichu", f1.beanName);
		assertEquals("111", f1.field);

	}
	@Test
	public void testXML() {
		Context ctx = CFG.build(null ,new Config(){
			@Override
			public void config(Binder b) throws Exception {
				b.bind(b.loader().getResource("test.xml"));
			}

		});

		Map map = (Map)ctx.getBean("map");
		assertEquals("", map.get("empty"));
		assertEquals(124,((Integer)map.get("int")).intValue());
		assertArrayEquals(new byte[]{3,4,5},(byte[]) map.get("bytearray"));
		Object[] objects = new Object[]{(byte)0,1,2L,(short)3,4d,5f,true,'C'};
		assertEquals(Arrays.asList(objects),map.get("objectlist"));
		FirstBean fb = (FirstBean)ctx.getBean("firstBean");
		assertNull(fb.getFirstString());
		assertEquals(999999,fb.getFirstInt());
		assertEquals(9999,fb.getFirstInteger().intValue());
		assertArrayEquals(new byte[]{1,3,5},fb.getFirstbytes());
		assertEquals("oooooooooo",ctx.getBean("output"));
		URL url = (URL)ctx.getBean("staticFactory");
		assertEquals(Objutil.toURL(new java.io.File(Objutil.systring("xutils.home"))),url);

	}
	@Test
	public void testInherit() {
		System.out.println("-------------testInherit: ");
		Context root;
		try{
		root = CFG.build(null ,new Config() {
			public void config(Binder b) throws Exception {
				typ(ArrayList.class).uni(b,"empty");
				typ(Disp.class, val("rootCfg-Disp").cast(String.class)).uni(b,"rDisp");
				typ(Disp.class, val("null name").cast(String.class)).uni(b,"");

			}
		});
		}catch(RuntimeException e){
			e.printStackTrace();
			throw e;
		}

		Context sun = CFG.build(root ,new Config() {
			public void config(Binder b) throws Exception {
				b.ref("empty").put(b,"empty");
				val(new TxInterceptor()).uni(b,"TxInterceptor");
				typ(CtxAwareTest.class).set("context",b.ref("")).uni(b,"innerbuilder").eager(true);
			}
		});

		Context sunsun =  CFG.build(sun ,new Config(){
			public void config(Binder b) throws Exception {
				b.ref("rDisp").put(b,"rDisp");
				b.ref("empty").put(b,"empty333");
				typ(Store.class, "ssssss", arr(Book.class,new Book("bkn", 11.1, 4))).call("start").put(b,"store");
				typ(Empty.class, b.ref("store").cast(TestService.class)).uni(b,"empty");
				typ(Empty.class, b.ref("store").cast(TestService.class), new java.sql.Date(1)).uni(b,"empty2");
				typ(Empty.class, b.ref("store"), new java.util.Date()).uni(b,"empty3");
			}
		});
		assertTrue(root.getBean("empty") instanceof ArrayList);
		assertTrue(sunsun.getBean("empty") instanceof Empty);
		sunsun.getBean("empty2");
		sunsun.getBean("empty3");
		sunsun.getBean("TxInterceptor");
		List<NProvider> names = sunsun.getProviders(Object.class);
		logger.info("order sunsun map :{}", names);
		assertTrue(names.indexOf(sunsun.getProvider("empty")) == names.size() - 3);
		sunsun.destroy();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		Disp ds=(Disp)sun.getBean("rDisp");
		assertTrue(!ds.isDestroyed());
		names = sun.getProviders(Object.class);
		logger.info("order sun map :{}", names);
		assertTrue(root.getProvider("empty").instance()==sun.getProvider("empty").instance());
		logger.info("raw ref empty "+ sun.getBean("empty"));

		Context empty =  CFG.build(root ,new Config(){
			public void config(Binder b) throws Exception {
			}});

		root.destroy();
		Exception ex = null;
		try {
			sun.getBean("txobject");
		} catch (Exception e) {
			ex = e;
		}
		assertTrue(ex!=null);
		System.gc();
	}
	static class ContextThread extends Thread{
		volatile Context ctx;
		volatile boolean exit;

		public void run(){
			ctx= CFG.build(null ,new Config(){
				public void config(Binder b) throws Exception {
					typ(Store.class).call("no", "call no").call("echo", "call ech0").uni(b,"store",null,"txIntercept");
					typ(Empty.class).uni(b,"testService",null,"txIntercept","InterceptAll:");
					val(new TxInterceptor()).uni(b,"txIntercept");
					val(new InterceptAll()).uni(b,"InterceptAll:");
				}
			});
			while(!exit){
				try {
					sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ctx.destroy();
			ctx=null;
		}
	}
	private static final Map<Integer, Field[]> fieldCache = new LinkedHashMap<Integer, Field[]>(64, 0.75f, true) {
		private static final long serialVersionUID = 2106901336834881606L;
		protected boolean removeEldestEntry(Map.Entry<Integer,Field[]> eldest) {
			if(size() >= 96){
				System.out.println(">=96 "+eldest.getKey() + eldest.getValue());
				return true;
			}
			if(size()>= 64){
				if(eldest.getValue()==null){
					System.out.println(">=64 "+eldest.getKey() + eldest.getValue());
					return true;
				}
				get(eldest.getKey());
			}
			return false;
		}
	};
}
