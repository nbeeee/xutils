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
package zcu.xutil.msg.impl;

import java.util.List;

import zcu.xutil.Constants;
import zcu.xutil.Objutil;

public abstract class BrokerAgent {
	private static final List<String> testServices;
	static {
		String s = Objutil.systring(Constants.XUTILS_MSG_TEST);
		if (s == null)
			testServices = null;
		else {
			List<String> list = Objutil.split(s.trim(), ',');
			int len = list.size();
			while (--len >= 0)
				list.set(len, list.get(len).trim().intern());
			testServices = list;
		}
	}
	static boolean isTestMode(String services) {
		return testServices == null ? false : (testServices.isEmpty() || testServices.indexOf(services) >= 0 );
	}
	protected ServiceObject getLocalService(String canonicalName){
		return null;
	}

	protected abstract Object sendToRemote(Event canonical, int timeoutMillis,boolean testmode) throws Throwable;

}