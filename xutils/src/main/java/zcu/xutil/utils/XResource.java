package zcu.xutil.utils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

public final class XResource extends ResourceBundle {
	@Override
	public Enumeration<String> getKeys() {
		return Collections.enumeration(Collections.<String> emptyList());
	}

	@Override
	protected Object handleGetObject(String key) {
		return key;
	}
}
