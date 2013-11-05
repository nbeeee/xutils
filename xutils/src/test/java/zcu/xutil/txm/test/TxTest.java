package zcu.xutil.txm.test;

import static org.junit.Assert.*;
import static zcu.xutil.cfg.CFG.*;
import static zcu.xutil.utils.Matcher.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NoneBeanException;
import zcu.xutil.cfg.RootConfig;
import zcu.xutil.sql.IdentityEntity;
import zcu.xutil.sql.Query;
import zcu.xutil.txm.JtaTxManager;
import zcu.xutil.txm.Propagation;
import zcu.xutil.txm.Transactional;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.txm.TxObject;
import zcu.xutil.txm.TxTemplate;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public class TxTest {
	private static final Logger logger = Logger.getLogger(TxTest.class);

	Context ctx;

	@Before
	public void setUp() throws Exception {
		// final BasicDataSource ds = new BasicDataSource();
		// ds.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
		// ds.setUsername("user");
		// ds.setPassword("user");
		// ds.setUrl("jdbc:derby:D:\\myDB;create=true");
		// ds.setMaxActive(8);
		// ds.setMaxIdle(4);
		// ds.setValidationQuery("SELECT COUNT(*) FROM SYS.SYSTABLES WHERE 1 = 0");
		// ds.setTimeBetweenEvictionRunsMillis(1000L*60L*30L);
		// final SmartDataSource myDataSource new SmartDataSource(ds,"close");
//
//		final PoolingDataSource myDataSource = new PoolingDataSource();
//		// myDataSource.setClassName("org.apache.derby.jdbc.EmbeddedXADataSource");
//		myDataSource.setClassName("org.h2.jdbcx.JdbcDataSource");
//		// myDataSource.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
//		// myDataSource.setUniqueName("derby");
//		myDataSource.setUniqueName("h2db");
//		myDataSource.setMinPoolSize(1);
//		myDataSource.setMaxPoolSize(5);
//		// myDataSource.setAcquireIncrement(1);
//		myDataSource.setAllowLocalTransactions(true);
//		// myDataSource.setTestQuery("SELECT COUNT(*) FROM SYS.SYSTABLES WHERE 1 = 0");
//		myDataSource.setTestQuery("SELECT 1 FROM DUAL");
//		// myDataSource.getDriverProperties().setProperty("driverClassName",
//		// "org.h2.Driver");
//		myDataSource.getDriverProperties()
//				.setProperty("URL", "jdbc:h2:file:d:/temp/h2db/txtest;DB_CLOSE_ON_EXIT=FALSE");
//		// myDataSource.getDriverProperties().setProperty("url",
//		// "jdbc:h2:file:d:/temp/h2db/txtest;DB_CLOSE_DELAY=-1");
//		myDataSource.getDriverProperties().setProperty("user", "sa");
		// myDataSource.getDriverProperties().setProperty("password","");
		// myDataSource.getDriverProperties().setProperty("user", "app");
		// myDataSource.getDriverProperties().setProperty("password", "app");
		// myDataSource.getDriverProperties().setProperty("createDatabase",
		// "true");
		// myDataSource.getDriverProperties().setProperty("databaseName",
		// "D:\\DerbyDatabases\\myDB");
		//myDataSource.init();
		//TransactionManagerServices.getConfiguration().setServerId("xzcutil-btm");
		System.setProperty(Constants.XUTILS_CFG_ROOT,RootConfig.class.getName());
		System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,"bitronix.tm.jndi.BitronixInitialContextFactory");
		System.setProperty("btm.root", Objutil.systring("btm.root"));
		System.setProperty("bitronix.tm.configuration", Objutil.systring("btm.root")+"/conf/btm-config.properties");
		ctx = CFG.build(CFG.root() ,new Config(){
			public void config(Binder b) throws Exception {
				typ(Query.class, b.ref("datasource")).uni(b,"query");
				typ(EntityDaoImpl.class, b.ref("query")).uni(b,"entityDao",null,"");
				val(new TxTemplate()).set("txManager",b.ref("txmanager")).uni(b,"txTemplate");
				val(new TxTemplate(Propagation.REQUIRES_NEW)).set("txManager",b.ref("txmanager")).uni(b,"reqNewTemplate");
			}
		});
		final Query query = (Query) ctx.getBean("query");
		if(query.tableExist("t_user"))
		try {

			query.update(User.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		try {
			query.update(IdentityEntity.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		query.update(User.createtable);
		query.update(IdentityEntity.createtable);

	}

	@After
	public void tearDown() throws Exception {
		if (ctx != null)
			ctx.destroy();
	}

	@Test
	public void testCommit() {
		TxTemplate tp = (TxTemplate) ctx.getBean("txTemplate");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		final Query query = (Query) ctx.getBean("query");
		UserTransaction ut = (UserTransaction) ctx.getBean("_txm");

		final User user1 = new User("1", "xiao");
		try {
			ed.create(user1);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		final User user2 = new User("2", "xzcxzc");
		final User user3 = new User("3", "yuejie");

		try {
			ut.begin();
			TxManager.registerSync(new Synchronization() {
				public void afterCompletion(int status) {
					logger.info("testCommit ============afterCompletion {}", TxObject.nameOfStatus(status));
				}

				public void beforeCompletion() {
					logger.info("testCommit ============beforeCompletion {}");

				}
			});
			ed.create(user2);
			ed.create(user3);
			query.entityUpdate(User.update, user1);
			ut.commit();

		} catch (Throwable e) {
			try {
				ut.rollback();
			} catch (Exception se) {
				e.printStackTrace();
			}
			// TODO Auto-generated catch block
			fail(e.getMessage());
			return;
		}
		try {
			User u = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(), u.getName());
			u = query.query(User.retrieve, User.handler, user2.getId());
			logger.info("user2 id={} user={}", user2.getId(), u);
			assertEquals(user2.getName(), u.getName());
			u = query.query(User.retrieve, User.handler, user3.getId());
			assertEquals(user3.getName(), u.getName());
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	}

	@Test
	public void testRollback() {
		final TxTemplate tp = (TxTemplate) ctx.getBean("txTemplate");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		final Query query = (Query) ctx.getBean("query");
		final User user2 = new User("2", "xzcxzc");
		final User user3 = new User("3", "yuejie");
		UserTransaction ut = (UserTransaction) ctx.getBean("_txm");

		try {
			ut.begin();
			TxManager.registerSync(new Synchronization() {
				public void afterCompletion(int status) {
					logger.info("testRollback ============afterCompletion {}", TxObject.nameOfStatus(status));
				}

				public void beforeCompletion() {
					logger.info("testRollback ============beforeCompletion {}");

				}
			});

			ed.create(user2);
			ed.create(user3);
			tp.execute(new Callable<Object>() {
				public Object call() throws Exception {
					throw new RuntimeException("=============rollback exception");
				}
			});

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception se) {
				e.printStackTrace();
			}
			logger.info("-----rollback exception-----{}", e.getMessage());
		}
		try {
			assertNull(query.query(User.retrieve, User.handler, user2.getId()));
			assertNull(query.query(User.retrieve, User.handler, user3.getId()));
		} catch (SQLException e) {
			fail(e.getMessage());
		}
	}
	//@Test
	public void testNotSupport() {
		final TxTemplate tp = (TxTemplate) ctx.getBean("txTemplate");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		final Query query = (Query) ctx.getBean("query");
		final User user1 = new User("1", "xiao");
		try {
			ed.create(user1);
			User uuuuu = query.query(User.retrieve, User.handler, user1.getId());
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		final UserTransaction ut = (UserTransaction) ctx.getBean("_utx");
		// final User user2=new User("2","xzcxzc");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		try {
			logger.info("111111trx status: {}",ut.getStatus() );
			tp.execute(new Callable<Object>() {
				public Object call() throws Exception {
					User uu = query.query(User.retrieve, User.handler, user1.getId());
					logger.info("uuuuuuu: {}",uu.getName());
					query.entityUpdate(IdentityEntity.create, identityEntity);
					logger.info("222222trx status: {}",ut.getStatus() );
					// ed.create(user2);
					ed.update(user1);
					logger.info("33333trx {} status: {}",uu.getName() , ut.getStatus() );
					throw new NoneBeanException("rollback exception");
				}
			});
		} catch (NoneBeanException e) {
			logger.info("-----notsupport exception----------{}", e.toString());
		}catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
			return;
		}

		try {
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
			logger.info("444444trx status: {}",ut.getStatus() );
			User u1 = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(), u1.getName());
		} catch (SQLException e) {
			logger.info("555555555", e);
			fail(e.getMessage());
			return;
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	//@Test
	public void testRequireNew() {
		logger.info("==================testRequireNew==========================");
		final TxTemplate tp = (TxTemplate) ctx.getBean("txTemplate");
		final TxTemplate reqNew = (TxTemplate) ctx.getBean("reqNewTemplate");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		final Query query = (Query) ctx.getBean("query");
		final User user1 = new User("1", "xiao");
		try {
			ed.create(user1);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		// final User user2=new User("2","xzcxzc");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		try {
			tp.execute(new Callable<Object>() {
				public Object call() throws Exception {
					query.entityUpdate(IdentityEntity.create, identityEntity);
					// ed.create(user2);
					try {
						reqNew.execute(new Callable<Object>() {
							public Object call() throws Exception {
								query.entityUpdate(User.update, user1);
								return null;
							}
						});
					} catch (Throwable e) {
						e.printStackTrace();
					}
					throw new NoneBeanException("rollback exception");
				}
			});
		} catch (NoneBeanException e) {
			logger.info("---- requirenew exception---- {}", e.toString());
		}catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		try {
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
			User u1 = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(), u1.getName());
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	}
}
