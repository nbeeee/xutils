package zcu.xutil.misc;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

public class CaptchaFormAuthcFilter extends FormAuthenticationFilter {
	private String captchaParam = "captcha";

	public void setCaptchaParam(String s) {
		captchaParam = s;
	}

	protected String getCaptcha(ServletRequest request) {
		return WebUtils.getCleanParam(request, captchaParam);
	}

	@Override
	protected boolean isLoginSubmission(ServletRequest request, ServletResponse response) {
		if (!super.isLoginSubmission(request, response))
			return false;
		String captcha = WebUtils.getCleanParam(request, captchaParam);
		if (CaptchaAction.verifyCaptcha(WebUtils.toHttp(request), captcha))
			return true;
		request.setAttribute(FormAuthenticationFilter.DEFAULT_ERROR_KEY_ATTRIBUTE_NAME,
				"zcu.xutil.misc.CaptchaFormAuthcFilter");
		return false;
	}

}
