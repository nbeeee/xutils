/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.msg.impl;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.sql.ID;
import zcu.xutil.sql.MiniDataSource;
import zcu.xutil.sql.NpSQL;
import zcu.xutil.sql.Handler;
import zcu.xutil.sql.Query;
import zcu.xutil.sql.handl.BeanRow;
import zcu.xutil.sql.handl.FirstField;
import zcu.xutil.utils.Util;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class EventDao implements Runnable {
	private static final Logger discardLogger = Logger.getLogger(Event.class);

	static final Handler<List<Event>> listHandle = new BeanRow<Event>(Event.class).list(25, 50);
	static final Logger logger = Logger.getLogger(EventDao.class);
	static final String create = "CREATE TABLE EVENT (ID IDENTITY,NAME VARCHAR(100),VALUE VARCHAR(100),"
			+ "DATAS VARBINARY(8190),EXPIRE TIMESTAMP,PRIMARY KEY(ID))";
	static final String retrieve = "SELECT ID,VALUE,DATAS,EXPIRE FROM EVENT WHERE EXPIRE IS NOT NULL AND NAME=? ORDER BY ID";
	static final String updateToSent = "UPDATE EVENT SET EXPIRE=NULL WHERE ID=?";
	static final String drop = "DROP TABLE EVENT";
	static final String delete = "DELETE FROM EVENT WHERE EXPIRE IS NULL";
	static final String eventNames = "SELECT DISTINCT NAME FROM EVENT";
	static final String insertSql = "INSERT INTO EVENT VALUES(DEFAULT,:name,:value,:datas,:expire)";

	final Query query;
	final BrokerAgent broker;
	private final NpSQL insert;
	private final AtomicInteger discardNumber = new AtomicInteger();
	private final AtomicReference<Service[]> allService;
	private volatile Thread worker; // blinker moribund
	

	public EventDao(final String name, DataSource ds, final BrokerAgent agent) {
		if (ds == null) {
			JdbcDataSource h2ds = new JdbcDataSource();
			h2ds.setURL("jdbc:h2:" + Objutil.systring(Constants.XUTILS_HOME) + "/xmessage/" + name
					+ ";DB_CLOSE_ON_EXIT=FALSE");
			logger.info("dburl={}", h2ds.getURL());
			h2ds.setUser("sa");
			ds = new MiniDataSource(h2ds, 4, 8);
		}
		this.query = new Query(ds);
		this.insert = new NpSQL(new ID("id"), insertSql);
		this.broker = agent;
		try {
			Service[] services;
			if (query.tableExist("EVENT")) {
				logger.info("delete stale event {}", query.update(delete));
				List<String> list = query.query(eventNames, FirstField.get(String.class).list(50, -1));
				int len = list.size();
				services = new Service[len];
				while (--len >= 0)
					services[len] = new Service(list.get(len));
			} else {
				query.update(create);
				query.update("SET DEFAULT_LOCK_TIMEOUT 5000"); // h2 table lock
				services = new Service[0];
			}
			allService = new AtomicReference<Service[]>(services);
		} catch (SQLException e) {
			throw new XutilRuntimeException(e);
		}
		worker = Util.newThread(this, "MsgSender", false);
	}

	public boolean dropTable() throws SQLException {
		if (!query.tableExist("EVENT"))
			return false;
		query.update(drop);
		return true;
	}

	void start() {
		worker.start();
	}

	void destroy() {
		try {
			Thread moribund = worker;
			if (moribund == null)
				return;
			synchronized (this) {
				worker = null;
				notify();
			}
			moribund.join(2000);
		} catch (Throwable e) {
			// ignore
		}
	}

	void store(Event event) {
		if (event.getExpire() == null) // default expire 2 hours
			event.setExpire(new Date(Util.now() + 7200 * 1000));
		try {
			query.entityUpdate(insert, event);
		} catch (SQLException e) {
			throw new XutilRuntimeException(e);
		}
		final String name = event.getName();
		Service[] array, news;
		do {
			int len = (array = allService.get()).length;
			while (--len >= 0) {
				if (array[len].canonicalName.equals(name))
					return;
			}
			(news = Arrays.copyOf(array, array.length + 1))[array.length] = new Service(name);
		} while (allService.compareAndSet(array, news));
	}

	void discardLogger(Event event, Object cause) {
		discardNumber.getAndIncrement();
		Object params;
		try {
			params = event.parameters();
		} catch (Throwable ex) {
			params = ex;
		}
		discardLogger.warn("{}: name={} ,value={} ,params={}", cause, event.getName(), event.getValue(), params);
	}

	@Override
	public void run() {
		long current, lastClearMillis = current = Util.now();
		int index = Integer.MAX_VALUE, sleepMillis = 5000;
		while (worker != null) {
			try {
				if (sleepMillis > 0) {
					if (current - lastClearMillis > 60000) {
						lastClearMillis = current;
						logger.info("delete stale event: {}", query.update(delete));
					}
					synchronized (this) {
						if (worker != null)
							wait(sleepMillis);
					}
				}
				Service[] array = allService.get();
				int count = 0;
				current = Util.now();
				for (int tail, i = tail = array.length - 1; i >= 0; i--) {
					Service service = array[index = index < tail ? index + 1 : 0];
					if (service.sendRequired(current))
						count += service.send();
				}
				if (count > 0) {
					sleepMillis = count > 9 ? 0 : 1000;
				} else if (sleepMillis < 8000)
					sleepMillis += 2000;
			} catch (Throwable e) {
				sleepMillis = 4000;
				logger.warn("!!!!!!!!! exception !!!!!!!!!", e);
			}
		}
		current = Util.now();
		for (Service s : allService.get()) {
			try {
				s.load(current);
				s.send();
			} catch (SQLException e) {
				break;
			} catch (Throwable e) {
				// ignore
			}
		}
		logger.warn("SendDaemon shutdown.discard number:{} , Names:{}", discardNumber, allService.get());
	}

	private final class Service {
		final String canonicalName;
		final Deque<Event> events = new ArrayDeque<Event>();
		private int sendNumber;
		private long untilMillis;

		Service(String name) {
			this.canonicalName = name.intern();
		}

		@Override
		public String toString() {
			return canonicalName + " send " + sendNumber;
		}

		boolean sendRequired(long current) throws SQLException {
			if (current < untilMillis)
				return false;
			if (events.isEmpty())
				load(current);
			if (events.isEmpty()) {
				untilMillis = current + 4000;
				return false;
			}
			return true;
		}

		void load(long current) throws SQLException {
			List<Event> ret = query.query(retrieve, listHandle, canonicalName);
			for (Event e : ret) {
				e.setName(canonicalName);
				if (e.getExpire().getTime() < current) {
					query.update(updateToSent, e.getId());
					discardLogger(e, "expire");
				} else
					events.add(e);
			}
		}

		int send() throws Throwable {
			Event event;
			int count = 0;
			ServiceObject sobj = broker.getSOBJ(canonicalName);
			while ((event = events.peekFirst()) != null) {
				try {
					if (sobj == null)
						broker.sendToRemote(event, 0);
					else
						sobj.handle(event);
					sendNumber++;
				} catch (IllegalMsgException e) {
					discardLogger(event, e);
				} catch (UnavailableException e) {
					untilMillis = Util.now() + 4000;
					logger.warn("service unavailable. {}", e, event.getName());
					break;
				} catch (Throwable e) {
					throw e;
				}
				events.remove();
				query.update(updateToSent, event.getId());
				count++;
			}
			return count;
		}
	}
}
