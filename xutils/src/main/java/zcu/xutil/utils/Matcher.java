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
package zcu.xutil.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class Matcher<T> implements Checker<T> {
	public final static Matcher<Object> ANY = new Matcher<Object>();
	@Override
	public boolean checks(T o) {
		return true;
	}

	public final Matcher<T> and(final Checker<? super T> other) {
		return new Matcher<T>() {
			@Override
			public boolean checks(T t) {
				return Matcher.this.checks(t) && other.checks(t);
			}
		};
	}

	public final Matcher<T> or(final Checker<? super T> other) {
		return new Matcher<T>() {
			@Override
			public boolean checks(T t) {
				return Matcher.this.checks(t) || other.checks(t);
			}
		};
	}

	public final Matcher<T> not() {
		return new Matcher<T>() {
			@Override
			public boolean checks(T t) {
				return !Matcher.this.checks(t);
			}
		};
	}

	public static Matcher<AnnotatedElement> annoWith(final Class<? extends Annotation> annotationType) {
		return new Matcher<AnnotatedElement>() {
			@Override
			public boolean checks(AnnotatedElement element) {
				return element != null && element.isAnnotationPresent(annotationType);
			}
		};
	}

	public static Matcher<Class> annoInherit(final Class<? extends Annotation> annotationType) {
		return new Matcher<Class>() {
			@Override
			public boolean checks(Class clazz) {
				return getAnnotation(clazz, annotationType) != null;
			}
		};
	}

	public static Matcher<Class> subOf(final Class superclass) {
		return new Matcher<Class>() {
			@Override
			@SuppressWarnings("unchecked")
			public boolean checks(Class subclass) {
				return subclass != null && superclass.isAssignableFrom(subclass);
			}
		};
	}

	public static Matcher<Object> only(final Object o) {
		return new Matcher<Object>() {
			@Override
			public boolean checks(Object other) {
				return o.equals(other);
			}
		};
	}

	public static Matcher<Class> inPackage(final Package p) {
		return new Matcher<Class>() {
			@Override
			public boolean checks(Class c) {
				return c != null && c.getPackage().equals(p);
			}
		};
	}

	public static Matcher<Method> returns(final Checker<? super Class> returnType) {
		return new Matcher<Method>() {
			@Override
			public boolean checks(Method m) {
				return m != null && returnType.checks(m.getReturnType());
			}
		};
	}

	public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType) {
		if (clazz == null || clazz == Object.class)
			return null;
		T t = clazz.getAnnotation(annotationType);
		if (t != null)
			return t;
		for (Class<?> iface : clazz.getInterfaces()) {
			if ((t = getAnnotation(iface, annotationType)) != null)
				return t;
		}
		return getAnnotation(clazz.getSuperclass(), annotationType);
	}
}
