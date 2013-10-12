package zcu.xutil.utils;

import static org.junit.Assert.*;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import sun.misc.Signal;
import zcu.xutil.DisposeManager;
import zcu.xutil.PathBuilder;
import zcu.xutil.Logger;
import zcu.xutil.utils.AbstractDispose;

import java.io.File;
import java.io.IOException;

public class DisposeTest extends AbstractDispose {
	private final static Logger logger = Logger.getLogger(DisposeTest.class);

	@Test
	public void testDisposable() {
		logger.info("begin testDisposable");
		for(int j=20;j>0;j--){
			System.gc();
			Thread.yield();
		}
		int i = Disp.disposeSize();
		logger.info("first: " + i);

		DisposeTest jt = new DisposeTest();
		jt = new DisposeTest();
		jt = new DisposeTest();
		assertTrue(!jt.isDestroyed());
		jt = null;
		System.gc();
		System.gc();
		Thread.yield();
		logger.info(String.valueOf(Disp.disposeSize()));
		System.gc();
		Thread.yield();
		System.gc();
		Thread.yield();
		System.gc();
		System.gc();
		System.gc();
		Thread.yield();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		logger.info(String.valueOf(Disp.disposeSize()));
		Disp disp = new Disp();
		assertEquals(i + 1, Disp.disposeSize());
		boolean dst = disp.isDestroyed();
		assertTrue(!dst);
		disp = null;
		for(int j=20;j>0;j--)
			System.gc();
		assertEquals(i, Disp.disposeSize());
		logger.info("end testDisposable");
	}

	@Test
	public void testDisposableDestory() {
		int i = Disp.disposeSize();
		System.out.println("scoend");
		Disp disp = new Disp();
		assertEquals(i + 1, Disp.disposeSize());
		assertTrue(!disp.isDestroyed());
		disp.destroy();
		assertTrue(disp.isDestroyed());
		for(int j=20;j>0;j--)
			System.gc();
		assertEquals(i, Disp.disposeSize());
		System.out.println("end testDisposableDestory");
	}

	@Test
	public void testShutDispose() {
		System.out.println("begin testShutDispose");
		new Disp();
		new Disp();
		new Disp();
		new Disp();
		System.out.println("weakRef size=" + Disp.disposeSize());
	}

	@Test
	public void testNow() {
		final Random random = new Random();
		Util.getScheduler().scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					Thread.sleep(random.nextInt(30) + 1);
				} catch (InterruptedException e) {
					//
				}
			}
		}, 100, 60, TimeUnit.MILLISECONDS);
		final int length = 10000;
		long[] millis = new long[length];
		try {
			Thread.sleep(200);
			for (int i = 0; i < length; i++) {
				millis[i] = Util.now();
				if (i % 10 == 0)
					Thread.sleep(random.nextInt(10) + 1);
			}
		} catch (InterruptedException e) {
			//
		}
		long[] copy = millis.clone();
		Arrays.sort(copy);
		assertArrayEquals(millis,copy);
	}

	@Override
	protected void doDestroy() {
		System.out.println("-----DisposeTest doDestroy----");
	}
}
