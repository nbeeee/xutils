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
import java.sql.SQLException;

import javax.sql.DataSource;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.utils.Util;

/**
 * Informix identity params: 类型为 SERIAL8 时 为 serial8 ; 类型为 SERIAL时, ESQL/C为
 * sqlca.sqlerrd1 ,4GL为 sqlca.sqlerrd2<br/>
 * PostgreSQL identity params: 类型为 SERIAL时为 table + '_' + column+ '_seq'
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public enum DBType {
	oracle("SELECT {}.NEXTVAL FROM dual", "SELECT {}.CURRVAL FROM dual", "SELECT 1 FROM dual", "ORACLE", "Oracle"), // oracle
	db2("VALUES NEXTVAL FOR {}", "VALUES IDENTITY_VAL_LOCAL()", "SELECT 1 FROM sysibm.sysdummy1", "DB2", "QDB2"), // db2
	sqlserver(null, "SELECT @@IDENTITY", "SELECT 1", "Microsoft SQL Server"), // sqlserver
	mysql(null, "SELECT LAST_INSERT_ID()", "SELECT 1", "MySQL"), // mysql
	hsql("SELECT NEXT VALUE FOR {} FROM system_sequences", "CALL IDENTITY()", "SELECT 1 FROM dual",
			"HSQL Database Engine"), // hsql
	derby(null, "VALUES IDENTITY_VAL_LOCAL()", "SELECT count(*) FROM sys.systables WHERE 1=0", "Apache Derby"), // derby
	postgresql("SELECT NEXTVAL ('{}')", "SELECT CURRVAL ('{}')", "SELECT 1", "PostgreSQL"), // postgresql
	sybase(null, "SELECT @@IDENTITY", "SELECT 1", "Sybase SQL Server", "Adaptive Server Enterprise"), // sybase
	h2("CALL NEXT VALUE FOR {}", "CALL IDENTITY()", "SELECT 1 FROM dual", "H2"), // h2db
	sapdb("SELECT {}.NEXTVAL FROM dual", "SELECT {}.CURRVAL FROM dual", "SELECT 1 FROM dual", "SAP DB"), // sapdb
	informix("SELECT {}.NEXTVAL FROM systables WHERE tabid=1", "SELECT DBINFO('{}') FROM systables WHERE tabid=1",
			"SELECT count(*) FROM systables WHERE 1 = 0", "Informix Dynamic Server");// informix

	private final String[] products;

	private final String sequence;

	private final String identity;

	private final String validation;

	private DBType(String seq, String iden, String valid, String... aProducts) {
		this.sequence = seq;
		this.identity = iden;
		this.validation = valid;
		this.products = aProducts;
	}

	/**
	 * 数据库连接有效性查询SQL.
	 *
	 * @return the validation query.
	 */
	public String validateSQL() {
		return validation;
	}

	/**
	 * Gets the identity sql.
	 *
	 * @param identityParam
	 *            {@link #oracle} , {@link #postgresql} link #sapdb} 是用 sequence
	 *            模拟的自增.使用 Sequence Name 作为参数. 其他使用自增类型的忽略该参数.
	 *
	 * @return the identity sql.
	 */
	public String identitySQL(String identityParam) {
		return Objutil.format(Objutil.notNull(identity, "unsupported identity."), identityParam);
	}

	/**
	 * 根据 sequence Name 得到 Squence SQL
	 *
	 * @param sequenceName
	 *            the sequence name
	 *
	 * @return the squence sql.
	 */
	public String sequenceSQL(String sequenceName) {
		return Objutil.format(sequence, sequenceName);
	}

	/**
	 * 取得数据库类型
	 *
	 * @param ds
	 *            the DataSource
	 *
	 * @return 数据库类型.
	 */

	public static DBType getDBType(DataSource ds) {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			String product = conn.getMetaData().getDatabaseProductName();
			for (DBType type : DBType.values()) {
				for (String name : type.products) {
					if (product.startsWith(name))
						return type;
				}
			}
			throw new EnumConstantNotPresentException(DBType.class, product);
		} catch (SQLException e) {
			throw new XutilRuntimeException(e);
		} finally {
			Util.closeQuietly(conn);
		}
	}
}
