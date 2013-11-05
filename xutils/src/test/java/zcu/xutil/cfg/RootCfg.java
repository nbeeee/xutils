package zcu.xutil.cfg;


import static zcu.xutil.utils.Matcher.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Test;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.Binder;
import zcu.xutil.sql.Query;
import zcu.xutil.txm.Transactional;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.utils.Disp;




public class RootCfg implements Config{
	private static final Logger logger = Logger.getLogger(RootCfg.class);
	
	public void config(Binder b) throws Exception{
		Object obj=b.getEnv("javax.naming.Context");

		if(obj instanceof javax.naming.Context){
			Logger.LOG.info("javax.naming.Context. {} ",obj);
			//javax.naming.Context c = (javax.naming.Context)obj;

		}
		CFG.typ(Disp.class,"rootCfg-Disp").uni(b,"rDisp");
		Logger.LOG.info("root config end.");
	}
	public static void main(String[] args) {
		Context container = CFG.build(CFG.root() ,new Config(){
			Binder binder;
			  public void config(Binder b) throws Exception {
				b.setEnv("env.test", "myttttt");
				CFG.typ(Store.class,"ssssss",CFG.arr(Book.class,new Book("bkn",11.1,4))).call("start").put(b,"store",null,"");
	
			  }
		  });
		  Query query = (Query)container.getBean("query");
		  logger.info("datasourec: {}",query.getDataSource());
		 
		  TestService ts=(TestService)container.getBean("store");
		  logger.info(ts.echo("echo method"));
		  logger.info(ts.no("no method"));
		  logger.info(ts.toString());
		  logger.info(ts.getClass().toString());

		  TestService ts2=(TestService)container.getBean("store");
		  logger.info(" equals " + (ts==ts2));
		  Provider store = (Provider)container.getProvider("store");
		for(NProvider np : CFG.root().listMe())
				logger.info("{}:{}",np.getName(),container.getBean(np.getName()));
		for(NProvider np : container.listMe())
			logger.info("{}:{}",np.getName(),container.getBean(np.getName()));
		try {
			((zcu.xutil.web.Action)container.getBean("action")).execute(null);
		} catch (Exception e) {
			e.printStackTrace();
			Objutil.rethrow(e);
		}
		container.destroy();
	}
}
