package zcu.xutil.ws;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import com.sun.xml.wss.RealmAuthenticationAdapter;
import com.sun.xml.wss.XWSSecurityException;

public class Authenticator extends RealmAuthenticationAdapter {

	@Override
	public boolean authenticate(final Subject callerSubject, final String username, final String password)
			throws XWSSecurityException {
		String pass = getPassword(username);
		if (pass == null || !pass.equals(password))
			return false;
//		if (username != null) {
//			@SuppressWarnings("unchecked")
//			Map<String, List<String>> headers = (Map<String, List<String>>) cb.getRuntimeProperties().get(
//					MessageContext.HTTP_REQUEST_HEADERS);
//			headers.put("Username-token", Collections.singletonList(username));
//		}
		// populate the subject
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				String x500Name = "CN=" + username;
				Principal principal = new X500Principal(x500Name);
				callerSubject.getPrincipals().add(principal);
				if (password != null)
					callerSubject.getPrivateCredentials().add(password);
				return null; // nothing to return
			}
		});
		return true;
	}

	protected String getPassword(String username) {
		return username;
	}
}
