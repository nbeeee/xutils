package zcu.xutil.cfg.jmx;

import static org.junit.Assert.*;
import static zcu.xutil.cfg.CFG.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.NProvider;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.Provider;
import zcu.xutil.jmx.JMXManager;
import zcu.xutil.jmx.MbeanResource;
import zcu.xutil.jmx.SimpleDynamic;
import static zcu.xutil.cfg.CFG.*;


public class AgentTest {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInitMbeanServer() {
		Person person = new Person();
		JMXManager manager = new JMXManager();
		try {
			ObjectName objectName = new ObjectName("default:name=test");
			manager.registerMbean(new SimpleDynamic(person), objectName );
			MBeanServer server = manager.getMBeanServer();
			String[] names = new String[] { "xiao", "zaichu" };
			Attribute nameAttribute = new Attribute("name", names);
			server.setAttribute(objectName, nameAttribute);
			Object retval = server.getAttribute(objectName, "name");
			assertEquals(names, retval);
			Attribute ageAttribute = new Attribute("age", 40);
			server.setAttribute(objectName, ageAttribute);
			retval = server.getAttribute(objectName, "age");
			assertEquals(40, retval);
			Attribute manAttribute = new Attribute("man", true);
			server.setAttribute(objectName, manAttribute);
			retval = server.getAttribute(objectName, "man");
			assertEquals(true, retval);
		} catch (AssertionError ae) {
			throw ae;
		} catch (Exception e) {
			fail(e.toString());
		} finally {
			manager.destroy();
		}
	}

	@Test
	public void testMain() {
		main(new String[] { "nowait" });
	}

	public static void main(String[] args) {
		Context ctx = CFG.build(null ,new Config() {
			public void config(Binder b) throws Exception {
				val(new Person(41, new String[] { "xiao", "zaichu" }, true)).uni(b,"first");
				val(new Person(40, new String[] { "yue", "jie" }, false)).uni(b,"");
				val(new JMXManager()).call("manage",b.ref("")).uni(b,"manager");
				val(new JMXAgent()).call("start",b.ref("manager")).uni(b,"agent");
			}
			public void onStart(Context ctx){
				ctx.getBean("agent");
			}
		});
		System.out.println("!!!!!!!!! build over!!!!!!!!!!");
		Person psen =(Person) ctx.getBean("first");
		if (args.length == 0) {
			try {
				Thread.sleep(30000);
				psen.setAge(255);
				Thread.sleep(30000);
				psen.setAge(300);
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ctx.destroy();
	}
}
