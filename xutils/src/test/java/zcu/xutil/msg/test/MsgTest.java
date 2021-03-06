﻿package zcu.xutil.msg.test;

import java.io.IOException;
import java.util.Date;
import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.msg.Server;
import zcu.xutil.msg.impl.BrokerFactory;


public class MsgTest implements Runnable {
//	static{
//	System.setProperty("java.net.preferIPv4Stack", "true");
//	}
	static Logger logger = Logger.getLogger(MsgTest.class);
	RemoteService rs;

	public static void main(String[] args) {

		
		MsgTest msgtest = new MsgTest();
		msgtest.rs = BrokerFactory.instance().create(RemoteService.class);

		ExceptionService es = BrokerFactory.instance().create(ExceptionService.class);
		Logger remoteLogger = Logger.getLogger("remote");

		// Thread t1=new Thread(msgtest);
		// t1.setDaemon(true);
		// Thread t2=new Thread(msgtest);
		// t2.setDaemon(true);
		//
		// t1.start();
		// t2.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Notify notify = new Notify();
		BrokerFactory.instance().setNotification(notify);
		BrokerFactory.instance().addListener(notify);
		logger.info("allmembers {}", BrokerFactory.instance().getMembers());
		for (Server s : BrokerFactory.instance())
			logger.info("Server {}", s);

		RemoteService remote = BrokerFactory.instance().create(RemoteService.class);
		
		for (int i = 0; i < 3; i++) {
			// try {
			// logger.info("server return: {}",es.syncException(i));
			// } catch (Exception e) {
			// logger.info("sync exception {} number: {}", e.toString(),i);
			// }

			remoteLogger.debug("============== " +i);
			BrokerFactory.instance().sendToNode(Objutil.systring(Constants.XUTILS_LOCALHOST), "sendMessage", "" + i);
			Date date = new Date();
			try {
				
				remote.ansyCall(date, "" + i);
				remote.hello("sync ", i);
				es.ansyException(i);
				es.syncException(i);
			} catch (Throwable e) {
				logger.info("hello number: {}", e, i);
			}



			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// try {
		// es.ansyException(2);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// try {
		// es.ansyException(2);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		while (true) {
			try {
				int c = System.in.read();
				if (c == -1 || c == 'x') { // -1 means Ctrl-C has been pressed
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.exit(0);
		// BrokerSingleFactory.instance().destroy();
	}

	public void run() {
		for (int i = 0; i < 20; i++) {
			String str = Integer.toString(i);
			// try {
			// logger.info(rs.hello(str,i));
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			rs.ansyCall(null, str);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
