package zcu.xutil.web.struts2;

import java.util.Map;

import javax.servlet.ServletContext;

import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.Provider;
import zcu.xutil.web.Webutil;

import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.inject.Inject;

@SuppressWarnings("serial")
public class XutilObjectFactory extends ObjectFactory {
	private Context context;

	@Inject
	public void setServletContext(ServletContext ctx) {
		context = Webutil.getAppContext(ctx);
	}

	@Override
	public boolean isNoArgConstructorRequired() {
		return false;
	}

	@Override
	public Object buildBean(String className, Map<String, Object> extraContext, boolean injectInternal)
			throws Exception {
		Provider p = context.getProvider(className);
		if (p != null) {
			Object obj = p.instance();
			if (injectInternal)
				injectInternalBeans(obj);
			return obj;
		}
		return super.buildBean(className, extraContext, injectInternal);
	}
}
