package zcu.xutil.ws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.handler.Handler;

import zcu.xutil.Disposable;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.DefaultBinder;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.cfg.RefCaller;
import zcu.xutil.utils.Function;
import zcu.xutil.ws.ServerSecurityHandler;
import zcu.xutil.ws.WSEFactory;
import zcu.xutil.ws.XutilContainer;

import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.HttpAdapterList;
import com.sun.xml.ws.transport.http.server.HttpEndpoint;
import com.sun.xml.ws.transport.http.server.ServerAdapterList;

public class ServerTest implements Config {

	@Override
	public void config(Binder b) throws Exception {
		Handler h = new ServerSecurityHandler();
		RefCaller wsef = CFG.typ(WSEFactory.class).set("handlers",Collections.singletonList(h)).set("container",b);
		wsef.set("implClass",Add.class).set("target",new Add()).ext("getObject").uni(b,"add");
		wsef.set("target",new Mutile()).ext("getObject").uni(b,"mul");
	}

	public static class TestPasswordRetriever implements Function<String, String> {
		public String apply(String username) {
			return username == null ? null : username.toUpperCase();
		}

	}

	public static void main(String[] args) {
		String className = ServerTest.class.getName();
		String address = "http://dev.xzc.com:8080/Cas/";
		if (args.length > 0) {
			if (args.length > 1)
				className = args[0];
			else if (args.length > 2)
				address = args[1];
			else {
				System.out.println(usage);
				return;
			}
		}
		if (address.charAt(address.length() - 1) != '/')
			address = address + "/";
		
		 DefaultBinder builder = new DefaultBinder("Servertest",null);
		builder.put(true, Container.class.getName(), CFG.val(new XutilContainer(null)));
		final CFG config;
		try {
			config = (CFG) Class.forName(className).newInstance();
			config.config(builder);
		} catch (Exception e) {
			System.out.println(usage);
			e.printStackTrace();
			return;
		}
		final Context context =builder.startup();
		final List<HttpEndpoint> list = new ArrayList<HttpEndpoint>();
		final ThreadPoolExecutor pool = new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());
		HttpAdapterList<?> hal = new ServerAdapterList();
		for (NProvider entry : context.getProviders(WSEndpoint.class)) {
			WSEndpoint endpoint = (WSEndpoint) entry.instance();
			HttpAdapter adapter = hal.createAdapter(entry.getName(), "/" + entry.getName() + ".ws", endpoint);
			HttpEndpoint he = new HttpEndpoint(pool, adapter);
			he.publish(address + entry.getName() + ".ws");
			list.add(he);
		}
		Disposable endpointDispose = new Disposable() {
			public void destroy() {
				try {
					for (HttpEndpoint h : list)
						h.stop();
					context.destroy();
				} finally {
					pool.shutdown();
				}
			}
		};
	}

	private static final String usage = "java XutilContainer.class configClassName [url]";
}
