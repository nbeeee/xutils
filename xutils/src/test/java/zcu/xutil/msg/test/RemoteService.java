package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

public interface RemoteService extends Remote{
	String hello(String str,int i) throws RemoteException;
	byte[] ansyCall(Date date,String str);
}
