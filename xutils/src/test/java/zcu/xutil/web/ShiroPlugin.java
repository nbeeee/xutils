package zcu.xutil.web;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;

public class ShiroPlugin extends Plugin {

	@Override
	protected View intercept(Invocation invocation) throws ServletException, IOException {
		String perm = invocation.getPermission();
		if (perm != null) {
			Subject currentUser = SecurityUtils.getSubject();
			if (!currentUser.isAuthenticated())
				throw new UnauthenticatedException();
			if (perm.length() > 0 && !currentUser.isPermitted(perm))
				throw new UnauthenticatedException();
		}
		return invocation.proceed();
	}
}
