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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.sql.handl.FirstField;
import zcu.xutil.utils.AccessProperty;
import zcu.xutil.utils.Accessor;
import zcu.xutil.utils.LRUCache;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class DBTool {

	private static final LRUCache<Class, Map<String, Accessor>> cache = new LRUCache<Class, Map<String, Accessor>>(
			Objutil.systring(Constants.XUTILS_SQL_DBTOOL_CACHE, 95), null);

	static Map<String, Accessor> getAllAccessor(Class clazz) {
		Map<String, Accessor> result = cache.get(clazz);
		if (result != null)
			return result;
		result = AccessProperty.build(clazz, null);
		return Objutil.ifNull(cache.putIfAbsent(clazz, result), result);
	}

	public static <T> T query(Connection conn, String sql, Handler<T> rsh) throws SQLException {
		Logger.LOG.debug(sql);
		Statement stmt = conn.createStatement();
		try {
			rsh.setOptions(stmt);
			return rsh.handle(stmt.executeQuery(sql));
		} finally {
			stmt.close();
		}
	}

	private static PreparedStatement fillParams(PreparedStatement statement, Object[] params) throws SQLException {
		Object o;
		for (int i = params.length; i > 0; i--) {
			if ((o = params[i - 1]) == null)
				statement.setNull(i, Types.NULL);
			else if (o instanceof Class)
				SQLType.setNull(statement, i, (Class) o);
			else
				SQLType.setValue(statement, i, o);
		}
		return statement;
	}

	public static <T> T query(Connection conn, String sql, Handler<T> rsh, Object... params) throws SQLException {
		if (params == null || params.length == 0)
			return query(conn, sql, rsh);
		Logger.LOG.debug("{} ,params: {}", sql, params);
		PreparedStatement stmt = conn.prepareStatement(sql);
		try {
			rsh.setOptions(stmt);
			return rsh.handle(fillParams(stmt, params).executeQuery());
		} finally {
			stmt.close();
		}
	}

	public static int update(Connection conn, String sql) throws SQLException {
		Logger.LOG.debug(sql);
		Statement stmt = conn.createStatement();
		try {
			return stmt.executeUpdate(sql);
		} finally {
			stmt.close();
		}
	}

	public static int update(Connection conn, String sql, Object... params) throws SQLException {
		if (params == null || params.length == 0)
			return update(conn, sql);
		Logger.LOG.debug("{} ,params: {}", sql, params);
		PreparedStatement stmt = conn.prepareStatement(sql);
		try {
			return fillParams(stmt, params).executeUpdate();
		} finally {
			stmt.close();
		}
	}

	public static <T> T returnGK(Connection conn, String sql, Handler<T> rsh, Object... params) throws SQLException {
		Logger.LOG.debug("{} ,params: {}", sql, params);
		PreparedStatement pstmt = params == null || params.length == 0 ? null : conn.prepareStatement(sql,
				Statement.RETURN_GENERATED_KEYS);
		Statement stmt = pstmt == null ? conn.createStatement() : pstmt;
		try {
			if (pstmt == null)
				stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			else
				fillParams(pstmt, params).executeUpdate();
			return rsh.handle(stmt.getGeneratedKeys());
		} finally {
			stmt.close();
		}
	}

	public static int[] batch(Connection conn, String sql, Object[]... params) throws SQLException {
		Logger.LOG.debug("{} ,batchs: {}", sql, params.length);
		PreparedStatement stmt = conn.prepareStatement(sql);
		try {
			for (Object[] array : params)
				fillParams(stmt, array).addBatch();
			return stmt.executeBatch();
		} finally {
			stmt.close();
		}
	}

	public static <T> T mapQuery(Connection conn, NpSQL npsql, Handler<T> rsh, Map<String, Object> namedParams)
			throws SQLException {
		return query(conn, npsql.getSql(), rsh, npsql.fromMap(namedParams));
	}

	public static int mapUpdate(Connection conn, NpSQL npsql, Map<String, Object> paramsMap) throws SQLException {
		ID id = npsql.getID();
		if (id == null)
			return update(conn, npsql.getSql(), npsql.fromMap(paramsMap));
		Object cls = paramsMap.get(id.getName());
		Objutil.validate(cls instanceof Class, "params map must contains [ID name , ID type]");
		Object primaryKey;
		if (id.getPolicy() == ID.SEQUENCE) {
			primaryKey = query(conn, id.getGenSQL(), FirstField.get((Class<?>) cls));
			paramsMap.put(id.getName(), Objutil.notNull(primaryKey, "null primary key"));
			return update(conn, npsql.getSql(), npsql.fromMap(paramsMap));
		}
		if (id.getPolicy() == ID.AUTO)
			primaryKey = returnGK(conn, npsql.getSql(), FirstField.get((Class<?>) cls), npsql.fromMap(paramsMap));
		else {
			update(conn, npsql.getSql(), npsql.fromMap(paramsMap));
			primaryKey = query(conn, id.getGenSQL(), FirstField.get((Class<?>) cls));
		}
		paramsMap.put(id.getName(), Objutil.notNull(primaryKey, "null primary key"));
		return 1;
	}

	public static int[] mapBatch(Connection conn, NpSQL npsql, Map<String, Object>... mapParams) throws SQLException {
		int len = mapParams.length;
		if (npsql.getID() != null && len > 0) {
			int[] result = new int[len];
			for (int i = 0; i < len; i++)
				result[i] = mapUpdate(conn, npsql, mapParams[i]);
			return result;
		}
		Object[][] params = new Object[len][];
		while (--len >= 0)
			params[len] = npsql.fromMap(mapParams[len]);
		return batch(conn, npsql.getSql(), params);
	}

	public static <T> T entityQuery(Connection conn, NpSQL npsql, Handler<T> rsh, Object entity) throws SQLException {
		return query(conn, npsql.getSql(), rsh, npsql.fromBean(entity));
	}

	public static int entityUpdate(Connection conn, NpSQL npsql, Object entity) throws SQLException {
		ID id = npsql.getID();
		if (id == null)
			return update(conn, npsql.getSql(), npsql.fromBean(entity));
		Accessor acs = getAllAccessor(entity.getClass()).get(id.getName());
		Object primaryKey;
		if (id.getPolicy() == ID.SEQUENCE) {
			primaryKey = query(conn, id.getGenSQL(), FirstField.get(acs.getType()));
			acs.setValue(entity, Objutil.notNull(primaryKey, "null primary key"));
			return update(conn, npsql.getSql(), npsql.fromBean(entity));
		}
		if (id.getPolicy() == ID.AUTO)
			primaryKey = returnGK(conn, npsql.getSql(), FirstField.get(acs.getType()), npsql.fromBean(entity));
		else {
			update(conn, npsql.getSql(), npsql.fromBean(entity));
			primaryKey = query(conn, id.getGenSQL(), FirstField.get(acs.getType()));
		}
		acs.setValue(entity, Objutil.notNull(primaryKey, "null primary key"));
		return 1;
	}

	public static int[] entityBatch(Connection conn, NpSQL npsql, Object... entityParams) throws SQLException {
		ID idgen = npsql.getID();
		int len = entityParams.length;
		if (idgen != null && len > 0) {
			int[] result = new int[len];
			for (int i = 0; i < len; i++)
				result[i] = entityUpdate(conn, npsql, entityParams[i]);
			return result;

		}
		Object[][] params = new Object[len][];
		while (--len >= 0)
			params[len] = npsql.fromBean(entityParams[len]);
		return batch(conn, npsql.getSql(), params);
	}

	abstract Connection connect() throws SQLException;

	/**
	 * 判断表是否存在.
	 * 
	 * @param tableName
	 *            the table name
	 * 
	 * @return true, if successful
	 */
	public final boolean tableExist(String tableName) throws SQLException {
		Connection conn = connect();
		try {
			Statement stmt = conn.createStatement();
			try {
				return stmt.executeQuery("SELECT count(*) FROM " + tableName + " WHERE 1=0").next();
			} catch (SQLException e) {
				return false;
			} finally {
				stmt.close();
			}
		} finally {
			conn.close();
		}
	}

	/**
	 * 普通SQL查询.
	 * 
	 * @param sql
	 *            标准SQL查询语句
	 * @param rsh
	 *            ResultSet 处理器
	 * @param params
	 *            查询语句参数
	 * 
	 * @return {@link Handler} 处理结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final <T> T query(String sql, Handler<T> rsh, Object... params) throws SQLException {
		Connection conn = connect();
		try {
			return query(conn, sql, rsh, params);
		} finally {
			conn.close();
		}
	}

	public final <T> T query(String sql, Handler<T> rsh) throws SQLException {
		return query(sql, rsh, (Object[]) null);
	}

	/**
	 * 普通SQL执行.
	 * 
	 * @param sql
	 *            标准SQL执行语句,an SQL INSERT, UPDATE or DELETE statement or an SQL
	 *            statement that returns nothing.
	 * @param params
	 *            执行语句参数
	 * 
	 * @return either the row count for INSERT, UPDATE or DELETE statements, or
	 *         0 for SQL statements that return nothing
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int update(String sql, Object... params) throws SQLException {
		Connection conn = connect();
		try {
			return update(conn, sql, params);
		} finally {
			conn.close();
		}
	}

	public final int update(String sql) throws SQLException {
		return update(sql, (Object[]) null);
	}

	/**
	 * 普通SQL批处理执行.
	 * 
	 * @param sql
	 *            标准SQL执行语句
	 * @param params
	 *            执行语句参数数组
	 * 
	 * @return 每组参数对应的执行结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int[] batch(String sql, Object[]... params) throws SQLException {
		Connection conn = connect();
		try {
			return batch(conn, sql, params);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL查询. 命名参数SQL: 用 :name 代替 ? 的SQL.
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param rsh
	 *            ResultSet 处理器
	 * @param namedParams
	 *            命名参数
	 * 
	 * @return {@link Handler} 处理结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final <T> T mapQuery(NpSQL npsql, Handler<T> rsh, Map<String, Object> namedParams) throws SQLException {
		Connection conn = connect();
		try {
			return mapQuery(conn, npsql, rsh, namedParams);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL执行.
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param paramsMap
	 *            命名参数.如果{@link NpSQL}指定了{@link ID} ,则参数map中应包含
	 *            {@link ID#getName()},<br>
	 *            其值为主键class,执行成功后.主键class被替换为主键值.
	 * 
	 * @return row counts
	 * @see #update(String, Object...)
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int mapUpdate(NpSQL npsql, Map<String, Object> paramsMap) throws SQLException {
		Connection conn = connect();
		try {
			return mapUpdate(conn, npsql, paramsMap);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL批处理执行.
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param mapParams
	 *            命名参数数组。
	 * @see #mapUpdate(NpSQL, Map).
	 * 
	 * @return 每组参数对应的执行结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int[] mapBatch(NpSQL npsql, Map<String, Object>... mapParams) throws SQLException {
		Connection conn = connect();
		try {
			return mapBatch(conn, npsql, mapParams);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL查询.用实体entity的属性作为参数
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param rsh
	 *            ResultSet 处理器
	 * @param entity
	 *            实体参数
	 * 
	 * @return {@link Handler} 处理结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final <T> T entityQuery(NpSQL npsql, Handler<T> rsh, Object entity) throws SQLException {
		Connection conn = connect();
		try {
			return entityQuery(conn, npsql, rsh, entity);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL执行.用实体entity的属性作为参数.
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param entity
	 *            实体参数.如果{@link NpSQL}指定了{@link ID}.实体应有 {@link ID#getName()}
	 *            属性,执行成功后.该属性被设置为主键值.
	 * 
	 * @return row counts
	 * @see #update(String, Object...)
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int entityUpdate(NpSQL npsql, Object entity) throws SQLException {
		Connection conn = connect();
		try {
			return entityUpdate(conn, npsql, entity);
		} finally {
			conn.close();
		}
	}

	/**
	 * 命名参数SQL批处理执行.用实体entity的属性作为参数
	 * 
	 * @param npsql
	 *            {@link NpSQL}
	 * @param entityParams
	 *            实体参数数组
	 * 
	 * @return 每组参数对应的执行结果.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	public final int[] entityBatch(NpSQL npsql, Object... entityParams) throws SQLException {
		Connection conn = connect();
		try {
			return entityBatch(conn, npsql, entityParams);
		} finally {
			conn.close();
		}
	}
}
