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

import zcu.xutil.utils.Checker;
import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.MethodInvocation;
import zcu.xutil.utils.Matcher;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public abstract class Plugin implements Interceptor {
	private static final Matcher<Class> DEFAULT = Matcher.subOf(Action.class);
	private Checker<? super Class> matcher = DEFAULT;

	public final void setMatcher(Checker<? super Class> aMatcher) {
		this.matcher = DEFAULT.and(aMatcher);
	}
	@Override
	public final boolean checks(Class t) {
		return matcher.checks(t);
	}

	protected abstract View intercept(Invocation invocation) throws ServletException, IOException;

	@Override
	public final Object invoke(MethodInvocation mi) throws Throwable {
		Object[] args = mi.getArguments();
		if (args.length == 1 && args[0] instanceof WebContext && "execute".equals(mi.getMethod().getName()))
			return intercept(((WebContext) args[0]).setMethodInvocation(mi));
		return mi.proceed();
	}
}
