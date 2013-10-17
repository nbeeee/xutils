package zcu.xutil.web;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

public class LoginSubmit {
	String username;
	String password;
	boolean rememberMe = false;


	public String authenticate() {
		// Example using most common scenario of username/password pair:
		UsernamePasswordToken token = new UsernamePasswordToken(username, password);
		// "Remember Me" built-in:
		token.setRememberMe(rememberMe);
		Subject currentUser = SecurityUtils.getSubject();
		
		try {
			currentUser.login(token);
		} catch (AuthenticationException e) {
			// Could catch a subclass of AuthenticationException if you like

			return "/login";
		}
		return "protected?faces-redirect=true";
	}

	public String logout() {
		Subject currentUser = SecurityUtils.getSubject();
		try {
			currentUser.logout();
		} catch (Exception e) {
			
		}
		return "index";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean getRememberMe() {
		return rememberMe;
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}
}
