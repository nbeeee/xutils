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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import zcu.xutil.Objutil;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class Xcron {
	private final static byte minDayOfWeek = 0, maxDayOfWeek = 6, minMonth = 1, maxMonth = 12;
	private final byte[] minutes, hours, daysOfMonth, months, daysOfWeek;
	private transient long alarmTime;

	/**
	 * <p>
	 * Creates a new Xcron. Extend cron format - valid chars: digit space ','
	 * '*'
	 * </p>
	 * space split fields, fields order: minutes hours daysOfMonth months
	 * daysOfWeek <br>
	 * '*' allow all values for that field. field values split by comma ',' <br>
	 * <p>
	 * e.g. "30 13,14**1" schedule an for 1:30pm 2:30pm every Monday.
	 * </p>
	 * minutes allowed values 0-59 <br>
	 * hours allowed values 0-23 <br>
	 * daysOfMonth allowed values 1-31 (value greater than actualMaximum, act as
	 * actualMaximum)<br>
	 * months allowed values 1-12 (1 = January, 2 = February, ...)<br>
	 * daysOfWeek allowed values 0-6 (0 = Sunday, 1 = Monday, ...)<br>
	 *
	 */
	public Xcron(String cron) {
		List<String> fields = new ArrayList<String>();
		if (cron != null) {
			int pos = 0, len = cron.length();
			for (int i = 0; i < len; i++) {
				char c = cron.charAt(i);
				if (c == ' ' || c == '*') {
					if (i > pos)
						fields.add(cron.substring(pos, i));
					if (c == '*')
						fields.add("");
					pos = i + 1;
				}
			}
			if (pos < len)
				fields.add(cron.substring(pos));
		}
		Objutil.validate(fields.size() < 6, "invalid cron expressin: {}", fields);
		String[] strs = fields.toArray(new String[5]);
		this.minutes = canonical(0, 59, strs[0]);
		this.hours = canonical(0, 23, strs[1]);
		this.daysOfMonth = canonical(1, 31, strs[2]);
		this.months = canonical(minMonth, maxMonth, strs[3]);
		this.daysOfWeek = canonical(minDayOfWeek, maxDayOfWeek, strs[4]);
	}

	public long getNextTimeAfter(long currentTimeMillis) {
		if (currentTimeMillis < alarmTime)
			return alarmTime;
		Calendar alarm = new GregorianCalendar();
		alarm.setTimeInMillis(currentTimeMillis);
		alarm.set(Calendar.SECOND, 0);
		int current = alarm.get(Calendar.MINUTE);
		int offset = minutes.length == 0 ? 1 : 0;
		for (int i = 0; offset <= 0 && i < minutes.length; i++)
			offset = minutes[i] - current;
		alarm.add(Calendar.MINUTE, offset <= 0 ? (60 - current + minutes[0]) : offset);
		current = alarm.get(Calendar.HOUR_OF_DAY); // (updated by minute shift)
		if ((offset = getOffsetToNext(current, 0, 23, hours)) > 0) {
			alarm.add(Calendar.HOUR_OF_DAY, offset);
			alarm.set(Calendar.MINUTE, minutes.length == 0 ? 0 : minutes[0]);
		}
		if (daysOfMonth.length > 0) {
			if (daysOfWeek.length > 0) {
				Calendar dayOfWeekAlarm = (Calendar) alarm.clone();
				updateDayOfWeek(dayOfWeekAlarm);
				updateDayOfMonth(alarm);
				if (alarm.getTimeInMillis() > dayOfWeekAlarm.getTimeInMillis())
					alarm = dayOfWeekAlarm;
			} else
				updateDayOfMonth(alarm);
		} else if (daysOfWeek.length > 0)
			updateDayOfWeek(alarm);
		alarmTime = alarm.getTimeInMillis();
		Objutil.validate(alarmTime > currentTimeMillis, "never occur. debug.");
		return alarmTime;
	}

	/**
	 * daysInMonth can't use simple offsets like the other fields, because the
	 * number of days varies per month (think of an alarm that executes on every
	 * 31st). Instead we advance month and dayInMonth together until we're on a
	 * matching value pair.
	 */
	private void updateDayOfMonth(Calendar alarm) {
		int offset;
		while ((offset = getOffsetToNext(alarm.get(Calendar.MONTH) + minMonth, minMonth, maxMonth, months)) == 0) {
			byte maxDayOfMonth = (byte) alarm.getActualMaximum(Calendar.DAY_OF_MONTH);
			byte[] days = new byte[daysOfMonth.length];
			for (int i = 0, len = days.length; i < len; i++) {
				byte day = daysOfMonth[i];
				days[i] = day > maxDayOfMonth ? maxDayOfMonth : day;
			}
			offset = getOffsetToNext(alarm.get(Calendar.DAY_OF_MONTH), 1, maxDayOfMonth, days);
			if (offset > 0) {
				alarm.add(Calendar.DAY_OF_MONTH, offset);
				alarm.set(Calendar.HOUR_OF_DAY, hours.length == 0 ? 0 : hours[0]);
				alarm.set(Calendar.MINUTE, minutes.length == 0 ? 0 : minutes[0]);
			} else
				return;
		}
		alarm.add(Calendar.MONTH, offset);
		int f = daysOfMonth.length == 0 ? 0 : Math.min(daysOfMonth[0], alarm.getActualMaximum(Calendar.DAY_OF_MONTH));
		alarm.set(Calendar.DAY_OF_MONTH, f);
		alarm.set(Calendar.HOUR_OF_DAY, hours.length == 0 ? 0 : hours[0]);
		alarm.set(Calendar.MINUTE, minutes.length == 0 ? 0 : minutes[0]);
		return;
	}

	private void updateDayOfWeek(Calendar alarm) {
		while (true) {
			int offset = getOffsetToNext(alarm.get(Calendar.MONTH) + minMonth, minMonth, maxMonth, months);
			if (offset > 0) {
				alarm.add(Calendar.MONTH, offset);
				alarm.set(Calendar.DAY_OF_MONTH, 1);
				alarm.set(Calendar.HOUR_OF_DAY, hours.length == 0 ? 0 : hours[0]);
				alarm.set(Calendar.MINUTE, minutes.length == 0 ? 0 : minutes[0]);
			}
			offset = getOffsetToNext(alarm.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + minDayOfWeek, minDayOfWeek,
					maxDayOfWeek, daysOfWeek);
			if (offset > 0) {
				alarm.add(Calendar.DAY_OF_YEAR, offset);
				alarm.set(Calendar.HOUR_OF_DAY, hours.length == 0 ? 0 : hours[0]);
				alarm.set(Calendar.MINUTE, minutes.length == 0 ? 0 : minutes[0]);
			} else
				return;
		}
	}

	/**
	 * if empty values or current is in values offset is 0. if current < maximum
	 * of values offset is diff to next valid value if current > maximum of
	 * values offset is diff to values[0], wrapping from max to min
	 */
	private static int getOffsetToNext(int current, int min, int max, byte[] values) {
		if (values.length == 0)
			return 0;
		for (int v : values) {
			if (v == current)
				return 0;
			if (v > current)
				return v - current;
		}
		return max - current + 1 + values[0] - min;
	}

	private static byte[] canonical(int min, int max, String values) {
		List<String> list = Objutil.split(values, ',');
		int len = list.size();
		byte[] ret = new byte[len];
		while (--len >= 0) {
			byte v = Byte.parseByte(list.get(len).trim());
			Objutil.validate(v >= min && v <= max, "out of range value: {}", v);
			ret[len] = v;
		}
		Arrays.sort(ret);
		return ret;
	}

	@Override
	public String toString() {
		return new StringBuilder(64).append("Xcron: minute=").append(Arrays.toString(minutes)).append(" hour=").append(
				Arrays.toString(hours)).append(" dayOfMonth=").append(Arrays.toString(daysOfMonth)).append(" month=")
				.append(Arrays.toString(months)).append(" dayOfWeek=").append(Arrays.toString(daysOfWeek)).toString();
	}
}
