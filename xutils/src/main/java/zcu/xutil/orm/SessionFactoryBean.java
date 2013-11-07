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
package zcu.xutil.orm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.transaction.JTATransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;

import zcu.xutil.sql.DBTool;
import zcu.xutil.txm.JdbcTxManager;
import zcu.xutil.txm.JtaTxManager;
import zcu.xutil.txm.SmartDataSource;
import zcu.xutil.txm.TxManager;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class SessionFactoryBean {
	static final ThreadLocal<SessionFactoryBean> configTimeHolder = new ThreadLocal<SessionFactoryBean>();

	TxManager txManager;
	DataSource dataSource;
	ClassLoader beanClassLoader;
	Configuration configure;
	
	public SessionFactoryBean(Configuration configuration) {
		this.configure = configuration;
	}

	public void setDataSource(DataSource aDataSource) {
		this.dataSource = aDataSource;
	}

	public void setBeanClassLoader(ClassLoader aBeanClassLoader) {
		this.beanClassLoader = aBeanClassLoader;
	}


	public void setTxManager(TxManager manager) {
		if (txManager == null)
			txManager = manager;
		else if (txManager != manager)
			throw new IllegalArgumentException("txManager setted.");
	}

	public SessionFactory getObject() {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextCL = currentThread.getContextClassLoader();
		boolean overrideClassLoader = beanClassLoader != null && !beanClassLoader.equals(threadContextCL);
		if (overrideClassLoader)
			currentThread.setContextClassLoader(beanClassLoader);
		try {
			configTimeHolder.set(this);
			configure.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, XutilSessionContext.class.getName());
			if (txManager == null)
				txManager = JdbcTxManager.instance;
			if (txManager instanceof JtaTxManager) {
				configure.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, TML.class.getName());
				configure.setProperty(Environment.TRANSACTION_STRATEGY, JTATransactionFactory.class.getName());
				configure.setProperty(Environment.JNDI_CLASS, ICF.class.getName());
			}
			if (dataSource != null)
				configure.setProperty(Environment.CONNECTION_PROVIDER, CP.class.getName());
			return configure.buildSessionFactory();
		} finally {
			configTimeHolder.remove();
			if (overrideClassLoader)
				currentThread.setContextClassLoader(threadContextCL);
		}
	}

	public void dropDatabaseSchema() throws SQLException {
		Dialect dialect = Dialect.getDialect(configure.getProperties());
		String[] sqls = configure.generateDropSchemaScript(dialect);
		Connection conn = dataSource.getConnection();
		try {
			for (String sql : sqls)
				DBTool.update(conn, sql);
		} finally {
			conn.close();
		}
	}

	public void createDatabaseSchema() throws SQLException {
		Dialect dialect = Dialect.getDialect(configure.getProperties());
		String[] sqls = configure.generateSchemaCreationScript(dialect);
		Connection conn = dataSource.getConnection();
		try {
			for (String sql : sqls)
				DBTool.update(conn, sql);
		} finally {
			conn.close();
		}
	}

	public final static class TML implements TransactionManagerLookup {
		private final JtaTxManager jtaManager = (JtaTxManager) configTimeHolder.get().txManager;
		@Override
		public TransactionManager getTransactionManager(Properties props) {
			return jtaManager.getTransactionManager();
		}
		@Override
		public String getUserTransactionName() {
			return "java:comp/UserTransaction";
		}
		@Override
		public Object getTransactionIdentifier(Transaction transaction) {
			return transaction;
		}
	}

	public final static class CP implements ConnectionProvider {
		DataSource datasource;
		boolean aggressive;
		@Override
		public void close() throws HibernateException {
			// TODO Auto-generated method stub
		}
		@Override
		public void closeConnection(Connection connection) throws SQLException {
			connection.close();
		}
		@Override
		public void configure(Properties arg0) throws HibernateException {
			SessionFactoryBean sfb = configTimeHolder.get();
			datasource = sfb.dataSource;
			aggressive = sfb.txManager instanceof JtaTxManager || datasource instanceof SmartDataSource;
		}
		@Override
		public Connection getConnection() throws SQLException {
			return datasource.getConnection();
		}
		@Override
		public boolean supportsAggressiveRelease() {
			return aggressive;
		}
	}

	public final static class ICF extends InitialContext implements InitialContextFactory {
		private final JtaTxManager jtaManager;

		public ICF() throws NamingException {
			super(true);
			this.jtaManager = (JtaTxManager) configTimeHolder.get().txManager;
		}
		@Override
		public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
			return this;
		}

		@Override
		public Object lookup(String name) throws NamingException {
			if (name.equals("java:comp/UserTransaction"))
				return jtaManager.getUserTransaction();
			throw new NameNotFoundException(name);
		}
	}

	// public final static class JTF extends JTATransactionFactory{
	// JtaTxManager jtaManager;
	// @Override
	// public void configure(Properties props) throws HibernateException {
	// jtaManager=(JtaTxManager) configTimeHolder.get().txManager;
	// }
	// @Override
	// protected UserTransaction getUserTransaction() {
	// return jtaManager.getUserTransaction();
	// }
	// }
}
