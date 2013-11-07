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
package zcu.xutil.cfg;

import java.util.Collection;
import java.util.List;

import zcu.xutil.Disposable;


/**
 * 容器对外接口
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface Context extends Disposable{

	/**
	 * Gets the bean.
	 *
	 * @param name the name
	 *
	 * @return the bean
	 *
	 * @throws NoneBeanException the none bean exception
	 */
	Object getBean(String name) throws NoneBeanException;

	/**
	 * Gets the provider.
	 *
	 * @param name the name
	 *
	 * @return the provider 如果不存在,则返回null
	 */
	Provider getProvider(String name);

	/**
	 * Gets the providers.
	 *
	 * @param type the type
	 *
	 * @return the providers
	 */
	List<NProvider> getProviders(Class<?> type);

	Collection<NProvider> listMe();
}
