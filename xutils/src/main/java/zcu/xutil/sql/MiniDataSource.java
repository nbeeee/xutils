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
package zcu.xutil.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import zcu.xutil.Disposable;
import zcu.xutil.DisposeManager;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.jmx.MbeanAttribute;
import zcu.xutil.jmx.MbeanResource;
import zcu.xutil.utils.Util;

/**
 * For H2 (embedded mode):<br>
 * org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();<br>
 * dataSource.setURL ("jdbc:h2:file:c:/temp/testDB;DB_CLOSE_DELAY=-1");<br>
 * MiniDataSource poolMgr = new MiniDataSource(dataSource,maxConnections);<br>
 * ...<br>
 * Connection connection = poolMgr.getConnection();<br>
 * ...<br>
 * connection.close();
 * <p>
 * For Apache Derby (embedded mode):<br>
 * org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource dataSource = new
 * org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource();<br>
 * dataSource.setDatabaseName ("c:/temp/testDB");<br>
 * dataSource.setCreateDatabase ("create");<br>
 * MiniDataSource poolMgr = new MiniDataSource(dataSource,maxConnections);<br>
 * ... Connection connection = poolMgr.getConnection();<br>
 * ... connection.close();
 * <p>
 * For JTDS:<br>
 * net.sourceforge.jtds.jdbcx.JtdsDataSource dataSource = new
 * net.sourceforge.jtds.jdbcx.JtdsDataSource();<br>
 * dataSource.setDatabaseName ("Northwind");<br>
 * dataSource.setServerName ("localhost");<br>
 * dataSource.setUser ("sa");<br>
 * dataSource.setPassword ("sesame");<br>
 * MiniDataSource poolMgr = new MiniDataSource(dataSource,maxConnections);<br>
 * ...<br>
 * Connection connection = poolMgr.getConnection();<br>
 * ...<br>
 * connection.close();
 * <p>
 * For the Microsoft SQL Server driver:<br>
 * com.microsoft.sqlserver.jdbc.SQLServerXADataSource dataSource = new
 * com.microsoft.sqlserver.jdbc.SQLServerXADataSource();<br>
 * The sqljdbc 1.1 documentation, chapter "Using Connection Pooling", recommends
 * to use SQLServerXADataSource instead of SQLServerConnectionPoolDataSource.<br>
 * dataSource.setDatabaseName ("Northwind");<br>
 * dataSource.setServerName ("localhost");<br>
 * dataSource.setUser ("sa");<br>
 * dataSource.setPassword ("sesame");<br>
 * dataSource.setPortNumber (50000);<br>
 * MiniDataSource poolMgr = new MiniDataSource(dataSource,maxConnections);<br>
 * ...<br>
 * Connection connection = poolMgr.getConnection();<br>
 * ...<br>
 * connection.close();<br>
 * <p>
 * For Other<br>
 * com.ibm.db2.jcc.DB2ConnectionPoolDataSource<br>
 * oracle.jdbc.pool.OracleConnectionPoolDataSource<br>
 * com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource<br>
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
@MbeanResource
public final class MiniDataSource implements DataSource, Disposable {
	static final Logger logger = Logger.getLogger(MiniDataSource.class);

	final short redundancy;
	final Stack stack;
	final Semaphore permits;
	private final ConnectionPoolDataSource cpds;
	private final int maxSize;
	private String testQuery;
	private volatile boolean destroyed;
	volatile boolean forceCheck;

	public MiniDataSource(ConnectionPoolDataSource dataSource, int corePoolSize) {
		this(dataSource, corePoolSize, corePoolSize);
	}

	public MiniDataSource(ConnectionPoolDataSource dataSource, int corePoolSize, int maxPoolSize) {
		if ((redundancy = (short) (maxPoolSize - corePoolSize)) < 0 || corePoolSize < 0 || maxPoolSize <= 0)
			throw new IllegalArgumentException("invalid args.");
		this.permits = new Semaphore(maxSize = maxPoolSize);
		this.stack = new Stack();
		this.cpds = dataSource;
		DisposeManager.register(this);
	}
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return cpds.getLogWriter();
	}
	@Override
	public int getLoginTimeout() throws SQLException {
		return cpds.getLoginTimeout();
	}
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		cpds.setLogWriter(out);
	}
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		cpds.setLoginTimeout(seconds);
	}

	/**
	 * 设置连接检验SQL
	 * 
	 */
	public void setTestQuery(String query) {
		testQuery = query;
	}

	private Connection retrieve() {
		while (true) {
			Node node = stack.pop();
			if (node == null)
				return null;
			try {
				Connection result = node.item.getConnection();
				long now = Util.now();
				if (forceCheck || now - node.millis > 4000) {
					Util.testConnection(result, testQuery);
					forceCheck = false;
				}
				node.item.addConnectionEventListener(node);
				node.millis = now;
				return new Wrapper(result);
			} catch (Throwable e) {
				closeQuietly(node.item);
				logger.info("close invalid connection.", e);
			}
		}
	}

	@MbeanAttribute
	public int getIdleNumber() {
		return stack.size();
	}
	@Override
	public void destroy() {
		if (!destroyed) {
			destroyed = true;
			int count = stack.size();
			stack.destroy();
			logger.debug("destoryed. in recycle: {} , available: {}", count, permits.availablePermits());
		}
	}

	@MbeanAttribute
	public int getActiveNumber() {
		return maxSize - permits.availablePermits();
	}
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		throw new UnsupportedOperationException("Cannot specify username/password for connections");
	}
	@Override
	public Connection getConnection() throws SQLException {
		try {
			if (!permits.tryAcquire(10, TimeUnit.SECONDS))
				throw new SQLException("waiting timeout.");
		} catch (InterruptedException e) {
			throw new XutilRuntimeException(e);
		}
		PooledConnection pooledConn = null;
		try {
			Connection result = retrieve();
			if (result == null) {
				if (destroyed)
					throw new SQLException("datasource destroyed.");
				logger.debug("pool is empty,create connection.");
				pooledConn = cpds.getPooledConnection();
				result = pooledConn.getConnection();
				pooledConn.addConnectionEventListener(new Node(pooledConn));
				result = new Wrapper(result);
			}
			return result;
		} catch (Throwable e) {
			closeQuietly(pooledConn);
			permits.release();
			if (e instanceof SQLException)
				throw (SQLException) e;
			throw Objutil.rethrow(e);
		}
	}

	/**
	 * @param iface
	 * @throws SQLException
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	/**
	 * @param iface
	 * @throws SQLException
	 */
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	static void closeQuietly(PooledConnection pooledConnection) {
		if (pooledConnection != null)
			try {
				pooledConnection.close();
			} catch (Throwable e) { // do nothing
			}
	}

	private final class Wrapper extends CloseSuppressing {
		Wrapper(Connection c) {
			super(c);
		}

		@Override
		public void close() throws SQLException {
			Connection c = delegate;
			delegate = null;
			if (c != null)
				try {
					c.close();
				} catch (SQLException e) {
					forceCheck = true;
					throw e;
				} finally {
					permits.release();
				}
		}

		@Override
		protected void finalize() throws Throwable {
			if (delegate != null) {
				logger.warn("connection not close.");
				close();
			}
		}
	}

	private final class Node implements ConnectionEventListener {
		final PooledConnection item;
		volatile long millis;
		Node next, previous;

		Node(PooledConnection element) {
			item = element;
			millis = Util.now();
		}
		@Override
		public void connectionClosed(ConnectionEvent event) {
			item.removeConnectionEventListener(this);
			long now = Util.now();
			int activeMillis = (int) (now - millis);
			millis = now;
			stack.add(this);
			if (activeMillis > 500) {
				if (activeMillis > 5000)
					logger.warn("connection use millis: {}", activeMillis);
				else
					logger.info("connection use millis: {}", activeMillis);
			}
		}
		@Override
		public void connectionErrorOccurred(ConnectionEvent event) {
			forceCheck = true;
			item.removeConnectionEventListener(this);
			closeQuietly(item);
			logger.warn("connection use millis: {}", event.getSQLException(), Util.now() - millis);
		}
	}

	private final class Stack implements Runnable {
		private final Node header = new Node(null);
		private Future future;
		private int size;

		Stack() {
			header.next = header.previous = header;
			future = Util.getScheduler().scheduleWithFixedDelay(this, 10000, 10000, TimeUnit.MILLISECONDS);
		}

		synchronized Node pop() {
			Node e = header.next;
			if (e == header)
				return null;
			(e.next.previous = e.previous).next = e.next;
			e.next = e.previous = null;
			size--;
			return e;
		}

		synchronized Node check(long now) {
			Node e = header.previous;
			if (e == header || now - e.millis < 20000)
				return null;
			(e.next.previous = e.previous).next = e.next;
			e.next = e.previous = null;
			size--;
			return e;
		}

		synchronized void add(Node e) {
			if (future == null)
				closeQuietly(e.item);
			else {
				e.next = (e.previous = header).next;
				e.previous.next = e.next.previous = e;
				size++;
			}
		}

		synchronized void destroy() {
			if (future == null)
				return;
			future.cancel(false);
			future = null;
			Node e;
			while ((e = pop()) != null)
				closeQuietly(e.item);
		}

		synchronized int size() {
			return size;
		}
		@Override
		public void run() {
			int i = size();
			if (i > 0) {
				i = redundancy + i - permits.availablePermits();
				Node e;
				long now = Util.now();
				while (--i >= 0 && (e = check(now)) != null)
					closeQuietly(e.item);
			}
		}
	}
}
