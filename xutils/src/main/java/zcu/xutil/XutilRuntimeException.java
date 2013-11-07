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
package zcu.xutil;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class XutilRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 6143790381884307183L;

	public XutilRuntimeException(Throwable e) {
		this(null,e);
	}

	public XutilRuntimeException(String message, Throwable e) {
		super(message, e);
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
	@Override
	public String getMessage() {
		Throwable nested =getCause();
		if(nested==null)
			return super.getMessage();
		return super.getMessage()+ "; nested exception: "+ nested.toString();
	}
	@SuppressWarnings("unchecked")
	public <E extends Throwable> RuntimeException rethrowIf(Class<E> exceptionType) throws E {
		Throwable e = getCause();
		if (exceptionType.isInstance(e))
			throw (E) e;
		if (e instanceof Error)
			throw (Error) e;
		throw e instanceof RuntimeException ? (RuntimeException) e : this;
	}
}
