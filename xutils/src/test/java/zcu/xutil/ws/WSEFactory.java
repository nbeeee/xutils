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

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.ResourceLoader;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.server.EndpointFactory;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.util.xml.XmlUtil;

import org.xml.sax.EntityResolver;

import zcu.xutil.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class WSEFactory{
	private Class<?> implClass;
	private Object target;
	private Container container;
	private WSBinding binding;
	private SDDocumentSource primaryWsdl;
	private List<SDDocumentSource> metadata;

	private QName serviceName;
	private QName portName;
	private Object primaryWsdlResource;
	private Collection<?> metadataResource;
	private BindingID bindingID;
	private List<WebServiceFeature> features;

	/**
	 * Technically speaking, handlers belong to {@link WSBinding} and as such it
	 * should be configured there, but it's just more convenient to let people
	 * do so at this object, because often people use a stock binding ID
	 * constant instead of a configured {@link WSBinding} bean.
	 */
	private List<Handler> handlers;
	/**
	 * Entity resolver to use for resolving XML resources.
	 *
	 * @see #setResolver(org.xml.sax.EntityResolver)
	 */
	private EntityResolver resolver;

	public void setImplClass(Class implClazz) {
		this.implClass = implClazz;
	}

	public void setTarget(Object impl) {
		this.target = impl;
		if (implClass == null)
			implClass = impl.getClass();
	}

	public void setContainer(Container c) {
		this.container = c;
	}

	/**
	 * Sets the service name of this endpoint. Defaults to the name inferred
	 * from the impl attribute.
	 */
	public void setServiceName(QName _serviceName) {
		this.serviceName = _serviceName;
	}

	/**
	 * Sets the port name of this endpoint. Defaults to the name inferred from
	 * the impl attribute.
	 */
	public void setPortName(QName _portName) {
		this.portName = _portName;
	}

	/**
	 * Sets the binding ID, such as
	 * <tt>{@value javax.xml.ws.soap.SOAPBinding#SOAP11HTTP_BINDING}</tt> or
	 * <tt>{@value javax.xml.ws.soap.SOAPBinding#SOAP12HTTP_BINDING}</tt>.
	 *
	 * <p>
	 * If none is specified, {@link BindingType} annotation on SEI is consulted.
	 * If that fails, {@link SOAPBinding#SOAP11HTTP_BINDING}.
	 *
	 * @see HTTPBinding#HTTP_BINDING
	 */
	public void setBindingID(String id) {
		this.bindingID = BindingID.parse(id);
	}

	/**
	 * {@link WebServiceFeature}s that are activated in this endpoint.
	 */
	public void setFeatures(List<WebServiceFeature> _features) {
		this.features = _features;
	}

	/**
	 * {@link Handler}s for this endpoint. Note that the order is significant.
	 *
	 * <p>
	 * If there's just one handler and that handler is declared elsewhere, you
	 * can use this as a nested attribute like <tt>handlers="#myHandler"</tt>.
	 * Or otherwise a nested &lt;bean&gt; or &lt;ref&gt; tag can be used to
	 * specify multiple handlers.
	 */
	public void setHandlers(List<Handler> _handlers) {
		this.handlers = _handlers;
	}

	/**
	 * Optional WSDL for this endpoint.
	 * <p>
	 * Defaults to the WSDL discovered in <tt>META-INF/wsdl</tt>,
	 * <p>
	 * It can be either {@link String}, {@link URL}, or {@link SDDocumentSource}.
	 * <p>
	 * If <code>primaryWsdl</code> is a <code>String</code>, ServletContext (if
	 * available) and {@link ClassLoader} are searched for this path, then
	 * failing that, it's treated as an absolute {@link URL}.
	 */
	public void setPrimaryWsdl(Object _primaryWsdlResource) {
		this.primaryWsdlResource = _primaryWsdlResource;
	}

	/**
	 * Optional metadata for this endpoint.
	 * <p>
	 * The collection can contain {@link String}, {@link URL}, or
	 * {@link SDDocumentSource} elements.
	 * <p>
	 * If element is a <code>String</code>, ServletContext (if available) and
	 * {@link ClassLoader} are searched for this path, then failing that, it's
	 * treated as an absolute {@link URL}.
	 */
	public void setMetadata(Collection<Object> metadataResources) {
		this.metadataResource = metadataResources;
	}

	/**
	 * Sets the {@link EntityResolver} to be used for resolving schemas/WSDLs
	 * that are referenced. Optional.
	 *
	 * <p>
	 * If omitted, the default catalog resolver is created by looking at
	 * <tt>/WEB-INF/jax-ws-catalog.xml</tt> (if we run as a servlet) or
	 * <tt>/META-INF/jax-ws-catalog.xml</tt> (otherwise.)
	 */
	public void setResolver(EntityResolver _resolver) {
		this.resolver = _resolver;
	}

	private SDDocumentSource resolveSDDocumentSource(Object resource) {
		if (resource instanceof SDDocumentSource)
			return (SDDocumentSource) resource;
		URL url = null;
		if (resource instanceof URL)
			url = (URL) resource;
		else if (resource instanceof String)
			url = resolveURL((String) resource);
		if (url != null)
			return SDDocumentSource.create(url);
		throw new ServerRtException("cannot.load.wsdl", resource);

	}

	private URL resolveURL(String resource) {
		ResourceLoader loader = container.getSPI(ResourceLoader.class);
		if (loader != null) {
			try {
				return loader.getResource(resource);
			} catch (MalformedURLException e) {
				Logger.LOG.info("invalid url: {}", e, resource);
			}
		}
		return null;
	}

	public WSEndpoint getObject() {
		if (binding == null) {
			if (bindingID == null)
				bindingID = BindingID.parse(implClass);
			if (features == null || features.isEmpty())
				binding = BindingImpl.create(bindingID);
			else
				binding = BindingImpl.create(bindingID, features.toArray(new WebServiceFeature[features.size()]));
			if (handlers != null) {
				List<Handler> chain = binding.getHandlerChain();
				chain.addAll(handlers);
				binding.setHandlerChain(chain);
			}
			if (primaryWsdlResource == null) {
				EndpointFactory.verifyImplementorClass(implClass);
				primaryWsdlResource = EndpointFactory.getWsdlLocation(implClass);
			}
			if (primaryWsdlResource != null)
				primaryWsdl = resolveSDDocumentSource(primaryWsdlResource);
			if (metadataResource != null && metadataResource.size() > 0) {
				metadata = new ArrayList<SDDocumentSource>(metadataResource.size());
				for (Object resource : metadataResource)
					metadata.add(resolveSDDocumentSource(resource));
			}
			if (resolver == null) {
				URL url = resolveURL("jax-ws-catalog.xml");
				if (url != null)
					resolver = XmlUtil.createEntityResolver(url);
			}
		}
		Invoker invoker = InstanceResolver.createSingleton(target).createInvoker();
		return WSEndpoint.create(implClass, false, invoker, serviceName, portName, container, binding, primaryWsdl,
				metadata, resolver, true);
	}
}
