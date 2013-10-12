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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.utils.LRUCache;
import zcu.xutil.utils.Util;
import zcu.xutil.web.Resolver;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class MvelResolver implements Resolver {
	private final LRUCache<String, CTStamp> cache;
	private final TemplateRegistry registry;
	private final String path;

	public MvelResolver(String directory, int cacheSize) {
		this(new File(directory), cacheSize);
	}

	public MvelResolver(URL directory, int cacheSize) {
		this(new File(URI.create(directory.toString())), cacheSize);
	}

	public MvelResolver(File directory, int cacheSize) {
		this.cache = new LRUCache<String, CTStamp>(cacheSize, null);
		this.registry = new SimpleTemplateRegistry();
		path = Util.relativize(directory, new File(".")).getPath();
	}

	public void registry(String name, CharSequence template) {
		Objutil.validate(!registry.contains(name), "template name exist.");
		registry.addNamedTemplate(name, TemplateCompiler.compileTemplate(template));
	}

	public void registry(String name, Reader reader) {
		registry(name, Util.readAndClose(reader, new StringBuilder(1024)));
	}


	@Override
	public void resolve(String view, Map<String, Object> variables, final Writer writer) throws IOException {
		Objutil.dupChkPut(variables, "t_dir", path);
		TemplateRuntime.execute(getTemplate(view.toLowerCase()), null, new MapVariableResolverFactory(variables), registry, new TemplateOutputStream() {
			@Override
			public TemplateOutputStream append(CharSequence s) {
				try {
					writer.append(s);
				} catch (IOException e) {
					throw new XutilRuntimeException(e);
				}
				return this;
			}
			@Override
			public TemplateOutputStream append(char[] s) {
				try {
					writer.write(s);
				} catch (IOException e) {
					throw new XutilRuntimeException(e);
				}
				return this;
			}

			@Override
			public String toString() {
				return "";
			}
		});
	}

	public void clearCache() {
		cache.clear();
	}

	private CompiledTemplate getTemplate(String id) throws IOException {
		File f = new File(path, id);
		int lastModified = (int) ((f.lastModified() >> 10) & Integer.MAX_VALUE);
		CTStamp t = cache.get(id);
		if (t != null) {
			if (lastModified == t.stamp)
				return t.ct;
			if (!cache.remove(id, t))
				return getTemplate(id);
		}
		t = new CTStamp(TemplateCompiler.compileTemplate(Util.readAndClose(new FileReader(f),new StringBuilder(1024))), lastModified);
		return Objutil.ifNull(cache.putIfAbsent(id, t), t).ct;
	}

	private static final class CTStamp {
		final int stamp;
		final CompiledTemplate ct;

		CTStamp(CompiledTemplate c, int secondstamp) {
			ct = c;
			stamp = secondstamp;
		}
	}
}
