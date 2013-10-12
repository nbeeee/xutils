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
		RemoteService httpremote = HttpBrokerFactory.instance().create(RemoteService.class,0);
		// httpremote.ansyCall(new Date(), "http ansy");
		logger.info("RemoteService begin");
		for (int i = 0; i < 1000; i++) {
			try {
				httpremote.hello("http hello", i);
			} catch (RemoteException e) {
				throw new XutilRuntimeException(e);
			}
		}
		logger.info("RemoteService end");
	}

	//@Test
	public void testPerformanceB() {
		TestService httptest = HttpBrokerFactory.instance().create(TestService.class,0);
		logger.info("TestService begin");
		for (int i = 0; i < 1000; i++)
			try {
				httptest.call(i);
			} catch (RemoteException e) {
				throw new XutilRuntimeException(e);
			}
		logger.info("TestService end");
	}

	public static void main(String[] args) {
		SimpleBroker hb = HttpBrokerFactory.instance();
		RemoteService httpremote = hb.create(RemoteService.class,0);
		TestService httptest = hb.create(TestService.class,0);
		for (int i = 0; i < 10; i++) {
			try {
				logger.info( "remote ansyCall return: {}",httpremote.ansyCall(new Date(), "http ansy"));
				logger.info( "remote hello return: {}",httpremote.hello("http hello", i));
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
