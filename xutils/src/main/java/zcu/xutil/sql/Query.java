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

import zcu.xutil.utils.Util;

/**
 * 数据库操作类.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class Query extends DBTool {
	private final DataSource ds;

	public Query(DataSource datasource) {
		this.ds = datasource;
	}

	@Override
	Connection connect() throws SQLException {
		return ds.getConnection();
	}

	public DataSource getDataSource() {
		return ds;
	}

	public <T> T execute(Callback<Sesion,T> callback) throws SQLException {
		Sesion session = new Sesion();
		try {
			return callback.call(session);
		} finally {
			Util.closeQuietly(session.connection);
		}
	}

	public final class Sesion extends DBTool {
		Connection connection;

		@Override
		public Connection connect() throws SQLException {
			if (connection == null)
				connection = Query.this.connect();
			return new CloseSuppressing(connection);
		}
	}
}
