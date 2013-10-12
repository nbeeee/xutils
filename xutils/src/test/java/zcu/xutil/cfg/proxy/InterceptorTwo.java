package zcu.xutil.cfg.proxy;

import zcu.xutil.utils.Interceptor;
import zcu.xutil.utils.MethodInvocation;

public class InterceptorTwo implements Interceptor{

	InterceptorOne interceptorOne;
	public volatile int  count=0;
	InterceptorTwo(InterceptorOne interceptorOne){
		this.interceptorOne=interceptorOne;
		}
	public Object invoke(MethodInvocation invocation) throws Throwable {
		count=interceptorOne.count+count+1;
		return invocation.proceed();
	}
	public boolean checks(Class t) {
		throw new RuntimeException();
	}

}
