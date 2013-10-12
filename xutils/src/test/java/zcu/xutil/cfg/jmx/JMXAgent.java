package zcu.xutil.cfg.jmx;

import java.io.IOException;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import zcu.xutil.Logger;
import zcu.xutil.cfg.Context;
import zcu.xutil.jmx.JMXManager;
import zcu.xutil.utils.AbstractDispose;
import zcu.xutil.utils.Checker;


//import com.sun.jdmk.comm.HtmlAdaptorServer;
public class JMXAgent extends AbstractDispose{
	private static final String defaultURL = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
	private final String serviceUrl;
	private JMXConnectorServer jmxConnectorServer;

	public JMXAgent() {
		this(defaultURL);
	}

	public JMXAgent(String _serviceUrl) {
		this.serviceUrl = _serviceUrl;
	}
	public void start(JMXManager jmxanager) {
		try {
			JMXServiceURL url = new JMXServiceURL(serviceUrl);
			jmxConnectorServer= JMXConnectorServerFactory.newJMXConnectorServer(url, null, jmxanager.getMBeanServer());
			jmxConnectorServer.start();
			Logger.LOG.info("JMXAgent connectorServer started url={}", url);
		} catch (IOException e) {
			Logger.LOG.warn("JMXAgent start fail: {}", e.toString());
			destroy();
		}
	}

	@Override
	protected void doDestroy() {
		try {
			if(jmxConnectorServer!=null)
				jmxConnectorServer.stop();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			jmxConnectorServer=null;
		}
		Logger.LOG.info("JMXAgent destroyed.");
	}
}
