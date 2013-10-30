package zcu.xutil.msg.impl;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.msg.impl.Event;
import zcu.xutil.msg.impl.EventDao;
import zcu.xutil.sql.MiniDataSource;

//import com.yeepay.xutil.msg.impl.SendDaemon;
//import com.yeepay.xutil.txmgr.jdbc.SmartDataSource;

//import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;

public class EventDaoTest {
	private static final Logger logger = Logger.getLogger(EventDaoTest.class);
	public final BrokerAgent broekr = new BrokerAgent(){

		@Override
		public ServiceObject getLocalService(String canonicalName) {
			return !"local".equals(canonicalName) ? null :
			new ServiceObject(){

				@Override
				public Object handle(Event event) throws Throwable {
					targetlocal = event;
					logger.info("local: {}" ,event);
					return null;
				}
	
				
			};
		}

		@Override
		public Object sendToRemote(Event canonical, int timeoutMillis) throws Throwable {
			targetremote = canonical;
			logger.info("remote: {}" ,canonical);
			return null;
		}
		
	};
	

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {

	}
	
	final Event local= new Event("local", "eventValue", 3.14, new Date());
	final Event remote= new Event("remote", null, Integer.MAX_VALUE, Long.MAX_VALUE);
	volatile Event targetlocal,targetremote;
	@Test
	public void testAll() {
		EventDao eventDao = new EventDao("eventDao",null,broekr);
		eventDao.start();
		eventDao.store(local);
		eventDao.store(remote);
		
		try {
			for(int i = 0 ; i < 10;i++ ){
				if(targetlocal == null || targetremote==null)
					Thread.sleep(1000);
				else
					break;
			}
			assertArrayEquals(local.parameters(), targetlocal.parameters());
			assertEquals(local.getName(), targetlocal.getName());
			assertEquals(local.getValue(), targetlocal.getValue());
			assertEquals(local.getExpire(), targetlocal.getExpire());
			assertArrayEquals(remote.parameters(), targetremote.parameters());
			assertEquals(remote.getName(), targetremote.getName());
			assertEquals(remote.getValue(), targetremote.getValue());
			assertEquals(remote.getExpire(), targetremote.getExpire());
		} catch (InterruptedException e) {

			fail(e.getMessage());
		}finally {
			eventDao.destroy();
		}
	}
}
