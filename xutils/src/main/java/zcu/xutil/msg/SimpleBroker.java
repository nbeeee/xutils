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

import java.rmi.Remote;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 *
 */

public interface SimpleBroker {
	/**
	 * 创建服务接口调用代理(同步调用).
	 *
	 * @param iface
	 *            服务接口.
	 * @param timeoutMillis
	 *            调用通讯等待超时毫秒数,0 为缺省时间.
	 *
	 * @return 服务接口实现
	 */
	<T extends Remote> T create(Class<T> iface,int timeoutMillis);

	/**
	 * 创建{@link GroupService} 调用代理(异步调用).
	 *
	 * @param serviceName
	 *            服务名称 . {@link GroupService} 在服务端的部署名.
	 * @param sendprefer
	 *            首选发送,true 先发送,如果成功则不保存,失败则保存重发; false 先保存.
	 * @param expireMinutes
	 * 			     到期分钟数，消息保存在数据库的最大时间，0 为缺省时间60分钟
	 * @return {@link GroupService}
	 */
	GroupService create(String serviceName, boolean sendprefer,int expireMinutes);
}
