package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TestService extends Remote{
	Object call(int i) throws RemoteException;
	void signal(int i,boolean b,double d,long l,char c,byte[] bytes);
}
