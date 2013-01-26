/*
 * Copyright (C) 2013 Felix Bechstein
 * 
 * This file is part of Portal Timer.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.portaltimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

public class Timer {

	private static final long COOLDOWN = 5 * 60 * 1000;

	private static final String FORMAT = "m:ss";

	private static final String PREF_TARGET = "target_";

	public static final String TIMER0 = "timer0";
	public static final String TIMER1 = "timer1";
	public static final String TIMER2 = "timer2";

	public static final String[] TIMER_ALL = new String[] { TIMER0, TIMER1,
			TIMER2 };

	private final SharedPreferences mPrefs;
	private final String mKey;
	private long mTarget;

	public Timer(final Context context, final String key) {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mKey = key;
		refresh();
	}

	public long getTarget() {
		return mTarget;
	}

	public CharSequence getFormated() {
		if (mTarget == 0) {
			return DateFormat.format(FORMAT, COOLDOWN);
		} else {
			long t = mTarget - SystemClock.elapsedRealtime();
			if (t < 0) {
				t = 0;
			}
			return DateFormat.format(FORMAT, t);
		}
	}

	public void start(final Context context) {
		mTarget = SystemClock.elapsedRealtime() + COOLDOWN;
		persist();
		UpdateReceiver.trigger(context);
	}

	public void resetFromReceiver(final Context context) {
		mTarget = 0;
		persist();
	}

	public void reset(final Context context) {
		resetFromReceiver(context);
		UpdateReceiver.trigger(context);
	}

	public void refresh() {
		mTarget = mPrefs.getLong(PREF_TARGET + mKey, 0);
		if (mTarget < SystemClock.elapsedRealtime() - COOLDOWN) {
			mTarget = 0;
		}
	}

	private void persist() {
		mPrefs.edit().putLong(PREF_TARGET + mKey, mTarget).commit();
	}
}
