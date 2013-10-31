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
package zcu.xutil.msg;

/**
 * Message Broker. 高可用的消息服务.同时支持集群服务模式和节点服务模式.<br>
 * 集群服务模式: 通过 startServer 方法 部署服务,客户端通过 create方法
 * 创建调用代理.支持同步和异步调用.异步调用可先保存在本地,保证可靠传送.<br>
 * 节点服务模式: 通过{@link #addListener(MsgListener)}接受消息.使用
 * {@link #sendToAll(boolean, String, String, Object...)}
 * {@link #sendToNode(String, String, String, Object...)}异步发送消息. 消息不保存.<br>
 *
 * 客户端使用例子:
 *
 * <pre>
 *
 * @GroupService
 * public interface RemoteService{
 * 	String method(String str,int i);
 * 	void   call(Date date,String str);
 * }
 * 	...
 * 	RemoteService remoteService=BrokerFactory.instance().create(RemoteService.class);
 * 	// use remoteService
 * </pre>
 *
 * 服务端使用例子(服务可部署在多台服务器,每台服务器可部署多个服务):
 *
 * <pre>
 *  public class RemoteServiceImpl implements RemoteService {
 * 	private static final Logger logger = Logger.getLogger();
 * 	public String method(String str,int i){
 * 		logger.info(&quot;server hello, num: {}&quot;,i);
 * 		return &quot;from server:&quot;+i;
 * 	}
 * 	public void call(Date date, String str){
 * 		logger.info(&quot;server asyncall param date: {}&quot;, date);
 * 	}
 * }
 * 	...
 *  
 * 	BrokerFactory.instance().startServer(new RemoteServiceImpl());
 *  ...
 * 	RemoteService remoteService=BrokerFactory.instance().create(RemoteService.class);
 * 	// use remoteService
 * 	...
 * </pre>
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public interface Broker extends SimpleBroker {
	/**
	 * 部署服务.
	 *
	 * @param services
	 *            接口服务实现
	 *
	 */
	void startServer(Object... services);


	/**
	 * 广播事件. 接收者为 {@link MsgListener} 消息不保存.
	 *
	 * @param includeSelf
	 *            是否包括发给自身, ture 发给自身, false 不发给自身
	 * @param eventName
	 *            the event name
	 * @param eventValue
	 *            the event value
	 * @param params
	 *            the params
	 */

	void sendToAll(boolean includeSelf, String eventName, String eventValue, Object... params);

	/**
	 * 发送事件. 接收者为 {@link MsgListener} 消息不保存.
	 *
	 * @param nodeName
	 *            目的节点名.
	 * @param eventName
	 *            the event name
	 * @param eventValue
	 *            the event value
	 * @param params
	 *            the params
	 */

	void sendToNode(String nodeName, String eventName, String eventValue, Object... params);

	/**
	 * 增加事件收听器.
	 *
	 * @param listener
	 *            {@link MsgListener}
	 *
	 */
	boolean addListener(MsgListener listener);

	/**
	 * 删除事件收听器.
	 *
	 * @param listener
	 *            {@link MsgListener}
	 *
	 */
	boolean removeListener(MsgListener listener);

	/**
	 * 设置节点监视通知,当节点机加入或离开时调用
	 *
	 * @param notify
	 *            {@link Notification}
	 *
	 */
	void setNotification(Notification notify);

}
