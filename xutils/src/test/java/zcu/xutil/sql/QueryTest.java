package zcu.xutil.sql;


import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sun.reflect.Reflection;
import zcu.xutil.Logger;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.sql.Callback;
import zcu.xutil.sql.MiniDataSource;
import zcu.xutil.sql.Query;
import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.handl.MapRow;
import zcu.xutil.utils.Util;


public class QueryTest {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(QueryTest.class);
	Query query;
	MiniDataSource ds;

	@Before
	public void setUp() throws Exception {
		logger.info("setUP:");
//		org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource dataSource = new org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource();
//		dataSource.setDatabaseName ("D:\\DerbyDatabases\\myDB");
//		dataSource.setCreateDatabase ("create");
		org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
		dataSource.setURL ("jdbc:h2:~/h2db/test;DB_CLOSE_DELAY=-1");
		//dataSource.setURL ("jdbc:h2:~/h2db/test;DB_CLOSE_ON_EXIT=FALSE");

		dataSource.setUser("sa");
		logger.info("setUP111111:");
		ds = new MiniDataSource(dataSource,8);
		ds.setTestQuery("");
		Connection conn = ds.getConnection();
		logger.info("getCatalog: {} dbtype: {}",conn.getCatalog());
		conn.close();
		query = new Query(ds);
	}

	@After
	public void tearDown() throws Exception {
		if(ds!=null)
			ds.destroy();
	}

	@Test
	public void testMap(){
		logger.info("========testMAP begin=============");
		try {
			if(query.tableExist("t_event")){
				logger.debug(" table t_event exist");
				query.update(IdentityEntity.droptable);
			}else{
				logger.debug(" table t_event not exist");
			}
			query.update(IdentityEntity.createtable);
			HashMap<String,Object> ee1 =new HashMap<String,Object>();
			ee1.put("id", Long.class);
			ee1.put("eventName", "111");
			HashMap<String,Object> ee2 =new HashMap<String,Object>();
			ee2.put("id", Long.class);
			ee2.put("eventName", "222");
			HashMap<String,Object> ee3 =new HashMap<String,Object>();
			ee3.put("id", Long.class);
			ee3.put("eventName", "333");
			query.mapBatch(IdentityEntity.create, ee1,ee2,ee3);

			IdentityEntity ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee1.get("id"));
			assertEquals("111",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee2.get("id"));
			assertEquals("222",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee3.get("id"));
			assertEquals("333",ee.getEventName());

			ee1.put("eventName","aaa");
			ee2.put("eventName","bbb");
			ee3.put("eventName","ccc");
			query.mapBatch(IdentityEntity.update, ee1,ee2,ee3);
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee1.get("id"));
			assertEquals("aaa",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee2.get("id"));
			assertEquals("bbb",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee3.get("id"));
			assertEquals("ccc",ee.getEventName());

			Map<String,Class<?>> typeMap =new HashMap<String,Class<?>>();
			typeMap.put("ID", Long.class);
			typeMap.put("EVENTName", String.class);

