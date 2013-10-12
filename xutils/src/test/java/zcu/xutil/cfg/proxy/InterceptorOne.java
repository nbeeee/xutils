package zcu.xutil.cfg.proxy;


import zcu.xutil.txm.Transactional;
import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.MethodInvocation;

public class InterceptorOne implements Interceptor{
	public volatile int  count=0;
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if(invocation.getMethod().getAnnotation(Transactional.class)!=null)
			count++;
		return invocation.proceed();
	}
	public boolean checks(Class t) {
		throw new RuntimeException();
	}

}
