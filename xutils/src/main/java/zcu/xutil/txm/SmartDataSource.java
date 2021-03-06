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
package zcu.xutil.txm;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import zcu.xutil.DisposeManager;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.sql.AbstractDataSource;
import zcu.xutil.sql.CloseSuppressing;
import zcu.xutil.utils.Util;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class SmartDataSource extends AbstractDataSource implements MResourceFactory {
	private final DataSource datasource;
	private int commitOrder;
	private volatile String destroyMethod;
	String testQuery;

	public SmartDataSource(DataSource dataSource) {
		Objutil.validate(!(dataSource instanceof SmartDataSource), "dataSource is SmartDataSource.");
		this.datasource = Objutil.notNull(dataSource, "dataSource is null");
	}
	@Override
	protected  CommonDataSource getBase(){
		return datasource;
	}
	
	public void setDestroyMethod(String method) {
		destroyMethod = method;
	}
	
	/**
	 * 在事务提交前检查连接
	 *
	 */
	public void setTestQuery(String query) {
		testQuery = query;
	}
	
	@Override
	public void destroy() {
		String s = destroyMethod;
		destroyMethod = null;
		if(s != null)
			DisposeManager.destroyCall(datasource, s);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		try {
			Connection ret = (Connection) JdbcTxManager.instance.getConnection(this);
			if (ret == null)
				return datasource.getConnection();
			Logger.LOG.debug("SmartDataSource get connection from transaction.");
			return ret;
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
	}

	public void setLastCommit(boolean lastCommit) {
		this.commitOrder = lastCommit ? 1 : 0 ;
	}
	@Override
	public MResource newResource(TxInfo txinfo) throws SQLException {
		return new CRes(datasource, txinfo);
	}
	@Override
	public int getCommitOrder() {
		return commitOrder;
	}

	private final class CRes implements MResource {
		final TxInfo txinfo;
		Connection target;

		CRes(DataSource ds, TxInfo info) throws SQLException {
			this.txinfo = info;
			Connection connection = ds.getConnection();
			try {
				int isolation = info.getIsolation();
				if (isolation != Connection.TRANSACTION_NONE)
					connection.setTransactionIsolation(isolation);
				connection.setAutoCommit(false);
				this.target = connection;
			} finally {
				if (target == null)
					connection.close();
			}
		}
		@Override
		public void afterCompletion() {
			Util.closeQuietly(target);
			target = null;
		}
		@Override
		public void beforeSuspend() {
			// do nothing
		}
		@Override
		public void beforeCompletion() {
			Util.testConnection(target, testQuery);
		}
		@Override
		public void commit() throws SQLException {
			target.commit();
		}
		@Override
		public void rollback() throws SQLException {
			target.rollback();
		}
		@Override
		public Object getHandle() {
			return new CloseSuppressing(target) {

				@Override
				protected void option(Statement statement) throws SQLException {
					int timeout = txinfo.getTimeout();
					if (timeout > 0)
						statement.setQueryTimeout(timeout);
				}

				@Override
				public void commit() throws SQLException {
					getDelegate();
				}

				@Override
				public void rollback() throws SQLException {
					getDelegate();
					txinfo.setRollbackOnly();
				}

				@Override
				public void setAutoCommit(boolean autoCommit) throws SQLException {
					getDelegate();
				}
			};
		}
		@Override
		public MResourceFactory getFactory() {
			return SmartDataSource.this;
		}
	}
}
