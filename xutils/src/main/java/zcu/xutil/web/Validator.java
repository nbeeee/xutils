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
package zcu.xutil.web;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface Validator {
	/**
	 * mapping request parameter name.
	 */
	String name() default "";
	/**
	 * Regular expression
	 */
	String value() default "";

	/**
	 * invalid parameter message.
	 */
	String message() default "invalid_value";

	boolean required() default false;
}
