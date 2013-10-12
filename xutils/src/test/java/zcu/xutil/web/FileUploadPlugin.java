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
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import zcu.xutil.Objutil;
import zcu.xutil.utils.Checker;
import zcu.xutil.utils.Matcher;
import zcu.xutil.utils.Util;
import zcu.xutil.web.Action;
import zcu.xutil.web.Invocation;
import zcu.xutil.web.Plugin;
import zcu.xutil.web.View;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.FileRenamePolicy;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class FileUploadPlugin extends Plugin {
	private String dir;

	private FileRenamePolicy policy = new DefaultFileRenamePolicy();

	private int maxsize = 1024 * 1024;

	private String encode = Util.FILE_ENCODING;

	public FileUploadPlugin() {
		this(null);
	}
	public FileUploadPlugin(ServletContext servletCtx) {
		File f = servletCtx == null ? null :(File) servletCtx.getAttribute("javax.servlet.context.tempdir");
		this.dir = f == null ? Objutil.systring("java.io.tmpdir",".") : f.toString();
	}

	public void setEncode(String enc) {
		this.encode = enc;
	}

	public void setDir(String directory) {
		this.dir = directory;
	}

	public void setMaxsize(int maxSize) {
		this.maxsize = maxSize;
	}

	public void setPolicy(FileRenamePolicy fileRenamePolicy) {
		this.policy = fileRenamePolicy;
	}

	public View intercept(Invocation invocation) throws ServletException, IOException {
		HttpServletRequest req = invocation.getRequest();
		String type = req.getHeader("Content-Type");
		if (type != null && type.startsWith("multipart/form-data")) {
			String enc = encode == null ? req.getCharacterEncoding() : encode;
			MultipartRequest multireq = new MultipartRequest(req, dir, maxsize, enc, policy);
			Enumeration params = multireq.getParameterNames();
			while (params.hasMoreElements()) {
				String name = (String) params.nextElement();
				req.setAttribute(name, multireq.getParameterValues(name));
			}
			params = multireq.getFileNames();
			while (params.hasMoreElements()) {
				String name = (String) params.nextElement();
				File file = multireq.getFile(name);
				req.setAttribute(name, file);
			}
		}
		return invocation.proceed();
	}
}
