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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Blob;

import javax.sql.rowset.serial.SerialBlob;

import java.sql.Clob;

import javax.sql.rowset.serial.SerialClob;

import zcu.xutil.Constants;
import zcu.xutil.Objutil;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class SQLType {
	private static final Map<Class, SQLType> maps = new HashMap<Class, SQLType>(32);
	static {
		new SQLType(String.class, Types.VARCHAR) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getString(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setString(index, value.toString());
			}
		};
		new SQLType(Integer.class, Types.INTEGER) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				int i = rs.getInt(index);
				return rs.wasNull() ? null : Integer.valueOf(i);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setInt(index, (Integer) value);
			}
		};
		new SQLType(Long.class, Types.BIGINT) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				long l = rs.getLong(index);
				return rs.wasNull() ? null : Long.valueOf(l);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setLong(index, (Long) value);
			}
		};
		new SQLType(Double.class, Types.DOUBLE) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				double d = rs.getDouble(index);
				return rs.wasNull() ? null : Double.valueOf(d);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setDouble(index, (Double) value);
			}
		};
		new SQLType(Boolean.class, Types.BIT) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				boolean bool = rs.getBoolean(index);
				return rs.wasNull() ? null : Boolean.valueOf(bool);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setBoolean(index, (Boolean) value);

			}
		};
		new SQLType(Byte.class, Types.TINYINT) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				byte b = rs.getByte(index);
				return rs.wasNull() ? null : Byte.valueOf(b);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setByte(index, (Byte) value);
			}
		};
		new SQLType(Short.class, Types.SMALLINT) {
			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				short s = rs.getShort(index);
				return rs.wasNull() ? null : Short.valueOf(s);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setShort(index, (Short) value);
			}
		};
		new SQLType(Character.class, Types.CHAR) {
			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				String str = rs.getString(index);
				return (str == null || str.length() == 0) ? null : Character.valueOf(str.charAt(0));
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setString(index, value.toString());
			}
		};
		new SQLType(Float.class, Types.FLOAT) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				float f = rs.getFloat(index);
				return rs.wasNull() ? null : Float.valueOf(f);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setFloat(index, (Float) value);
			}
		};
		new SQLType(char[].class, Types.VARCHAR) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				String str = rs.getString(index);
				return (str == null) ? null : str.toCharArray();
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setString(index, new String((char[]) value));
			}
		};
		new SQLType(byte[].class, Types.VARBINARY) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getBytes(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setBytes(index, (byte[]) value);
			}
		};
		new SQLType(BigInteger.class, Types.NUMERIC) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				BigDecimal bigDecimal = rs.getBigDecimal(index);
				return bigDecimal == null ? null : bigDecimal.toBigIntegerExact();
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setBigDecimal(index, new BigDecimal((BigInteger) value));
			}
		};
		new SQLType(BigDecimal.class, Types.NUMERIC) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getBigDecimal(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setBigDecimal(index, (BigDecimal) value);
			}
		};
		new SQLType(java.util.Date.class, Types.TIMESTAMP) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getTimestamp(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				if (!(value instanceof Timestamp))
					value = new Timestamp(((java.util.Date) value).getTime());
				stmt.setTimestamp(index, (Timestamp) value);

			}
		};
		new SQLType(Timestamp.class, Types.TIMESTAMP) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getTimestamp(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setTimestamp(index, (Timestamp) value);
			}
		};
		new SQLType(java.sql.Date.class, Types.DATE) {

			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getDate(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setDate(index, (java.sql.Date) value);
			}
		};
		new SQLType(Time.class, Types.TIME) {
			@Override
			public Object get(ResultSet rs, int index) throws SQLException {
				return rs.getTime(index);
			}

			@Override
			public void set(PreparedStatement stmt, int index, Object value) throws SQLException {
				stmt.setTime(index, (java.sql.Time) value);

			}
		};

	}
	public final Class javacls;
	public final int type;

	protected SQLType(Class javaclass, int sqltype) {
		type = sqltype;
		maps.put(javacls = javaclass, this);
	}

	protected abstract Object get(ResultSet rs, int index) throws SQLException;

	protected abstract void set(PreparedStatement stmt, int index, Object value) throws SQLException;

	@SuppressWarnings("unchecked")
	public static <T> T getValue(ResultSet rs, int index, Class<T> clazz) throws SQLException {
		Object obj = Objutil.defaults(clazz);
		if (obj != null)
			clazz = (Class<T>) obj.getClass();
		SQLType sqltype = maps.get(clazz);
		if (sqltype != null)
			return (T) Objutil.ifNull(sqltype.get(rs, index), obj);
		if (clazz.isEnum()) {
			String em = rs.getString(index);
			return (T) (em == null ? null : Enum.valueOf((Class) clazz, em));
		}
		boolean blob = Blob.class.isAssignableFrom(clazz);
		if (blob || Clob.class.isAssignableFrom(clazz)) {
			obj = blob ? rs.getBlob(index) : rs.getClob(index);
			if (obj == null || clazz.isInstance(obj))
				return (T) obj;
			Callback handle = Lobs.callbacks.get(clazz);
			if (handle == null)
				throw new SQLException("can't handle LOB " + clazz);
			obj = handle.call(obj);
		} else
			obj = rs.getObject(index);
		return clazz.cast(obj);
	}

	public static void setValue(PreparedStatement stmt, int index, Object value) throws SQLException {
		SQLType sqltype = maps.get(value.getClass());
		if (sqltype != null)
			sqltype.set(stmt, index, value);
		else if (value instanceof Enum) {
			stmt.setString(index, ((Enum) value).name());
		} else if (value instanceof Blob) {
			stmt.setBlob(index, (Blob) value);
		} else if (value instanceof Clob) {
			stmt.setClob(index, (Clob) value);
		} else
			stmt.setObject(index, value);
	}

	public static void setNull(PreparedStatement stmt, int index, Class clazz) throws SQLException {
		Object obj = Objutil.defaults(clazz);
		if (obj != null)
			clazz = obj.getClass();
		SQLType sqltype = maps.get(clazz);
		if (sqltype != null)
			stmt.setNull(index, sqltype.type);
		else if (clazz.isEnum())
			stmt.setNull(index, Types.VARCHAR);
		else if (Blob.class.isAssignableFrom(clazz))
			stmt.setNull(index, Types.BLOB);
		else if (Clob.class.isAssignableFrom(clazz))
			stmt.setNull(index, Types.CLOB);
		else
			stmt.setNull(index, Types.NULL);
	}

	public static SQLType get(Class<?> clazz) {
		return maps.get(clazz);
	}

	private static final class Lobs implements Callback {
		static final Map<Class, Callback> callbacks = new HashMap<Class, Callback>();
		static {
			new Lobs(SerialBlob.class);
			new Lobs(SerialClob.class);
			List<String> lobs = Objutil.split(Objutil.systring(Constants.XUTILS_SQL_SQLTYPE_LOBS), ',');
			int i = lobs.size();
			if (i > 0) {
				ClassLoader cl = Objutil.contextLoader();
				do {
					String s = lobs.get(i);
					int j = s.indexOf('=');
					Callback cb = (Callback) Objutil.newInstance(Objutil.loadclass(cl, s.substring(j + 1).trim()));
					callbacks.put(Objutil.loadclass(cl, s.substring(0, j).trim()), cb);
				} while (--i >= 0);
			}
		}

		private Lobs(Class c) {
			callbacks.put(cls = c, this);
		}

		private final Class cls;
		@Override
		public Object call(Object from) throws SQLException {
			return cls == SerialBlob.class ? new SerialBlob((Blob) from) : new SerialClob((Clob) from);
		}
	}
}
