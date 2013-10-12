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

import java.io.IOException;

import javax.servlet.ServletException;

import zcu.xutil.utils.UrlBuilder;

/**
 * 重定向到url, model的数据设置到url的query参数中.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class Redirect extends UrlBuilder implements View {

	public Redirect(String url) {
		this(url, null);
	}

	public Redirect(String url, String enc) {
		super(url, enc);
	}
	@Override
	public void handle(WebContext vc) throws ServletException, IOException {
		vc.getResponse().sendRedirect(vc.getResponse().encodeRedirectURL(toString()));
	}

	@Override
	public Redirect add(String key, String value) {
		return(Redirect)super.add(key, value);
	}
}
