package zcu.xutil.msg.test;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Date;
import zcu.xutil.Logger;


public class RemoteServiceImpl implements RemoteService {
	private static final Logger logger = Logger.getLogger(RemoteServiceImpl.class);


	public void hello(String str,int i)  {
		String s = str.length() > 50 ? str.substring(0,50) : str;
		logger.info("{}  server hello, num: {}",s,i);
		
	}


	public void ansyCall(Date date, String str){
		logger.info("asyncall param: {} {}", date,str);
		
	}

	public static void main(String[] args) throws UnknownHostException{
		logger.info(InetAddress.getLocalHost().toString());
	}
}
