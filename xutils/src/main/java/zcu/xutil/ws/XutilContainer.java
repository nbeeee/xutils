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
package zcu.xutil.ws;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.server.BoundEndpoint;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.transport.http.servlet.ServletModule;

import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceException;

import zcu.xutil.Objutil;
import com.sun.xml.ws.api.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class XutilContainer extends Container {
	final ServletContext servletContext;
	private final ServletModule module = new ServletModule() {
		private final List<BoundEndpoint> endpoints = new ArrayList<BoundEndpoint>();

		@Override
		public @NotNull
		List<BoundEndpoint> getBoundEndpoints() {
			return endpoints;
		}

		@Override
		public @NotNull
		String getContextPath() {
			throw new WebServiceException("doesn't support getContextPath()");
		}
	};
	
	private final ResourceLoader loader = new ResourceLoader() {
		@Override
		public URL getResource(String resource) throws MalformedURLException {
			if (servletContext == null)
				return Objutil.contextLoader().getResource("META-INF/" + resource);
			return servletContext.getResource("/WEB-INF/" + resource);
		}
	};
	
	public XutilContainer(ServletContext sc) {
		servletContext = sc;
	}

	@Override
	public <T> T getSPI(Class<T> spiType) {
		if (spiType == ServletContext.class)
			return spiType.cast(servletContext);
		if (spiType.isAssignableFrom(ServletModule.class))
			return spiType.cast(module);
		if (spiType == ResourceLoader.class)
			return spiType.cast(loader);
		return null;
	}
}
