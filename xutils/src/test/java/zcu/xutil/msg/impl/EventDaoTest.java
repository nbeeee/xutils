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

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testAll() {
		EventDao eventDao = new EventDao("eventDao",null,null);

		try {
			Event entity = new Event("eventName", "eventValue", 3.14, new Date());
			eventDao.store(entity);
			EventDao.Service service = eventDao.new Service("eventName");
			Event event, queue =event= service.loadIfNecessary();
			int count =0;
			while(queue!=null){
				if (queue.getId().equals(entity.getId())) {
					logger.info("found event:{}", queue);
					assertEquals(entity.getId(), queue.getId());
					assertSame("eventName", queue.getName());
					assertEquals(entity.getValue(), queue.getValue());
				}
				count++;
				queue=queue.next;
			}
			logger.info("unsend eventName size number:{}", count);
			service.okAndNext(event);
			logger.info("delete stale event {}",eventDao.query.update(EventDao.delete));
			entity = new Event("eventName", "value", 666, new Date());

			eventDao.store(entity);
			entity.setExpire(new Date());
			eventDao.store(entity);
			logger.info("insert last: {},{}", entity.getId(), entity);
		} catch (SQLException e) {

			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			eventDao.destroy();
		}
	}
}
