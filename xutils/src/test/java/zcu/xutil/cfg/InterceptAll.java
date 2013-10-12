package zcu.xutil.cfg;

import zcu.xutil.Logger;
import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.MethodInvocation;


public class InterceptAll implements Interceptor {
	public Object invoke(MethodInvocation arg0) throws Throwable {
		Logger.LOG.info("InterceptAll: " + arg0.getThis() + " method: "+arg0.getMethod());
		Logger.LOG.info("InterceptAll: " + " method declareClass: "+arg0.getMethod().getDeclaringClass());
		return arg0.proceed();
	}

	public boolean checks(Class t) {
		// TODO Auto-generated method stub
		throw new RuntimeException();
	}
}
