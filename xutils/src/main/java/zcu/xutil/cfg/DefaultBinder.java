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
import zcu.xutil.Objutil;
import zcu.xutil.Replace;
import zcu.xutil.XutilRuntimeException;

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
		loader = cl == null ? Objutil.contextLoader() : cl;
	}

	@Override
	public LifeCtrl put(boolean cache, String name, Provider provider) {
		return context.put(cache, name, provider);
	}

	@Override
	public LifeCtrl put(boolean cache, String name, Provider provider, Class<?> iface, String[] aops) {
		return put(cache, name, aops == null && iface == null ? provider : new ProxyDecorator(iface, provider, aops));
	}

	public void batch(String configs, URL urlcontext) {
		List<String> list = Objutil.split(configs, ',');
		for (int i = 0, len = list.size(); i < len; i++) {
			String s = list.get(i).trim();
			if (s.startsWith("path:"))
				bind(Objutil.notNull(loader.getResource(s.substring("path:".length())), "{} not found.", s));
			else {
				Class<?> cls;
				try {
					cls = loader.loadClass(s);
				} catch (ClassNotFoundException e) {
					cls = null;
				}
				if (cls != null) {
					Logger.LOG.info("{} binding begin......", cls);
					try {
						((Config) cls.newInstance()).config(this);
					} catch (Exception e) {
						Logger.LOG.warn("{} bind fail.", e, cls);
						throw Objutil.rethrow(e);
					}
					Logger.LOG.info("{} binding end......", cls);
					continue;
				}
				try {
					URL url = new URL(urlcontext == null ? new File(Objutil.systring(Constants.XUTILS_HOME)).toURI()
							.toURL() : urlcontext, s);
					bind(url);
				} catch (MalformedURLException e) {
					throw new XutilRuntimeException(e);
				}
			}
		}
	}

	@Override
	public void bind(URL url) {
		String s = url.getPath();
		s = s.substring(s.indexOf('.') + 1);
		InputStream in = null;
		try {
			Logger.LOG.info("{} binding begin......", url);
			if (s.equals("xml")) {
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
				ScriptEngineManager engineManger = new ScriptEngineManager(loader);
				ScriptEngine engine = engineManger.getEngineByExtension(s);
				((Config) engine.eval(new InputStreamReader(in = url.openStream()))).config(this);
			}
			Logger.LOG.info("{} binding end......", url);
		} catch (Exception e) {
			Logger.LOG.warn("{} bind fail.", e, url);
			throw Objutil.rethrow(e);
		} finally {
			Objutil.closeQuietly(in);
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
		if (environ == null)
			environ = new HashMap<String, Object>();
		Objutil.validate(environ.put(name, value)==null, "duplicated name: {}",name);
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
		if (placeholder == null)
			placeholder = new HashMap<String, String>();
		Objutil.validate(placeholder.put(name, value)==null, "duplicated name: {}",name);
	}

	@Override
	public String replace(String name) {
		String ret = placeholder == null ? null : placeholder.get(name);
		return ret != null ? ret : Objutil.systring(name);
	}

	public Context startup() {
		try {
			context.startup();
			environ = null;
			placeholder = null;
			return context;
		} catch (RuntimeException e) {
			context.destroy();
			Logger.LOG.info("startup fail.", e);
			throw e;
		}
	}
}
