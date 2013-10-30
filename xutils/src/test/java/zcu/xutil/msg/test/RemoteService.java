package zcu.xutil.msg.test;

import java.rmi.RemoteException;
import java.util.Date;

import zcu.xutil.msg.GroupService;

@GroupService(asyncall=true)
public interface RemoteService{
	void hello(String str,int i);
	void ansyCall(Date date,String str);
}
