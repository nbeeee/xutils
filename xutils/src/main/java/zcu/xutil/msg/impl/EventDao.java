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
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
	static final Handler<List<Event>> listHandle = new BeanRow<Event>(Event.class).list(25, 50);
	static final Logger logger = Logger.getLogger(EventDao.class);
	/*
	 * for derby
	 * 
	 * static final String create =
	 * "CREATE TABLE msg_event (id int generated always as identity,"+
	 * "eventname VARCHAR(100),eventvalue VARCHAR(100),eventdata VARCHAR(2000) FOR BIT DATA,"
	 * + "repeat INTEGER,status SMALLINT,createTime TIMESTAMP,PRIMARY KEY(id))";
	 */
	/*
	 * for H2
	 */
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
	final AtomicInteger total = new AtomicInteger();
	private final NpSQL insert;
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

	Service getService(String name) {
		Service[] array = allService.get();
		int len = array.length;
		for (int i = 0; i < len; i++) {
			if (array[i].canonicalName.equals(name))
				return array[i];
		}
		Service[] news = new Service[len + 1];
		news[len] = new Service(name);
		while (--len >= 0) {
			news[len] = array[len];
		}
		if (allService.compareAndSet(array, news))
			return news[len];
		return getService(name);
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

	void store(Event event, boolean rescue) {
		long current = Util.now();
		try {
			Date date = event.getExpire();
			if (date == null)
				event.setExpire(new Date(current + 7200 * 1000)); //default expire 2 hours
			else if (date.getTime() < current) {
				event.discardLogger("expire");
				return;
			}
			query.entityUpdate(insert, event);
		} catch (SQLException e) {
			throw new XutilRuntimeException(e);
		}
		Service s = getService(event.getName());
		if (rescue)
			s.untilMillis = current + 4000;
	}

	@Override
	public void run() {
		long current, lastClearMillis = current = Util.now();
		int index = Integer.MAX_VALUE, sleepMillis = 5000;
		while (worker != null) {
			try {
				int number = total.get();
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
				current = Util.now();
				for (int tail, i = tail = array.length - 1; i >= 0; i--) {
					Service s = array[index = index < tail ? index + 1 : 0];
					if (current > s.untilMillis)
						s.sentEvents(current);
				}
				if ((number = total.get() - number) > 0) {
					sleepMillis = number > 9 ? 0 : 1000;
				} else if (sleepMillis < 8000)
					sleepMillis += 2000;
			} catch (Throwable e) {
				sleepMillis = 4000;
				logger.warn("!!!!!!!!! exception !!!!!!!!!", e);
			}
		}
		for (Service s : allService.get()) {
			try {
				s.sentEvents(current);
			} catch (SQLException e) {
				break;
			} catch (Throwable e) {
				// ignore
			}
		}
		logger.info("SendDaemon shutdown. Total={} Names:{}", total, allService);
	}

	final class Service implements Runnable {
		private final AtomicBoolean running = new AtomicBoolean();
		final String canonicalName;
		private Event head;
		private ServiceObject sobj;
		volatile long untilMillis;

		Service(String name) {
			this.canonicalName = name.intern();
		}

		@Override
		public String toString() {
			return head != null ? canonicalName.concat("#####") : canonicalName;
		}

		void sentEvents(long current) throws Throwable {
			Event event = loadOrGet();
			if (event == null) {
				untilMillis = current + 4000;
				return;
			}
			if (sobj != null || (sobj = broker.getSOBJ(canonicalName)) != null) {
				try {
					sobj.handler.executor.execute(this);
				} catch (Throwable e) {
					untilMillis = current + 4000;
					logger.info("TOO MANY TASK. ", e);
				}
				return;
			}
			do {
				if (event.getExpire().getTime() < current)
					event.discardLogger("expire");
				else
					try {
						broker.sendToRemote(event, 0);
					} catch (IllegalMsgException e) {
						event.discardLogger(e.toString());
					} catch (UnavailableException e) {
						untilMillis = current + 4000;
						logger.warn("{} unavailable. recall latter", e, event.getName());
						break;
					} catch (Throwable e) {
						throw e;
					}
			} while ((event = okAndNext(event)) != null);
		}

		@Override
		public void run() {
			if (!running.compareAndSet(false, true))
				return;
			try {
				long current = Util.now();
				for (Event event = head; event != null; event = okAndNext(event)) {
					if (event.getExpire().getTime() < current)
						event.discardLogger("expire");
					else
						try {
							sobj.invoke(event);
						} catch (UnavailableException e) {
							untilMillis = current + 8000;
							logger.warn("{} unavailable. recall latter", e, event.getName());
							break;
						} catch (Throwable e) {
							event.discardLogger(e.toString());
						}
				}
			} finally {
				running.set(false);
			}
		}

		Event okAndNext(Event event) {
			try {
				head = event.next;
				query.update(updateToSent, event.getId());
				total.getAndIncrement();
				return head;
			} catch (SQLException e) {
				throw new XutilRuntimeException(e);
			}

		}

		Event loadOrGet() throws SQLException {
			if (head == null) {
				List<Event> ret = query.query(retrieve, listHandle, canonicalName);
				int size = ret.size();
				Event tmp, next = null;
				while (--size >= 0) {
					(tmp = ret.get(size)).next = next;
					(next = tmp).setName(canonicalName);
				}
				head = next;
			}
			return head;
		}
	}
}
