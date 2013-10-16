package zcu.xutil.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;
import zcu.xutil.Objutil;

public class ShiroPlugin extends Plugin {
	private String unauthorizedUrl;

	public void setUnauthorizedUrl(String unauthorizedUrl) {
		this.unauthorizedUrl = unauthorizedUrl;
	}

	@Override
	protected View intercept(Invocation invocation) throws ServletException, IOException {
		String perm = invocation.getPermission();
		if (perm != null) {
			Subject currentUser = SecurityUtils.getSubject();
			if (!currentUser.isAuthenticated())
				throw new UnauthenticatedException();
			if (perm.length() > 0 && !currentUser.isPermitted(perm)) {
				HttpServletResponse resp = invocation.getResponse();
				if (Objutil.isEmpty(unauthorizedUrl))
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				else
					resp.sendRedirect(resp.encodeRedirectURL(unauthorizedUrl));
				return null;
			}
		}
		return invocation.proceed();
	}
}
