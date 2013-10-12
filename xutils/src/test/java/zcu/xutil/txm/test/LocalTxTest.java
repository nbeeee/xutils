package zcu.xutil.txm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static zcu.xutil.cfg.CFG.*;
import static zcu.xutil.utils.Matcher.annoInherit;
import static zcu.xutil.utils.Matcher.annoWith;
import static zcu.xutil.utils.Matcher.subOf;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import javax.sql.DataSource;
import javax.transaction.Synchronization;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.sql.IdentityEntity;
import zcu.xutil.sql.MiniDataSource;
import zcu.xutil.sql.Query;
import zcu.xutil.txm.Propagation;
import zcu.xutil.txm.SmartDataSource;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.txm.TxObject;
import zcu.xutil.txm.TxTemplate;



public class LocalTxTest {
	private static final Logger logger = Logger.getLogger(LocalTxTest.class);
	Context ctx;

	@Before
	public void setUp() throws Exception {
		final org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
		dataSource.setURL("jdbc:h2:~/h2db/localtxtest;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		ctx=CFG.build(null ,new Config(){
			public void config(Binder b) throws Exception {
				b.setEnv("db.url", "jdbc:h2:~/h2db/localtxtest;DB_CLOSE_DELAY=-1");
				typ(MiniDataSource.class,dataSource,8).uni(b,"@datasource");
				typ(SmartDataSource.class,b.ref("@datasource")).set("testQuery","SELECT 1 FROM DUAL").uni(b,DataSource.class.getName());
				typ(Query.class,b.ref(DataSource.class.getName())).uni(b,"query");
				typ(EntityDaoImpl.class,b.ref("query")).uni(b,"entityDao",null,"txIntercept");
				val(new TxTemplate()).uni(b,"txTemplate");
				val(new TxTemplate(Propagation.REQUIRES_NEW)).uni(b,"reqNewTemplate");
				val(new TxInterceptor()).uni(b,"txIntercept");
			}
		});
		final Query query=(Query)ctx.getBean("query");
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
		if(ctx!=null)
			ctx.destroy();
	}

	@Test
	public void testCommit() {
		TxTemplate tp=(TxTemplate)ctx.getBean("txTemplate");
		final EntityDao ed=(EntityDao)ctx.getBean("entityDao");
		final Query query=(Query)ctx.getBean("query");
		final User user1=new User("1","xiao");
		try {
			ed.create(user1);
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		final User user2=new User("2","xzcxzc");
		final User user3=new User("3","yuejie");
		try {
			tp.execute(new Callable<Object>(){
				public Object call() throws Exception {
					TxManager.registerSync(new Synchronization(){
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
					return null;
				}

			});
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		try {
			User u = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(), u.getName());
			u = query.query(User.retrieve, User.handler, user2.getId());
			logger.info("user2 id={} user={}", user2.getId(),u);
			assertEquals(user2.getName(), u.getName());
			u = query.query(User.retrieve, User.handler, user3.getId());
			assertEquals(user3.getName(), u.getName());
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	}
	@Test
	public void testRollback() {
		TxTemplate tp=(TxTemplate)ctx.getBean("txTemplate");
		final EntityDao ed=(EntityDao)ctx.getBean("entityDao");
		final Query query=(Query)ctx.getBean("query");
		final User user2=new User("2","xzcxzc");
		final User user3=new User("3","yuejie");
		try {
			tp.execute(new Callable<Object>(){
				public Object call() throws Exception {
					TxManager.registerSync(new Synchronization(){
						public void afterCompletion(int status) {
							logger.info("testRollback ============afterCompletion {}", TxObject.nameOfStatus(status));
						}

						public void beforeCompletion() {
							logger.info("testRollback ============beforeCompletion {}");

						}
					});
					ed.create(user2);
					ed.create(user3);
					throw new RuntimeException("rollback exception");
				}
			});
		} catch (Throwable e) {
			logger.info("-----rollback exception-----{}",e.toString());
		}
		try {
			assertNull(query.query(User.retrieve, User.handler, user2.getId()));
			assertNull(query.query(User.retrieve, User.handler, user3.getId()));
		}catch (SQLException e) {
			fail(e.getMessage());
		}
	}
	@Test
	public void testRequireNew() {
		logger.info("==================testRequireNew==========================");
		final TxTemplate tp=(TxTemplate)ctx.getBean("txTemplate");
		final TxTemplate reqNew=(TxTemplate)ctx.getBean("reqNewTemplate");
		final EntityDao ed=(EntityDao)ctx.getBean("entityDao");
		final Query query=(Query)ctx.getBean("query");
		final User user1=new User("1","xiao");
		try {
			ed.create(user1);
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		//final User user2=new User("2","xzcxzc");
		final IdentityEntity identityEntity=new IdentityEntity("hello");
		try {
			tp.execute(new Callable<Object>(){
				public Object call() throws Exception {
					query.entityUpdate(IdentityEntity.create,identityEntity);
					//ed.create(user2);
					try {
						reqNew.execute(new Callable<Object>(){
							public Object call() throws Exception {
								query.entityUpdate(User.update, user1);
								return null;
							}
						});
					} catch (Throwable e) {
						e.printStackTrace();
					}
					throw new RuntimeException("rollback exception");
				}
			});
		} catch (Throwable e) {
			logger.info("---- requirenew exception---- {}",e.toString());
		}
		try {
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
			User u1 = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(),u1.getName());
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	}

	@Test
	public void testNotSupport() {
		final TxTemplate tp=(TxTemplate)ctx.getBean("txTemplate");
		final EntityDao ed=(EntityDao)ctx.getBean("entityDao");
		final Query query=(Query)ctx.getBean("query");
		final User user1=new User("1","xiao");
		try {
			ed.create(user1);
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		user1.setName("another");
		//final User user2=new User("2","xzcxzc");
		final IdentityEntity identityEntity=new IdentityEntity("hello");
		try {
			tp.execute(new Callable<Object>(){
				public Object call() throws Exception {
					query.entityUpdate(IdentityEntity.create,identityEntity);
					//ed.create(user2);
					ed.update(user1);
					throw new RuntimeException("rollback exception");
				}
			});
		} catch (Throwable e) {
			logger.info("-----notsupport exception----------{}",e.toString());
		}
		try {
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
			User u1 = query.query(User.retrieve, User.handler, user1.getId());
			assertEquals(user1.getName(),u1.getName());
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	}
}
