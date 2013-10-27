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
package zcu.xutil.msg.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.MembershipListener;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;

import zcu.xutil.Disposable;
import zcu.xutil.DisposeManager;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.msg.Broker;
import zcu.xutil.msg.GroupService;
import zcu.xutil.msg.MsgListener;
import zcu.xutil.msg.Notification;
import zcu.xutil.msg.Server;
import zcu.xutil.utils.ByteArray;
import zcu.xutil.utils.Util;
import zcu.xutil.utils.ProxyHandler;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
final class BrokerImpl implements Broker, BrokerAgent, Server, RequestHandler, MessageListener, Disposable {
	static final AtomicLong lastUsed = new AtomicLong();
	static final RequestOptions defalutOptions= new RequestOptions(ResponseMode.GET_ALL,30000);
	static final Logger logger = Logger.getLogger(BrokerImpl.class);


	private final CopyOnWriteArrayList<MsgListener> listeners = new CopyOnWriteArrayList<MsgListener>();
	private final Channel channel;
	private final Handler handler;
	private final Address localAddr;
	private final Membship membship;
	private final int clistamp, srvstamp;
	private volatile Map<String, ServiceObject> serviceObjects = Collections.emptyMap();
	volatile boolean blocked, destroyed;
	final EventDao eventDao;
	final MessageDispatcher dispatcher;
	final ConcurrentMap<Address, Snode> serverMap = new ConcurrentHashMap<Address, Snode>(64);

	BrokerImpl(BrokerFactory factory) {
		this.srvstamp = factory.serverStamp;
		this.clistamp = factory.clientStamp;
		this.eventDao = new EventDao(factory.clusterName, factory.datasource, this);
		this.handler = new Handler(factory.maxPoolSize, eventDao);
		try {
			(channel = new JChannel(factory.config)).setDiscardOwnMessages(true);
			if (!Objutil.isEmpty(factory.nodeName))
				channel.setName(factory.nodeName);
			this.dispatcher = new MessageDispatcher(channel, this, membship = new Membship(), this);
			channel.connect(factory.clusterName);
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
		this.localAddr = channel.getAddress();
		DisposeManager.register(this); // destory before EventDao.
		eventDao.start();
	}
	@Override
	public void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		if (!serviceObjects.isEmpty())
			try {
				Event event = new Event();
				event.syncall = true;
				Message msg = toMessage(event);
				msg.setFlag(Message.OOB);
				blockWait();
				channel.send(msg);
			} catch (Throwable e) {
				logger.warn("send stop event fail.", e);
			}
		eventDao.destroy();
		serverMap.clear();
		try {
			Thread.sleep(1000);
		} catch (Throwable e) {
			// ignore;
		}
		dispatcher.stop();
		channel.close();
		handler.shutdown();
	}
	@Override
	public void startServer(Remote... interfaceservice) {
		startServer(Collections.<String, GroupService> emptyMap(), interfaceservice);
	}
	@Override
	public synchronized void startServer(Map<String, ? extends GroupService> asyncServices, Remote... interfaceservice) {
		if (asyncServices.isEmpty() && interfaceservice.length == 0)
			return;
		Objutil.validate(!destroyed && serviceObjects.isEmpty(), "destroyed or strated.");
		serviceObjects = handler.initiate(asyncServices, interfaceservice);
		try {
			sendStartEvent(null);
		} catch (Exception e) {
			throw new XutilRuntimeException(e);
		}
	}

	void sendStartEvent(List<Address> dest) throws Exception  {
		if (serviceObjects.isEmpty())
			return;
		StringBuilder sb = new StringBuilder(512);
		for (String s : serviceObjects.keySet())
			sb.append('\t').append(s);
		Event event = new Event(Integer.toString(srvstamp), sb.substring(1), (Object[]) null);
		event.syncall = true;
		Message msg = toMessage(event);
		msg.setFlag(Message.OOB);
		blockWait();
		if (dest != null)
			for (int i = dest.size() - 1; i >= 0 && !destroyed; i--) {
				msg.setDest(dest.get(i));
				channel.send(msg);
			}
		else if (!destroyed)
			channel.send(msg);
	}

