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
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.utils.Base64;
import zcu.xutil.utils.ByteArray;
import zcu.xutil.utils.UrlBuilder;
import zcu.xutil.utils.Util;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */

public final class Stream implements View {
	private final String name;
	private final Object content;
	private final int length;
	private boolean partial;
	private boolean inline;
	private boolean attachment;
	private long lastModified;
	private int cacheSeconds;

	public Stream(File file) {
		name = file.getName();
		length = (int) file.length();
		content = file;
		lastModified = file.lastModified();
	}

	public Stream(String view, byte[] bytes) {
		this(view, bytes, 0, bytes.length);
	}

	public Stream(String view, byte[] bytes, int offset, int len) {
		name = view;
		length = len;
		content = ByteArray.toStream(bytes, offset, len);
	}

	public Stream(String view, String str) {
		name = view;
		length = str.length();
		content = str;
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

	public Stream setLastModified(long lastmodified) {
		this.lastModified = lastmodified;
		return this;
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
		InputStream in = content instanceof File ? new FileInputStream((File) content)
				: (content instanceof InputStream ? (InputStream) content : null);
		try {
			String str = resp.getContentType();
			if (str == null) {
				if (name == null || (str = vc.getServletContext().getMimeType(name)) == null)
					str = in == null ? "text/plain" : "application/octet-stream";
				resp.setContentType(str + "; charset=" + (in == null ? "UTF-8" : Util.FILE_ENCODING));
			}
			if ((inline | attachment) && name != null && !resp.containsHeader("Content-Disposition")) {
				str = encodeFilename(vc.getRequest().getHeader("User-Agent"), name, attachment);
				resp.setHeader("Content-Disposition", str);
			}
			Webutil.applyCacheSeconds(resp, cacheSeconds, lastModified > 0);
			if (length == 0)
				return;
			int len = length;
			if (in == null) {
				resp.getWriter().write((String) content);
				return;
			}
			if (len > 0) {
				if (partial) {
					resp.setHeader("Accept-Ranges", "bytes");
					long[] pos = parseRange(vc.getRequest().getHeader("Range"), len);
					if (pos != null) {
						long first = pos[0], last = pos[1];
						if (first < 0 || first >= len || last < first) {
							resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
							resp.setHeader("Content-Range", "bytes */" + len);
							return;
						}
						Objutil.validate(in.skip(first) == first, "can't skip size: {}", first);
						resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						resp.setHeader("Content-Range", "bytes " + first + "-" + last + "/" + len);
						len = (int) (last - first + 1);
					}
				}
				resp.setContentLength(len);
			}
			int send = Util.transfer(in, resp.getOutputStream(), len);
			if (len >= 0 && send != len)
				Logger.LOG.warn("{}: content length:{} .written length:{}", this, len, send);
			resp.flushBuffer();
		} finally {
			Objutil.closeQuietly(in);
		}
	}

	public static String encodeFilename(String userAgent, String filename, boolean attachment) {
		try {
			StringBuilder sb = new StringBuilder(64).append(attachment ? "attachment" : "inline")
					.append(";filename=\"");
			if (null == userAgent || userAgent.indexOf("MSIE") < 0)
				sb.append("=?UTF-8?B?").append(Base64.encode(filename.getBytes("UTF-8"), false)).append("?=");
			else {
				int len = sb.length();
				UrlBuilder.encode(filename, "UTF-8", sb);
				if (sb.length() > len + 150) {
					sb.setLength(len);
					sb.append(new String(filename.getBytes(Util.FILE_ENCODING), "ISO-8859-1"));
				}
			}
			return sb.append("\"").toString();
		} catch (UnsupportedEncodingException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static long[] parseRange(String range, int len) {
		if (range == null || !range.startsWith("bytes="))
			return null;
		int i = range.indexOf('-', 6);// 6: "bytes=".length()
		if (i < 0)
			return null;
		String s = range.substring(6, i).trim();
		try {
			long firstPos, lastPos;
			if (s.length() == 0) { // suffix mode
				firstPos = Math.max(0, len - Integer.parseInt(range.substring(i + 1).trim()));
				lastPos = len - 1;
			} else {
				firstPos = Integer.parseInt(s);
				if ((s = range.substring(i + 1).trim()).length() == 0)
					lastPos = len - 1; // prefix mode
				else if ((lastPos = Integer.parseInt(s)) < firstPos)
					return null;
				else if (lastPos >= len)
					lastPos = len - 1;
			}
			return new long[] { firstPos, lastPos };
		} catch (NumberFormatException e) {
			Logger.LOG.info("{}: ignore invalid range:{}", Stream.class, range);
			return null;
		}
	}
}
