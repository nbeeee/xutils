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
package zcu.xutil.utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import zcu.xutil.Disposable;
import zcu.xutil.Logger;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class Trigger implements Disposable, Runnable {
	private final AtomicReference<ScheduledFuture> ref;
	private final Xcron xcron;
	private final ScheduledExecutorService scheduler;
	private final Runnable task;
	private volatile long nextTime;

	/**
	 * Instantiates a new trigger.
	 *
	 * @param initialDelay
	 *            the initial delay millis
	 * @param cron
	 *            the cron expression
	 * @param schedule
	 *            the schedule
	 * @param command
	 *            the command
	 */
	public Trigger(ScheduledExecutorService schedule, Runnable command, int initialDelay, String cron) {
		this.xcron = new Xcron(cron);
		this.scheduler = schedule;
		this.task = command;
		if (initialDelay < 0)
			initialDelay = 0;
		long now = System.currentTimeMillis();
		nextTime = xcron.getNextTimeAfter(now + initialDelay);
		ref = new AtomicReference<ScheduledFuture>(scheduler.schedule(this, nextTime - now, TimeUnit.MILLISECONDS));
	}

	/**
	 * Instantiates a new trigger.
	 *
	 * @param initialDelay
	 *            the initial delay millis
	 * @param period
	 *            the a period millis, if period >0 scheduleAtFixedRate, if
	 *            period <0 scheduleWithFixedDelay,if period==0 schedule once.
	 * @param schedule
	 *            the schedule
	 * @param command
	 *            the command
	 */
	public Trigger(int initialDelay, int period, ScheduledExecutorService schedule, Runnable command) {
		this.xcron = null;
		this.scheduler = schedule;
		this.task = command;
		ScheduledFuture future;
		if (period == 0)
			future = scheduler.schedule(this, initialDelay, TimeUnit.MILLISECONDS);
		else if (period > 0)
			future = scheduler.scheduleAtFixedRate(this, initialDelay, period, TimeUnit.MILLISECONDS);
		else
			future = scheduler.scheduleWithFixedDelay(this, initialDelay, -period, TimeUnit.MILLISECONDS);
		ref = new AtomicReference<ScheduledFuture>(future);
	}
	@Override
	public void destroy() {
		ScheduledFuture future = ref.getAndSet(null);
		if (future != null)
			future.cancel(false);
	}
	@Override
	public void run() {
		try {
			task.run();
		} catch (Throwable e) {
			Logger.LOG.warn("task {} run error.", e, task.getClass());
		}
		if (xcron == null)
			return;
		long now = System.currentTimeMillis();
		nextTime = xcron.getNextTimeAfter(now > nextTime ? now : nextTime);
		if (ref.get() != null) {
			if (ref.getAndSet(scheduler.schedule(this, nextTime - now, TimeUnit.MILLISECONDS)) == null)
				destroy();
		}
	}

	public long getDelayMillis() {
		ScheduledFuture future = ref.get();
		return future == null ? 0 : future.getDelay(TimeUnit.MILLISECONDS);
	}
}
