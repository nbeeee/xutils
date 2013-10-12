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
package zcu.xutil.msg;

import java.util.List;

import org.jgroups.Address;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface MsgListener {


	/**
	 * On event.
	 *
	 * @param from 事件发送者地址.
	 * @param name the name
	 * @param value the value
	 * @param params the params
	 *
	 */
	void onEvent(Address from,String name,String value,List<Object> params);

}
