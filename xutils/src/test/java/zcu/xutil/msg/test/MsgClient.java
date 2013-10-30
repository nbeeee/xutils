package zcu.xutil.msg.test;

import java.sql.SQLException;
import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.impl.BrokerFactory;
import zcu.xutil.utils.Util;


public class MsgClient implements Runnable{
	static Logger logger = Logger.getLogger(MsgClient.class);
	RemoteService rs;
	ExceptionService es ;
	TestService ts;
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
		RemoteService rs = BrokerFactory.instance().create(RemoteService.class);
		ExceptionService es = BrokerFactory.instance().create(ExceptionService.class);
		TestService ts =BrokerFactory.instance().create(TestService.class);

		logger.info("members:{}",BrokerFactory.instance().getMembers());
		logger.info("to String:{}",rs);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		MsgClient mclient= new MsgClient(rs);
		mclient.ts=ts;
		mclient.es=es;
		new Thread(mclient,"ClientThread").start();
		String str ="MSGCLIENT";

		Logger remotelog =Logger.getLogger("remote");

		for (int i = 0; true; i++) {
			Date date = new Date();

			remotelog.info("MSGCLIENT : {} {}",date,i );
			rs.hello(str,i);
			try{
				es.ansyException(i);
			} catch (Exception e) {
				logger.warn("!!!!!! fail ansyCall {}", e,date);
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
			rs.ansyCall(date, str);
			rs.hello(str, i);
			es.ansyException(i);
			try {
				es.syncException(i);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				logger.warn("!!!!!! fail ansyCall", e1);
			}
			ts.call(i);
			ts.signal(i, true, i, i, 'c', new byte[]{1,2,3});
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
