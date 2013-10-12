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

import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.SQLType;

/**
*
* @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
*/
public final class FirstField extends ResultHandler {
	private static final ResultHandler STR = new FirstField(String.class);
	private static final ResultHandler INT = new FirstField(Integer.class);
	private static final ResultHandler LONG = new FirstField(Long.class);

	@SuppressWarnings("unchecked")
	public static <T> ResultHandler<T> get(Class<T> clazz){
		if(clazz==String.class)
			return STR;
		if(clazz==Integer.TYPE || clazz==Integer.class)
			return INT;
		if(clazz==Long.TYPE || clazz==Long.class)
			return LONG;
		return new FirstField(clazz);
	}

	private final Class<?> type;

	private FirstField(Class<?> clazz) {
		this.type = clazz;
	}

	@Override
	public Object handleRow(ResultSet rs) throws SQLException{
		return SQLType.getValue(rs, 1,type);
	}
}
