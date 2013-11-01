package zcu.xutil.msg;

import java.util.List;

import zcu.xutil.msg.impl.Event;

public interface BrokerMgt {
	List<String> getServers();
	
	List<String> getMembers();
	
	/**
	 * 作为 http 和 jgroups 之间的代理。
	 **/
	byte[] proxy(Event event,boolean test);
}
