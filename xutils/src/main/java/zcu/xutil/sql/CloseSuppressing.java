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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLXML;
import java.sql.Array;
import java.sql.Clob;
import java.sql.Blob;
import java.sql.Struct;
import java.util.Properties;

public class CloseSuppressing implements Connection {
	Connection delegate;

	public CloseSuppressing(Connection connection) {
		this.delegate = connection;
	}

	/**
	 * @param statement
	 * @throws SQLException
	 */
	protected void option(Statement statement) throws SQLException {
		// nothing
	}

	protected final Connection getDelegate() throws SQLException {
		if (delegate == null)
			throw new SQLException("closed.");
		return delegate;
	}
	@Override
	public boolean isClosed() throws SQLException {
		return delegate == null;
	}
	@Override
	public void close() throws SQLException {
		delegate = null;
	}
	@Override
	public void clearWarnings() throws SQLException {
		getDelegate().clearWarnings();
	}
	@Override
	public void commit() throws SQLException {
		getDelegate().commit();
	}
	@Override
	public boolean getAutoCommit() throws SQLException {
		return getDelegate().getAutoCommit();
	}
	@Override
	public String getCatalog() throws SQLException {
		return getDelegate().getCatalog();
	}
	@Override
	public int getHoldability() throws SQLException {
		return getDelegate().getHoldability();
	}
	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return getDelegate().getMetaData();
	}
	@Override
	public int getTransactionIsolation() throws SQLException {
		return getDelegate().getTransactionIsolation();
	}
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return getDelegate().getTypeMap();
	}
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return getDelegate().getWarnings();
	}
	@Override
	public boolean isReadOnly() throws SQLException {
		return getDelegate().isReadOnly();
	}
	@Override
	public String nativeSQL(String sql) throws SQLException {
		return getDelegate().nativeSQL(sql);
	}
	@Override
	public Statement createStatement() throws SQLException {
		Statement s = getDelegate().createStatement();
		option(s);
		return s;
	}
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		Statement s = getDelegate().createStatement(resultSetType, resultSetConcurrency);
		option(s);
		return s;
	}
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		Statement s = getDelegate().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		option(s);
		return s;
	}
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		CallableStatement s = getDelegate().prepareCall(sql);
		option(s);
		return s;
	}
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		CallableStatement s = getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency);
		option(s);
		return s;
	}
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		CallableStatement s = getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql, autoGeneratedKeys);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql, columnIndexes);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql, columnNames);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency);
		option(s);
		return s;
	}
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		PreparedStatement s = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
		option(s);
		return s;
	}
	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		getDelegate().releaseSavepoint(savepoint);

	}
	@Override
	public void rollback() throws SQLException {
		getDelegate().rollback();

	}
	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		getDelegate().rollback(savepoint);

	}
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		getDelegate().setAutoCommit(autoCommit);

	}
	@Override
	public void setCatalog(String catalog) throws SQLException {
		getDelegate().setCatalog(catalog);

	}
	@Override
	public void setHoldability(int holdability) throws SQLException {
		getDelegate().setHoldability(holdability);

	}
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		getDelegate().setReadOnly(readOnly);

	}
	@Override
	public Savepoint setSavepoint() throws SQLException {
		return getDelegate().setSavepoint();
	}
	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return getDelegate().setSavepoint(name);
	}
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		getDelegate().setTransactionIsolation(level);

	}
	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		getDelegate().setTypeMap(map);
	}

	/**
	 * 1.6
	 */
	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return getDelegate().createArrayOf(typeName, elements);
	}
	@Override
	public Blob createBlob() throws SQLException {
		return getDelegate().createBlob();
	}
	@Override
	public Clob createClob() throws SQLException {
		return getDelegate().createClob();
	}
	@Override
	public NClob createNClob() throws SQLException {
		return getDelegate().createNClob();
	}
	@Override
	public SQLXML createSQLXML() throws SQLException {
		return getDelegate().createSQLXML();
	}
	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return getDelegate().createStruct(typeName, attributes);
	}
	@Override
	public Properties getClientInfo() throws SQLException {
		return getDelegate().getClientInfo();
	}
	@Override
	public String getClientInfo(String name) throws SQLException {
		return getDelegate().getClientInfo(name);
	}
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return getDelegate().isWrapperFor(iface);
	}
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return getDelegate().unwrap(iface);
	}
	@Override
	public boolean isValid(int timeout) throws SQLException {
		return delegate != null && delegate.isValid(timeout);
	}
	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		if (delegate == null)
			throw new SQLClientInfoException();
		delegate.setClientInfo(properties);
	}
	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		if (delegate == null)
			throw new SQLClientInfoException();
		delegate.setClientInfo(name, value);
	}
}
