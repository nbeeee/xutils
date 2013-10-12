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

import java.util.Collection;
import org.jgroups.Address;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface Server {

	Address getAddress();

	Collection<String> getServiceNames();

	/**
	 * 检查是否是有效的服务器
	 *
	 *
	 */
	boolean isValid();

	/**
	 * 同步调用服务.用于服务器测试.
	 *
	 * @param name
	 *            服务部署名
	 * @param value
	 *            方法 签名{@link zcu.xutil.utils.Util#signature(String, Class...)}
	 *
	 * @param timeoutMillis
	 *            通讯等待超时毫秒数,0 为缺省时间.
	 *
	 * @param params
	 *            方法参数
	 */

	Object call(String name, String value, int timeoutMillis, Object... params) throws Throwable;
}