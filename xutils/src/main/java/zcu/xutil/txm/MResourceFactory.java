﻿/* 
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
/**
*
* @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
*/
public interface MResourceFactory{
	MResource newResource(TxInfo txinfo) throws Exception;
	int getCommitOrder();

	interface TxInfo {
		void setRollbackOnly();

		int getStatus();

		int getTimeout();

		int getIsolation();
	}
}
