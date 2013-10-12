package zcu.xutil.msg.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jgroups.Address;

import zcu.xutil.Logger;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.Server;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.utils.Util;

public class MsgTest implements Runnable {
	static Logger logger = Logger.getLogger(MsgTest.class);
	RemoteService rs;

	public static void main(String[] args) {

		System.setProperty("java.net.preferIPv4Stack", "true");
		MsgTest msgtest = new MsgTest();
		msgtest.rs = BrokerFactory.instance().create(RemoteService.class, 0);

		ExceptionService es = BrokerFactory.instance().create(ExceptionService.class, 0);
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

		List<Address> address = new ArrayList<Address>(BrokerFactory.instance().getMembers());
		logger.info("allmembers {}", address);
		int length = address.size();

		RemoteService remote = BrokerFactory.instance().create(RemoteService.class, 0);
		GroupService as1 = BrokerFactory.instance().create(RemoteService.class.getName(), false, 1);
		String methsign = Util.signature("ansyCall", Date.class,String.class);
		for (Server server : BrokerFactory.instance()) {
			// remote =server.create(RemoteService.class,0);
			logger.info("Server: {}", server);
		}

		for (int i = 0; i < 10; i++) {
			// try {
			// logger.info("server return: {}",es.syncException(i));
			// } catch (Exception e) {
			// logger.info("sync exception {} number: {}", e.toString(),i);
			// }

			remoteLogger.debug("==============");
			BrokerFactory.instance().sendToNode(address.get(i % length), "sendMessage", "" + i);
			Date date = new Date();
			as1.service(methsign, date, "MSGTEST:"+i);
			try {
				logger.info("anaycall : return {}", Arrays.toString(remote.ansyCall(date, "" + i)));
				logger.info("call hello return: {}", remote.hello("sync ", i));
				es.ansyException(i);
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
