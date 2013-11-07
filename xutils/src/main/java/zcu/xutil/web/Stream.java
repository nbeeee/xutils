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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.utils.Base64;
import zcu.xutil.utils.ByteArray;
import zcu.xutil.utils.UrlBuilder;
import zcu.xutil.utils.Util;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public final class Stream implements View {
	private final Object content;
	private int length;
	private String name;
	private boolean partial;
	private boolean inline;
	private boolean attachment;
	private long lastModified;
	private int cacheSeconds = -1;

	public Stream(File file) {
		content = file;
		length = (int) file.length();
		name = file.getName();
		lastModified = file.lastModified();
	}

	public Stream(String str) {
		content = str;
	}

	public Stream(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	public Stream(byte[] bytes, int offset, int len) {
		this(ByteArray.toStream(bytes, offset, len), len);
	}

	public Stream(InputStream in, int len) {
		content = in;
		length = len;
	}

	public Stream name(String filename) {
		this.name = filename;
		return this;
	}

	public Stream inline() {
		inline = true;
		return this;
	}

	public Stream attachment() {
		attachment = true;
		return this;
	}

	public Stream partial() {
		partial = true;
		return this;
	}
	public Stream cache(int seconds) {
		cacheSeconds = seconds;
		return this;
	}
	
	public Stream setLastModified(long lastmodified) {
		this.lastModified = lastmodified;
		return this;
	}

	private String getContentType(WebContext vc) {
		String str;
		if (name == null || (str = vc.getServletContext().getMimeType(name)) == null) {
			if ((str = vc.getServletContext().getMimeType(vc.getActionName())) == null)
				str = content instanceof String ? "text/plain" : "application/octet-stream";
		}
		return str + "; charset=" + (content instanceof String ? "UTF-8" : Util.filencode);
	}

	@Override
	public void handle(WebContext vc) throws ServletException, IOException {
		HttpServletResponse resp = vc.getResponse();
		if (lastModified > 0) {
			long ifModifiedSince = vc.getRequest().getDateHeader("If-Modified-Since");
			if (ifModifiedSince >= lastModified / 1000 * 1000) {
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
			resp.setDateHeader("Last-Modified", lastModified);
		}
		if (resp.getContentType() == null)
			resp.setContentType(getContentType(vc));
		if ((inline | attachment) && name != null && !resp.containsHeader("Content-Disposition"))
			resp.setHeader("Content-Disposition",
					encodeFilename(vc.getRequest().getHeader("User-Agent"), name, attachment));
		Webutil.applyCacheSeconds(resp, cacheSeconds, lastModified > 0);
		final InputStream in;
		if (content instanceof String) {
			byte[] bytes = ((String) content).getBytes(resp.getCharacterEncoding());
			length = bytes.length;
			in = ByteArray.toStream(bytes);
		} else if (content instanceof File)
			in = new FileInputStream((File) content);
		else
			in = (InputStream) content;
		try {
			int len = partial ? partial(resp, in, vc.getRequest().getHeader("Range"), length) : length;
			if (len == 0)
				return;
			if (len > 0)
				resp.setContentLength(len);
			int send = Util.transfer(in, resp.getOutputStream(), len);
			if (len >= 0 && send != len)
				Logger.LOG.warn("{}: content length:{} .written length:{}", this, len, send);
			resp.flushBuffer();
		} finally {
			Objutil.closeQuietly(in);
		}
	}

	public static String encodeFilename(String userAgent, String file, boolean attachment) {
		StringBuilder sb = new StringBuilder(64).append(attachment ? "attachment" : "inline").append(";filename=\"");
		if (null == userAgent || userAgent.indexOf("MSIE") < 0)
			return Base64.minencode(file, sb).append("\"").toString();
		int len = sb.length();
		if (UrlBuilder.encode(file, "UTF-8", sb).length() > len + 150) {
			sb.setLength(len);
			sb.append(new String(file.getBytes(Charset.forName(Util.filencode)), Charset.forName("ISO-8859-1")));
		}
		return sb.append("\"").toString();
	}

	private int partial(HttpServletResponse resp, InputStream in, String range, final int len) throws IOException {
		resp.setHeader("Accept-Ranges", "bytes");
		if (range == null || !range.startsWith("bytes="))
			return len;
		int i = range.indexOf('-', 6);// 6: "bytes=".length()
		if (i < 0)
			return len;
		String s = range.substring(6, i).trim();
		long first, last;
		if (s.length() == 0) { // suffix mode
			first = Math.max(0, len - Integer.parseInt(range.substring(i + 1).trim()));
			last = len - 1;
		} else {
			first = Integer.parseInt(s);
			if ((s = range.substring(i + 1).trim()).length() == 0)
				last = len - 1; // prefix mode
			else if ((last = Integer.parseInt(s)) < first)
				return 0;
			else if (last >= len)
				last = len - 1;
		}
		if (first < 0 || first >= len || last < first) {
			resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			resp.setHeader("Content-Range", "bytes */" + len);
			return 0;
		}
		Objutil.validate(in.skip(first) == first, "can't skip size: {}", first);
		resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		resp.setHeader("Content-Range", "bytes " + first + "-" + last + "/" + len);
		return (int) (last - first + 1);
	}
}
