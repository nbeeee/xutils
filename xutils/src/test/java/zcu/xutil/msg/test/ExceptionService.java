package zcu.xutil.msg.test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

import zcu.xutil.msg.GroupService;

@GroupService
public interface ExceptionService {
	int syncException(int i) throws SQLException;
	void ansyException(int i);
}
