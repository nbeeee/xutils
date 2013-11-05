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
package zcu.xutil.cfg;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import zcu.xutil.Constants;
import zcu.xutil.Logger;
import zcu.xutil.Replace;
import zcu.xutil.XutilRuntimeException;
import static zcu.xutil.Objutil.*;

/**
 * 
 * 容器组件绑定
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class DefaultBinder implements Binder, Replace {
	private final ClassLoader loader;
	private final CtxImpl context;
	private Map<String, Object> environ;
	private Map<String, String> placeholder;

	public DefaultBinder(String name, Context parent) {
		this(name, parent, null);
	}

	public DefaultBinder(String name, Context parent, ClassLoader cl) {
		context = new CtxImpl(name, parent);
		loader = cl == null ? contextLoader() : cl;
	}

	@Override
	public LifeCtrl put(boolean cache, String name, Provider provider) {
		return context.put(cache, name, provider);
	}

	@Override
	public LifeCtrl put(boolean cache, String name, Provider provider, Class<?> iface, String interceptors) {
		if(interceptors != null || iface != null)
			provider =  new ProxyDecorator(iface, provider, interceptors);
		return put(cache, name, provider);
	}

	public void batch(String configs, URL ctx) {
		List<String> list = split(configs, ',');
		for (int i = 0, len = list.size(); i < len; i++) {
			String s = list.get(i).trim();
			if (s.startsWith("path:"))
				bind(notNull(loader.getResource(s.substring("path:".length())), "{} not found.", s));
			else if (loader.getResource(s.replace('.', '/').concat(".class")) == null)
				try {
					bind(new URL(ctx == null ? new File(systring(Constants.XUTILS_HOME)).toURI().toURL() : ctx, s));
				} catch (MalformedURLException e) {
					throw new XutilRuntimeException(e);
				}
			else {
				Logger.LOG.info("{} binding begin......", s);
				try {
					((Config) loader.loadClass(s).newInstance()).config(this);
				} catch (Exception e) {
					throw rethrow(e);
				}
				Logger.LOG.info("{} binding end......", s);
			}
		}
	}

	@Override
	public void bind(URL url) {
		String ext = url.getPath();
		ext = ext.substring(ext.lastIndexOf('.') + 1);
		InputStream in = null;
		try {
			Logger.LOG.info("{} binding begin......", url);
			if (ext.equals("xml")) {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setValidating(true);
				factory.setNamespaceAware(true);
				XMLReader reader = factory.newSAXParser().getXMLReader();
				XMLHandler handler = new XMLHandler(this, loader, url);
				reader.setEntityResolver(handler);
				reader.setContentHandler(handler);
				reader.setErrorHandler(handler);
				reader.parse(new InputSource(in = url.openStream()));
			} else {
				ScriptEngineManager mgr = new ScriptEngineManager(loader);
				ScriptEngine engine = notNull(mgr.getEngineByExtension(ext), "not found engine: {}", ext);
				((Config) engine.eval(new InputStreamReader(in = url.openStream()))).config(this);
			}
			Logger.LOG.info("{} binding end......", url);
		} catch (Exception e) {
			throw rethrow(e);
		} finally {
			closeQuietly(in);
		}
	}

	@Override
	public boolean exist(String beanName) {
		return beanName.length() == 0 || context.getProvider(beanName) != null;
	}

	@Override
	public BeanReference ref(String name) {
		if (name.length() == 0)
			return Instance.value(context);
		Provider provider = context.getProvider(name);
		if (provider != null)
			return (BeanReference) provider;
		Object v = getEnv(name);
		if (v == null)
			throw new NoneBeanException(name);
		return Instance.value(v);
	}

	@Override
	public void setEnv(String name, Object value) {
		if (value != null) {
			if (environ == null)
				environ = new HashMap<String, Object>();
			validate(environ.put(name, value) == null, "duplicated name: {}", name);
		}
	}

	@Override
	public Object getEnv(String name) {
		return environ == null ? null : environ.get(name);
	}

	@Override
	public ClassLoader loader() {
		return loader;
	}

	void setPlaceholder(String name, String value) {
		if (value != null) {
			if (placeholder == null)
				placeholder = new HashMap<String, String>();
			validate(placeholder.put(name, value) == null, "duplicated name: {}", name);
		}
	}

	@Override
	public String replace(String name) {
		String ret = placeholder == null ? null : placeholder.get(name);
		return ret != null ? ret : systring(name);
	}

	public Context startup() {
		context.startup();
		environ = null;
		placeholder = null;
		return context;
	}
}
