package zcu.xutil.msg.test;

import java.rmi.RemoteException;
import java.util.Date;

import sun.misc.Signal;

import zcu.xutil.Logger;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.msg.SimpleBroker;
import zcu.xutil.msg.impl.HttpBrokerFactory;

public class HttpTest {
	static Logger logger = Logger.getLogger(HttpTest.class);

	//@Test
	public void testPerformanceA() {
		RemoteService httpremote = HttpBrokerFactory.instance().create(RemoteService.class);
		// httpremote.ansyCall(new Date(), "http ansy");
		logger.info("RemoteService begin");
		for (int i = 0; i < 1000; i++) {
			httpremote.hello("http hello", i);
		}
		logger.info("RemoteService end");
	}

	//@Test
	public void testPerformanceB() {
		TestService httptest = HttpBrokerFactory.instance().create(TestService.class);
		logger.info("TestService begin");
		for (int i = 0; i < 1000; i++)
				httptest.call(i);
		logger.info("TestService end");
	}

	public static void main(String[] args) {
		SimpleBroker hb = HttpBrokerFactory.instance();
		RemoteService httpremote = hb.create(RemoteService.class);
		TestService httptest = hb.create(TestService.class);
		for (int i = 0; i < 10; i++) {
			try {
				httpremote.ansyCall(new Date(), "http ansy");
				httpremote.hello("http hello", i);
				httptest.signal(1,true,20.00,9876543210L, 'x', new byte[]{1,2});

				logger.info( "test call return: {}",httptest.call(i));
				Thread.sleep(1000);
			} catch (Exception e) {
				logger.warn("ex:",e);
			}
		}
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Signal.raise(new Signal("INT"));
	}

}
