package zcu.xutil.web;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ContainerInitializer implements ServletContainerInitializer{
	@Override
	public void onStartup(Set<Class<?>> set, ServletContext sc) throws ServletException {
		sc.addListener(ContextListener.class);
	}
}
