package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.misc.LogbackService;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.msg.impl.Handler;

public class TestServer implements TestService {

	public Object call(int i)  {
		Logger.LOG.info("TestServer {}   ", i);
		int mod = i%3;
		return mod==0 ? i : (mod==1 ? new Date() : "eeeee"+i);
	}

	public void signal(int i, boolean b, double d, long l, char c, byte[] bytes) {
		Logger.LOG.info("TestServer {} {} ", i,bytes);
	}
	public static void main(String[] args){
		System.setProperty("java.net.preferIPv4Stack", "true");
		Logger remotelog =Logger.getLogger("remote");
		remotelog.warn("=================");

		remotelog.warn("=================");
		BrokerFactory.instance().startServer(new LogbackService(),new TestServer(),new RemoteServiceImpl());
		remotelog.warn("=================");
		remotelog.warn("=================333333");
		Logger.getLogger("remote").warn("1111111111111111111");
		remotelog.warn("22222222222222222222");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Logger.getLogger("remote").warn("333333333333333333333");
		remotelog.warn("44444444444444444444444");

		Notify notify =new Notify();
		BrokerFactory.instance().setNotification(notify);
		BrokerFactory.instance().addListener(notify);

		RemoteService rs = BrokerFactory.instance().create(RemoteService.class);
		ExceptionService es =BrokerFactory.instance().create(ExceptionService.class);

		int i=0;
		while(true){
			String str="TESTSERVER: "+i;
			try{
				rs.ansyCall(new Date(), str);
				rs.hello("1111", i);
				BrokerFactory.instance().sendToAll(true,"TestServer multicast",str);

			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				es.ansyException(i);
				//es.syncException(i);
			}catch(Exception e){
				e.printStackTrace();
			}

			i++;
			try{
				Thread.sleep(2000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
