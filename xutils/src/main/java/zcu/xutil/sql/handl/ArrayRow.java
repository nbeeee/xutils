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
package zcu.xutil.sql.handl;

import java.sql.ResultSet;
import java.sql.SQLException;

import zcu.xutil.Objutil;
import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.SQLType;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class ArrayRow extends ResultHandler<Object[]> {
	private final Class<?>[] types;

	public ArrayRow(Class... returnTypes) {
		this.types = Objutil.isEmpty(returnTypes) ? null : returnTypes;
	}

	@Override
	public Object[] handleRow(ResultSet rs) throws SQLException {
		final boolean raw = types == null;
		int len = raw ? rs.getMetaData().getColumnCount() : types.length;
		Object[] result = new Object[len];
		while (--len >= 0)
			result[len] = raw ? rs.getObject(len + 1) : SQLType.getValue(rs, len + 1, types[len]);
		return result;
	}
}
