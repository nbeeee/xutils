package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;


public interface ExceptionService extends Remote{
	int syncException(int i) throws RemoteException,SQLException;
	void ansyException(int i);
}
