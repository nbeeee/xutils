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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import zcu.xutil.utils.Accessor;


/**
 * The Class ResultHandler.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class ResultHandler<T> implements Handler<T>{
	private boolean multiRowsCheck;

	public abstract T handleRow(ResultSet rs) throws SQLException;
	@Override
	public void setOptions(Statement st) throws SQLException{
		//nothing
	}

	public ResultHandler<T> multiRowsCheck(){
		this.multiRowsCheck=true;
		return this;
	}
	/**
	 *
	 * @throws ManyResultException
	 * 							结果集多条且设置了{@link #multiRowsCheck()}时抛出
	 * @throws NullResultException
	 *							只有{@link zcu.xutil.sql.handl.FirstField} 才出现.
	 * @return 结果集为空返回null.
	 */
	@Override
	public T handle(ResultSet rs) throws SQLException,ManyResultException,NullResultException {
		if(rs.next()){
			T result=handleRow(rs);
			if(multiRowsCheck && !rs.isLast())
				throw new ManyResultException(result);
			if(result==null)
				throw new NullResultException();
			return result;
		}
		return null;
	}
	/**
	 * @see #list(int, int, int)
	 */
	public final Handler<List<T>> list(){
		return list(0,0,0);
	}
	/**
	 * @see #list(int, int, int)
	 */
	public final Handler<List<T>> list(int fetchsize,int maxrows){
		return list(fetchsize,maxrows,0);
	}

	/**
	 * 多行处理句柄
	 *
	 * @param fetchsize the fetchsize
	 * @param maxrows the maxrows
	 * @param timeoutSeconds query timeout seconds
	 *
	 * @return the result handler< list< t>>
	 */
	public final Handler<List<T>> list(final int fetchsize,final int maxrows,final int timeoutSeconds){
		return new Handler<List<T>>(){
			@Override
			public List<T> handle(ResultSet rs) throws SQLException {
				List<T> list = new ArrayList<T>();
				while(rs.next())
					list.add(ResultHandler.this.handleRow(rs));
				return list.isEmpty() ? Collections.<T>emptyList() : list;
			}
			@Override
			public void setOptions(Statement st) throws SQLException {
				if(fetchsize>0)
					st.setFetchSize(fetchsize);
				if(maxrows>0)
					st.setMaxRows(maxrows);
				if(timeoutSeconds>0)
					st.setQueryTimeout(timeoutSeconds);
			}
		};
	}
	protected static Map<String,Accessor> getAllAccessor(Class clazz){
		return DBTool.CHK.getAllAccessor(clazz);
	}
}
