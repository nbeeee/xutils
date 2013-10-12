package zcu.xutil.web;

import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import zcu.xutil.Logger;
@HandlesTypes({Action.class,Filter.class})
public class XuilsServletContainerInitializer implements ServletContainerInitializer{
static{
	Logger.LOG.info("Class XuilsServletContainerInitializer}");
}
	@Override
	public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
		Logger.LOG.info("Class {} {}",arg0,arg0.size());
		for(Class c : arg0){
			Logger.LOG.info("Class {}",c);
		}
		
	}
}
