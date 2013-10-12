package zcu.xutil.web;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;

import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.cfg.CFG;
import zcu.xutil.cfg.Binder;
import zcu.xutil.cfg.Config;
import zcu.xutil.cfg.Context;
import zcu.xutil.misc.MailHelper;
import zcu.xutil.misc.MailSender;
import zcu.xutil.misc.Preparator;
import zcu.xutil.utils.UrlBuilder;
import zcu.xutil.utils.Util;

public class WebTest implements Config {

	@Test
	public void testHtmlMail() {
		ArrayList imageList = new ArrayList();
		String s = MailHelper.imageReplace("<img src=\"tomcat.jpg\"> adcfg <img src='./tomcat.jpg'>", imageList);

		assertEquals(s, "<img src=\"cid:0\"> adcfg <img src='cid:0'>");
		assertEquals(1, imageList.size());
	}

	@Test
	public void testRangePaser() {
		long[] pos = Stream.parseRange("bytes=-1", 200);
		assertEquals(pos[0], 199);
		assertEquals(pos[1], 199);
		pos = Stream.parseRange("bytes=100-", 200);
		assertEquals(pos[0], 100);
		assertEquals(pos[1], 199);
		pos = Stream.parseRange("bytes=100-120", 200);
		assertEquals(pos[0], 100);
		assertEquals(pos[1], 120);
		pos = Stream.parseRange("bytes=100-500", 200);
		assertEquals(pos[0], 100);
		assertEquals(pos[1], 199);
		pos = Stream.parseRange("bytes=400-500", 200);
		assertTrue(pos != null);
		Logger.LOG.info("bytes=400-500   {}-{}", pos[0], pos[1]);
		pos = Stream.parseRange("bytes=-600", 200);
		assertTrue(pos != null);
		Logger.LOG.info("bytes=-600   {}-{}", pos[0], pos[1]);
		pos = Stream.parseRange("bytes=250-", 200);
		assertTrue(pos != null);
		Logger.LOG.info("bytes=250-   {}-{}", pos[0], pos[1]);
		pos = Stream.parseRange("bytes=-0", 200);
		assertTrue(pos != null);
		Logger.LOG.info("bytes=-0   {}-{}", pos[0], pos[1]);
		pos = Stream.parseRange("bytes=120-100", 200);
		assertTrue(pos == null);
		pos = Stream.parseRange("bytes=-120-100", 200);
		assertTrue(pos == null);
		String str;

		str = UrlBuilder.encode(
				"1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ~!@#$%^&*()_-+=|\\}]{[':;?/>.<,", "GBK");
		Logger.LOG.info(str);
		assertEquals(
				str,
				"1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ%7E%21%40%23%24%25%5E%26*%28%29_-%2B%3D%7C%5C%7D%5D%7B%5B%27%3A%3B%3F%2F%3E.%3C%2C");
	}

	@Override
	public void config(Binder b) throws Exception {
		WebTest.class.desiredAssertionStatus();
		assert b != null : "aaa";
		CFG.val(new MailSender()).set("properties",
						Objutil.loadProps(getClass().getResource("/email.properties"), new Properties())).uni(b,"mailsession");
	}

	public static void main(String[] args) throws MessagingException, UnsupportedEncodingException {
		Context ctx = CFG.build(null,new WebTest());
		MailSender et = (MailSender) ctx.getBean("mailsession");
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("hello", ctx);
		String text = "<html> test <img src=\"E:/apache-tomcat/webapps/docs/images/tomcat.jpg\"></html>";
		et.send(text, "test邮件", "zaichu.xiao@yeepay.com", null, "肖<zaichu.xiao@yeepay.com>", new File[] { new File(
				"loggerService.xml") });
		Logger.LOG.info("====={}", variables);
		// MailSender s = (MailSender)ctx.getBean("mailsession");
		// Preparator p =new Preparator(){
		// public void prepare(MimeMessage mimeMessage) throws
		// MessagingException {
		// MailHelper h = new MailHelper(mimeMessage,true);
		// h.setFrom("zxiao@yeepay.com", "肖再初");
		// h.setSubject("测试邮件");
		// h.addTo("zaichu.xiao@yeepay.com");
		// h.setText("<html><head><title>测试</title></head><body>测试<br><img src='cid:img'></body></html>",
		// true);
		// h.addInline("img", new
		// File(Objutil.systring("user.home",".")+"/tomcat.gif"));
		// h.addAttachment("团体gongzhu报告.txt", new
		// File(Objutil.systring("user.home",".")+"/jshrink.ini"));
		// }
		//
		// };
		// s.send(p);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
