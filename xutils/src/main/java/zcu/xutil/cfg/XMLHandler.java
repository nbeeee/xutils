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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.utils.Convertor;
import zcu.xutil.utils.Util;

public final class XMLHandler extends DefaultHandler {
	final DefaultBinder binder;
	private final URL xmlurl;
	private final StringBuilder buffer = new StringBuilder();
	private Locator locator;
	private Entry current;
	private String varkey;

	public XMLHandler(DefaultBinder b, ClassLoader cl, URL url) {
		binder = b;
		xmlurl = url;
	}

	String getBuffer(boolean nullable) {
		String s = buffer.toString();
		return nullable && s.equals("${}") ? null : Objutil.placeholder(s, binder);
	}

	Class<?> clsOf(String s) {
		if (s == null)
			return null;
		Class cls = Convertor.getPrimitive(s = Objutil.placeholder(s, binder).trim());
		return cls != null ? cls : Objutil.loadclass(binder.loader(), s);
	}

	@Override
	public void startElement(final String uri,final String local,final String qName,final Attributes attrs) throws SAXException {
		try {
			if ("import".equals(local))
				varkey = attrs.getValue("key");
			else if ("bean".equals(local) || "array".equals(local) || "alias".equals(local))
				current = new Entry(local, attrs, current);
			else if (current != null)
				current.startElement(local, attrs);
			buffer.setLength(0);
		} catch (RuntimeException e) {
			log("startElement", local);
			throw e;
		}
	}

	@Override
	public void endElement(final String uri, final String local, final String qName) throws SAXException {
		try {
			if ("import".equals(local)) {
				String s = getBuffer(false);
				if (varkey != null)
					binder.setPlaceholder(varkey, s);
				else 
					binder.batch(s, xmlurl);
			} else if (current != null && current.endElement(local))
				current = current.previous;
		} catch (RuntimeException e) {
			log("endElement", local);
			throw e;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		buffer.append(ch, start, length);
	}

	private void log(String method, String detail) {
		Logger.LOG.warn("xml error. {} {} at line: {}  file {}", method, detail, locator.getLineNumber(), xmlurl);
	}

	@Override
	public void warning(SAXParseException e) {
		log("warning", e.getMessage());
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		log("error", e.getMessage());
		throw e;
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		log("fatalError", e.getMessage());
		throw e;
	}

	@Override
	public InputSource resolveEntity(String pid, String sid) {
		Objutil.validate(sid.endsWith("xutils-config.dtd"), "Invalid system identifier: {}", sid);
		InputSource ret = new InputSource(getClass().getResourceAsStream("xutils-config.dtd"));
		ret.setSystemId(sid);
		return ret;
	}

	@Override
	public void setDocumentLocator(Locator l) {
		locator = l;
	}

	@SuppressWarnings("serial")
	private final class Entry extends ArrayList<Call> {
		private boolean cache;
		private boolean eager;
		private String id, product, destroy, intercepts[];
		private Class aoptype;
		Entry previous;

		Entry(String local, Attributes attrs, Entry prev) {
			previous = prev;
			id = attrs.getValue("id");
			cache = Boolean.parseBoolean(attrs.getValue("cache"));
			if ("alias".equals(local))
				product = attrs.getValue("ref");
			else {
				product = attrs.getValue("class");
				destroy = attrs.getValue("destroy");
				eager = Boolean.parseBoolean(attrs.getValue("eager"));
				add(new Call(attrs.getValue("method"), false));
			}
		}

		void startElement(String local, Attributes attrs) {
			boolean set = "set".equals(local) ? add(new Call(attrs.getValue("property"), true)) : false;
			if (set || "arg".equals(local)) {
				get(size() - 1).addArg(clsOf(attrs.getValue("class")), attrs.getValue("ref"));
			} else if ("call".equals(local)) {
				add(new Call(attrs.getValue("method"), false));
			} else if ("out".equals(local)) {
				add(new Call(attrs.getValue("method"), clsOf(attrs.getValue("class"))));
			} else if ("aop".equals(local)) {
				aoptype = clsOf(attrs.getValue("class"));
			}
		}

		boolean endElement(String local) {
			boolean alias = false, array = false;
			if ("set".equals(local) || "arg".equals(local)) {
				get(size() - 1).tail.val = getBuffer(true);
			} else if ("aop".equals(local)) {
				intercepts = Objutil.split(getBuffer(false).trim(), ',').toArray(new String[] {});
			} else if ("bean".equals(local) || (array = "array".equals(local)) || (alias = "alias".equals(local))) {
				id = binder.put(cache, id, alias ? binder.ref(product).get() : bind(array), aoptype, intercepts)
						.die(destroy).eager(eager).name();
				if (previous != null)
					previous.get(previous.size() - 1).tail.ref = id;
				return true;
			}
			return false;
		}

		Provider bind(boolean array) {
			Class<?> type = clsOf(product);
			Call c = get(0);
			List<Object> work = new ArrayList<Object>();
			Object[] params = c.getParamters(binder, work);
			if (array)
				return new Xarray(type, params);
			RefCaller ret;
			String s = c.method;
			int i, len = size();
			if (Objutil.isEmpty(s))
				ret = Prototype.create(type, params);
			else if ((i = s.lastIndexOf(':')) >= 0)
				ret = binder.ref(s.substring(0, i)).ext(type, s.substring(i + 1), params);
			else {
				i = s.lastIndexOf('.');
				ret = Prototype.create(type, i > 0 ? clsOf(s.substring(0, i)) : type, s.substring(i + 1), params);
			}
			boolean map = Map.class.isAssignableFrom(ret.getType());
			for (i = 1; i < len; i++) {
				params = (c = get(i)).getParamters(binder, work);
				if (c.output)
					ret = ret.ext(c.product, c.method, params);
				else if (!c.setter)
					ret = ret.call(c.method, params);
				else if (map)
					ret = ret.call("put", c.method, params[0]);
				else
					ret = ret.call(Util.nameOfSetter(c.method), params);
			}
			return ret;
		}
	}

	private static class Call {
		final String method;
		Arg tail;
		Class product;
		boolean setter, output;

		Call(String m, boolean set) {
			method = m;
			setter = set;
		}

		Call(String m, Class c) {
			method = m;
			output = true; // product output
			product = c;
		}

		void addArg(Class type, String ref) {
			(tail = new Arg(tail, type)).ref = ref;
		}

		Object[] getParamters(Binder binder, List<Object> work) {
			if (tail == null)
				return null;
			work.clear();
			tail.get(binder, work);
			return work.toArray();
		}
	}

	private static final class Arg {
		private final Class type;
		private final Arg prev;
		String ref, val;

		Arg(Arg previous, Class c) {
			prev = previous;
			type = c;
		}

		void get(Binder binder, List<Object> out) {
			if (prev != null)
				prev.get(binder, out);
			BeanReference o = ref == null ? Instance.value(val) : binder.ref(ref);
			out.add(type == null ? o : o.cast(type));
		}
	}
}