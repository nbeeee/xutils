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

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Context;
import zcu.xutil.utils.AccessField;
import zcu.xutil.utils.Accessor;
import zcu.xutil.utils.Checker;
import zcu.xutil.utils.Convertor;
import zcu.xutil.utils.LRUCache;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.XResource;
import static zcu.xutil.Constants.*;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public final class Webutil implements Checker<Accessor> {

	/**
	 * Standard Servlet 2.3+ spec request attributes for include URI and paths.
	 * <p>
	 * If included via a RequestDispatcher, the current resource will see the
	 * originating request. Its own URI and paths are exposed as request
	 * attributes.
	 * 
	 * forward 则相反.
	 * javax.servlet.[forward|include].[request_uri|context_path|servlet_path
	 * |path_info|query_string]
	 * 
	 * Standard Servlet 2.4+ spec request attributes for forward URI and paths.
	 * <p>
	 * If forwarded to via a RequestDispatcher, the current resource will see
	 * its own URI and paths. The originating URI and paths are exposed as
	 * request attributes.
	 */
	public static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";

	public static String getFilename(HttpServletRequest request) {
		String uri = (String) request.getAttribute(INCLUDE_REQUEST_URI);
		if (uri == null)
			uri = request.getRequestURI();
		int begin = uri.lastIndexOf('/') + 1;
		int end = uri.indexOf(';', begin);// for ;jsessionid=xxxxx?p=v
		return end < 0 ? uri.substring(begin) : uri.substring(begin, end);
	}

	public static void preventCaching(HttpServletResponse response) {
		// Set to expire far in the past.  
		response.setDateHeader("Expires", 0);
		// Set standard HTTP/1.1 no-cache headers.  
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		// Set IE extended HTTP/1.1 no-cache headers (use addHeader).  
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");  
		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");
	}

	/**
	 * Apply the given cache seconds and generate respective HTTP headers.
	 * <p>
	 * That is, allow caching for the given number of seconds in the case of a
	 * positive value, prevent caching if given a 0 value, else do nothing (i.e.
	 * leave caching to the client).
	 * 
	 * @param response
	 *            the current HTTP response
	 * @param seconds
	 *            the (positive) number of seconds into the future that the
	 *            response should be cacheable for; 0 to prevent caching; and a
	 *            negative value to leave caching to the client.
	 * @param mustRevalidate
	 *            whether the client should revalidate the resource (typically
	 *            only necessary for controllers with last-modified support)
	 */
	public static void applyCacheSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (seconds > 0) {
			// HTTP 1.0 header --- useExpiresHeader
			response.setDateHeader("Expires", System.currentTimeMillis() + seconds * 1000L);
			// HTTP 1.1 header --- useCacheControlHeader
			response.setHeader("Cache-Control", (mustRevalidate ? "must-revalidate,max-age=" : "max-age=") + seconds);
		} else if (seconds == 0)
			preventCaching(response);
	}

	static final String XUTILS_WEBAPP_CONTEXT = "xutils.webapp.context";

	public static Context getAppContext(ServletContext servletContext) {
		Context ret = (Context) servletContext.getAttribute(XUTILS_WEBAPP_CONTEXT);
		return ret == null ? CFG.root() : ret;
	}

	public static Context getAppContext(WebServiceContext webServiceContext) {
		ServletContext sc = (ServletContext) webServiceContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
		return sc == null ? CFG.root() : getAppContext(sc);
	}

	private static volatile Webutil injector;

	public static Map<String, String> inject(HttpServletRequest request, Object obj) {
		if (injector == null)
			synchronized (Webutil.class) {
				if (injector == null)
					injector = new Webutil();
			}
		return injector.doInject(request, obj);
	}

	@Validator
	private final Validator defaults;
	private final Convertor convertor;
	private final ResourceBundle bundle;
	private final Map<String, Pattern> patterns;
	private final LRUCache<Class, Map<String, Accessor>> accessors;

	private Webutil() {
		convertor = new Convertor();
		patterns = Util.lruMap(Objutil.systring(XUTILS_WEB_PATTERNS_CACHE, 95), null);
		accessors = new LRUCache<Class, Map<String, Accessor>>(Objutil.systring(XUTILS_WEB_ACTIONS_CACHE, 95), null);
		bundle = ResourceBundle.getBundle(Objutil.systring(XUTILS_WEB_BUNDLE_NAME, XResource.class.getName()));
		try {
			defaults = Webutil.class.getDeclaredField("defaults").getAnnotation(Validator.class);
		} catch (NoSuchFieldException e) {
			throw new XutilRuntimeException(e);
		}
	}

	private synchronized Pattern getPattern(String pattern) {
		Pattern result = patterns.get(pattern);
		if (result == null)
			patterns.put(pattern, result = Pattern.compile(pattern));
		return result;
	}

	private Map<String, Accessor> getAllAccessor(Class clazz) {
		Map<String, Accessor> result = accessors.get(clazz);
		if (result != null)
			return result;
		result = AccessField.build(clazz, this, true);
		return Objutil.ifNull(accessors.putIfAbsent(clazz, result), result);
	}

	@Override
	public boolean checks(Accessor accessor) {
		return (accessor.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0;
	}

	private String localMsg(String value) {
		try {
			return bundle.getString(value);
		} catch (MissingResourceException e) {
			Logger.LOG.info("missing resource.", e);
			return value;
		}
	}

	private Map<String, String> doInject(HttpServletRequest request, Object action) {
		Map<String, String> errors = new HashMap<String, String>();
		outer: for (Accessor accessor : getAllAccessor(action.getClass()).values()) {
			Validator vp = Objutil.ifNull(accessor.getAnnotation(Validator.class), defaults);
			String array[], name = vp.name().length() == 0 ? accessor.getName() : vp.name();
			Object obj = request.getAttribute(name);
			if (obj == null) {
				if ((array = request.getParameterValues(name)) == null) {
					if (vp.required())
						errors.put(name, localMsg(vp.message()));
					continue;
				}
			} else if (obj instanceof String[])
				array = (String[]) obj;
			else {
				accessor.setValue(action, obj);
				continue;
			}
			final Class type;
			if (array.length == 0) {
				if ((type = accessor.getType().getComponentType()) != null)
					accessor.setValue(action, Array.newInstance(type, 0));
				continue;
			}
			if (vp.value().length() > 0) {
				Pattern patter = getPattern(vp.value());
				for (int i = array.length - 1; i >= 0; i--)
					if (!patter.matcher(array[i]).matches()) {
						errors.put(name, localMsg(vp.message()));
						continue outer;
					}
			}
			if ((type = accessor.getType()) == String.class)
				accessor.setValue(action, array[0]);
			else if (type == String[].class)
				accessor.setValue(action, array);
			else if (array[0].length() > 0) {
				obj = (array.length == 1 || type.getComponentType() == null) ? array[0] : array;
				try {
					accessor.setValue(action, convertor.convert(obj, type));
				} catch (RuntimeException e) {
					Logger.LOG.warn("field {} , invalid value: {}", e, name, obj);
					throw e;
				}
			}
		}
		return errors.isEmpty() ? Collections.<String, String> emptyMap() : errors;
	}
}