			ResultHandler<Map<String,Object>> maphandler = new MapRow(typeMap);
			Map<String,Object> map= query.query(IdentityEntity.retrieve,maphandler,ee2.get("id"));
			assertEquals("bbb",map.get("EVENTName"));
			assertEquals(ee2.get("id"),map.get("ID"));
			logger.info("========testMAP end=============");

		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	@Test
	public void testBatch() {
		logger.info("========testBatch begin=============");
		try {
			if(query.tableExist("t_event")){
				logger.debug(" table t_event exist");
				query.update(IdentityEntity.droptable);
			}else{
				logger.debug(" table t_event not exist");
			}

			query.update(IdentityEntity.createtable);
			IdentityEntity ee1 = new IdentityEntity("111");
			IdentityEntity ee2 = new IdentityEntity("222");
			IdentityEntity ee3 = new IdentityEntity("333");
			query.entityBatch(IdentityEntity.create, ee1,ee2,ee3);

			IdentityEntity ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee1.getId());
			assertEquals("111",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee2.getId());
			assertEquals("222",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee3.getId());
			assertEquals("333",ee.getEventName());

			ee1.setEventName("aaa");
			ee2.setEventName("bbb");
			ee3.setEventName("ccc");
			query.entityBatch(IdentityEntity.update, ee1,ee2,ee3);
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee1.getId());
			assertEquals("aaa",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee2.getId());
			assertEquals("bbb",ee.getEventName());
			ee = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee3.getId());
			assertEquals("ccc",ee.getEventName());
			logger.info("========testBatch end=============");
		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testAutoIDBatch() {
		logger.info("========testAutoIDBatch begin=============");

		try {
			if(query.tableExist("t_event")){
				logger.debug(" table t_event exist");
				query.update(IdentityEntity.droptable);
			}else{
				logger.debug(" table t_event not exist");
			}

			query.update(AutoEntity.createtable);
			AutoEntity ee1 = new AutoEntity("111");
			AutoEntity ee2 = new AutoEntity("222");
			AutoEntity ee3 = new AutoEntity("333");
			query.entityBatch(IdentityEntity.create, ee1,ee2,ee3);

			AutoEntity ee = query.query(AutoEntity.retrieve,AutoEntity.handler,ee1.getId());
			assertEquals("111",ee.getEventName());
			ee = query.query(AutoEntity.retrieve,AutoEntity.handler,ee2.getId());
			assertEquals("222",ee.getEventName());
			ee =query.query(AutoEntity.retrieve,AutoEntity.handler,ee3.getId());
			assertEquals("333",ee.getEventName());

			ee1.setEventName("aaa");
			ee2.setEventName("bbb");
			ee3.setEventName("ccc");
			query.entityBatch(AutoEntity.update, ee1,ee2,ee3);
			ee = query.query(AutoEntity.retrieve,AutoEntity.handler,ee1.getId());
			assertEquals("aaa",ee.getEventName());
			ee = query.query(AutoEntity.retrieve,AutoEntity.handler,ee2.getId());
			assertEquals("bbb",ee.getEventName());
			ee = query.query(AutoEntity.retrieve,AutoEntity.handler,ee3.getId());
			assertEquals("ccc",ee.getEventName());
			logger.info("========testAutoIDBatch end=============");

		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testIdentityKey() {
		logger.info("========testIdentityKey begin=============");

		try {
			if(query.tableExist("t_event")){
				logger.debug(" table t_event exist");
				query.update(IdentityEntity.droptable);
			}else{
				logger.debug(" table t_event not exist");
			}
			String eventName="eventName";
			query.update(IdentityEntity.createtable);
			IdentityEntity ee = new IdentityEntity(eventName);
			query.entityUpdate(IdentityEntity.create, ee);
			assertNotNull(ee.id);
			IdentityEntity ee2 = query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee.getId());
			assertEquals(eventName,ee2.getEventName());
			query.update(IdentityEntity.delete, ee2.getId());
			assertNull(query.query(IdentityEntity.retrieve,IdentityEntity.handler,ee.getId()));
			logger.info("========testIdentityKey end=============");

		}catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	@Test
	public void testDataTpe() {
		logger.info("========testDataTpe begin=============");
		try {
			if(query.tableExist("t_datetype")){
				logger.debug(" table t_datetype exist");
				query.update(DateType.droptable);
			}else{
				logger.debug(" table t_datetype not exist");
			}
			query.update(DateType.createtable);
			DateType dt = new DateType();
			dt.setId(1);
			query.entityUpdate(DateType.create, dt);
			final DateType dt2 = query.query(DateType.retrieve, DateType.handler, dt.getId());
			logger.debug("retrieve after create : {}", dt2);
			assertEquals(dt, dt2);
			dt2.setEnumtype(DBType.derby);
			dt2.setBytes(new byte[] { 6, 7, 3, 4, 6, 2, 8, 8 });
			dt2.setChars(new char[] { 'y', 'e', 'e', 'p', 'a', 'y' });
			dt2.setBigInt(BigInteger.TEN.pow(15));
			dt2.setBigDec(BigDecimal.TEN.pow(15).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP));
			dt2.setBooltype(true);
			dt2.setCharcls('c');
			dt2.setChartype('t');
			dt2.setDoubletype(3.1415);
			dt2.setBytetype((byte) 121);
			SerialClob clob = new SerialClob(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' });
			SerialBlob blob = new SerialBlob(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
			dt2.setClob(clob);
			dt2.setBlob(blob);
			query.entityUpdate(DateType.update, dt2);
			query.execute(new Callback<Query.Sesion,Void>(){
				public Void call(Query.Sesion s) throws SQLException{
					DateType dt3 = s.query(DateType.retrieve, DateType.handler, dt2.getId());
					logger.debug("retrieve after update : {}", dt3);
					assertEquals(dt2, dt3);

					// logger.debug("lob after update : {}
					// {}",dt3.getClob().getSubString(1,4),dt3.getBlob().getBytes(1,
					// 5));
					assertEquals(dt2.getClob().getSubString(1L, (int) dt2.getClob().length()), dt3.getClob().getSubString(1L,
							(int) dt3.getClob().length()));
					assertArrayEquals(dt2.getBlob().getBytes(1L, (int) dt2.getBlob().length()), dt3.getBlob().getBytes(1L,
							(int) dt3.getBlob().length()));
					return null;
				}

			});
			logger.info("========testDataTpe end=============");
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
