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
package zcu.xutil.txm;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.Connection;
/**
*
* @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
*/
@Inherited
@Retention(RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface Transactional {
	Propagation value() default Propagation.REQUIRED;
	Class<? extends Exception>[] rollbackfor() default {};
	int timeoutSecs() default 0;
	int isolation() default Connection.TRANSACTION_NONE; //0
}
