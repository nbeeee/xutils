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
 * 主键产生器.三种方式产生主键: auto,identity,sequence.
 * auto: 使用自增主键字段,通过设置Statement.RETURN_GENERATED_KEYS返回主键值.
 * identity: 执行插入语句后, 通过在同一会话中执行查询得到主键值.
 * sequence: 执行插入语句前, 执行查询得到主键.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class ID {
	public static final int AUTO=0,IDENTITY=1,SEQUENCE=2;
	private final String name;
	private int  policy;
	private String genSQL;

	/**
	 * 自动方式产生主键.
	 *
	 * @param propertyName 主键属性
	 */
	public ID(String propertyName){
		this.name = propertyName;
	}


	public String getName() {
		return name;
	}
	public String getGenSQL() {
		return genSQL;
	}
	public int getPolicy(){
		return policy;
	}
	/**
	 * sequence 方式产生主键.
	 *
	 * @param sequenceSQL 产生主键的SQL
	 * @return this Object
	 */
	public ID seq(String sequenceSQL){
		genSQL=sequenceSQL;
		policy=SEQUENCE;
		return this;
	}
	/**
	 * identity 方式产生主键.
	 *
	 * @param identitySQL 产生主键的SQL
	 * @return this Object
	 */
	public ID ide(String identitySQL){
		genSQL=identitySQL;
		policy=IDENTITY;
		return this;
	}
}
