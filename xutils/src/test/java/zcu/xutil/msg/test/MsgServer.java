package zcu.xutil.msg.test;

import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.utils.Util;

public class MsgServer{
	static Logger  logger = Logger.getLogger(MsgServer.class);
	/**
	 * @param args
	 */
	public static void main(String[] args){
		System.setProperty("java.net.preferIPv4Stack", "true");

		BrokerFactory.instance().startServer(new RemoteServiceImpl(),new ExceptionServiceImpl());

		Notify notify =new Notify();
		BrokerFactory.instance().setNotification(notify);
		BrokerFactory.instance().addListener(notify);


		RemoteService rs = BrokerFactory.instance().create(RemoteService.class,0);
		TestService ts =BrokerFactory.instance().create(TestService.class,0);
		ExceptionService es = BrokerFactory.instance().create(ExceptionService.class,0);
		GroupService as1 = BrokerFactory.instance().create(RemoteService.class.getName(), false,0);
		String methsign = Util.signature("ansyCall", Date.class,String.class);
		int i=0;
		while(true){
			String str="MSGSERVER: "+ i;
			try{
				as1.service(methsign, new Date(), str);

				logger.info("call hello return:{}",rs.hello(str, i));
				BrokerFactory.instance().sendToAll(true,"MSGSERVER multicast",str);
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				ts.signal(i, true, 1.00, 33, 'g', new byte[]{1,2,3});
				logger.info("call TestService return:{}",ts.call(i));
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				es.ansyException(i);
				//logger.info("call syncException return:{}",es.syncException(i));
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
