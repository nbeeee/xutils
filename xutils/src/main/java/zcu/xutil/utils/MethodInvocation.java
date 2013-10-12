package zcu.xutil.utils;

import java.lang.reflect.Method;

public interface MethodInvocation {
	Method getMethod();

	Object[] getArguments();

	Object proceed() throws Throwable;

	Object getThis();
}