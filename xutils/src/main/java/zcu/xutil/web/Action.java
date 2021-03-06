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
package zcu.xutil.web;

import java.io.IOException;
import javax.servlet.ServletException;


/**
 * 
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 * @see Dispatcher
 * 
 */
public interface Action {

	/**
	 * Execute.
	 *
	 * @param ac
	 *            the action context
	 *
	 * @return {@link View} 返回null则框架不再处理。
	 *
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	View execute(ActionContext ac) throws ServletException, IOException;
}