	void mustExecute(Runnable task) {
		try {
			handler.executor.execute(task);
		} catch (RejectedExecutionException e) {
			handler.newThread(task).start();
		}
	}
	@Override
	public void sendToAll(boolean includeSelf, String eventName, String eventValue, Object... params) {
		Event event = new Event(eventName, eventValue, params);
		if (includeSelf && !listeners.isEmpty())
			mustExecute(new Listen(event, localAddr, listeners));
		Message msg = toMessage(event);
		blockWait();
		try {
			channel.send(msg);
		} catch (Exception e) {
			logger.info("{} send exception", e, eventName);
		}
	}
	@Override
	public void sendToNode(Address dest, String eventName, String eventValue, Object... params) {
		Event event = new Event(eventName, eventValue, params);
		if (!dest.equals(localAddr)) {
			Message msg = toMessage(event);
			msg.setDest(dest);
			blockWait();
			try {
				channel.send(msg);
			} catch (Exception e) {
				logger.info("{} send exception", e, eventName);
			}
		} else if (!listeners.isEmpty())
			mustExecute(new Listen(event, localAddr, listeners));
	}
	@Override
	public void receive(Message msg) {
		Event event = toEvent(msg);
		Address addr = msg.getSrc();
		if (event.syncall) {
			String name = event.getName();
			if (name == null) {
				if (serverMap.remove(addr) != null)
					logger.info("======>remove lefted server: {}", addr);
			} else {
				Snode s = new Snode(addr, Integer.parseInt(name), Objutil.split(event.getValue(), '\t'));
				if (serverMap.putIfAbsent(addr, s) == null)
					logger.debug("======>add server: {}", s);
			}
		} else if (!listeners.isEmpty())
			mustExecute(new Listen(event, addr, listeners));
	}
	@Override
	public ServiceObject getSOBJ(String name) {
		return serviceObjects.get(name);
	}

