package zcu.xutil.msg.test;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;

import zcu.xutil.Logger;
import zcu.xutil.msg.SaveForRecallException;


public class RemoteServiceImpl implements RemoteService {
	private static final Logger logger = Logger.getLogger(RemoteServiceImpl.class);
	ArrayList<Integer>  list = new ArrayList<Integer>();

	public void hello(String str,int i)  {
		if(list.indexOf(i)<0){
			list.add(i);
			throw new SaveForRecallException("test SaveForRecall"+1);
		}else
			list.remove((Object)i);
		
		logger.info("{}  server hello, num: {}",str,i);
	}


	public void ansyCall(Date date, String str){
		logger.info("asyncall param: {} {}", date,str);
		
	}

	public static void main(String[] args) throws UnknownHostException{
		logger.info(InetAddress.getLocalHost().toString());
	}
}
