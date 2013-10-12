package zcu.xutil.txm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static zcu.xutil.cfg.CFG.*;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.RootConfig;
import zcu.xutil.orm.HEntityManager;
import zcu.xutil.orm.SessionFactoryBean;
import zcu.xutil.sql.IdentityEntity;
import zcu.xutil.sql.Query;
import zcu.xutil.txm.JtaTxManager;
import zcu.xutil.txm.Transactional;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.txm.TxTemplate;
import zcu.xutil.utils.Function;

public class HibernateJtaTest {
	static Logger logger = Logger.getLogger(HibernateJtaTest.class);

	Context ctx;

	@Before
	public void setUp() throws Exception {
		System.setProperty(Constants.XUTILS_CFG_ROOT,RootConfig.class.getName());
		System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,"bitronix.tm.jndi.BitronixInitialContextFactory");
		System.setProperty("btm.root", Objutil.systring("btm.root"));
		System.setProperty("bitronix.tm.configuration", Objutil.systring("btm.root")+"/conf/btm-config.properties");

		ctx = CFG.build(CFG.root() ,new Config() {
			public void config(Binder b) throws Exception {

				AnnotationConfiguration cfg = new AnnotationConfiguration();
				cfg.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
				cfg.setProperty(Environment.HBM2DDL_AUTO, "create-drop");// "update"
				cfg.setProperty(Environment.SHOW_SQL, "true");
				cfg.addAnnotatedClass(Account.class);
				val(new SessionFactoryBean(cfg)).set("dataSource",b.ref("datasource")).set("txManager",b.ref("txmanager")).ext("getObject").uni(b,"sessionFactory").die("close");
				typ(HEntityManager.class, b.ref("sessionFactory")).uni(b,"manager");
				typ(Query.class, b.ref("datasource")).uni(b,"query");
				typ(EntityDaoImpl.class, b.ref("query")).uni(b,"entityDao",null,"txIntercept");
				val(new TxTemplate()).set("txManager",b.ref("txmanager")).uni(b,"txTemplate");
				// typ(TxTemplate.class,
				// b,Propagation.REQUIRES_NEW).sig("reqNewTemplate");


			}
		});
	}

	@After
	public void tearDown() throws Exception {
		if (ctx != null)
			ctx.destroy();
	}

	Session getSession(HEntityManager manager) {
		return manager.execute(new Function<Session,Session>() {

			public Session apply(Session from) {
				return from;
			}
		});
	}

	boolean opensession;

	Session s;

	@Test
	public void testCommon() {
		final HEntityManager manager = (HEntityManager) ctx.getBean("manager");
		final Query query = (Query) ctx.getBean("query");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		try {
			query.update(IdentityEntity.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		try {
			query.update(User.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		TxTemplate txt = (TxTemplate) ctx.getBean("txTemplate");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		final User user = new User("2", "xzcxzc");
		try {
			query.update(IdentityEntity.createtable);
			query.update(User.createtable);
			int[] id = txt.execute(new Callable<int[]>() {
				public int[] call() throws Exception {
					s = getSession(manager);
					Account account = new Account();
					account.setName("xiaozaichu");
					manager.persist(account);
					logger.info("--------------id: {} name {}", account.getId(), account.getName());
					query.entityUpdate(IdentityEntity.create, identityEntity);
					ed.create(user);
					Account account1 = new Account();
					account1.setName("yuejie");
					manager.persist(account1);
					assertTrue(s == getSession(manager));
					return new int[] { account.getId(), account1.getId() };
				}
			});
			Account acc = manager.find(Account.class, id[0]);
			logger.info("id: {} name {}", acc.getId(), acc.getName());
			assertEquals("xiaozaichu", acc.getName());
			acc = manager.find(Account.class, id[1]);
			logger.info("id: {} name {}", acc.getId(), acc.getName());
			assertEquals("yuejie", acc.getName());
			assertTrue("hibeanete session match.", opensession == (s == getSession(manager)));
			IdentityEntity identity = query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity
					.getId());
			assertEquals("hello", identity.getEventName());
			User u = query.query(User.retrieve, User.handler, user.getId());
			assertEquals(user.getName(), u.getName());
		} catch (Throwable e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testCommonIntercept() throws SQLException {
		opensession = true;
		try {
			final HEntityManager manager = (HEntityManager) ctx.getBean("manager");
			manager.execute(new Function<Session,Object>() {
				public Object apply(Session session){
					Account account = new Account();
					account.setName("xiaozaichu");
					manager.getCurrentSession().persist(account);
					manager.getCurrentSession().flush();
					logger.info("+++++++++++id: {} name {}", account.getId(), account.getName());
					manager.getCurrentSession().close();
					Account acc = manager.find(Account.class, account.getId());
					logger.info("id: {} name {}", acc.getId(), acc.getName());

					HibernateJtaTest.this.testCommon();
					Session s2 = s;
					HibernateJtaTest.this.testCommon();
					assertTrue(s2 == s);

					return null;
				}

			});
		} finally {
			opensession = false;
		}
	}

	@Test
	public void testRollback() {
		final HEntityManager manager = (HEntityManager) ctx.getBean("manager");
		final Query query = (Query) ctx.getBean("query");
		final EntityDao ed = (EntityDao) ctx.getBean("entityDao");
		try {
			query.update(IdentityEntity.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		try {
			query.update(User.droptable);
		} catch (SQLException se) {
			logger.debug(" table maybe not exist:{}", se.getMessage());
		}
		TxTemplate txt = (TxTemplate) ctx.getBean("txTemplate");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		final User user = new User("2", "xzcxzc");
		try {
			query.update(IdentityEntity.createtable);
			query.update(User.createtable);
			int[] id = txt.execute(new Callable<int[]>() {
				public int[] call() throws Exception {
					s = getSession(manager);
					Account account = new Account();
					account.setName("xiaozaichu");
					manager.persist(account);
					logger.info("--------------id: {} name {}", account.getId(), account.getName());
					query.entityUpdate(IdentityEntity.create, identityEntity);
					ed.create(user);
					Account account1 = new Account();
					account1.setName("yuejie");
					manager.persist(account1);
					assertTrue(s == getSession(manager));
					throw new RuntimeException("rollback test");
				}
			});
		} catch (Throwable e) {
			logger.info("-----rollback exception-----{}", e.getMessage());
		}
		try {
			assertNull(query.query(User.retrieve, User.handler, user.getId()));
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
		} catch (SQLException e) {
			fail(e.getMessage());
			return;
		}
		List list = manager.loadAll(Account.class, -1);
		assertTrue(list.isEmpty());
	}
}
