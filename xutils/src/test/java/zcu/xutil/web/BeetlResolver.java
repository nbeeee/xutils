package zcu.xutil.web;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.bee.tl.core.BeeException;
import org.bee.tl.core.GroupTemplate;
import org.bee.tl.core.Template;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

public class BeetlResolver implements Resolver {
	private static final Logger logger = Logger.getLogger(BeetlResolver.class);

	private volatile GroupTemplate group;
	private String root = ".";
	private String tempFolder = Objutil.systring("java.io.tmpdir","./temp");
	private boolean optimize = true;
	private boolean nativeCall = true;
	private String placeholderStart = "${";
	private String placeholderEnd = "}";
	private String charset = "GBK";

	// 每2秒检测一次,用于开发
	int check = 2;

	public void setServletContext(ServletContext sc) {
		root = sc.getRealPath("/");
		Object s = sc.getAttribute("javax.servlet.context.tempdir");
		if(s instanceof String)
			tempFolder = (String)s;
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
	private GroupTemplate getGroupTemplate(){
		if(group == null)
			synchronized (this) {
				if(group == null){
					group = new GroupTemplate(new File(root+File.separator +"beetl"));
					group.config("<!--:", "-->", placeholderStart, placeholderEnd);
					group.setTempFolder(tempFolder);
					if (nativeCall)
						group.enableNativeCall();
					if (optimize) {
						group.enableOptimize();
						logger.info("Beetl允许优化，位于:{}", tempFolder);
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
		//如注册方法，格式化函数等
		/**
		 * group.register......
		 */
	}

	@Override
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
