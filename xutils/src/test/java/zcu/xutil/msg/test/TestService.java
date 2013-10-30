package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;

import zcu.xutil.msg.GroupService;

@GroupService
public interface TestService{
	Object call(int i);
	void signal(int i,boolean b,double d,long l,char c,byte[] bytes);
}
