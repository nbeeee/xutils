package zcu.xutil.msg.test;

import java.util.List;

import org.jgroups.Address;

import zcu.xutil.Logger;
import zcu.xutil.msg.MsgListener;
import zcu.xutil.msg.Notification;



public class Notify implements Notification , MsgListener {
	static Logger logger =Logger.getLogger(Notify.class);
	public void onViewChange(List<Address> lefts,List<Address> joins) {
		logger.info("============lefts: {}  joins: {}",lefts,joins);
	}
	public void onEvent( Address from,String name,String value,List<Object> params ) {
		logger.info("multicast:{}, {}, {} , {}",from, name,value,params);
	}
}
