package zcu.xutil.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Test;


import static org.junit.Assert.*;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.utils.Trigger;
import zcu.xutil.utils.Xcron;
import zcu.xutil.web.Webutil;
public class XcronTest implements Runnable{
	int i;
	@Test
	public void testCommon() {
		Xcron cron= new Xcron("10 12 3 2 0");
		Calendar c=Calendar.getInstance();
		c.set(2000, 1, 28,13,10);
		Date d = new Date(cron.getNextTimeAfter(c.getTimeInMillis()));
		assertEquals(2001,d.getYear()+1900);
		assertEquals(1,d.getMonth());
		assertEquals(3,d.getDate());
		assertEquals(12,d.getHours());
		assertEquals(10,d.getMinutes());
		Logger.LOG.info("\"10 12 3 2 0\"  {}  {}  " ,cron, d);
		cron= new Xcron("10 12 3 2 5");   // friday  2001/2/2
		d = new Date(cron.getNextTimeAfter(c.getTimeInMillis()));

		assertEquals(2001,d.getYear()+1900);
		assertEquals(1,d.getMonth());
		assertEquals(2,d.getDate());
		assertEquals(12,d.getHours());
		assertEquals(10,d.getMinutes());
		Logger.LOG.info("\"10 12 3 2 5\"  {}  {}  " ,cron, d);
		Logger.LOG.info("\"10 12***\"  {} " ,new Xcron("10 12***"));
		File f =new File(Objutil.systring("xutil.home","xutils.properties"));
		Logger.LOG.info("path={} abs={} canon={}",f.getPath(),f.getAbsolutePath(),f.isAbsolute());
	}
	@Test
	public void testBase64() throws IOException {
		File f =new File(Objutil.systring("xutil.home","xutils.properties"));
		InputStream in =new FileInputStream(f);
		String encoded =Base64.encode(in,true);
		byte[] b =new BASE64Decoder().decodeBuffer(encoded);
		String expected = new String(b,"GBK");
		b=Base64.decode(new StringReader(encoded));
		assertEquals(expected,new String(b,"GBK"));

		encoded = new BASE64Encoder().encodeBuffer(b);
		b = Base64.decode(new StringReader(encoded));
		expected = new String(b,"GBK");
		b =new BASE64Decoder().decodeBuffer(encoded);
		assertEquals(expected,new String(b,"GBK"));
		in.close();
	}
	public void run() {
		Logger.LOG.info("numbers: {}",i++);
	}

	public static void main(String[] args) {
		System.err.println(String[][].class.getName());
		System.err.println(String[].class.getName());
		System.err.println(int[][].class.getName());

		//Scheduler s = new Scheduler(2);
		//ScheduledFuture f=s.schedule(new XcronTest(),"*9,11***");

		ScheduledThreadPoolExecutor s = new ScheduledThreadPoolExecutor(2);
		Trigger t= new Trigger(s,new XcronTest(),100000,"*****");
		Logger.LOG.info("begin");
		try {
			Thread.sleep(3000000);
			t.destroy();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
