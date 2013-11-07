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
 * 节点变化通知接口.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface Notification {

	/**
	 * 当节点机加入或离开时调用.
	 *
	 * @param lefts 离开的节点IP地址
	 * @param joins 加入的节点IP地址
	 */
	void onViewChange(List<Address> lefts,List<Address> joins);
}
