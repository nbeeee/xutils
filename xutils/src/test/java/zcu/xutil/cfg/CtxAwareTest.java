package zcu.xutil.cfg;

import static org.junit.Assert.*;
import static zcu.xutil.cfg.CFG.*;
import static zcu.xutil.utils.Matcher.subOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Context;
import zcu.xutil.cfg.Binder;
import zcu.xutil.txm.TxInterceptor;
import zcu.xutil.txm.TxManager;
import zcu.xutil.utils.Disp;


public class CtxAwareTest{
	private static final Logger logger = Logger.getLogger(CtxAwareTest.class);

	static String value;

	public static void setValue(String aval) {
		value = aval;
	}

	public static String getValue() {
		return value;
	}

	String field;
	CtxAwareTest test;
	List<CtxAwareTest> list;
	Set<CtxAwareTest> set;
	String beanName;
	Context context;
	Provider provider;

	public CtxAwareTest() {

	}
	public CtxAwareTest(String... strings) {
		logger.debug("string[]{}: {} ",strings);
	}

	public CtxAwareTest(CtxAwareTest v) {
		test = v;
	}

	public CtxAwareTest getTest() {
		return test;
	}

	public void setFactory(Provider factory) {
		this.provider = factory;
	}

	public void setTest(CtxAwareTest test) {
		this.test = test;
	}

	public String getField() {
		return field;
	}

	public void setContext(final Context context) {
		this.context = context;
		Context empty =  CFG.build(context ,new Config(){
			public void config(Binder b) throws Exception {
				List<NProvider> list=context.getProviders(Object.class);
				logger.info("============outer list:{}",list);
				String n="";
				for(NProvider np : list){
					if(!np.getType().equals(CtxAwareTest.class))
						context.getBean(np.getName());
					else n=np.getName();
				}
			}});
		logger.info("=========inner context sun map :{}", empty.getProviders(Object.class));
	}

	public void setBeanName(String name) {
		if(beanName!=null)
			throw new RuntimeException("=============beanName seted.");
		this.beanName = name;

	}

	public void setField(String field) {
		this.field = field;
	}

	public void setList(List<CtxAwareTest> list) {
		this.list = list;
	}

	public void setSet(Set<CtxAwareTest> set) {
		this.set = set;
	}



	@Override
	public String toString() {
		return beanName + " ctx: " + context;
	}
	public static class TestFactory implements Provider {

		public Class getType() {
			return String.class;
		}

		public Object instance() {
			// TODO Auto-generated method stub
			return "factory instance";
		}

		public Object getObject() {
			// TODO Auto-generated method stub
			return "factory getObject";
		}

	}
}
