package zcu.xutil.txm.test;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Disposable;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.orm.HEntityManager;
import zcu.xutil.orm.SessionFactoryBean;
import zcu.xutil.sql.IdentityEntity;
import zcu.xutil.sql.MiniDataSource;
import zcu.xutil.sql.Query;
import zcu.xutil.txm.Propagation;
import zcu.xutil.txm.SmartDataSource;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxTemplate;
import zcu.xutil.utils.Function;
import static zcu.xutil.cfg.CFG.*;

public class HibernateTest{
	static Logger logger = Logger.getLogger(HibernateTest.class);
	Context ctx;

	@Before
	public void setUp() throws Exception {
		final org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
		dataSource.setURL("jdbc:h2:~/h2db/localtxtest;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		ctx = CFG.build(null ,new Config(){
			public void config(Binder b) throws Exception {
				String name = val(new MiniDataSource(dataSource, 8)).uni(b,"").die("destroy").name();
				typ(SmartDataSource.class, b.ref(name)).uni(b,"datasource");

				AnnotationConfiguration cfg = new AnnotationConfiguration();
				cfg.configure("hibernate.cfg.xml");
				//cfg.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
				//cfg.setProperty(Environment.HBM2DDL_AUTO, "create-drop");// "update"
				//cfg.setProperty(Environment.SHOW_SQL, "true");
				//cfg.addAnnotatedClass(Account.class);
				SessionFactoryBean sfb = new SessionFactoryBean(cfg);

				val(sfb).set("dataSource",b.ref("datasource")).ext("getObject").uni(b,"sessionFactory").die("close");
				typ(HEntityManager.class, b.ref("sessionFactory")).uni(b,"manager");
				typ(Query.class, b.ref("datasource")).uni(b,"query");
				typ(EntityDaoImpl.class,b.ref("query")).uni(b,"entityDao", null,"txIntercept");
				val(new TxTemplate()).uni(b,"txTemplate");
				val(new TxTemplate(Propagation.REQUIRES_NEW)).uni(b,"reqNewTemplate");
				val(new TxInterceptor()).uni(b,"txIntercept");
				typ(OsTest.class,b.ref("manager")).uni(b,"ostest",null);
			}
		});
		final Query query = (Query) ctx.getBean("query");
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
		TxTemplate txt = (TxTemplate) ctx.getBean("txTemplate");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		final User user = new User("2", "xzcxzc");
		int[] id ;
		try {
			id =  txt.execute(new Callable<int[]>() {
				public int[] call() throws Exception {
					try {
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
					} catch (Exception e) {
						e.printStackTrace();
						throw e;
					}
				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
			throw Objutil.rethrow(e);
		}
			Account acc = manager.find(Account.class, id[0]);
			logger.info("id: {} name {}", acc.getId(), acc.getName());
			assertEquals("xiaozaichu", acc.getName());
			acc = manager.find(Account.class, id[1]);
			logger.info("id: {} name {}", acc.getId(), acc.getName());
			assertEquals("yuejie", acc.getName());
			assertTrue("hibeanete session match.", opensession == (s == getSession(manager)));
			try {
				IdentityEntity identity = query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity
						.getId());
				assertEquals("hello", identity.getEventName());
				User u = query.query(User.retrieve, User.handler, user.getId());
				assertEquals(user.getName(), u.getName());
			} catch (SQLException e) {
				throw Objutil.rethrow(e);
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
					Session s2 = manager.getCurrentSession();
					HibernateTest.this.testCommon();
					assertTrue(s2 == s);
					assertTrue(s == manager.getCurrentSession());
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

		TxTemplate txt = (TxTemplate) ctx.getBean("txTemplate");
		final IdentityEntity identityEntity = new IdentityEntity("hello");
		final User user = new User("2", "xzcxzc");
		try {
			int[] id =  txt.execute(new Callable<int[]>() {
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
					throw new RuntimeException("roolbact test");
				}
			});
		} catch (Throwable e) {
			logger.info("-----rollback exception-----{}",e.getMessage());
		}
		try {
			assertNull(query.query(User.retrieve, User.handler, user.getId()));
			assertNull(query.query(IdentityEntity.retrieve, IdentityEntity.handler, identityEntity.getId()));
		} catch (SQLException e) {
			fail(e.getMessage());
			return;
		}
		List list = manager.loadAll(Account.class,-1);
		assertTrue(list.isEmpty());
	}
	@Test
	public void testSessionInterceptor(){
		final OS os = (OS) ctx.getBean("ostest");
		os.opesSession();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
	}

	public interface OS{
		void opesSession();
	}
	public static class  OsTest implements OS{
		final HEntityManager manager;
		public OsTest(HEntityManager manager){
			this.manager=manager;
		}
		public void opesSession() {
			Disposable sessionCloser = manager.opensession();
			try {

				manager.execute(new Function<Session,Object>(){
					public Object apply(Session session) {
						assertTrue(session.isOpen());
						return null;
					}

				});
				Session s1=manager.getCurrentSession();
				assertTrue(s1.isOpen());
				Session s2=manager.getCurrentSession();
				assertTrue(s1.isOpen());
				assertTrue("opensession not match",s1==s2);
			} catch (Exception e) {
				throw Objutil.rethrow(e);
			}finally{
				//sessionCloser.destroy();
				sessionCloser = null;
			}
		}
	}

	@Test
	public void testRequireNew() {
		logger.info("==================testRequireNew==========================");
		final TxTemplate tp=(TxTemplate)ctx.getBean("txTemplate");
		final TxTemplate reqNew=(TxTemplate)ctx.getBean("reqNewTemplate");
		final HEntityManager manager = (HEntityManager) ctx.getBean("manager");
		try {
			tp.execute(new Callable<Object>(){
				public Object call() throws Exception {
					final Session s1 =manager.getCurrentSession();
					try {
						Integer id = reqNew.execute(new Callable<Integer>(){
							public Integer call() throws Exception {
								logger.info("---------reqNew session equal  -- {}}",s1.equals(manager.getCurrentSession()) );
								Account act = new Account();
								act.setName("yuejie");
								manager.persist(act);
								return act.getId();
							}
						});
						Account act = manager.find(Account.class, id);
						logger.info("---------reqNew--id: {} name {}", act.getId(), act.getName());
					} catch (Throwable e) {
						e.printStackTrace();
					}
					Account account = new Account();
					account.setName("xiaozaichu");
					manager.persist(account);
					logger.info("---------persist--id: {} name {}", account.getId(), account.getName());

					logger.info("---------session equal  -- {}}",s1.equals(manager.getCurrentSession()) );
					return null;
				}

			});
		} catch (Throwable e) {
			logger.info("---- requirenew exception---- {}",e.toString());
		}
	}
}
