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
package zcu.xutil.misc;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import org.bee.tl.core.BeeException;
import org.bee.tl.core.GroupTemplate;
import org.bee.tl.core.Template;

import zcu.xutil.Logger;
import zcu.xutil.XutilRuntimeException;

public class BeeTemplate{
	private volatile GroupTemplate group;
	private final String root;
	private final String tempFolder;
	private boolean optimize = true;
	private boolean nativeCall = true;
	private String placeholderStart = "${";
	private String placeholderEnd = "}";
	private String charset = "GBK";

	// 每2秒检测一次,用于开发
	int check = 2;

	public BeeTemplate(String rootpath) {
		root = rootpath + File.separator + "beetl";
		tempFolder = rootpath + File.separator + "temp";
	}

	public void setCharset(String encoding) {
		this.charset = encoding;
	}

	public void setOptimize(boolean bool) {
		this.optimize = bool;
	}

	public void setPlaceholderStart(String start) {
		this.placeholderStart = start;
	}

	public void setPlaceholderEnd(String end) {
		this.placeholderEnd = end;
	}

	public void setNativeCall(boolean nativecall) {
		this.nativeCall = nativecall;
	}

	public void setCheck(int seconds) {
		this.check = seconds;
	}

	private GroupTemplate getGroupTemplate() {
		if (group == null)
			synchronized (this) {
				if (group == null) {
					group = new GroupTemplate(new File(root));
					group.config("<!--:", "-->", placeholderStart, placeholderEnd);
					group.setTempFolder(tempFolder);
					if (nativeCall)
						group.enableNativeCall();
					if (optimize) {
						group.enableOptimize();
						Logger.LOG.info("Beetl允许优化，位于:{}", tempFolder);
					}
					if (check != 0)
						group.enableChecker(check);
					group.setCharset(charset);
					init(group);
				}
			}
		return group;
	}

	public void init(GroupTemplate mygroup) {
		// 如注册方法，格式化函数等
		/**
		 * group.register......
		 */
	}

	public void resolve(String view, Map<String, Object> variables, Writer writer) throws IOException {
		Template t = getGroupTemplate().getFileTemplate(view);
		for (Entry<String, Object> entry : variables.entrySet())
			t.set(entry.getKey(), entry.getValue());
		try {
			t.getText(writer);
		} catch (BeeException e) {
			throw new XutilRuntimeException(e);
		}
	}
}
