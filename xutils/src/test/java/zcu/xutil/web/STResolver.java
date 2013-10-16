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
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletContext;

import org.antlr.stringtemplate.AttributeRenderer;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateWriter;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
//import zcu.xutil.misc.MailInline;

public final class STResolver implements Resolver {
	private final StringTemplateGroup group;
	File dir;
	boolean useGroupWriter;

	public STResolver(String path) {
		try {
			dir = new File(path);
			this.group = new StringTemplateGroup("mygroup", dir.getAbsolutePath());
			File stgfile = new File(dir, "templates.stg");
			if (stgfile.exists()) {
				StringTemplateGroup father = new StringTemplateGroup(new FileReader(stgfile),
						DefaultTemplateLexer.class);
				this.group.setSuperGroup(father);
				registerDateNumberRenders(father);
			} else
				registerDateNumberRenders(group);
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public STResolver(StringTemplateGroup aGroup) {
		this.group = aGroup;
	}

	public StringTemplateGroup getGroup() {
		return group;
	}

	public void setUseGroupWriter(boolean bool) {
		this.useGroupWriter = bool;
	}


	public void resolve(String view, Map<String, Object> variables,Writer out) throws IOException {
		int i = view.lastIndexOf('.');
		StringTemplate template = group.getInstanceOf(view.substring(0, i));
		Objutil.validate(variables.put("_dir", dir.getPath())==null, "duplicated name: {}","_dir");
		template.setAttributes(variables);
		StringTemplateWriter wr;
		if (useGroupWriter)
			wr = getGroup().getStringTemplateWriter(out);
		else
			wr = new NoIndentWriter(out);
		template.write(wr);

	}

	public void registerDateNumberRenders(StringTemplateGroup aGroup) {
		aGroup.registerRenderer(Date.class, this);
		aGroup.registerRenderer(java.sql.Date.class, this);
		aGroup.registerRenderer(java.sql.Time.class, this);
		aGroup.registerRenderer(java.sql.Timestamp.class, this);
		aGroup.registerRenderer(Double.class, this);
		aGroup.registerRenderer(Float.class, this);
		aGroup.registerRenderer(Short.class, this);
		aGroup.registerRenderer(Long.class, this);
		aGroup.registerRenderer(Integer.class, this);
		aGroup.registerRenderer(BigDecimal.class, this);
		aGroup.registerRenderer(BigInteger.class, this);
//		aGroup.registerRenderer(MailInline.class, new AttributeRenderer() {
//			
//			@Override
//			public String toString(Object arg0, String arg1) {
//				// TODO Auto-generated method stub
//				return ((MailInline)arg0).cid(arg1);
//			}
//			
//			@Override
//			public String toString(Object arg0) {
//				// TODO Auto-generated method stub
//				return arg0.toString();
//			}
//		});
	}

	public String toString(Object o) {
		if (o instanceof Date)
			return toString(o, "yy-MM-dd HH:mm:ss");
		return o.toString();
	}

	public String toString(Object o, String formatName) {
		boolean date = o instanceof Date;
		if (!date && !(o instanceof Number))
			throw new UnsupportedOperationException("neither Date nor Number.");
		return (date ? new SimpleDateFormat(formatName) : new DecimalFormat(formatName)).format(o);
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}
}