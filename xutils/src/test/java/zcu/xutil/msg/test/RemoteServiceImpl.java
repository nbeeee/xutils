package zcu.xutil.msg.test;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Date;
import zcu.xutil.Logger;


public class RemoteServiceImpl implements RemoteService {
	private static final Logger logger = Logger.getLogger(RemoteServiceImpl.class);


	public String hello(String str,int i) throws RemoteException {
		String s = str.length() > 50 ? str.substring(0,50) : str;
		logger.info("{}  server hello, num: {}",s,i);
		return s+"  from server:"+i;
	}


	public byte[] ansyCall(Date date, String str){
		logger.info("asyncall param: {} {}", date,str);
		return new byte[]{1,2,-1,3,4};
	}

	public static void main(String[] args) throws UnknownHostException{
		logger.info(InetAddress.getLocalHost().toString());
	}
}
