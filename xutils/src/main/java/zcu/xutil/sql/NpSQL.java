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

import java.util.ArrayList;
import java.util.Map;

import zcu.xutil.Objutil;
import zcu.xutil.utils.Accessor;

/**
 * 命名参数SQL: 用 :name 代替 ? 的SQL. 如果该语句涉及到主键产生,则指定{@link ID}
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class NpSQL {
	private static final byte[] INSERT = { 'I', 'N', 'S', 'E', 'R', 'T' };

	private final String sql;
	private final String[] paramNames;
	private final boolean insert;
	private final ID ID;

	public NpSQL(String namedParamSQL) {
		this(null, namedParamSQL);
	}

	public NpSQL(ID id, String namedParamSQL) {
		char[] chars = namedParamSQL.trim().toCharArray();
		int j, length = chars.length;
		boolean isInsert = length > 5;
		for (int i = 5; isInsert && i >= 0; i--) {
			j = chars[i] - INSERT[i];
			isInsert = j == 0 || j == ('a' - 'A');
		}
		this.insert = isInsert;
		this.ID = isInsert ? id : null;
		StringBuilder parsedQuery = new StringBuilder(length);
		ArrayList<String> list = new ArrayList<String>();
		boolean inSingleQuote = false, inDoubleQuote = false;
		for (int i = 0; i < length; i++) {
			char c = chars[i];
			if (inSingleQuote) {
				inSingleQuote = c != '\'';
			} else if (inDoubleQuote) {
				inDoubleQuote = c != '"';
			} else if (c == ':') {
				j = i + 1;
				if (j < length && Character.isJavaIdentifierStart(chars[j])) {
					do
						j++;
					while (j < length && Character.isJavaIdentifierPart(chars[j]));
					list.add(namedParamSQL.substring(i + 1, j));
					c = '?'; // replace the parameter with a question mark
					i = j - 1; // skip past the end of the parameter.
				}
			} else {
				inSingleQuote = c == '\'';
				inDoubleQuote = c == '"';
			}
			parsedQuery.append(c);
		}
		this.sql = parsedQuery.toString();
		this.paramNames = list.toArray(new String[list.size()]);
	}

	public boolean isInsert() {
		return insert;
	}

	public String getSql() {
		return sql;
	}

	public ID getID() {
		return ID;
	}

	public Object[] fromBean(Object bean) {
		int len = paramNames.length;
		Object[] params = new Object[len];
		Map<String, Accessor> accessors = DBTool.getAllAccessor(bean.getClass());
		String s;
		while (--len >= 0) {
			Accessor accesssor = Objutil.notNull(accessors.get(s = paramNames[len]), s);
			if ((params[len] = accesssor.getValue(bean)) == null)
				params[len] = accesssor.getType();
		}
		return params;
	}

	public Object[] fromMap(Map<String, Object> map) {
		int len = paramNames.length;
		Object[] params = new Object[len];
		String s;
		while (--len >= 0) {
			// may be null value in Map
			Objutil.validate(map.containsKey(s = paramNames[len]), "not found: {}", s);
			params[len] = map.get(s);
		}
		return params;
	}
}
