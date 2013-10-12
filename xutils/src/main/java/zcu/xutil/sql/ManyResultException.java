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



/**
 * 单条记录查询处理时,如果有多条记录时抛出该异常.
 * @see ResultHandler#handle(java.sql.ResultSet)
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
@SuppressWarnings("serial")
public class ManyResultException extends RuntimeException{
	/** The result. */
	private final Object result;

	/**
	 * Instantiates a new unique result exception.
	 *
	 * @param object the first result
	 */
	public ManyResultException(Object object){
		this.result=object;
	}


	public Object getFirstResult(){
		return result;
	}
}
