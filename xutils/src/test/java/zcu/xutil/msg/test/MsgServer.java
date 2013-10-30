package zcu.xutil.msg.test;

import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.misc.XLoggerService;
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
		BrokerFactory.instance().startServer(new RemoteServiceImpl(),new TestServer(),new ExceptionServiceImpl(),new XLoggerService());
		Notify notify =new Notify();
		BrokerFactory.instance().setNotification(notify);
		BrokerFactory.instance().addListener(notify);
	}
}
