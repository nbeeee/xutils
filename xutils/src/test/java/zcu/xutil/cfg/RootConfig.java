package zcu.xutil.cfg;

import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import zcu.xutil.Logger;
import zcu.xutil.txm.JtaTxManager;
import zcu.xutil.txm.TxInterceptor;
import static zcu.xutil.cfg.CFG.*;

public class RootConfig implements Config{
	public void config(Binder b) throws Exception {
		val(new InitialContext()).put(b,"jndi");
		b.ref("jndi").ext(DataSource.class, "lookup","jdbc/h2db").uni(b,"datasource").die("close");
		//ref(javax.naming.Context.class).ext(DataSource.class, "lookup","jdbc/sqlserver").sig("datasource").apply("close");
		b.ref("jndi").ext(TransactionManager.class,"lookup", "java:comp/UserTransaction").uni(b,"_txm").die("shutdown");
		typ(JtaTxManager.class,null,
				b.ref("jndi").ext("lookup", "java:comp/TransactionSynchronizationRegistry"),b.ref("_txm")).uni(b,"txmanager");
		val(new TxInterceptor()).set("txManager",b.ref("txmanager")).uni(b,"txIntercept");
	}
	public static void print(NamingEnumeration e){
		Logger.LOG.info("jndi context elements");
		while( e.hasMoreElements())
			Logger.LOG.info("element: {}",e.nextElement());
	}
}
