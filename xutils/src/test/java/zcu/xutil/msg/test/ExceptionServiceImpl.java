package zcu.xutil.msg.test;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Random;

import zcu.xutil.Logger;
import zcu.xutil.msg.impl.UnavailableException;



public class ExceptionServiceImpl implements ExceptionService {
	private static final Logger logger = Logger.getLogger(ExceptionServiceImpl.class);
	private static final Random rand = new Random();

	public void ansyException(int i){
		rand.nextInt(10);
		if(rand.nextInt(10)==0)
			throw new UnavailableException(" ============ user rescue test "+i);
		logger.info("ansyCall, num: {}",i);
	}

	public int syncException(int i) throws RemoteException,SQLException {
		if(rand.nextInt(10)==0)
			throw new SQLException("syncException from server: "+i);
		logger.info("syncCall ,num: {}",i);
		return i;
	}
}
