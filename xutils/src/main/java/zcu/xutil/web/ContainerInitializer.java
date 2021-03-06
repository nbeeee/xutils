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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;


public class ContainerInitializer implements ServletContainerInitializer {
	@Override
	public void onStartup(Set<Class<?>> set, ServletContext sc) throws ServletException {
		if (ContextListener.getConfig(sc) != null){
			sc.addListener(ContextListener.class);
			Map<String,? extends FilterRegistration> map = sc.getFilterRegistrations();
			for(FilterRegistration fr: map.values()){
				if(fr.getClassName().equals(FilterProxy.class.getName()))
					return;
			}
			sc.addFilter(FilterProxy.class.getName(), FilterProxy.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		}
	}
}
