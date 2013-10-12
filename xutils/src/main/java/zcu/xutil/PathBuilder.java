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
package zcu.xutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import static zcu.xutil.Constants.*;

/**
 * 启动工具类,主要功能是构建类路径 <br>
 * 使用配置文件xutils.properties的xutils.main 和 xutils.path 启动应用:<br>
 * xutils.main xutils.path分隔字符为pipe '|' , xutils.main 格式: mainClass|p|p
 * 参数p为缺省参数,如:<br>
 * xutils.main=org.mortbay.xml.XmlConfiguration|jetty.xml|plus.xml<br>
 * xutils.path 格式: path1|path2|path3 以 "/*" 结束的path表示该目录下的所有 jar 或 zip 文件, 以
 * "/**" 结束的path表示该目录下的所有 jar 或 zip 文件，包括子目录,如:<br>
 * xutils.path=${jetty.home}/lib/**|${xutils.home}/classes<br>
 *
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class PathBuilder {
	private List<File> elements = new ArrayList<File>();

	public void addComponent(String pattern) {
		pattern = pattern.trim();
		int i = pattern.endsWith("/*") ? 1 : (pattern.endsWith("/**") ? 2 : 0);
		File f = new File(pattern.substring(0, pattern.length() - i).replace('/', File.separatorChar));
		try {
			if (i == 0)
				addComponent(f);
			else
				addJars(f, i == 2);
		} catch (IOException e) {
			throw new XutilRuntimeException(pattern, e);
		}
	}

	public int size() {
		return elements.size();
	}

	public void addClasspath(String s) {
		for (String e : Objutil.split(s, File.pathSeparatorChar))
			addComponent(e);
	}

	private void addComponent(File f) throws IOException {
		if (!f.exists())
			Objutil.log(PathBuilder.class, "invalid path: {}", null, f.getPath());
		else if (!elements.contains(f = f.getCanonicalFile()))
			elements.add(f);
	}

	private void addJars(File dir, boolean recurse) throws IOException {
		File[] entries = dir.listFiles();
		if (entries == null)
			Objutil.log(PathBuilder.class, "invalid path: {}", null, dir.getPath());
		else
			for (File f : entries) {
				if (f.isDirectory() && recurse)
					addJars(f, recurse);
				else {
					String name = f.getName();
					if (name.endsWith(".jar") || name.endsWith(".zip"))
						addComponent(f);
				}
			}
	}

	@Override
	public String toString() {
		StringBuilder cp = new StringBuilder(1024);
		for (int i = 0, len = elements.size(); i < len; i++)
			(i > 0 ? cp.append(File.pathSeparatorChar) : cp).append(elements.get(i).getPath());
		return cp.toString();
	}
	
	public URL[] getUrls(){
		return getUrls(0);
	}

	private URL[] getUrls(int begin) {
		int len = size() - begin;
		URL[] urls = new URL[len];
		while (--len >= 0)
			urls[len] = Objutil.toURL(elements.get(begin + len));
		return urls;
	}

	private static Method init(Object[] params) throws Exception {
		List<String> list = Objutil.split(Objutil.systring(XUTILS_MAIN), '|');
		int size = list.size();
		final String mainClass = size > 0 ? list.get(0).trim() : PathBuilder.class.getName();
		if (size > 1 && ((String[]) params[0]).length == 0)
			params[0] = list.subList(1, size).toArray(new String[size - 1]);
		if (mainClass.equals(PathBuilder.class.getName())) {
			if (Objutil.systring(XUTILS_DEBUG) != null) {
				for (Map.Entry e : Objutil.properties().entrySet())
					Objutil.log(PathBuilder.class, "key=[{}],value=[{}]", null, e.getKey(), e.getValue());
			}
			System.exit(0);
		}
		Thread thread = Thread.currentThread();
		ClassLoader loader = thread.getContextClassLoader();
		String classpath = System.getProperty("java.class.path");
		list = Objutil.split(Objutil.systring(XUTILS_PATH), '|');
		if ((size = list.size()) > 0) {
			PathBuilder cp = new PathBuilder();
			cp.addClasspath(classpath);
			int begin = cp.size();
			for (int i = 0; i < size; i++)
				cp.addComponent(list.get(i));
			URL[] urls = cp.getUrls(begin);
			if (urls.length > 0) {
				System.setProperty("java.class.path", classpath = cp.toString());
				thread.setContextClassLoader(loader = new URLClassLoader(urls, loader));
			}
		}
		Objutil.properties().remove(XUTILS_MAIN);
		Objutil.properties().remove(XUTILS_PATH);
		Objutil.log(PathBuilder.class, "classpath: {}", null, classpath);
		Objutil.log(PathBuilder.class, "invoke: {}.main args={}", null, mainClass, params[0]);
		return loader.loadClass(mainClass).getDeclaredMethod("main", String[].class);
	}

	public static void main(String[] args) {
		try {
			Object[] params = new Object[] { args };
			init(params).invoke(null, params);
		} catch (Exception e) {
			Objutil.log(PathBuilder.class, "launch error.", e);
		}
	}
}
