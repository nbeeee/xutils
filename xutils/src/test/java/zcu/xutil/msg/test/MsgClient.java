package zcu.xutil.msg.test;

import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.utils.Util;


public class MsgClient implements Runnable{
	static Logger logger = Logger.getLogger(MsgClient.class);
	RemoteService rs;
	MsgClient(RemoteService rs){
		this.rs=rs;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Notify notify = new Notify();
		BrokerFactory.instance().setNotification(notify);
		BrokerFactory.instance().addListener(notify);
		RemoteService rs = BrokerFactory.instance().create(RemoteService.class,0);
		ExceptionService es = BrokerFactory.instance().create(ExceptionService.class,0);
		logger.info("members:{}",BrokerFactory.instance().getMembers());
		logger.info("to String:{}",rs);

		GroupService as1 = BrokerFactory.instance().create(RemoteService.class.getName(), false,0);
		String methsign = Util.signature("ansyCall", Date.class,String.class);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		new Thread(new MsgClient(rs),"ClientThread").start();
		String str ="MSGCLIENT";

		Logger remotelog =Logger.getLogger("remote");

		for (int i = 0; true; i++) {
			Date date = new Date();

			remotelog.info("MSGCLIENT : {} {}",date,i );
			try {
				logger.info("syncCall returned: {}", rs.hello(str,i));
			} catch (Exception e) {
				logger.warn("!!!!!!! fail hello num: {} err: {}", i,e);
			}
			try{
				as1.service(methsign, date, "MSGCLIENT:"+i);
				es.ansyException(i);
			} catch (Exception e) {
				logger.warn("!!!!!! fail ansyCall", date);
			}
			BrokerFactory.instance().sendToAll(true,"MSGCLIENT multicast",str);
			try {

				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void run() {
		String str ="MSGCLIENT MSGCLIENT: ";
		for (int i = 0; true; i++) {
			Date date = new Date();
			try {
				logger.info("syncCall returned: {}", rs.hello(str,i));
			} catch (Exception e) {
				logger.warn("!!!!!!! fail hello num: {} err: {}", i,e.getMessage());
			}
			try{
				rs.ansyCall(date, str+ i);
				logger.info("ansyCall: {}",date);
			} catch (Exception e) {
				logger.warn("!!!!!! fail ansyCall", date);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