	private Snode select(String name) {
		blockWait();
		Iterator<Snode> iter = serverMap.values().iterator();
		Snode s, target = null, candidate = null, susp = membship.suspected;
		while (iter.hasNext()) {
			if (!(s = iter.next()).services.containsKey(name))
				continue;
			if (s == susp)
				candidate = s;
			else if (target == null)
				target = s;
			else if (s.version == target.version) {
				if (s.used < target.used)
					target = s;
			} else if (s.version == clistamp || (s.version < target.version && target.version != clistamp))
				target = s;
		}
		if (target == null && (target = candidate) == null)
			throw new UnavailableException("SERVICE NOT FOUND. ".concat(name));
		return target;
	}
	@Override
	public Object sendToRemote(Event event, int timeoutMillis) throws Throwable {
		Message msg = toMessage(event);
		byte[] buf = select(event.getName()).dispatch(msg, timeoutMillis);
		return buf.length == 0 ? null : Event.unmarshall(ByteArray.toStream(buf));
	}
	@Override
	public byte[] proxy(Event event) {
		String name = event.getName();
		ServiceObject sobj = getSOBJ(name);
		try {
			if (sobj != null)
				return Event.marshall(sobj.handle(event));
			Message msg = toMessage(event);
			return select(name).dispatch(msg, 0);
		} catch (Throwable e) {
			Event.clearStack(e);
			return Event.marshall(e);
		}
	}
	@Override
	public Iterator<Server> iterator() {
		return Util.concat(isValid() ? Collections.singletonList(BrokerImpl.this).iterator() : null, serverMap.values()
				.iterator());
	}
	@Override
	public Object handle(Message msg) {
		Event event = toEvent(msg);
		ServiceObject sobj = serviceObjects.get(event.getName());
		if (sobj == null)
			throw new UnavailableException(event.getName());
		try {
			return Event.marshall(sobj.handle(event));
		} catch (MSGException e) {
			throw e;
		} catch (Throwable e) {
			Event.clearStack(e);
			return Event.marshall(e);
		}
	}
	@Override
	public void getState(OutputStream out) throws Exception {
		//
	}
	@Override
	public void setState(InputStream in) throws Exception {
		//
	}
	@Override
	public Collection<String> getServiceNames() {
		return Collections.unmodifiableCollection(serviceObjects.keySet());
	}
	@Override
	public boolean isValid() {
		return !serviceObjects.isEmpty();
	}
	@Override
	public Object call(String name, String value, int timeoutMillis, Object... params) throws Throwable {
		Event event = new Event(name, value, params);
		event.syncall = true;
		return Objutil.notNull(serviceObjects.get(name), "invalid service:{}", name).handle(event);
	}
	@Override
	public Address getAddress() {
		return localAddr;
	}
	@Override
	public Collection<Address> getMembers() {
		return membship.members;
	}
	@Override
	public String getPhysical(Address address) {
		if (address instanceof PhysicalAddress)
			return address.toString();
		Object o = channel.down(new org.jgroups.Event(org.jgroups.Event.GET_PHYSICAL_ADDRESS, address));
		return o == null ? null : o.toString();
	}
	@Override
	public void setNotification(Notification notify) {
		membship.notification = notify;
	}
	@Override
	public boolean addListener(MsgListener listener) {
		return listeners.addIfAbsent(listener);
	}
	@Override
	public boolean removeListener(MsgListener listener) {
		return listeners.remove(listener);
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Remote> T create(Class<T> iface, final int timeoutMillis) {
		final String cname = iface.getName();
		InvocationHandler h = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
				Object ret = ProxyHandler.proxyObjectMethod(proxy, m, args);
				if (ret != null)
					return ret;
				Event event = new Event(cname, Util.signature(m.getName(),m.getParameterTypes()), args);
				event.syncall = true;
				ServiceObject sobj = getSOBJ(event.getName());
				if (destroyed)
					throw new IllegalStateException("Broker destroyed.");
				return sobj != null ? sobj.handle(event) : sendToRemote(event, timeoutMillis);
			}
		};
		return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, h);
	}
	@Override
	public GroupService create(String serviceName, final boolean sendprefer, final int expireMinutes) {
		final String cname = serviceName.intern();
		return new GroupService() {
			@Override
			public void service(String value, Object... params) {
				Event event = new Event(cname, value, params);
				if (destroyed)
					throw new IllegalStateException("Broker destroyed.");
				if (expireMinutes > 0)
					event.setExpire(new java.util.Date(Util.now() + expireMinutes * 60000L));
				if (sendprefer)
					try {
						ServiceObject sobj = getSOBJ(event.getName());
						if (sobj != null) {
							sobj.handle(event);
							return;
						}
						if (!blocked) {
							sendToRemote(event, 0);
							return;
						}
					} catch (Throwable e) {
						logger.debug("prefer send fail. store event: {}", e, event);
					}
				eventDao.store(event, false);
			}
		};
	}

	private void blockWait() {
		if (!blocked)
			return;
		synchronized (membship) {
			while (blocked)
				try {
					membship.wait(8000);
				} catch (InterruptedException e) {
					throw new XutilRuntimeException(e);
				}
		}
	}

	private final class Membship implements MembershipListener {
		volatile Snode suspected;
		volatile List<Address> members;
		volatile Notification notification;

		Membship() {//
		}
		@Override
		public void block() {
			blocked = true;
		}
		@Override
		public synchronized void unblock() {
			blocked = false;
			notifyAll();
		}
		@Override
		public void suspect(Address addr) {
			Snode s = serverMap.get(addr);
			if (s != null)
				suspected = s;
		}
		@Override
		public void viewAccepted(View view) {
			Address[] news = view.getMembers().toArray(new Address[view.size()]);
			List<Address> olds = members;
			members = Arrays.asList(news);
			suspected = null;
			if (olds == null)
				return;
			Notify task = new Notify(notification);
			final int newlen = news.length;
			outer: for (Address addr : olds) {
				for (int i = 0; i < newlen; i++) {
					if (addr.equals(news[i]))
						continue outer;
				}
				task.add(addr);
				if (serverMap.remove(addr) != null)
					logger.info("======>remove crashed server: {}", addr);
			}
			if (task.listen == null && !isValid())
				return;
			task.lefts = task.size();
			for (int i = 0; i < newlen; i++) {
				if (!olds.contains(news[i]))
					task.add(news[i]);
			}
			if (task.lefts < task.size() || task.listen != null)
				mustExecute(task);
		}
	}

	@SuppressWarnings("serial")
	private final class Notify extends ArrayList<Address> implements Runnable {
		Notification listen;
		int lefts;

		Notify(Notification n) {
			listen = n;
		}
		@Override
		public void run() {
			int size = size();
			if (lefts < size) {
				List<Address> joinMbrs = lefts == 0 ? this : subList(lefts, size);
				try {
					sendStartEvent(joinMbrs);
				} catch (Exception e) {
					logger.info("start event send exception", e);
				}
				if (listen != null)
					listen.onViewChange(lefts == 0 ? Collections.<Address> emptyList() : subList(0, lefts), joinMbrs);
			} else if (size > 0 && listen != null)
				listen.onViewChange(this, Collections.<Address> emptyList());
		}
	}

	private final class Snode implements Server {
		final Map<String, Object> services = new HashMap<String, Object>();
		final int version;
		final Address addr;
		volatile long used;

		Snode(Address address, int ver, List<String> list) {
			this.version = ver;
			this.addr = address;
			for (int i = list.size() - 1; i >= 0; i--) {
				String canonical = list.get(i).intern();
				services.put(canonical, canonical);
			}
		}
		@Override
		public Address getAddress() {
			return addr;
		}
		@Override
		public Collection<String> getServiceNames() {
			return Collections.unmodifiableCollection(services.keySet());
		}
		@Override
		public boolean isValid() {
			return this == serverMap.get(addr);
		}
		@Override
		public Object call(String name, String value, int timeoutMillis, Object... params) throws Throwable {
			Event event = new Event(name, value, params);
			event.syncall = true;
			Objutil.validate(isValid() && services.containsKey(name), "invalid service.{}", this);
			byte[] buf = dispatch(toMessage(event), timeoutMillis);
			return buf.length == 0 ? null : Event.unmarshall(ByteArray.toStream(buf));
		}

		byte[] dispatch(Message msg, int timeout) {
			used = lastUsed.getAndIncrement();
			msg.setDest(addr);
			Object o;
			try {
				o = dispatcher.sendMessage(msg, timeout > 0 ? new RequestOptions(ResponseMode.GET_ALL,timeout) : defalutOptions);
			} catch (Exception e) {
				throw new MSGException(addr + " " + e.toString());
			}
			if (o instanceof byte[])
				return (byte[]) o;
			if (o instanceof MSGException)
				throw (MSGException) o;
			throw new MSGException("unexpected retval:" + o);
		}

		@Override
		public String toString() {
			return getAddress().toString() + getServiceNames() + version;
		}
	}

	@Override
	public String toString() {
		return getAddress().toString() + getServiceNames() + srvstamp;
	}

	private static final class Listen extends AbstractList<Object> implements Runnable, RandomAccess {
		private final Event event;
		private final Address from;
		private final List<MsgListener> listeners;

		Listen(Event e, Address src, List<MsgListener> l) {
			this.event = e;
			this.from = src;
			this.listeners = l;
		}

		@Override
		public Object get(int index) {
			return event.parameters()[index];
		}

		@Override
		public int size() {
			return event.parameters().length;
		}
		@Override
		public void run() {
			for (MsgListener item : listeners) {
				try {
					item.onEvent(from, event.getName(), event.getValue(), this);
				} catch (RuntimeException e) {
					logger.warn("MulticastListener:{} , event :{}", e, item, event);
				}
			}
		}
	}

	static Event toEvent(Message msg) {
		Event event = new Event();
		try {
			event
					.readFrom(new DataInputStream(ByteArray.toStream(msg.getRawBuffer(), msg.getOffset(), msg
							.getLength())));
		} catch (IOException e) {
			throw new IllegalMsgException(e.toString());
		}
		return event;
	}

	static Message toMessage(Event event) {
		ByteArray baos = new ByteArray(1024);
		try {
			event.writeTo(new DataOutputStream(baos));
		} catch (IOException e) {
			throw new IllegalMsgException(e.toString());
		}
		return new Message(null, null, baos.getRawBuffer(), 0, baos.size());
	}
}
