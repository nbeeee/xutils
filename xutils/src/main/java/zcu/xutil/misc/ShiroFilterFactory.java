package zcu.xutil.misc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.shiro.config.Ini;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.util.StringUtils;
import org.apache.shiro.web.config.IniFilterChainResolverFactory;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;

import zcu.xutil.Objutil;

public class ShiroFilterFactory {
	private Map<String, Filter> filters = new LinkedHashMap<String, Filter>();
	private Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
	private SecurityManager securityManager;
	private String loginUrl;
	private String successUrl;
	private String unauthorizedUrl;

	public XutilShiroFilter getObject() {
		PathMatchingFilterChainResolver chainResolver = new PathMatchingFilterChainResolver();
		chainResolver.setFilterChainManager(createFilterChainManager());
		return new XutilShiroFilter((WebSecurityManager) securityManager, chainResolver);
	}

	public void setSecurityManager(SecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public void setSuccessUrl(String successUrl) {
		this.successUrl = successUrl;
	}

	public void setUnauthorizedUrl(String unauthorizedUrl) {
		this.unauthorizedUrl = unauthorizedUrl;
	}

	public void addFilter(String name, Filter filter) {
		filters.put(name, filter);
	}

	public void setFilterChainDefinitions(String definitions) {
		Ini ini = new Ini();
		ini.load(definitions);
		Ini.Section section = ini.getSection(IniFilterChainResolverFactory.URLS);
		if (CollectionUtils.isEmpty(section)) 
			section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
		this.filterChainDefinitionMap = section;
	}

	private FilterChainManager createFilterChainManager() {
		DefaultFilterChainManager manager = new DefaultFilterChainManager();
		Map<String, Filter> defaultFilters = manager.getFilters();
		for (Filter filter : defaultFilters.values())
			applyGlobalPropertiesIfNecessary(filter);
		if (!CollectionUtils.isEmpty(filters)) {
			for (Map.Entry<String, Filter> entry : filters.entrySet()) {
				String name = entry.getKey();
				Filter filter = entry.getValue();
				applyGlobalPropertiesIfNecessary(filter);
				manager.addFilter(name, filter);
			}
		}
		if (!CollectionUtils.isEmpty(filterChainDefinitionMap)) {
			for (Map.Entry<String, String> entry : filterChainDefinitionMap.entrySet()) {
				String url = entry.getKey();
				String chainDefinition = entry.getValue();
				manager.createChain(url, chainDefinition);
			}
		}
		return manager;
	}

	private void applyLoginUrlIfNecessary(Filter filter) {
		if (StringUtils.hasText(loginUrl) && (filter instanceof AccessControlFilter)) {
			AccessControlFilter acFilter = (AccessControlFilter) filter;
			String existingLoginUrl = acFilter.getLoginUrl();
			if (AccessControlFilter.DEFAULT_LOGIN_URL.equals(existingLoginUrl))
				acFilter.setLoginUrl(loginUrl);
		}
	}

	private void applySuccessUrlIfNecessary(Filter filter) {
		if (StringUtils.hasText(successUrl) && (filter instanceof AuthenticationFilter)) {
			AuthenticationFilter authcFilter = (AuthenticationFilter) filter;
			String existingSuccessUrl = authcFilter.getSuccessUrl();
			if (AuthenticationFilter.DEFAULT_SUCCESS_URL.equals(existingSuccessUrl))
				authcFilter.setSuccessUrl(successUrl);
		}
	}

	private void applyUnauthorizedUrlIfNecessary(Filter filter) {
		if (StringUtils.hasText(unauthorizedUrl) && (filter instanceof AuthorizationFilter)) {
			AuthorizationFilter authzFilter = (AuthorizationFilter) filter;
			String existingUnauthorizedUrl = authzFilter.getUnauthorizedUrl();
			if (existingUnauthorizedUrl == null)
				authzFilter.setUnauthorizedUrl(unauthorizedUrl);
		}
	}

	private void applyGlobalPropertiesIfNecessary(Filter filter) {
		applyLoginUrlIfNecessary(filter);
		applySuccessUrlIfNecessary(filter);
		applyUnauthorizedUrlIfNecessary(filter);
	}

	private static final class XutilShiroFilter extends AbstractShiroFilter {
		XutilShiroFilter(WebSecurityManager webSecurityManager, FilterChainResolver resolver) {
			super();
			setSecurityManager(Objutil.notNull(webSecurityManager, "WebSecurityManager property cannot be null."));
			if (resolver != null)
				setFilterChainResolver(resolver);
		}
	}
}
